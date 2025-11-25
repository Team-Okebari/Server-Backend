package com.okebari.artbite.note.dto.answer;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 프론트에 노출할 답변 응답 DTO.
 * answerText 한 필드만 전달해 불필요한 정보 노출을 막는다.
 */
@Schema(description = "노트 답변 응답 DTO")
public record NoteAnswerResponse(
	@Schema(description = "답변 내용", example = "제가 생각하는 이 프로젝트의 핵심은...")
	String answerText
) {
}

