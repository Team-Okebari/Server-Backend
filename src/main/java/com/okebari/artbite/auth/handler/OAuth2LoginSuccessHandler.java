package com.okebari.artbite.auth.handler;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.okebari.artbite.auth.dto.OAuthAttributes;
import com.okebari.artbite.auth.jwt.JwtProvider;
import com.okebari.artbite.auth.service.RefreshTokenService;
import com.okebari.artbite.auth.service.SocialAuthService;
import com.okebari.artbite.domain.user.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final JwtProvider jwtProvider;
	private final RefreshTokenService refreshTokenService;
	private final SocialAuthService socialAuthService;
	private final ObjectMapper objectMapper;

	@Value("${oauth2.redirect-uri.success}")
	private String successRedirectUri;

	@Override
	@Transactional
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException, ServletException {
		log.info("OAuth2 Login Success Handler: Authentication successful.");
		clearAuthenticationAttributes(request);

		OAuth2User oAuth2User = (OAuth2User)authentication.getPrincipal();
		Map<String, Object> attributesMap = oAuth2User.getAttributes();
		String registrationId = ((OAuth2AuthenticationToken)authentication).getAuthorizedClientRegistrationId();

		log.debug("소셜 로그인 - provider: {}, attributes: {}", registrationId, attributesMap);

		String userNameAttributeName = getUserNameAttributeName(registrationId);
		OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, attributesMap);

		User user = socialAuthService.saveOrUpdate(attributes);

		sendTokenResponse(user, registrationId, response, request);
	}

	private void sendTokenResponse(User user, String registrationId, HttpServletResponse response,
		HttpServletRequest request) throws IOException {
		// User 엔티티를 기반으로 OAuth2User principal 생성
		OAuth2User oauth2UserPrincipal = new DefaultOAuth2User(
			Collections.singleton(new SimpleGrantedAuthority(user.getRole().getKey())),
			Map.of("email", user.getEmail(), "name", user.getUsername()),
			"email"
		);

		Authentication auth = new OAuth2AuthenticationToken(
			oauth2UserPrincipal, // principal
			Collections.singleton(new SimpleGrantedAuthority(user.getRole().getKey())),
			registrationId // authorizedClientRegistrationId
		);
		// 1. Access Token 생성
		String accessToken = jwtProvider.createToken(auth);

		// 2. Refresh Token 생성 및 HTTP-only 쿠키 설정
		String refreshToken = refreshTokenService.createRefreshToken(user, user.getTokenVersion());
		addRefreshTokenCookie(response, refreshToken);

		// 3. 프론트엔드 리다이렉트 URI에 Access Token을 쿼리 파라미터로 추가
		String targetUrl = UriComponentsBuilder.fromUriString(successRedirectUri)
			.queryParam("accessToken", accessToken)
			.build().toUriString();

		getRedirectStrategy().sendRedirect(request, response, targetUrl);
	}

	public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
		Cookie cookie = new Cookie("refreshToken", refreshToken);
		cookie.setHttpOnly(true);
		cookie.setSecure(true); // HTTPS 사용 시
		cookie.setPath("/"); // 모든 경로에서 접근 가능
		cookie.setMaxAge((int)(jwtProvider.getRefreshTokenExpireTime() / 1000)); // 초 단위
		response.addCookie(cookie);
	}

	private String getUserNameAttributeName(String registrationId) {
		return switch (registrationId.toLowerCase()) {
			case "google" -> "sub";
			case "kakao" -> "id"; // 카카오는 user-name-attribute가 id
			case "naver" -> "response"; // 네이버는 user-name-attribute가 response
			default -> "id";
		};
	}
}
