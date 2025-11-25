package com.okebari.artbite.note.dto.note;

import jakarta.validation.constraints.NotBlank;

import com.okebari.artbite.note.domain.NoteCategoryType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "노트 커버 정보")
public record NoteCoverDto(
	@Schema(description = "노트 제목", example = "나의 첫 프로젝트")
	@NotBlank String title,
	@Schema(description = "노트 티저 (짧은 소개)", example = "프로젝트의 시작부터 끝까지")
	@NotBlank String teaser,
	@Schema(description = "메인 이미지 URL", example = "https://example.com/cover_image.jpg")
	@NotBlank String mainImageUrl,
	@Schema(description = "작가 이름", example = "김아트")
	String creatorName,
	@Schema(description = "작가 직함", example = "컨셉 아티스트")
	String creatorJobTitle,
	@Schema(description = "노트 카테고리", example = "DESIGN")
	NoteCategoryType category
) {
}
