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

	private User testUser;

	@BeforeEach
	void setUp() throws Exception {
		testUser = User.builder()
			.email("test@example.com")
			.password("encodedPassword")
			.username("testuser")
			.role(UserRole.USER)
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
		when(refreshTokenService.createRefreshToken(testUser)).thenReturn("refreshToken");

		HttpServletResponse response = mock(HttpServletResponse.class);

		// when
		TokenDto tokenDto = authService.login(requestDto, response);

		// then
		assertEquals("accessToken", tokenDto.getAccessToken());
		verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
		verify(userRepository).findByEmail(testUser.getEmail());
		verify(jwtProvider).createToken(authentication);
		verify(refreshTokenService).createRefreshToken(testUser);
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

		when(refreshTokenService.findUserIdByRefreshToken(refreshToken)).thenReturn(Optional.of(testUser.getId()));
		when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
		when(jwtProvider.createToken(any(Authentication.class))).thenReturn("newAccessToken");
		when(refreshTokenService.createRefreshToken(testUser)).thenReturn("newRefreshToken");

		// when
		TokenDto tokenDto = authService.reissueAccessToken(request, response);

		// then
		assertEquals("newAccessToken", tokenDto.getAccessToken());
		verify(refreshTokenService).deleteRefreshToken(refreshToken);
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

		when(refreshTokenService.findUserIdByRefreshToken(invalidRefreshToken)).thenReturn(Optional.empty());

		// when & then
		assertThrows(InvalidTokenException.class, () -> {
			authService.reissueAccessToken(request, response);
		});
	}

	@Test
	@DisplayName("로그아웃 성공")
	void logout_success() {
		// given
		String accessToken = "accessToken";
		String refreshToken = "refreshToken";
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		Cookie cookie = new Cookie("refreshToken", refreshToken);

		when(request.getCookies()).thenReturn(new Cookie[] {cookie});
		when(jwtProvider.getExpiration(accessToken)).thenReturn(1000L);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		// when
		authService.logout(accessToken, testUser.getEmail(), request, response);

		// then
		verify(redisTemplate.opsForValue()).set(eq(accessToken), eq("logout"), anyLong(), any(TimeUnit.class));
		verify(refreshTokenService).deleteRefreshToken(refreshToken);
		verify(response).addCookie(any(Cookie.class));
	}
}
