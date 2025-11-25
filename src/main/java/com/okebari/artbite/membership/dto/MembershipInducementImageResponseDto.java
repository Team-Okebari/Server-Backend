package com.okebari.artbite.membership.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "멤버십 유도 이미지 응답 DTO")
public class MembershipInducementImageResponseDto {
	@Schema(description = "이미지 URL", example = "https://example.com/inducement.png")
	private String imageUrl;
}
