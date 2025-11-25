package com.okebari.artbite.note.dto.summary;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 지난 노트(ARCHIVED) 목록 카드에 필요한 최소 정보만 담은 요약 DTO.
 */
@Schema(description = "아카이브된 노트 요약 응답 DTO")
public record ArchivedNoteSummaryResponse(
	/**
	 * 지난 노트 상세 조회 시 사용할 노트 ID.
	 */
	@Schema(description = "노트 ID", example = "1")
	Long id,
	/**
	 * 노트의 태그 텍스트(분류/키워드).
	 */
	@Schema(description = "노트 태그", example = "디자인")
	String tagText,
	/**
	 * 노트 제목(카드 타이틀).
	 */
	@Schema(description = "노트 제목", example = "나의 디자인 여정")
	String title,
	/**
	 * 카드 썸네일로 쓰이는 대표 이미지 URL.
	 */
	@Schema(description = "대표 이미지 URL", example = "https://example.com/thumbnail.jpg")
	String mainImageUrl,
	/**
	 * 노트를 작성한 작가 이름.
	 */
	@Schema(description = "작가 이름", example = "김작가")
	String creatorName,
	/**
	 * 게시 일시에서 년/월/일까지만 추린 날짜.
	 */
	@Schema(description = "게시일", example = "2025-11-25")
	LocalDate publishedDate
) {
}
