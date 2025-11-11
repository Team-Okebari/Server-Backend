package com.okebari.artbite.note.dto.answer;

import jakarta.validation.constraints.NotBlank;

/**
 * 사용자 답변 입력 요청 DTO.
 */
public record NoteAnswerRequest(
	@NotBlank String answerText
) {
}
