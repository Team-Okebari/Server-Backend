package com.okebari.artbite.note.dto.note;

import jakarta.validation.constraints.NotBlank;

import com.okebari.artbite.note.domain.NoteCategoryType;

public record NoteCoverDto(
	@NotBlank String title,
	@NotBlank String teaser,
	@NotBlank String mainImageUrl,
	String creatorName,
	String creatorJobTitle,
	NoteCategoryType category
) {
}
