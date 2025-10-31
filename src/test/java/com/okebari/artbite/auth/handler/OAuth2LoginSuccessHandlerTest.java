package com.okebari.artbite.auth.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.test.util.ReflectionTestUtils;

import com.okebari.artbite.auth.dto.OAuthAttributes;
import com.okebari.artbite.auth.jwt.JwtProvider;
import com.okebari.artbite.auth.service.RefreshTokenService;
import com.okebari.artbite.auth.service.SocialAuthService;
import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.domain.user.UserRepository;
import com.okebari.artbite.domain.user.UserRole;
import com.okebari.artbite.domain.user.UserSocialLoginRepository;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

	@InjectMocks
	private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

	@Mock
	private JwtProvider jwtProvider;

	@Mock
	private RefreshTokenService refreshTokenService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private UserSocialLoginRepository userSocialLoginRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private SocialAuthService socialAuthService; // Injected SocialAuthService

	@Mock
	private RedirectStrategy redirectStrategy;

	private MockHttpServletResponse response;

	@BeforeEach
	void setUp() {
		response = new MockHttpServletResponse();
		ReflectionTestUtils.setField(oAuth2LoginSuccessHandler, "redirectStrategy", redirectStrategy);
		ReflectionTestUtils.setField(oAuth2LoginSuccessHandler, "successRedirectUri",
			"http://localhost:3000/oauth2/redirect");
	}

	private String getTestUserNameAttributeName(String registrationId) {
		return switch (registrationId.toLowerCase()) {
			case "google" -> "sub";
			case "kakao" -> "id";
			case "naver" -> "response";
			default -> "id";
		};
	}

	private OAuth2User createOAuth2User(String registrationId, Map<String, Object> attributes) {
		String userNameAttributeName = getTestUserNameAttributeName(registrationId);
		return new DefaultOAuth2User(
			Collections.singleton(new SimpleGrantedAuthority(UserRole.USER.getKey())),
			attributes,
			userNameAttributeName
		);
	}

	private Map<String, Object> createKakaoAttributes(String id, String email, String nickname) {
		Map<String, Object> kakaoAccount = new HashMap<>();
		kakaoAccount.put("email", email);
		kakaoAccount.put("profile", Map.of("nickname", nickname));

		Map<String, Object> attributes = new HashMap<>();
		attributes.put("id", id);
		attributes.put("kakao_account", kakaoAccount);
		return attributes;
	}

	@Test
	@DisplayName("OAuth2 로그인 성공 시 기존 사용자 처리 및 토큰 발급 후 리다이렉트")
	void onAuthenticationSuccess_existingUser_shouldIssueTokensAndRedirect() throws ServletException, IOException {
		// Given
		String registrationId = "kakao";
		Map<String, Object> attributes = createKakaoAttributes("12345", "test@example.com", "Test User");
		OAuth2User oAuth2User = createOAuth2User(registrationId, attributes);
		Authentication authentication = new OAuth2AuthenticationToken(oAuth2User, Collections.emptyList(),
			registrationId);

		User existingUser = spy(User.builder()
			.email("test@example.com")
			.username("Old Name")
			.role(UserRole.USER)
			.build());
		ReflectionTestUtils.setField(existingUser, "id", 1L);

		when(socialAuthService.saveOrUpdate(any(OAuthAttributes.class))).thenReturn(existingUser);
		when(jwtProvider.createToken(any(Authentication.class))).thenReturn("mockAccessToken");
		when(refreshTokenService.createRefreshToken(any(User.class), anyInt())).thenReturn("mockRefreshToken");

		// When
		oAuth2LoginSuccessHandler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

		// Then

		verify(redirectStrategy).sendRedirect(any(HttpServletRequest.class), any(MockHttpServletResponse.class),
			eq("http://localhost:3000/oauth2/redirect?accessToken=mockAccessToken"));

		Cookie refreshTokenCookie = response.getCookie("refreshToken");
		assertNotNull(refreshTokenCookie);
		assertEquals("mockRefreshToken", refreshTokenCookie.getValue());
	}

	@Test
	@DisplayName("OAuth2 로그인 성공 시 신규 사용자 등록 및 토큰 발급 후 리다이렉트")
	void onAuthenticationSuccess_newUser_shouldRegisterUserIssueTokensAndRedirect() throws
		ServletException,
		IOException {
		// Given
		String registrationId = "kakao";
		Map<String, Object> attributes = createKakaoAttributes("67890", "newuser@example.com", "New User");
		OAuth2User oAuth2User = createOAuth2User(registrationId, attributes);
		Authentication authentication = new OAuth2AuthenticationToken(oAuth2User, Collections.emptyList(),
			registrationId);

		User newUser = User.builder()
			.email("newuser@example.com")
			.username("New User")
			.role(UserRole.USER)
			.enabled(true)
			.accountNonExpired(true)
			.accountNonLocked(true)
			.credentialsNonExpired(true)
			.build();
		ReflectionTestUtils.setField(newUser, "id", 1L);

		when(socialAuthService.saveOrUpdate(any(OAuthAttributes.class))).thenReturn(newUser);
		when(jwtProvider.createToken(any(Authentication.class))).thenReturn("mockAccessToken");
		when(refreshTokenService.createRefreshToken(any(User.class), anyInt())).thenReturn("mockRefreshToken");

		// When
		oAuth2LoginSuccessHandler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

		// Then


		verify(redirectStrategy).sendRedirect(any(HttpServletRequest.class), any(MockHttpServletResponse.class),
			eq("http://localhost:3000/oauth2/redirect?accessToken=mockAccessToken"));
	}

	@Test
	@DisplayName("OAuth2 로그인 성공 시 기존 이메일 사용자에게 소셜 정보 연결 및 토큰 발급 후 리다이렉트")
	void onAuthenticationSuccess_existingEmailUser_shouldLinkSocialInfoIssueTokensAndRedirect() throws
		ServletException,
		IOException {
		// Given
		String registrationId = "kakao";
		Map<String, Object> attributes = createKakaoAttributes("54321", "existing@example.com", "Existing User");
		OAuth2User oAuth2User = createOAuth2User(registrationId, attributes);
		Authentication authentication = new OAuth2AuthenticationToken(oAuth2User, Collections.emptyList(),
			registrationId);

		User existingEmailUser = spy(
			User.builder().email("existing@example.com").username("Existing User").role(UserRole.USER).build());
		ReflectionTestUtils.setField(existingEmailUser, "id", 2L);

		when(socialAuthService.saveOrUpdate(any(OAuthAttributes.class))).thenReturn(existingEmailUser);
		when(jwtProvider.createToken(any(Authentication.class))).thenReturn("mockAccessToken");
		when(refreshTokenService.createRefreshToken(any(User.class), anyInt())).thenReturn("mockRefreshToken");

		// When
		oAuth2LoginSuccessHandler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

		// Then


		verify(redirectStrategy).sendRedirect(any(HttpServletRequest.class), any(MockHttpServletResponse.class),
			eq("http://localhost:3000/oauth2/redirect?accessToken=mockAccessToken"));
	}
}