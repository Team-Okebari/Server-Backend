package com.okebari.artbite.note.dto.bookmark;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "노트 북마크 내부 응답 DTO")
public record NoteBookmarkResponse(
	@Schema(description = "북마크 ID", example = "1")
	Long bookmarkId,
	@Schema(description = "노트 ID", example = "10")
	Long noteId,
	@Schema(description = "노트 제목", example = "영감을 주는 디자인")
	String title,
	@Schema(description = "노트 대표 이미지 URL", example = "https://example.com/image.png")
	String mainImageUrl,
	@Schema(description = "노트 태그", example = "디자인")
	String tagText,
	@Schema(description = "작가 이름", example = "김작가")
	String creatorName,
	@Schema(description = "북마크된 시간", example = "2025-11-25T10:30:00")
	LocalDateTime bookmarkedAt
) {
}
