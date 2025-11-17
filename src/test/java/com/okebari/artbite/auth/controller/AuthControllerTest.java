package com.okebari.artbite.auth.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.okebari.artbite.AbstractContainerBaseTest;
import com.okebari.artbite.auth.dto.LoginRequestDto;
import com.okebari.artbite.auth.dto.SignupRequestDto;
import com.okebari.artbite.auth.dto.TokenDto;
import com.okebari.artbite.auth.jwt.JwtAuthenticationFilter;
import com.okebari.artbite.auth.jwt.JwtProvider;
import com.okebari.artbite.auth.service.AuthService;
import com.okebari.artbite.auth.service.RefreshTokenService;
import com.okebari.artbite.auth.vo.CustomUserDetails;
import com.okebari.artbite.common.exception.EmailAlreadyExistsException;
import com.okebari.artbite.common.exception.GlobalExceptionHandler;
import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.domain.user.UserRole;
import org.springframework.test.context.TestPropertySource;

import software.amazon.awssdk.services.s3.S3Client;

@SpringBootTest
@TestPropertySource(properties = "cloud.aws.s3.bucket=dummy-bucket")
class AuthControllerTest extends AbstractContainerBaseTest {

	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired // Added
	private MappingJackson2HttpMessageConverter jacksonMessageConverter;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private AuthenticationManager authenticationManager;

	@MockitoBean
	private JwtProvider jwtProvider;

	@MockitoBean
	private RefreshTokenService refreshTokenService;

	@MockitoBean
	private RedisTemplate<String, String> redisTemplate;

	@MockitoBean
	private PasswordEncoder passwordEncoder;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@MockitoBean
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@MockitoBean
	private S3Client s3Client;

	@BeforeEach
	void setup() {
		this.mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, jwtProvider))
			.setControllerAdvice(new GlobalExceptionHandler())
			.setMessageConverters(jacksonMessageConverter)
			.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
			.build();
	}

	@Test
	@DisplayName("회원가입 성공")
	void signup_success() throws Exception {
		// given
		SignupRequestDto requestDto = new SignupRequestDto();
		requestDto.setEmail("test@example.com");
		requestDto.setPassword("password");
		requestDto.setUsername("testuser");

		when(authService.signup(any(SignupRequestDto.class))).thenReturn(1L);

		// when & then
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto))
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data").value(1L));
	}

	@Test
	@DisplayName("회원가입 실패 - 이메일 중복")
	void signup_fail_duplicateEmail() throws Exception {
		// given
		SignupRequestDto requestDto = new SignupRequestDto();
		requestDto.setEmail("test@example.com");
		requestDto.setPassword("password");
		requestDto.setUsername("testuser");

		when(authService.signup(any(SignupRequestDto.class))).thenThrow(new EmailAlreadyExistsException());

		// when & then
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto))
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("A001"));
	}

	@Test
	@DisplayName("로그인 성공")
	void login_success() throws Exception {
		// given
		LoginRequestDto requestDto = new LoginRequestDto();
		requestDto.setEmail("test@example.com");
		requestDto.setPassword("password");

		TokenDto tokenDto = TokenDto.builder().accessToken("accessToken").build();
		when(authService.login(any(LoginRequestDto.class), any())).thenReturn(tokenDto);

		// when & then
		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto))
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.accessToken").value("accessToken"));
	}

	@Test
	@DisplayName("토큰 재발급 성공")
	void reissue_success() throws Exception {
		// given
		TokenDto tokenDto = TokenDto.builder().accessToken("newAccessToken").build();
		when(authService.reissueAccessToken(any(), any())).thenReturn(tokenDto);

		// when & then
		mockMvc.perform(post("/api/auth/reissue")
				.cookie(new Cookie("refreshToken", "refreshToken"))
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.accessToken").value("newAccessToken"));
	}

	@Test
	@DisplayName("로그아웃 성공")
	@SuppressWarnings("unchecked")
	void logout_success() throws Exception {
		// given
		// RedisTemplate 모의 설정 추가
		org.springframework.data.redis.core.ValueOperations<String, String> valueOperations = mock(
			org.springframework.data.redis.core.ValueOperations.class);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(anyString())).thenReturn(null); // 블랙리스트에 없는 토큰으로 설정

		// Set up a mock Authentication object with CustomUserDetails as principal
		User mockUser = User.builder()
			.email("test@example.com")
			.password("encodedPassword")
			.username("testuser")
			.role(UserRole.USER)
			.enabled(true)
			.accountNonExpired(true)
			.accountNonLocked(true)
			.credentialsNonExpired(true)
			.build();
		CustomUserDetails customUserDetails = new CustomUserDetails(mockUser);

		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
			customUserDetails, null, customUserDetails.getAuthorities());
		SecurityContextHolder.getContext()
			.setAuthentication(authentication); // Set the authentication in SecurityContextHolder

		doNothing().when(jwtAuthenticationFilter)
			.doFilter(any(), any(), any()); // JwtAuthenticationFilter가 SecurityContext를 덮어쓰지 않도록 Mocking

		when(authService.logout(any(), any(), any(), any())).thenReturn(
			null); // Mock logout to return null for non-social logout
		// when & then
		mockMvc.perform(post("/api/auth/logout")
				.header("Authorization", "Bearer accessToken")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));
	}
}
