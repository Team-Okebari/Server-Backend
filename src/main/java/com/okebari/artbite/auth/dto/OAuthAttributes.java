package com.okebari.artbite.auth.dto;

import java.util.Map;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.domain.user.UserRole;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
public record OAuthAttributes(
	Map<String, Object> attributes, // 원본 사용자 정보 속성
	String nameAttributeKey,        // 사용자 이름 속성 키 (설정 파일의 user-name-attribute 값)
	String name,
	String email,
	String provider,                // 예: "google", "kakao", "naver"
	String providerId               // 소셜 플랫폼 고유 ID
) {

	// registrationId(provider)에 따라 분기하여 OAuthAttributes 객체 생성
	public static OAuthAttributes of(String registrationId, String userNameAttributeName,
		Map<String, Object> attributes) {
		log.debug("OAuthAttributes.of() called with registrationId: {}, userNameAttributeName: {}, attributes: {}",
			registrationId, userNameAttributeName, attributes);
		return switch (registrationId.toLowerCase()) {
			case "google" -> ofGoogle(userNameAttributeName, attributes);
			case "kakao" -> ofKakao(userNameAttributeName, attributes);
			case "naver" -> ofNaver("response", attributes); // 네이버는 user-name-attribute가 response 고정
			default -> throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다: " + registrationId);
		};
	}

	// Google 사용자 정보 추출
	private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
		return OAuthAttributes.builder()
			.name((String)attributes.get("name"))
			.email((String)attributes.get("email"))
			.provider("google")
			.providerId((String)attributes.get(userNameAttributeName)) // "sub"
			.attributes(attributes)
			.nameAttributeKey(userNameAttributeName)
			.build();
	}

	// Kakao 사용자 정보 추출
	@SuppressWarnings("unchecked") // Map 캐스팅 경고 무시
	private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
		log.debug("카카오 응답 속성: {}", attributes);

		String name = null;
		String email = null;
		String providerId = null; // Initialize to null

		if (attributes.containsKey("id")) { // Check for 'id' first
			providerId = String.valueOf(attributes.get("id"));
		} else if (attributes.containsKey("sub")) { // If 'id' not found, check for 'sub'
			providerId = String.valueOf(attributes.get("sub"));
		} else {
			log.error("카카오 응답에서 'id' 또는 'sub' 속성을 찾을 수 없습니다. attributes: {}", attributes);
			throw new OAuth2AuthenticationException("카카오 로그인 시 'id' 또는 'sub' 속성을 찾을 수 없어 로그인을 진행할 수 없습니다.");
		}
		// The existing logic for name and email extraction
		if (attributes.containsKey("kakao_account")) {
			Map<String, Object> kakaoAccount = (Map<String, Object>)attributes.get("kakao_account");

			if (kakaoAccount != null) {
				email = (String)kakaoAccount.get("email");

				if (kakaoAccount.containsKey("profile")) {
					Map<String, Object> profile = (Map<String, Object>)kakaoAccount.get("profile");
					if (profile != null) {
						name = (String)profile.get("nickname");
					}
				}
			}
		} else if (attributes.containsKey("email")) {
			email = (String)attributes.get("email");

			if (attributes.containsKey("name")) {
				name = (String)attributes.get("name");
			} else if (attributes.containsKey("nickname")) {
				name = (String)attributes.get("nickname");
			}
		}

		if (name == null) {
			name = "카카오 사용자";
		}

		if (email == null || email.isEmpty()) {
			log.error("카카오 로그인 실패: 이메일 정보 제공에 동의하지 않았습니다. attributes: {}", attributes);
			throw new OAuth2AuthenticationException("카카오 로그인 시 이메일 정보 제공에 동의해야 합니다.");
		}

		return OAuthAttributes.builder()
			.name(name)
			.email(email)
			.provider("kakao")
			.providerId(providerId)
			.attributes(attributes)
			.nameAttributeKey(userNameAttributeName)
			.build();
	}

	// Naver 사용자 정보 추출
	@SuppressWarnings("unchecked")
	private static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
		log.debug("네이버 속성: {}", attributes);

		Map<String, Object> responseData;
		if (attributes.containsKey("response")) {
			responseData = (Map<String, Object>)attributes.get("response");
		} else {
			responseData = attributes;
		}

		if (responseData == null || !responseData.containsKey("id")) {
			log.error("네이버 응답에서 사용자 정보를 찾을 수 없습니다. attributes: {}", attributes);
			throw new OAuth2AuthenticationException("네이버 응답에서 사용자 정보를 찾을 수 없습니다.");
		}

		String id = String.valueOf(responseData.get("id"));
		String name = (String)responseData.getOrDefault("nickname",
			(String)responseData.getOrDefault("name", "네이버 사용자"));
		String email = (String)responseData.getOrDefault("email", id + " @naver.com");

		return OAuthAttributes.builder()
			.name(name)
			.email(email)
			.provider("naver")
			.providerId(id)
			.attributes(responseData)
			.nameAttributeKey("id")
			.build();
	}

	// OAuthAttributes 정보를 바탕으로 User 엔티티 생성 (최초 가입 시)
	public User toEntity(PasswordEncoder passwordEncoder) {
		return User.builder()
			.username(name)
			.email(email)
			.role(UserRole.USER)
			.enabled(true)
			.accountNonExpired(true) // Set to true for social login
			.accountNonLocked(true) // Set to true for social login
			.credentialsNonExpired(true) // Set to true for social login
			.password(passwordEncoder.encode(UUID.randomUUID().toString())) // Hash the generated password
			.build();
	}
}
