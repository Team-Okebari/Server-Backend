package com.okebari.artbite.note.dto.bookmark;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 북마크 목록 API에서 프론트로 전달하는 전용 응답 DTO.
 * 화면에 필요한 최소 필드(노트 ID, 제목, 대표 이미지, 작가 이름, 태그 텍스트)만 포함한다.
 */
@Schema(description = "북마크 목록 아이템 응답 DTO")
public record BookmarkListItemResponse(
	@Schema(description = "북마크된 노트 ID", example = "10")
	Long noteId,
	@Schema(description = "노트 제목", example = "영감을 주는 디자인")
	String title,
	@Schema(description = "노트 대표 이미지 URL", example = "https://example.com/image.png")
	String mainImageUrl,
	@Schema(description = "작가 이름", example = "김작가")
	String creatorName,
	@Schema(description = "노트 태그", example = "디자인")
	String tagText
) {
}
