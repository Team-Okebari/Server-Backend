package com.okebari.artbite.creator.dto;

/**
 * 노트 상세나 목록에서 작가 이름/프로필을 보여줄 때 사용하는 요약 DTO.
 * bio 필드는 프론트에서 작가 직함/한줄 소개로 활용된다.
 */
public record CreatorSummaryDto(
	Long id,
	String name,
	String bio,
	String jobTitle,
	String profileImageUrl,
	String instagramUrl,
	String youtubeUrl,
	String behanceUrl,
	String xUrl,
	String blogUrl,
	String newsUrl
) {
}
