package com.okebari.artbite.note.dto.note;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.okebari.artbite.creator.dto.CreatorSummaryDto;
import com.okebari.artbite.note.domain.NoteStatus;
import com.okebari.artbite.note.dto.answer.NoteAnswerResponse;
import com.okebari.artbite.note.dto.question.NoteQuestionDto;

public record NoteResponse(
	Long id,
	NoteStatus status,
	String tagText,
	NoteCoverResponse cover,
	NoteOverviewDto overview,
	NoteRetrospectDto retrospect,
	List<NoteProcessDto> processes,
	NoteQuestionDto question,
	NoteAnswerResponse answer,
	Long creatorId,
	String creatorJobTitle,
	NoteExternalLinkDto externalLink,
	CreatorSummaryDto creator,
	LocalDate publishedAt,
	LocalDateTime archivedAt,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	Boolean isBookmarked
) {
}
