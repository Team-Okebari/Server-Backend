package com.okebari.artbite.creator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ADMIN이 작가 정보를 등록/수정할 때 사용하는 입력 DTO.
 * bio(직함/한줄 소개)와 각종 SNS URL은 선택사항이며 길이 제한만 둔다.
 */
public record CreatorRequest(
	@NotBlank @Size(max = 60) String name,
	@Size(max = 100) String bio,
	@Size(max = 60) String jobTitle,
	@Size(max = 500) String profileImageUrl,
	@Size(max = 500) String instagramUrl,
	@Size(max = 500) String youtubeUrl,
	@Size(max = 500) String behanceUrl,
	@Size(max = 500) String xUrl,
	@Size(max = 500) String blogUrl,
	@Size(max = 500) String newsUrl
) {
}
