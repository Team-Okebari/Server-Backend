package com.okebari.artbite.note.dto.answer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 사용자 답변 입력 요청 DTO.
 */
@Schema(description = "사용자 답변 생성/수정 요청 DTO")
public record NoteAnswerRequest(
	@Schema(description = "답변 내용", example = "제가 생각하는 이 프로젝트의 핵심은...", minLength = 1, maxLength = 200)
	@NotBlank(message = "답변 내용은 비워둘 수 없습니다.")
	@Size(max = 200, message = "답변은 최대 200자까지 입력할 수 있습니다.")
	String answerText
) {
}
