package com.okebari.artbite.note.dto.note;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.okebari.artbite.creator.dto.CreatorSummaryDto;
import com.okebari.artbite.note.domain.NoteStatus;
import com.okebari.artbite.note.dto.answer.NoteAnswerResponse;
import com.okebari.artbite.note.dto.question.NoteQuestionDto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "노트 상세 응답 DTO")
public record NoteResponse(
	@Schema(description = "노트 ID", example = "1")
	Long id,
	@Schema(description = "노트 상태", example = "COMPLETED")
	NoteStatus status,
	@Schema(description = "노트 태그", example = "디자인")
	String tagText,
	@Schema(description = "노트 커버 정보")
	NoteCoverResponse cover,
	@Schema(description = "노트 개요 정보")
	NoteOverviewDto overview,
	@Schema(description = "노트 회고 정보")
	NoteRetrospectDto retrospect,
	@Schema(description = "노트 제작 과정 정보")
	List<NoteProcessDto> processes,
	@Schema(description = "노트 질문 정보")
	NoteQuestionDto question,
	@Schema(description = "노트 답변 정보")
	NoteAnswerResponse answer,
	@Schema(description = "작가 ID", example = "1")
	Long creatorId,
	@Schema(description = "작가 직함", example = "일러스트레이터")
	String creatorJobTitle,
	@Schema(description = "외부 링크 정보")
	NoteExternalLinkDto externalLink,
	@Schema(description = "작가 요약 정보")
	CreatorSummaryDto creator,
	@Schema(description = "발행일", example = "2025-11-25")
	LocalDate publishedAt,
	@Schema(description = "아카이브된 시간", example = "2025-12-01T10:00:00")
	LocalDateTime archivedAt,
	@Schema(description = "생성일", example = "2025-11-01T10:00:00")
	LocalDateTime createdAt,
	@Schema(description = "최종 수정일", example = "2025-11-25T10:00:00")
	LocalDateTime updatedAt,
	@Schema(description = "북마크 여부", example = "true")
	Boolean isBookmarked
) {
}
