package com.okebari.artbite.note.dto.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 제작 과정 DTO는 노트 작성/수정 응답에서 함께 사용되므로 note 하위 패키지에 배치한다.
 */
@Schema(description = "노트 제작 과정 정보 DTO")
public record NoteProcessDto(
	@Schema(description = "제작 과정 순서", example = "1")
	@NotNull short position,
	@Schema(description = "제작 과정 섹션 제목", example = "아이디어 구상")
	@NotBlank String sectionTitle,
	@Schema(description = "제작 과정 본문 텍스트", example = "초기 아이디어는 여기서 출발했습니다.")
	@NotBlank String bodyText,
	@Schema(description = "제작 과정 이미지 URL", example = "https://example.com/process_image.jpg")
	@NotBlank String imageUrl
) {
}
