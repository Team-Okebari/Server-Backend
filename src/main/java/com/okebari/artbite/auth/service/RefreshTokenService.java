package com.okebari.artbite.auth.service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.okebari.artbite.domain.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private final RedisTemplate<String, String> redisTemplate;

	@Value("${jwt.refresh-token-expire-time}")
	private Duration refreshTokenExpireTime;

	public String createRefreshToken(User user, int tokenVersion) {
		String refreshToken = UUID.randomUUID().toString();
		// Store refreshToken:userId:tokenVersion mapping in Redis
		String value = user.getId() + ":" + tokenVersion;
		redisTemplate.opsForValue().set(refreshToken, value, refreshTokenExpireTime);
		return refreshToken;
	}

	public Optional<String> findUserIdAndTokenVersionByRefreshToken(String refreshToken) {
		String value = redisTemplate.opsForValue().get(refreshToken);
		return Optional.ofNullable(value);
	}

	public void deleteRefreshToken(String refreshToken) {
		redisTemplate.delete(refreshToken);
	}
}
