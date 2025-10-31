package com.okebari.artbite.auth.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
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

	public User saveOrUpdate(OAuthAttributes attributes) {
		Optional<UserSocialLogin> userSocialLoginOptional = userSocialLoginRepository.findByProviderAndProviderId(
			attributes.provider(), attributes.providerId());

		User user;
		if (userSocialLoginOptional.isPresent()) {
			// Case 1: Social login already exists
			UserSocialLogin socialLogin = userSocialLoginOptional.get();
			user = socialLogin.getUser();
			user.updateOAuthInfo(attributes.name()); // Update username if changed
			userRepository.save(user); // Persist the user update
			log.info("Existing social user found: provider={}, providerId={}, email={}",
				attributes.provider(), attributes.providerId(), user.getEmail());
		} else {
			// Case 2: Social login does not exist, check if user with email exists
			Optional<User> existingEmailUser = userRepository.findByEmail(attributes.email());
			if (existingEmailUser.isPresent()) {
				// Case 2a: User with email exists, link social account
				user = existingEmailUser.get();
				// Check if this social provider is already linked to this user
				Optional<UserSocialLogin> existingSocialForUser = userSocialLoginRepository.findByUserAndProvider(user,
					attributes.provider());
				if (existingSocialForUser.isPresent()) {
					// This should ideally not happen if unique constraint on provider+providerId is working,
					// but good to handle defensively.
					log.warn(
						"Attempted to link existing social account to user, but it's already linked. User ID: {}, Provider: {}",
						user.getId(), attributes.provider());
				} else {
					UserSocialLogin newSocialLogin = UserSocialLogin.builder()
						.user(user)
						.provider(attributes.provider())
						.providerId(attributes.providerId())
						.build();
					user.getSocialLogins().add(newSocialLogin); // Add to collection
					// userSocialLoginRepository.save(newSocialLogin); // Removed, as it will be cascaded
					log.info("Existing email user linked with new OAuth2: provider={}, providerId={}, email={}",
						attributes.provider(), attributes.providerId(), user.getEmail());
				}
				user.updateOAuthInfo(attributes.name()); // Update username if changed
				userRepository.save(user); // Persist the user update
			} else {
				// Case 2b: Neither social login nor user with email exists, create new user and social login
				user = attributes.toEntity(passwordEncoder); // Create new User
				userRepository.save(user); // Save User first to get ID

				UserSocialLogin newSocialLogin = UserSocialLogin.builder()
					.user(user)
					.provider(attributes.provider())
					.providerId(attributes.providerId())
					.build();
				user.getSocialLogins().add(newSocialLogin); // Add to collection
				// userSocialLoginRepository.save(newSocialLogin); // Removed, as it will be cascaded
				log.info("New user and OAuth2 registered: provider={}, providerId={}, email={}",
					attributes.provider(), attributes.providerId(), user.getEmail());
			}
		}
		return user;
	}

	public String getSocialLogoutRedirectUrl(String userEmail) {
		User user = userRepository.findByEmail(userEmail)
			.orElse(null); // Return null if user not found, or throw exception based on desired behavior

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
		return null; // No social logout redirect needed
	}
}
