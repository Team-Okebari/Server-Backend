package com.okebari.artbite.note.dto.note;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 노트 커버 영역을 프론트에 내려줄 때 사용하는 DTO.
 * 작성자 이름/직함과 게시 시각까지 포함해 히어로 섹션을 한 번에 구성한다.
 */
@JsonInclude(Include.NON_NULL)
@Schema(description = "노트 커버 응답 DTO")
public record NoteCoverResponse(
	@Schema(description = "노트 제목", example = "나의 첫 디자인 프로젝트")
	String title,
	@Schema(description = "노트 티저 (짧은 소개)", example = "이 프로젝트는 이렇게 시작되었습니다.")
	String teaser,
	@Schema(description = "메인 이미지 URL", example = "https://example.com/main_cover.jpg")
	String mainImageUrl,
	@Schema(description = "작가 이름", example = "김작가")
	String creatorName,
	@Schema(description = "작가 직함", example = "UX 디자이너")
	String creatorJobTitle,
	@Schema(description = "발행일", example = "2025-11-25")
	LocalDate publishedDate,
	@Schema(description = "카테고리 배지 정보")
	CategoryBadgeResponse category
) {
}
