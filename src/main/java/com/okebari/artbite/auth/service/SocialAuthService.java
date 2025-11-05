package com.okebari.artbite.auth.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import com.okebari.artbite.auth.dto.OAuthAttributes;
import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.domain.user.UserRepository;
import com.okebari.artbite.domain.user.UserSocialLogin;
import com.okebari.artbite.domain.user.UserSocialLoginRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class SocialAuthService {

	private final UserRepository userRepository;
	private final UserSocialLoginRepository userSocialLoginRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${spring.security.oauth2.client.registration.kakao.client-id}")
	private String kakaoClientId;

	@Value("${oauth2.logout.redirect-uri.kakao}")
	private String kakaoLogoutRedirectUri;

	@CacheEvict(value = "userDetails", key = "#attributes.email")
	public User saveOrUpdate(OAuthAttributes attributes) {
		Optional<UserSocialLogin> userSocialLoginOptional = userSocialLoginRepository.findByProviderAndProviderId(
			attributes.provider(), attributes.providerId());

		User user;
		if (userSocialLoginOptional.isPresent()) {
			// 케이스 1: 소셜 로그인이 이미 존재함
			UserSocialLogin socialLogin = userSocialLoginOptional.get();
			user = socialLogin.getUser();
			user.updateOAuthInfo(attributes.name()); // 변경된 경우 사용자 이름 업데이트
			userRepository.save(user); // 사용자 업데이트 영구 저장
			log.info("Existing social user found: provider={}, providerId={}, email={}",
				attributes.provider(), attributes.providerId(), user.getEmail());
		} else {
			// 케이스 2: 소셜 로그인이 존재하지 않음, 이메일로 사용자 존재 여부 확인
			Optional<User> existingEmailUser = userRepository.findByEmail(attributes.email());
			if (existingEmailUser.isPresent()) {
				// 케이스 2a: 이메일로 사용자가 존재함, 소셜 계정 연결
				user = existingEmailUser.get();
				// 이 소셜 제공자가 이 사용자에게 이미 연결되어 있는지 확인
				Optional<UserSocialLogin> existingSocialForUser = userSocialLoginRepository.findByUserAndProvider(user,
					attributes.provider());
				if (existingSocialForUser.isPresent()) {
					// provider+providerId에 대한 고유 제약 조건이 작동하는 경우 이상적으로는 발생하지 않아야 하지만,
					// 방어적으로 처리하는 것이 좋습니다.
					log.warn(
						"Attempted to link existing social account to user, but it's already linked. User ID: {}, Provider: {}",
						user.getId(), attributes.provider());
				} else {
					UserSocialLogin newSocialLogin = UserSocialLogin.builder()
						.user(user)
						.provider(attributes.provider())
						.providerId(attributes.providerId())
						.build();
					user.getSocialLogins().add(newSocialLogin); // 컬렉션에 추가
					log.info("Existing email user linked with new OAuth2: provider={}, providerId={}, email={}",
						attributes.provider(), attributes.providerId(), user.getEmail());
				}
				user.updateOAuthInfo(attributes.name()); // 변경된 경우 사용자 이름 업데이트
				userRepository.save(user); // 사용자 업데이트 영구 저장
			} else {
				// 케이스 2b: 소셜 로그인도 이메일로 사용자도 존재하지 않음, 새 사용자 및 소셜 로그인 생성
				user = attributes.toEntity(passwordEncoder); // 새 사용자 생성
				userRepository.save(user); // ID를 얻기 위해 먼저 사용자 저장

				UserSocialLogin newSocialLogin = UserSocialLogin.builder()
					.user(user)
					.provider(attributes.provider())
					.providerId(attributes.providerId())
					.build();
				user.getSocialLogins().add(newSocialLogin); // 컬렉션에 추가
				log.info("New user and OAuth2 registered: provider={}, providerId={}, email={}",
					attributes.provider(), attributes.providerId(), user.getEmail());
			}
		}
		return user;
	}

	public String getSocialLogoutRedirectUrl(String userEmail) {
		User user = userRepository.findByEmail(userEmail)
			.orElse(null); // 사용자를 찾을 수 없는 경우 null 반환, 또는 원하는 동작에 따라 예외 throw

		if (user != null) {
			boolean isKakaoSocialUser = user.getSocialLogins().stream()
				.anyMatch(sl -> "kakao".equalsIgnoreCase(sl.getProvider()));

			if (isKakaoSocialUser) {
				return UriComponentsBuilder.fromUriString("https://kauth.kakao.com/oauth/logout")
					.queryParam("client_id", kakaoClientId)
					.queryParam("logout_redirect_uri", kakaoLogoutRedirectUri)
					.build().toUriString();
			}
		}
		return null; // 소셜 로그아웃 리디렉션이 필요 없음
	}
}
