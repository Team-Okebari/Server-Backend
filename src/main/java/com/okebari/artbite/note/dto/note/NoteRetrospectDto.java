package com.okebari.artbite.note.dto.note;

import jakarta.validation.constraints.NotBlank;

public record NoteRetrospectDto(
	@NotBlank String sectionTitle,
	@NotBlank String bodyText
) {
}
