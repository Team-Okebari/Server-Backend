package com.okebari.artbite.note.dto.note;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.okebari.artbite.note.domain.NoteStatus;
import com.okebari.artbite.note.dto.question.NoteQuestionDto;

public record NoteCreateRequest(
	@NotNull NoteStatus status,
	@Size(max = 60) String tagText,
	@Valid NoteCoverDto cover,
	@Valid NoteOverviewDto overview,
	@Valid NoteRetrospectDto retrospect,
	@Size(min = 2, max = 2) @Valid List<NoteProcessDto> processes,
	@Valid NoteQuestionDto question,
	@NotNull Long creatorId,
	@Valid NoteExternalLinkDto externalLink
) {
}
