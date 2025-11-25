package com.okebari.artbite.note.dto.note;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "노트 개요 정보 DTO")
public record NoteOverviewDto(
	@Schema(description = "개요 섹션 제목", example = "프로젝트 배경")
	@NotBlank String sectionTitle,
	@Schema(description = "개요 본문 텍스트", example = "이 프로젝트는 사용자 경험 개선을 목표로 합니다.")
	@NotBlank String bodyText,
	@Schema(description = "개요 이미지 URL", example = "https://example.com/overview_image.jpg")
	@NotBlank String imageUrl
) {
}
