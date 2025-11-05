package com.okebari.artbite.note.dto.note;

import jakarta.validation.constraints.NotBlank;

public record NoteCoverDto(
	@NotBlank String title,
	@NotBlank String teaser,
	@NotBlank String mainImageUrl
) {
}
