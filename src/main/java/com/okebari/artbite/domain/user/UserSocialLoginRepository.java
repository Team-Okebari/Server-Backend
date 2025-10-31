package com.okebari.artbite.domain.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSocialLoginRepository extends JpaRepository<UserSocialLogin, Long> {
	Optional<UserSocialLogin> findByProviderAndProviderId(String provider, String providerId);

	Optional<UserSocialLogin> findByUserAndProvider(User user, String provider);
}
