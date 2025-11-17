package com.okebari.artbite.note.dto.question;

import jakarta.validation.constraints.NotBlank;

public record NoteQuestionDto(
	Long id,
	@NotBlank String questionText
) {
}
