package com.okebari.artbite.tracking.dto;

import java.time.LocalDateTime;

/**
 * 유료 콘텐츠 접근 로그를 표현하는 DTO.
 */
public record ContentAccessLogDto(
	Long noteId,
	String noteTitle, // Note 엔티티에서 가져올 정보
	LocalDateTime accessedAt
) {
}
