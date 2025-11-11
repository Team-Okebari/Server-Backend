package com.okebari.artbite.note.dto.summary;

import java.time.LocalDate;

/**
 * 지난 노트(ARCHIVED) 목록 카드에 필요한 최소 정보만 담은 요약 DTO.
 */
public record ArchivedNoteSummaryResponse(
	/**
	 * 지난 노트 상세 조회 시 사용할 노트 ID.
	 */
	Long id,
	/**
	 * 노트의 태그 텍스트(분류/키워드).
	 */
	String tagText,
	/**
	 * 노트 제목(카드 타이틀).
	 */
	String title,
	/**
	 * 카드 썸네일로 쓰이는 대표 이미지 URL.
	 */
	String mainImageUrl,
	/**
	 * 노트를 작성한 작가 이름.
	 */
	String creatorName,
	/**
	 * 게시 일시에서 년/월/일까지만 추린 날짜.
	 */
	LocalDate publishedDate
) {
}
