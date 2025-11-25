package com.okebari.artbite.note.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 오늘 게시된 노트를 조회할 때 사용한다.
 * 구독자 여부에 따라 전체 본문 또는 미리보기만 내려준다.
 */
@Schema(description = "오늘 게시된 노트 응답 DTO")
public record TodayPublishedResponse(
	@Schema(description = "노트 접근 가능 여부", example = "true")
	boolean accessible,
	@Schema(description = "노트 상세 정보 (accessible이 true일 경우)")
	NoteResponse note,
	@Schema(description = "노트 미리보기 정보 (accessible이 false일 경우)")
	NotePreviewResponse preview
) {
}
