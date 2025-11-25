package com.okebari.artbite.note.dto.note;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "노트 회고 정보 DTO")
public record NoteRetrospectDto(
	@Schema(description = "회고 섹션 제목", example = "성과 분석 및 개선점")
	@NotBlank String sectionTitle,
	@Schema(description = "회고 본문 텍스트", example = "주요 성과는... 개선할 점은...")
	@NotBlank String bodyText
) {
}
