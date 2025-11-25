package com.okebari.artbite.note.dto.question;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "노트 질문 정보 DTO")
public record NoteQuestionDto(
	@Schema(description = "질문 ID", example = "1")
	Long id,
	@Schema(description = "질문 내용", example = "이 프로젝트의 핵심은 무엇인가요?")
	@NotBlank String questionText
) {
}
