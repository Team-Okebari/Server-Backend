package com.okebari.artbite.auth.service;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.okebari.artbite.common.service.MdcLogging;
import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.domain.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);
	private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
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
				.orElseThrow(() -> new RuntimeException("User not found after authentication.")); // Should not happen

			String accessToken = jwtProvider.createToken(authentication);
			String refreshToken = refreshTokenService.createRefreshToken(user);

			// Refresh Token을 HTTP-only 쿠키로 설정
			addRefreshTokenCookie(response, refreshToken);

			log.info("로그인 성공: email={}", loginRequestDto.getEmail());
			return TokenDto.builder()
				.accessToken(accessToken)
				.build();
		}
	}

	@Transactional
	public TokenDto reissueAccessToken(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = getRefreshTokenFromCookie(request);
		if (!StringUtils.hasText(refreshToken)) {
			throw new InvalidTokenException("Refresh Token이 쿠키에 없습니다.");
		}

		// 1. Redis에서 Refresh Token 존재 여부 및 사용자 ID 확인
		Long userId = refreshTokenService.findUserIdByRefreshToken(refreshToken)
			.orElseThrow(() -> new InvalidTokenException("유효하지 않거나 만료된 Refresh Token입니다."));

		// 2. 사용자 정보 로드
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new InvalidTokenException("사용자를 찾을 수 없습니다."));

		// 3. 새로운 Access Token 생성
		Collection<? extends GrantedAuthority> authorities = List.of(
			new SimpleGrantedAuthority(user.getRole().getKey()));
		Authentication authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);
		String newAccessToken = jwtProvider.createToken(authentication);

		// 4. Refresh Token Rotation: 기존 토큰은 삭제하고 새로운 토큰을 발급한다.
		refreshTokenService.deleteRefreshToken(refreshToken);
		String newRefreshToken = refreshTokenService.createRefreshToken(user);

		// 새로운 Refresh Token을 HTTP-only 쿠키로 설정
		addRefreshTokenCookie(response, newRefreshToken);

		return TokenDto.builder()
			.accessToken(newAccessToken)
			.build();
	}

	public void logout(String accessToken, String userEmail, HttpServletRequest request, HttpServletResponse response) {
		try (var ignored = (userEmail != null) ? MdcLogging.withContext("email", userEmail) : null) {
			log.info("로그아웃 시도: email={}", userEmail != null ? userEmail : "unknown");

			// 1. Access Token 블랙리스트에 추가
			// Access Token의 남은 유효 시간
			Long expiration = jwtProvider.getExpiration(accessToken);
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
		}
	}

	private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
		Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
		cookie.setHttpOnly(true);
		cookie.setSecure(true); // HTTPS 사용 시
		cookie.setPath("/"); // 모든 경로에서 접근 가능
		cookie.setMaxAge((int)(jwtProvider.getRefreshTokenExpireTime() / 1000)); // 초 단위
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
