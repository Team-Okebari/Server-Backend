package com.okebari.artbite.creator.dto;

/**
 * ADMIN API에서 단일 작가 상세를 반환할 때 사용하는 DTO.
 * bio는 프론트에서 직함/한줄 소개(`creatorJobTitle`)로 노출된다.
 */
public record CreatorResponse(
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
