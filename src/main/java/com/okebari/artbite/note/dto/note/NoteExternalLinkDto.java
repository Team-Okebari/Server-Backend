package com.okebari.artbite.note.dto.note;

import jakarta.validation.constraints.Size;

/**
 * 노트와 함께 저장되는 외부 참고 링크(REQ_106)를 표현한다.
 */
public record NoteExternalLinkDto(
	@Size(max = 500) String sourceUrl
) {
}
