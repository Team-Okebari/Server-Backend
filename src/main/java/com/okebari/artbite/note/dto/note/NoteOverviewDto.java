package com.okebari.artbite.note.dto.note;

import jakarta.validation.constraints.NotBlank;

public record NoteOverviewDto(
	@NotBlank String sectionTitle,
	@NotBlank String bodyText,
	@NotBlank String imageUrl
) {
}
