package com.okebari.artbite.note.dto.note;

import com.okebari.artbite.note.domain.NoteCategoryType;

import jakarta.validation.constraints.NotBlank;

public record NoteCoverDto(
	@NotBlank String title,
	@NotBlank String teaser,
	@NotBlank String mainImageUrl,
	String creatorName,
	String creatorJobTitle,
	NoteCategoryType category
) {
}
