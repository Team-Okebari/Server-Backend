package com.okebari.artbite.note.dto.note;

/**
 * 노트 상세/미리보기에서 표시할 카테고리 배지 정보.
 */
public record CategoryBadgeResponse(
	String type,
	String label
) {
}
