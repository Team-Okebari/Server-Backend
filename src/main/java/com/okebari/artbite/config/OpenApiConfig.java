package com.okebari.artbite.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI customOpenApi() {
		return new OpenAPI()
			.components(new Components().addSecuritySchemes("bearerAuth",
				new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
			.info(new Info()
				.title("Sparki API 문서")
				.version("1.0")
				.description("Sparki 애플리케이션의 API 문서입니다."));
	}
}
