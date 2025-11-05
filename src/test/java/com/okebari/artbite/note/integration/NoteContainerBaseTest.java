package com.okebari.artbite.note.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 노트 도메인 통합 테스트용 컨테이너 베이스 클래스.
 * Postgres + Redis 컨테이너를 동시에 기동하고, 스프링 데이터소스/Redis 설정을 동적으로 덮어쓴다.
 */
@Testcontainers
public abstract class NoteContainerBaseTest {

	@Container
	protected static final PostgreSQLContainer<?> POSTGRES =
		new PostgreSQLContainer<>("postgres:14")
			.withDatabaseName("artbite-note-test")
			.withUsername("note_test_user")
			.withPassword("note_test_pass");

	@Container
	protected static final GenericContainer<?> REDIS =
		new GenericContainer<>("redis:7-alpine")
			.withExposedPorts(6379);

	@DynamicPropertySource
	static void overrideDatasource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl());
		registry.add("spring.datasource.username", () -> POSTGRES.getUsername());
		registry.add("spring.datasource.password", () -> POSTGRES.getPassword());
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

		registry.add("spring.data.redis.host", () -> REDIS.getHost());
		registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
	}
}
