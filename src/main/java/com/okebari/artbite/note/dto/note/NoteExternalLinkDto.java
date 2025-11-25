package com.okebari.artbite.note.dto.note;

import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 노트와 함께 저장되는 외부 참고 링크(REQ_106)를 표현한다.
 */
@Schema(description = "노트 외부 링크 정보 DTO")
public record NoteExternalLinkDto(
	@Schema(description = "외부 참고 링크 URL", example = "https://external.com/reference")
	@Size(max = 500) String sourceUrl
) {
}
