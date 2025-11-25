package com.okebari.artbite.note.dto.note;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.okebari.artbite.note.domain.NoteStatus;
import com.okebari.artbite.note.dto.question.NoteQuestionDto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "노트 생성 요청 DTO")
public record NoteCreateRequest(
	@Schema(description = "노트 상태 (IN_PROGRESS, COMPLETED, ARCHIVED 중 하나)", example = "IN_PROGRESS")
	@NotNull NoteStatus status,

	@Schema(description = "노트 태그", example = "디자인")
	@Size(max = 60) String tagText,

	@Schema(description = "노트 커버 정보")
	@Valid NoteCoverDto cover,

	@Schema(description = "노트 개요 정보")
	@Valid NoteOverviewDto overview,

	@Schema(description = "노트 회고 정보")
	@Valid NoteRetrospectDto retrospect,

	@Schema(description = "노트 제작 과정 정보 (반드시 2개 포함)")
	@Size(min = 2, max = 2) @Valid List<NoteProcessDto> processes,

	@Schema(description = "노트 질문 정보")
	@Valid NoteQuestionDto question,

	@Schema(description = "작가 ID", example = "1")
	@NotNull Long creatorId,

	@Schema(description = "외부 링크 정보")
	@Valid NoteExternalLinkDto externalLink
) {
}
