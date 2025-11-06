package com.okebari.artbite.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.okebari.artbite.auth.dto.LoginRequestDto;
import com.okebari.artbite.auth.dto.SignupRequestDto;
import com.okebari.artbite.auth.dto.TokenDto;
import com.okebari.artbite.auth.jwt.JwtProvider;
import com.okebari.artbite.common.exception.EmailAlreadyExistsException;
import com.okebari.artbite.common.exception.InvalidTokenException;
import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.domain.user.UserRepository;
import com.okebari.artbite.domain.user.UserRole;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@InjectMocks
	private AuthService authService;

	@Mock
	private UserRepository userRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private AuthenticationManager authenticationManager;
	@Mock
	private JwtProvider jwtProvider;
	@Mock
	private RefreshTokenService refreshTokenService;
	@Mock
	private RedisTemplate<String, String> redisTemplate;
	@Mock
	private ValueOperations<String, String> valueOperations;
	@Mock
	private SocialAuthService socialAuthService; // Injected SocialAuthService

	private User testUser;

	@BeforeEach
	void setUp() throws Exception {
		testUser = User.builder()
			.email("test@example.com")
			.password("encodedPassword")
			.username("testuser")
			.role(UserRole.USER)
			.tokenVersion(0) // Add tokenVersion
			.build();

		Field idField = User.class.getDeclaredField("id");
		idField.setAccessible(true);
		idField.set(testUser, 1L);
	}

	@Test
	@DisplayName("회원가입 성공")
	void signup_success() {
		// given
		SignupRequestDto requestDto = new SignupRequestDto();
		requestDto.setEmail("new@example.com");
		requestDto.setPassword("password");
		requestDto.setUsername("newuser");

		when(userRepository.findByEmail(requestDto.getEmail())).thenReturn(Optional.empty());
		when(passwordEncoder.encode(requestDto.getPassword())).thenReturn("encodedPassword");
		when(userRepository.save(any(User.class))).thenReturn(testUser);

		// when
		Long userId = authService.signup(requestDto);

		// then
		assertEquals(testUser.getId(), userId);
		verify(userRepository).findByEmail(requestDto.getEmail());
		verify(passwordEncoder).encode(requestDto.getPassword());
		verify(userRepository).save(any(User.class));
	}

	@Test
	@DisplayName("회원가입 실패 - 이메일 중복")
	void signup_fail_duplicateEmail() {
		// given
		SignupRequestDto requestDto = new SignupRequestDto();
		requestDto.setEmail(testUser.getEmail()); // Existing email
		requestDto.setPassword("password");
		requestDto.setUsername("newuser");

		when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

		// when & then
		assertThrows(EmailAlreadyExistsException.class, () -> {
			authService.signup(requestDto);
		});

		verify(userRepository).findByEmail(testUser.getEmail());
		verify(passwordEncoder, never()).encode(anyString());
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	@DisplayName("로그인 성공")
	void login_success() {
		// given
		LoginRequestDto requestDto = new LoginRequestDto();
		requestDto.setEmail(testUser.getEmail());
		requestDto.setPassword("password");

		Authentication authentication = new UsernamePasswordAuthenticationToken(testUser.getEmail(),
			testUser.getPassword());
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(
			authentication);
		when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
		when(jwtProvider.createToken(authentication)).thenReturn("accessToken");
		when(refreshTokenService.createRefreshToken(eq(testUser), eq(testUser.getTokenVersion()))).thenReturn(
			"refreshToken");

		HttpServletResponse response = mock(HttpServletResponse.class);

		// when
		TokenDto tokenDto = authService.login(requestDto, response);

		// then
		assertEquals("accessToken", tokenDto.getAccessToken());
		verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
		verify(userRepository).findByEmail(testUser.getEmail());
		verify(jwtProvider).createToken(authentication);
		verify(refreshTokenService).createRefreshToken(eq(testUser), eq(testUser.getTokenVersion()));
		verify(response).addCookie(any(Cookie.class));
	}

	@Test
	@DisplayName("토큰 재발급 성공")
	void reissue_success() {
		// given
		String refreshToken = "refreshToken";
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		Cookie cookie = new Cookie("refreshToken", refreshToken);
		when(request.getCookies()).thenReturn(new Cookie[] {cookie});

		when(refreshTokenService.getAndRemoveRefreshToken(refreshToken)) // Use atomic method
			.thenReturn(Optional.of(testUser.getId() + ":" + testUser.getTokenVersion()));
		when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
		when(jwtProvider.createToken(any(Authentication.class))).thenReturn("newAccessToken");
		when(refreshTokenService.createRefreshToken(eq(testUser), eq(testUser.getTokenVersion()))).thenReturn(
			"newRefreshToken");

		// when
		TokenDto tokenDto = authService.reissueAccessToken(request, response);

		// then
		assertEquals("newAccessToken", tokenDto.getAccessToken());
		verify(refreshTokenService, never()).deleteRefreshToken(anyString()); // Verify delete is NOT called separately
		verify(refreshTokenService).createRefreshToken(eq(testUser), eq(testUser.getTokenVersion()));
		verify(response).addCookie(any(Cookie.class));
	}

	@Test
	@DisplayName("토큰 재발급 실패 - 유효하지 않은 토큰")
	void reissue_fail_invalidToken() {
		// given
		String invalidRefreshToken = "invalidRefreshToken";
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		Cookie cookie = new Cookie("refreshToken", invalidRefreshToken);
		when(request.getCookies()).thenReturn(new Cookie[] {cookie});

		when(refreshTokenService.getAndRemoveRefreshToken(invalidRefreshToken)).thenReturn(
			Optional.empty()); // Use atomic method

		// when & then
		assertThrows(InvalidTokenException.class, () -> {
			authService.reissueAccessToken(request, response);
		});
	}

	@Test
	@DisplayName("토큰 재발급 실패 - 토큰 버전 불일치")
	void reissue_fail_tokenVersionMismatch() {
		// given
		String refreshToken = "refreshToken";
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		Cookie cookie = new Cookie("refreshToken", refreshToken);
		when(request.getCookies()).thenReturn(new Cookie[] {cookie});

		User userWithNewerTokenVersion = User.builder()
			.email(testUser.getEmail())
			.password(testUser.getPassword())
			.username(testUser.getUsername())
			.role(UserRole.USER)
			.tokenVersion(testUser.getTokenVersion() + 1) // User has newer token version
			.accountNonExpired(true)
			.accountNonLocked(true)
			.credentialsNonExpired(true)
			.enabled(true)
			.build();
		// Reflectively set ID for userWithNewerTokenVersion
		try {
			Field idField = User.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(userWithNewerTokenVersion, testUser.getId());
		} catch (NoSuchFieldException | IllegalAccessException e) {
			fail("Failed to set ID for userWithNewerTokenVersion: " + e.getMessage());
		}

		when(refreshTokenService.getAndRemoveRefreshToken(refreshToken)) // Use atomic method
			.thenReturn(
				Optional.of(testUser.getId() + ":" + testUser.getTokenVersion())); // Refresh token has old version
		when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(userWithNewerTokenVersion));
		when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(userWithNewerTokenVersion));
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// when & then
		assertThrows(InvalidTokenException.class, () -> {
			authService.reissueAccessToken(request, response);
		});

		// Verify that revokeAllUserTokens was called
		verify(userRepository).save(argThat(user -> user.getTokenVersion() == testUser.getTokenVersion() + 2));
	}

	@Test
	@DisplayName("로그아웃 성공 - Access Token 유효")
	void logout_success_validAccessToken() {
		// given
		String rawAccessToken = "accessToken";
		String bearerAccessToken = "Bearer " + rawAccessToken;
		String refreshToken = "refreshToken";
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		Cookie cookie = new Cookie("refreshToken", refreshToken);

		when(request.getCookies()).thenReturn(new Cookie[] {cookie});
		when(jwtProvider.getExpiration(rawAccessToken)).thenReturn(1000L); // Expect raw token
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(socialAuthService.getSocialLogoutRedirectUrl(testUser.getEmail())).thenReturn(null);

		// when
		authService.logout(bearerAccessToken, testUser.getEmail(), request, response);

		// then
		verify(redisTemplate.opsForValue()).set(eq(rawAccessToken), eq("logout"), anyLong(), any(TimeUnit.class));
		verify(refreshTokenService).deleteRefreshToken(refreshToken);
		verify(response).addCookie(any(Cookie.class));
	}

	@Test
	@DisplayName("로그아웃 성공 - Access Token 만료")
	void logout_success_expiredAccessToken() {
		// given
		String rawAccessToken = "expiredAccessToken";
		String bearerAccessToken = "Bearer " + rawAccessToken;
		String refreshToken = "refreshToken";
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		Cookie cookie = new Cookie("refreshToken", refreshToken);

		when(request.getCookies()).thenReturn(new Cookie[] {cookie});
		// Simulate expired token by throwing exception
		when(jwtProvider.getExpiration(rawAccessToken)).thenThrow(new com.okebari.artbite.common.exception.TokenExpiredException("Token expired"));
		when(redisTemplate.opsForValue()).thenReturn(valueOperations); // Add this missing stub
		when(socialAuthService.getSocialLogoutRedirectUrl(testUser.getEmail())).thenReturn(null);

		// when
		authService.logout(bearerAccessToken, testUser.getEmail(), request, response);

		// then
		// Verify that blacklist is NOT called for expired token
		verify(redisTemplate.opsForValue(), never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
		// Verify that refresh token is still deleted
		verify(refreshTokenService).deleteRefreshToken(refreshToken);
		verify(response).addCookie(any(Cookie.class));
	}

	@Test
	@DisplayName("사용자 모든 토큰 무효화 성공")
	void revokeAllUserTokens_success() {
		// given
		User userBeforeRevoke = User.builder()
			.email(testUser.getEmail())
			.password(testUser.getPassword())
			.username(testUser.getUsername())
			.role(testUser.getRole())
			.tokenVersion(testUser.getTokenVersion())
			.build();
		try {
			Field idField = User.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(userBeforeRevoke, testUser.getId());
		} catch (NoSuchFieldException | IllegalAccessException e) {
			fail("Failed to set ID for userBeforeRevoke: " + e.getMessage());
		}

		when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(userBeforeRevoke));
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// when
		authService.revokeAllUserTokens(testUser.getEmail());

		// then
		verify(userRepository).findByEmail(testUser.getEmail());
		verify(userRepository).save(argThat(user -> user.getTokenVersion() == testUser.getTokenVersion() + 1));
	}
}
