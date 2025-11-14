package com.okebari.artbite.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.okebari.artbite.AbstractContainerBaseTest;
import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.domain.user.UserRepository;
import com.okebari.artbite.domain.user.UserRole;

import software.amazon.awssdk.services.s3.S3Client;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "cloud.aws.s3.bucket=dummy-bucket")
public class AuthIntegrationTest extends AbstractContainerBaseTest {

	@MockitoBean
	private S3Client s3Client;

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private RefreshTokenService refreshTokenService;

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	@BeforeEach
	void setUp() {
		userRepository.deleteAll();
	}

	private String createURLWithPort(String uri) {
		return "http://localhost:" + port + uri;
	}

	@Test
	@DisplayName("통합 테스트: 만료된 Access Token으로 로그아웃 성공")
	void logoutWithExpiredToken() {
		// given: A user is created and has a valid refresh token but an expired access token
		User testUser = User.builder()
			.email("test@example.com")
			.password(passwordEncoder.encode("password"))
			.username("testuser")
			.role(UserRole.USER)
			.tokenVersion(0)
			.enabled(true)
			.accountNonExpired(true)
			.accountNonLocked(true)
			.credentialsNonExpired(true)
			.build();
		userRepository.save(testUser);

		String refreshToken = refreshTokenService.createRefreshToken(testUser, testUser.getTokenVersion());
		// Note: We don't need a valid access token for this test, just a placeholder string
		String expiredAccessToken = "expired-token";

		// Sanity check: ensure refresh token is in Redis
		Optional<String> refreshTokenInRedis = refreshTokenService.findUserIdAndTokenVersionByRefreshToken(
			refreshToken);
		assertTrue(refreshTokenInRedis.isPresent());

		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + expiredAccessToken);
		headers.add(HttpHeaders.COOKIE, "refreshToken=" + refreshToken);

		HttpEntity<String> entity = new HttpEntity<>(headers);

		// when: The user attempts to log out
		ResponseEntity<String> response = restTemplate.exchange(
			createURLWithPort("/api/auth/logout"),
			HttpMethod.POST,
			entity,
			String.class
		);

		// then: The logout should be successful
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		// And: The refresh token cookie should be cleared
		String setCookieHeader = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
		assertThat(setCookieHeader).isNotNull();
		assertThat(setCookieHeader).contains("refreshToken=;");
		assertThat(setCookieHeader).contains("Max-Age=0");

		// And: The refresh token should be deleted from Redis
		Optional<String> refreshTokenInRedisAfterLogout = refreshTokenService.findUserIdAndTokenVersionByRefreshToken(
			refreshToken);
		assertFalse(refreshTokenInRedisAfterLogout.isPresent());
	}
}
