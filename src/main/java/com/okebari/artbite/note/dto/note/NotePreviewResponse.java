package com.okebari.artbite.note.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 무료 사용자용 노트 미리보기 응답 DTO.
 * 커버와 개요(본문 일부 또는 전체)만 내려 사용자가 구독 여부를 판단하도록 한다.
 */
@Schema(description = "노트 미리보기 응답 DTO")
public record NotePreviewResponse(
	@Schema(description = "노트 ID", example = "1")
	Long id,
	@Schema(description = "노트 커버 정보")
	NoteCoverResponse cover,
	@Schema(description = "노트 개요 정보")
	NoteOverviewDto overview,
	@Schema(description = "북마크 여부", example = "false")
	Boolean isBookmarked
) {
}
