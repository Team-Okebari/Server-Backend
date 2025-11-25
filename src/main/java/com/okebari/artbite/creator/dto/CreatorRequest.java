package com.okebari.artbite.creator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * ADMIN이 작가 정보를 등록/수정할 때 사용하는 입력 DTO.
 * bio(직함/한줄 소개)와 각종 SNS URL은 선택사항이며 길이 제한만 둔다.
 */
@Schema(description = "작가 생성/수정 요청 DTO")
public record CreatorRequest(
	@Schema(description = "작가 이름", example = "김작가")
	@NotBlank @Size(max = 60) String name,

	@Schema(description = "작가 한 줄 소개", example = "세상을 그리는 일러스트레이터")
	@Size(max = 100) String bio,

	@Schema(description = "작가 직함", example = "일러스트레이터")
	@Size(max = 60) String jobTitle,

	@Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
	@Size(max = 500) String profileImageUrl,

	@Schema(description = "인스타그램 URL", example = "https://instagram.com/creator")
	@Size(max = 500) String instagramUrl,

	@Schema(description = "유튜브 URL", example = "https://youtube.com/creator")
	@Size(max = 500) String youtubeUrl,

	@Schema(description = "Behance URL", example = "https://behance.net/creator")
	@Size(max = 500) String behanceUrl,

	@Schema(description = "X (트위터) URL", example = "https://x.com/creator")
	@Size(max = 500) String xUrl,

	@Schema(description = "블로그 URL", example = "https://creator.blog.com")
	@Size(max = 500) String blogUrl,

	@Schema(description = "뉴스/웹사이트 URL", example = "https://creator.com")
	@Size(max = 500) String newsUrl
) {
}
