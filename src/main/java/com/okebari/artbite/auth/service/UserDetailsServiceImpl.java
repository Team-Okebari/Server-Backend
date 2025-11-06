package com.okebari.artbite.auth.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.okebari.artbite.auth.vo.CustomUserDetails;
import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.domain.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = "userDetails", key = "#email")
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		User user = userRepository.findByEmailWithMemberships(email)
			.orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));
		return new CustomUserDetails(user);
	}
}
