package com.okebari.artbite.auth.service;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

import com.okebari.artbite.auth.dto.LoginRequestDto;
import com.okebari.artbite.auth.dto.SignupRequestDto;
import com.okebari.artbite.auth.dto.TokenDto;
import com.okebari.artbite.auth.jwt.JwtProvider;
import com.okebari.artbite.common.exception.EmailAlreadyExistsException;
import com.okebari.artbite.common.exception.InvalidTokenException;
import com.okebari.artbite.common.exception.TokenExpiredException;
import com.okebari.artbite.common.exception.UserNotFoundException;
import com.okebari.artbite.common.service.MdcLogging;
import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.domain.user.UserRepository;
import com.okebari.artbite.domain.user.UserSocialLoginRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);
	private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
	private final UserRepository userRepository;
	private final SocialAuthService socialAuthService; // Injected SocialAuthService
	private final PasswordEncoder passwordEncoder;
	@Lazy
	private final AuthenticationManager authenticationManager;
	private final JwtProvider jwtProvider;
	private final RefreshTokenService refreshTokenService; // Injected
	private final RedisTemplate<String, String> redisTemplate; // Inject RedisTemplate

	@Transactional
	public Long signup(SignupRequestDto signupRequestDto) {
		try (var ignored = MdcLogging.withContext("email", signupRequestDto.getEmail())) {
			log.info("회원가입 시도: email={}", signupRequestDto.getEmail());
			if (userRepository.findByEmail(signupRequestDto.getEmail()).isPresent()) {
				log.warn("회원가입 실패: 이미 존재하는 이메일입니다. email={}", signupRequestDto.getEmail());
				throw new EmailAlreadyExistsException();
			}

			User user = signupRequestDto.toEntity(passwordEncoder);
			User savedUser = userRepository.save(user);
			try (var ignored2 = MdcLogging.withContext("signupId", savedUser.getId().toString())) {
				log.info("회원가입 성공: email={}, signupId={}", signupRequestDto.getEmail(), savedUser.getId());
				return savedUser.getId();
			}
		}
	}

	@Transactional
	public TokenDto login(LoginRequestDto loginRequestDto, HttpServletResponse response) {
		try (var ignored = MdcLogging.withContext("email", loginRequestDto.getEmail())) {
			log.info("로그인 시도: email={}", loginRequestDto.getEmail());
			Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequestDto.getEmail(), loginRequestDto.getPassword())
			);

			// Load the User entity to pass to RefreshTokenService
			User user = userRepository.findByEmail(authentication.getName())
				.orElseThrow(UserNotFoundException::new);

			String accessToken = jwtProvider.createToken(authentication);
			String refreshToken = refreshTokenService.createRefreshToken(user, user.getTokenVersion());

			// Refresh Token을 HTTP-only 쿠키로 설정
			addRefreshTokenCookie(response, refreshToken);

			log.info("로그인 성공: email={}", loginRequestDto.getEmail());
			return TokenDto.builder()
				.accessToken(accessToken)
				.build();
		}
	}

	@Transactional
	public void revokeAllUserTokens(String email) {
		User user = userRepository.findByEmail(email)
			.orElseThrow(() -> new RuntimeException("User not found: " + email));
		user.incrementTokenVersion();
		userRepository.save(user); // 변경된 tokenVersion 저장
		log.info("사용자 {}의 모든 토큰이 무효화되었습니다. 새로운 토큰 버전: {}", email, user.getTokenVersion());
	}

	@Transactional
	public TokenDto reissueAccessToken(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = getRefreshTokenFromCookie(request);
		if (!StringUtils.hasText(refreshToken)) {
			throw new InvalidTokenException("Refresh Token이 쿠키에 없습니다.");
		}

		// 1. Redis에서 Refresh Token 존재 여부 및 사용자 ID, tokenVersion 확인
		// 이 단계에서 Refresh Token의 존재 여부와 Redis에 의한 만료 여부가 함께 검증됩니다.
		String userIdAndTokenVersion = refreshTokenService.findUserIdAndTokenVersionByRefreshToken(refreshToken)
			.orElseThrow(() -> {
				// Redis에 없는 경우, 만료되었거나 유효하지 않은 것으로 간주하고 쿠키 삭제
				deleteRefreshTokenCookie(response);
				return new InvalidTokenException("유효하지 않거나 만료된 Refresh Token입니다.");
			});

		String[] parts = userIdAndTokenVersion.split(":");
		if (parts.length != 2) {
			// Redis에 있지만 형식이 잘못된 경우 (매우 드물지만 방어적 코딩)
			refreshTokenService.deleteRefreshToken(refreshToken); // 손상된 토큰 삭제
			deleteRefreshTokenCookie(response);
			throw new InvalidTokenException("손상된 Refresh Token 형식입니다.");
		}
		Long userId = Long.parseLong(parts[0]);
		int refreshTokenVersion = Integer.parseInt(parts[1]);

		// 2. 사용자 정보 로드
		User user = userRepository.findById(userId)
			.orElseThrow(() -> {
				// 사용자를 찾을 수 없는 경우 (예: 사용자 삭제), 토큰도 삭제
				refreshTokenService.deleteRefreshToken(refreshToken);
				deleteRefreshTokenCookie(response);
				return new InvalidTokenException("사용자를 찾을 수 없습니다.");
			});

		// 3. Refresh Token Rotation: 기존 토큰은 삭제하고 새로운 토큰을 발급한다.
		// 이전에 Redis에서 토큰을 찾지 못해 예외를 던졌으므로, 여기서는 Redis에 토큰이 존재한다고 가정.
		// 하지만 방어적으로 다시 한번 삭제 로직을 포함하는 것이 안전.
		refreshTokenService.deleteRefreshToken(refreshToken); // 기존 Refresh Token 삭제

		// 4. Refresh Token의 tokenVersion과 User의 현재 tokenVersion 일치 여부 확인
		if (user.getTokenVersion() != refreshTokenVersion) {
			log.warn("Refresh Token 버전 불일치: userEmail={}, userTokenVersion={}, refreshTokenVersion={}",
				user.getEmail(), user.getTokenVersion(), refreshTokenVersion);
			// 버전 불일치 시, 해당 사용자의 모든 토큰 무효화 (보안 강화)
			revokeAllUserTokens(user.getEmail()); // 해당 사용자의 모든 토큰 무효화
			deleteRefreshTokenCookie(response); // 클라이언트 쿠키 삭제
			throw new InvalidTokenException("토큰 버전이 일치하지 않아 Refresh Token이 무효화되었습니다. 재로그인하십시오.");
		}

		// 5. 새로운 Access Token 생성
		Collection<? extends GrantedAuthority> authorities = List.of(
			new SimpleGrantedAuthority(user.getRole().getKey()));
		Authentication authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);
		String newAccessToken = jwtProvider.createToken(authentication);

		// 6. 새로운 Refresh Token 발급
		String newRefreshToken = refreshTokenService.createRefreshToken(user, user.getTokenVersion());

		// 새로운 Refresh Token을 HTTP-only 쿠키로 설정
		addRefreshTokenCookie(response, newRefreshToken);

		return TokenDto.builder()
			.accessToken(newAccessToken)
			.build();
	}

	public String logout(String accessToken, String userEmail, HttpServletRequest request, HttpServletResponse response) {
		try (var ignored = (userEmail != null) ? MdcLogging.withContext("email", userEmail) : null) {
			log.info("로그아웃 시도: email={}", userEmail != null ? userEmail : "unknown");

			// 1. Access Token 블랙리스트에 추가
			// Access Token의 남은 유효 시간
			Long expiration;
			try {
				expiration = jwtProvider.getExpiration(accessToken);
			} catch (TokenExpiredException | InvalidTokenException e) {
				log.warn("로그아웃 시도 중 유효하지 않거나 만료된 Access Token: {}", e.getMessage());
				throw new InvalidTokenException("유효하지 않거나 만료된 Access Token입니다.");
			}

			if (expiration > 0) {
				redisTemplate.opsForValue().set(accessToken, "logout", expiration, TimeUnit.MILLISECONDS);
				log.debug("Access Token 블랙리스트에 추가됨: email={}, expiration={}ms",
					userEmail != null ? userEmail : "unknown", expiration);
			}

			// 2. Refresh Token 삭제 (쿠키에서 가져와서 삭제)
			String refreshToken = getRefreshTokenFromCookie(request);
			if (StringUtils.hasText(refreshToken)) {
				refreshTokenService.deleteRefreshToken(refreshToken);
				log.info("Refresh Token 삭제됨: email={}", userEmail != null ? userEmail : "unknown");
			}

			// 3. Refresh Token 쿠키 삭제
			deleteRefreshTokenCookie(response);

			// Delegate social logout URL construction to SocialAuthService
			return socialAuthService.getSocialLogoutRedirectUrl(userEmail);
		}
	}

	public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
		Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
		cookie.setHttpOnly(true);
		cookie.setSecure(true); // HTTPS 사용 시
		cookie.setPath("/"); // 모든 경로에서 접근 가능
		cookie.setMaxAge((int)(jwtProvider.getRefreshTokenExpireTime() / 1000)); // 초 단위
		response.addCookie(cookie);
	}

	public void addAccessTokenCookie(HttpServletResponse response, String accessToken) {
		Cookie cookie = new Cookie("accessToken", accessToken); // Access Token 쿠키 이름은 "accessToken"으로 가정
		cookie.setHttpOnly(true);
		cookie.setSecure(true); // HTTPS 사용 시
		cookie.setPath("/"); // 모든 경로에서 접근 가능
		cookie.setMaxAge((int)(jwtProvider.getAccessTokenExpireTime() / 1000)); // 초 단위
		response.addCookie(cookie);
	}

	private String getRefreshTokenFromCookie(HttpServletRequest request) {
		Cookie cookie = WebUtils.getCookie(request, REFRESH_TOKEN_COOKIE_NAME);
		return (cookie != null) ? cookie.getValue() : null;
	}

	private void deleteRefreshTokenCookie(HttpServletResponse response) {
		Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, null);
		cookie.setHttpOnly(true);
		cookie.setSecure(true); // HTTPS 사용 시
		cookie.setPath("/");
		cookie.setMaxAge(0); // 쿠키 즉시 만료
		response.addCookie(cookie);
	}
}
