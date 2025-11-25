package com.okebari.artbite.note.dto.answer;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 서비스 내부에서 사용하는 답변 조회 DTO.
 * 컨트롤러에서 외부 응답을 구성할 때 필요한 메타 정보까지 포함한다.
 */
@Schema(description = "노트 답변 내부 DTO")
public record NoteAnswerDto(
	@Schema(description = "답변 ID", example = "1")
	Long answerId,
	@Schema(description = "질문 ID", example = "10")
	Long questionId,
	@Schema(description = "응답자 ID", example = "100")
	Long respondentId,
	@Schema(description = "답변 내용", example = "서비스의 핵심 가치는 사용자 경험 개선입니다.")
	String answerText
) {
}
