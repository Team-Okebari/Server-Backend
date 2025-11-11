package com.okebari.artbite.note.dto.bookmark;

/**
 * 북마크 목록 API에서 프론트로 전달하는 전용 응답 DTO.
 * 화면에 필요한 최소 필드(제목, 대표 이미지, 작가 이름/직함)만 포함한다.
 */
public record BookmarkListItemResponse(
	String title,
	String mainImageUrl,
	String creatorName,
	String creatorJobTitle
) {
}
