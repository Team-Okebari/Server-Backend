package com.okebari.artbite.note.dto.answer;

/**
 * 프론트에 노출할 답변 응답 DTO.
 * answerText 한 필드만 전달해 불필요한 정보 노출을 막는다.
 */
public record NoteAnswerResponse(String answerText) {
}

