package com.okebari.artbite.creator.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 노트 상세나 목록에서 작가 이름/프로필을 보여줄 때 사용하는 요약 DTO.
 * bio 필드는 프론트에서 작가 직함/한줄 소개로 활용된다.
 */
@Schema(description = "작가 요약 정보 응답 DTO")
public record CreatorSummaryDto(
	@Schema(description = "작가 ID", example = "1")
	Long id,
	@Schema(description = "작가 이름", example = "김작가")
	String name,
	@Schema(description = "작가 한 줄 소개", example = "세상을 그리는 일러스트레이터")
	String bio,
	@Schema(description = "작가 직함", example = "일러스트레이터")
	String jobTitle,
	@Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
	String profileImageUrl,
	@Schema(description = "인스타그램 URL", example = "https://instagram.com/creator")
	String instagramUrl,
	@Schema(description = "유튜브 URL", example = "https://youtube.com/creator")
	String youtubeUrl,
	@Schema(description = "Behance URL", example = "https://behance.net/creator")
	String behanceUrl,
	@Schema(description = "X (트위터) URL", example = "https://x.com/creator")
	String xUrl,
	@Schema(description = "블로그 URL", example = "https://creator.blog.com")
	String blogUrl,
	@Schema(description = "뉴스/웹사이트 URL", example = "https://creator.com")
	String newsUrl
) {
}
