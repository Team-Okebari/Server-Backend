package com.okebari.artbite.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Access Token 응답 DTO")
public class TokenDto {
	@Schema(description = "JWT Access Token", example = "eyJhbGciOiJIUzM4NCJ9...")
	private String accessToken;
}
