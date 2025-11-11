package com.okebari.artbite.note.dto.answer;

/**
 * 서비스 내부에서 사용하는 답변 조회 DTO.
 * 컨트롤러에서 외부 응답을 구성할 때 필요한 메타 정보까지 포함한다.
 */
public record NoteAnswerDto(
	Long answerId,
	Long questionId,
	Long respondentId,
	String answerText
) {
}
