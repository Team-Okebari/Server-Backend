package com.okebari.artbite.note.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.okebari.artbite.creator.domain.Creator;
import com.okebari.artbite.creator.dto.CreatorSummaryDto;
import com.okebari.artbite.creator.mapper.CreatorMapper;
import com.okebari.artbite.note.domain.Note;
import com.okebari.artbite.note.domain.NoteAnswer;
import com.okebari.artbite.note.domain.NoteBookmark;
import com.okebari.artbite.note.domain.NoteCategoryType;
import com.okebari.artbite.note.domain.NoteCover;
import com.okebari.artbite.note.domain.NoteOverview;
import com.okebari.artbite.note.domain.NoteProcess;
import com.okebari.artbite.note.domain.NoteQuestion;
import com.okebari.artbite.note.domain.NoteRetrospect;
import com.okebari.artbite.note.dto.answer.NoteAnswerDto;
import com.okebari.artbite.note.dto.answer.NoteAnswerResponse;
import com.okebari.artbite.note.dto.bookmark.NoteBookmarkResponse;
import com.okebari.artbite.note.dto.note.CategoryBadgeResponse;
import com.okebari.artbite.note.dto.note.NoteCoverDto;
import com.okebari.artbite.note.dto.note.NoteCoverResponse;
import com.okebari.artbite.note.dto.note.NoteCreateRequest;
import com.okebari.artbite.note.dto.note.NoteExternalLinkDto;
import com.okebari.artbite.note.dto.note.NoteOverviewDto;
import com.okebari.artbite.note.dto.note.NotePreviewResponse;
import com.okebari.artbite.note.dto.note.NoteProcessDto;
import com.okebari.artbite.note.dto.note.NoteResponse;
import com.okebari.artbite.note.dto.note.NoteRetrospectDto;
import com.okebari.artbite.note.dto.question.NoteQuestionDto;
import com.okebari.artbite.note.dto.summary.ArchivedNoteSummaryResponse;

import lombok.RequiredArgsConstructor;

/**
 * 노트 도메인 ↔ DTO 간 변환을 전담하는 매퍼.
 * 서비스 계층에서는 이 클래스를 통해 일관된 데이터 구조를 유지한다.
 */
@Component
@RequiredArgsConstructor
public class NoteMapper {

	private final CreatorMapper creatorMapper;

	// ADMIN 입력 DTO를 받아 하위 DTO(cover/overview/process/question)를
	// 각각 엔티티로 변환하고 연관관계를 모두 세팅한다.
	public Note toEntity(NoteCreateRequest request) {
		NoteExternalLinkDto linkDto = request.externalLink();
		Note note = Note.builder()
			.status(request.status())
			.tagText(request.tagText())
			.sourceUrl(linkDto != null ? linkDto.sourceUrl() : null)
			.build();

		note.assignCover(toCover(request.cover()));
		note.assignOverview(toOverview(request.overview()));
		note.assignRetrospect(toRetrospect(request.retrospect()));
		note.replaceProcesses(toProcesses(request.processes()));

		NoteQuestion question = toQuestion(request.question());
		if (question != null) {
			note.assignQuestion(question);
		}

		return note;
	}

	// Note 엔티티를 단건 조회 응답 DTO로 변환한다.
	public NoteResponse toResponse(Note note, NoteAnswerDto userAnswer) {
		return buildNoteResponse(note, false, userAnswer, false);
	}

	public NoteResponse toResponseWithCoverCategory(Note note, NoteAnswerDto userAnswer, Boolean isBookmarked) {
		return buildNoteResponse(note, true, userAnswer, isBookmarked);
	}

	private NoteResponse buildNoteResponse(Note note, boolean includeCategory, NoteAnswerDto userAnswer,
		Boolean isBookmarked) {
		Creator creator = note.getCreator();
		CreatorSummaryDto creatorSummary = creator != null ? creatorMapper.toSummary(creator) : null;
		Long creatorId = creator != null ? creator.getId() : null;
		String creatorJobTitle = creator != null ? creator.getJobTitle() : null;
		NoteExternalLinkDto externalLink = note.getSourceUrl() != null
			? new NoteExternalLinkDto(note.getSourceUrl())
			: null;

		return new NoteResponse(
			note.getId(),
			note.getStatus(),
			note.getTagText(),
			buildCoverResponse(note, false, includeCategory),
			toOverviewDto(note.getOverview()),
			toRetrospectDto(note.getRetrospect()),
			note.getProcesses() != null
				? note.getProcesses().stream()
				.map(process -> this.toProcessDto(process))
				.toList()
				: List.of(),
			toQuestionDto(note.getQuestion()),
			toAnswerResponse(userAnswer),
			creatorId,
			creatorJobTitle,
			externalLink,
			creatorSummary,
			toDateOnly(note.getPublishedAt()),
			note.getArchivedAt(),
			note.getCreatedAt(),
			note.getUpdatedAt(),
			isBookmarked
		);
	}

	public NoteResponse toResponseForAdmin(Note note) {
		Creator creator = note.getCreator();
		CreatorSummaryDto creatorSummary = creator != null ? creatorMapper.toSummary(creator) : null;
		Long creatorId = creator != null ? creator.getId() : null;
		String creatorJobTitle = creator != null ? creator.getJobTitle() : null;
		NoteExternalLinkDto externalLink = note.getSourceUrl() != null
			? new NoteExternalLinkDto(note.getSourceUrl())
			: null;

		return new NoteResponse(
			note.getId(),
			note.getStatus(),
			note.getTagText(),
			buildCoverResponse(note, true, true), // includeTeaser = true, includeCategory = true
			toOverviewDto(note.getOverview()),
			toRetrospectDto(note.getRetrospect()),
			note.getProcesses() != null
				? note.getProcesses().stream()
				.map(this::toProcessDto)
				.toList()
				: List.of(),
			toQuestionDto(note.getQuestion()),
			null, // No user answer for admin
			creatorId,
			creatorJobTitle,
			externalLink,
			creatorSummary,
			toDateOnly(note.getPublishedAt()),
			note.getArchivedAt(),
			note.getCreatedAt(),
			note.getUpdatedAt(),
			false // isBookmarked for admin
		);
	}

	// 무료 사용자 미리보기 용도로 커버/요약 텍스트만 추린다.
	public NotePreviewResponse toPreview(Note note, int overviewLimit) {
		return buildPreview(note, overviewLimit, false, false);
	}

	public NotePreviewResponse toPreviewWithCategory(Note note, int overviewLimit, Boolean isBookmarked) {
		return buildPreview(note, overviewLimit, true, isBookmarked);
	}

	private NotePreviewResponse buildPreview(Note note, int overviewLimit, boolean includeCategory,
		Boolean isBookmarked) {
		NoteOverviewDto overview = toOverviewDto(note.getOverview());
		if (overview != null && overview.bodyText() != null && overview.bodyText().length() > overviewLimit) {
			overview = new NoteOverviewDto(
				overview.sectionTitle(),
				overview.bodyText().substring(0, overviewLimit),
				overview.imageUrl()
			);
		}

		return new NotePreviewResponse(
			note.getId(),
			buildCoverResponse(note, false, includeCategory),
			overview,
			isBookmarked
		);
	}

	// 지난 노트 목록(ARCHIVED) 조회용 요약 DTO를 만든다.
	public ArchivedNoteSummaryResponse toArchivedSummary(Note note) {
		NoteCover cover = note.getCover();
		Creator creator = note.getCreator();
		return new ArchivedNoteSummaryResponse(
			note.getId(),
			note.getTagText(),
			cover != null ? cover.getTitle() : null,
			cover != null ? cover.getMainImageUrl() : null,
			creator != null ? creator.getName() : null,
			note.getPublishedAt() != null ? note.getPublishedAt().toLocalDate() : null
		);
	}

	// 북마크 카드에서 바로 렌더링할 수 있도록 제목/이미지/작가명만 추려 전달한다.
	public NoteBookmarkResponse toBookmarkResponse(NoteBookmark bookmark) {
		Note note = bookmark.getNote();
		NoteCover cover = note.getCover();
		Creator creator = note.getCreator();
		return new NoteBookmarkResponse(
			bookmark.getId(),
			note.getId(),
			cover != null ? cover.getTitle() : null,
			cover != null ? cover.getMainImageUrl() : null,
			note.getTagText(),
			creator != null ? creator.getName() : null,
			bookmark.getCreatedAt()
		);
	}

	// NoteAnswer → 서비스 내부 DTO. 테스트 및 권한 검증에서 사용한다.
	public NoteAnswerDto toAnswerDto(NoteAnswer answer) {
		if (answer == null) {
			return null;
		}
		return new NoteAnswerDto(
			answer.getId(),
			answer.getQuestion() != null ? answer.getQuestion().getId() : null,
			answer.getRespondent() != null ? answer.getRespondent().getId() : null,
			answer.getAnswerText()
		);
	}

	// NoteAnswer → 프론트 응답 DTO. answerText만 싣는다.
	public NoteAnswerResponse toAnswerResponse(NoteAnswerDto answerDto) {
		if (answerDto == null) {
			return null;
		}
		return new NoteAnswerResponse(answerDto.answerText());
	}

	// 표지 DTO → 엔티티 변환.
	public NoteCover toCover(NoteCoverDto dto) {
		if (dto == null) {
			return null;
		}
		return NoteCover.builder()
			.title(dto.title())
			.teaser(dto.teaser())
			.mainImageUrl(dto.mainImageUrl())
			.category(dto.category())
			.build();
	}

	public NoteCoverResponse toCoverResponse(Note note) {
		return buildCoverResponse(note, true);
	}

	// 개요(overview) DTO → 엔티티 변환.
	public NoteOverview toOverview(NoteOverviewDto dto) {
		if (dto == null) {
			return null;
		}
		return NoteOverview.builder()
			.sectionTitle(dto.sectionTitle())
			.bodyText(dto.bodyText())
			.imageUrl(dto.imageUrl())
			.build();
	}

	// 회고(retrospect) DTO → 엔티티 변환.
	public NoteRetrospect toRetrospect(NoteRetrospectDto dto) {
		if (dto == null) {
			return null;
		}
		return NoteRetrospect.builder()
			.sectionTitle(dto.sectionTitle())
			.bodyText(dto.bodyText())
			.build();
	}

	// 제작 과정 DTO 목록을 엔티티 목록으로 변환한다.
	public List<NoteProcess> toProcesses(List<NoteProcessDto> dtos) {
		if (dtos == null) {
			return List.of();
		}
		return dtos.stream()
			.map(this::toProcess)
			.toList();
	}

	public NoteProcess toProcess(NoteProcessDto dto) {
		if (dto == null) {
			return null;
		}
		return NoteProcess.builder()
			.position(dto.position())
			.sectionTitle(dto.sectionTitle())
			.bodyText(dto.bodyText())
			.imageUrl(dto.imageUrl())
			.build();
	}

	// 질문 DTO → 질문 엔티티. 질문 텍스트만 세팅한다.
	public NoteQuestion toQuestion(NoteQuestionDto dto) {
		if (dto == null) {
			return null;
		}
		return NoteQuestion.builder()
			.questionText(dto.questionText())
			.build();
	}

	// 개요 엔티티 → DTO 변환.
	private NoteOverviewDto toOverviewDto(NoteOverview overview) {
		if (overview == null) {
			return null;
		}
		return new NoteOverviewDto(overview.getSectionTitle(), overview.getBodyText(), overview.getImageUrl());
	}

	// 회고 엔티티 → DTO 변환.
	private NoteRetrospectDto toRetrospectDto(NoteRetrospect retrospect) {
		if (retrospect == null) {
			return null;
		}
		return new NoteRetrospectDto(retrospect.getSectionTitle(), retrospect.getBodyText());
	}

	// 제작 과정 엔티티 → DTO 변환.
	private NoteProcessDto toProcessDto(NoteProcess process) {
		return new NoteProcessDto(process.getId().getPosition(), process.getSectionTitle(),
			process.getBodyText(), process.getImageUrl());
	}

	// 질문 엔티티 → 질문 DTO. 답변 내용은 별도 응답 필드(answer)로 분리된다.
	private NoteQuestionDto toQuestionDto(NoteQuestion question) {
		if (question == null) {
			return null;
		}
		return new NoteQuestionDto(question.getId(), question.getQuestionText());
	}

	private NoteCoverResponse buildCoverResponse(Note note, boolean includeTeaser) {
		return buildCoverResponse(note, includeTeaser, false);
	}

	private NoteCoverResponse buildCoverResponse(Note note, boolean includeTeaser, boolean includeCategory) {
		NoteCover cover = note.getCover();
		String creatorName = note.getCreator() != null ? note.getCreator().getName() : null;
		String creatorJobTitle = note.getCreator() != null ? note.getCreator().getJobTitle() : null;
		String teaser = cover != null && includeTeaser ? cover.getTeaser() : null;

		CategoryBadgeResponse categoryBadge = null;
		if (includeCategory) {
			categoryBadge = toCategoryBadge(cover);
			if (categoryBadge == null) { // If toCategoryBadge returned null (meaning cover was null), default to NONE
				categoryBadge = new CategoryBadgeResponse(NoteCategoryType.NONE.name(),
					NoteCategoryType.NONE.getLabel());
			}
		}

		return new NoteCoverResponse(
			cover != null ? cover.getTitle() : null,
			teaser,
			cover != null ? cover.getMainImageUrl() : null,
			creatorName,
			creatorJobTitle,
			toDateOnly(note.getPublishedAt()),
			categoryBadge // Use the potentially defaulted categoryBadge
		);
	}

	private CategoryBadgeResponse toCategoryBadge(NoteCover cover) {
		if (cover == null) {
			return null; // No cover, so no category badge
		}
		NoteCategoryType categoryType = cover.getCategory();
		if (categoryType == null) {
			categoryType = NoteCategoryType.NONE; // Default to NONE if category is null
		}
		return new CategoryBadgeResponse(categoryType.name(), categoryType.getLabel());
	}

	private LocalDate toDateOnly(LocalDateTime dateTime) {
		return dateTime != null ? dateTime.toLocalDate() : null;
	}
}
