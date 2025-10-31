package com.okebari.artbite.auth.service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.okebari.artbite.auth.dto.OAuthAttributes;
import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.domain.user.UserRepository;
import com.okebari.artbite.domain.user.UserSocialLogin;
import com.okebari.artbite.domain.user.UserSocialLoginRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

	private final SocialAuthService socialAuthService; // Injected SocialAuthService

	@Override
	@Transactional
	public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
		log.debug("CustomOidcUserService: Loading user for request: {}",
			userRequest.getClientRegistration().getRegistrationId());

		OidcUser oidcUser = super.loadUser(userRequest);

		String registrationId = userRequest.getClientRegistration().getRegistrationId();
		String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
			.getUserInfoEndpoint().getUserNameAttributeName();

		Map<String, Object> attributesMap = oidcUser.getAttributes();
		OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, attributesMap);

		User user = socialAuthService.saveOrUpdate(attributes);

		return new DefaultOidcUser(
			Collections.singleton(new SimpleGrantedAuthority(user.getRole().name())),
			oidcUser.getIdToken(),
			oidcUser.getUserInfo(),
			userNameAttributeName);
	}


}
