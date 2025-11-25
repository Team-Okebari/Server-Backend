package com.okebari.artbite.membership.dto;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "멤버십 유도 이미지 수정 요청 DTO")
public class MembershipInducementImageUpdateRequestDto {
	@Schema(description = "이미지 URL", example = "https://example.com/updated_inducement.png")
	@NotBlank(message = "Image URL cannot be blank")
	private String imageUrl;
}
