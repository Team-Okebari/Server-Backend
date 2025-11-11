package com.okebari.artbite;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("test")
public abstract class AbstractContainerBaseTest {

	private static final PostgreSQLContainer<?> POSTGRES_CONTAINER;
	private static final GenericContainer<?> REDIS_CONTAINER;

	static {
		POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres:14")
			.withDatabaseName("artbite-test-db")
			.withUsername("testuser")
			.withPassword("testpass");

		REDIS_CONTAINER = new GenericContainer<>(DockerImageName.parse("redis:6-alpine"))
			.withExposedPorts(6379);

		POSTGRES_CONTAINER.start();
		REDIS_CONTAINER.start();
	}

	@DynamicPropertySource
	static void containerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
		registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

		registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
		registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379).toString());
	}
}
