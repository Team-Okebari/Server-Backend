package com.okebari.artbite.note.dto.note;

/**
 * 무료 사용자용 노트 미리보기 응답 DTO.
 * 커버와 개요(본문 일부 또는 전체)만 내려 사용자가 구독 여부를 판단하도록 한다.
 */
public record NotePreviewResponse(
	Long id,
	NoteCoverResponse cover,
	NoteOverviewDto overview
) {
}
