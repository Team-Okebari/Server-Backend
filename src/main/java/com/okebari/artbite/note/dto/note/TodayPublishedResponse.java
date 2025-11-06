package com.okebari.artbite.note.dto.note;

/**
 * 오늘 게시된 노트를 조회할 때 사용한다.
 * 구독자 여부에 따라 전체 본문 또는 미리보기만 내려준다.
 */
public record TodayPublishedResponse(
	boolean accessible,
	NoteResponse note,
	NotePreviewResponse preview
) {
}
