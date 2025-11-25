package com.okebari.artbite.note.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 노트 상세/미리보기에서 표시할 카테고리 배지 정보.
 */
@Schema(description = "노트 카테고리 배지 응답 DTO")
public record CategoryBadgeResponse(
	@Schema(description = "카테고리 타입", example = "DESIGN")
	String type,
	@Schema(description = "카테고리 라벨", example = "디자인")
	String label
) {
}
