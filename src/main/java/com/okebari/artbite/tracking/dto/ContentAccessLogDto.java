package com.okebari.artbite.tracking.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 유료 콘텐츠 접근 로그를 표현하는 DTO.
 */
@Schema(description = "유료 콘텐츠 접근 로그 DTO")
public record ContentAccessLogDto(
	@Schema(description = "접근한 노트 ID", example = "1")
	Long noteId,
	@Schema(description = "접근한 노트 제목", example = "나의 첫 디자인 프로젝트")
	String noteTitle, // Note 엔티티에서 가져올 정보
	@Schema(description = "접근 시각", example = "2025-11-25T10:00:00")
	LocalDateTime accessedAt
) {
}
