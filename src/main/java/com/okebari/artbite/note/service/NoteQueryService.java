package com.okebari.artbite.note.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.okebari.artbite.common.exception.NoteNotFoundException;
import com.okebari.artbite.note.domain.Note;
import com.okebari.artbite.note.domain.NoteStatus;
import com.okebari.artbite.note.dto.note.NoteCoverResponse;
import com.okebari.artbite.note.dto.note.NotePreviewResponse;
import com.okebari.artbite.note.dto.note.NoteResponse;
import com.okebari.artbite.note.dto.note.TodayPublishedResponse;
import com.okebari.artbite.note.exception.NoteAccessDeniedException;
import com.okebari.artbite.note.exception.NoteInvalidStatusException;
import com.okebari.artbite.note.dto.summary.ArchivedNoteSummaryResponse;
import com.okebari.artbite.note.mapper.NoteMapper;
import com.okebari.artbite.note.repository.NoteRepository;

import lombok.RequiredArgsConstructor;

/**
 * 메인/지난 노트 조회 시나리오를 담당하는 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteQueryService {

	private static final int OVERVIEW_PREVIEW_LIMIT = 100;
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final NoteRepository noteRepository;
	private final NoteMapper noteMapper;
	private final SubscriptionService subscriptionService;

	/**
	 * 무료 사용자에게 제공하는 노트 미리보기.
	 * 개요 본문은 100자까지만 잘라서 내려준다.
	 */
	public NotePreviewResponse getTodayPreview() {
		Note note = findTodayPublishedNote();
		return noteMapper.toPreview(note, OVERVIEW_PREVIEW_LIMIT);
	}

	/**
	 * 메인 화면(온보딩 이후)에 노출할 금일 게시 노트의 커버 정보를 제공한다.
	 */
	public NoteCoverResponse getTodayCover() {
		return noteMapper.toCoverResponse(findTodayPublishedNote());
	}

	/**
	 * 유료 구독자를 위한 금일 게시 노트 상세.
	 */
 	public TodayPublishedResponse getTodayPublishedDetail(Long userId) {
 		Note note = findTodayPublishedNote();
 		boolean accessible = subscriptionService.isActiveSubscriber(userId);
 		if (!accessible) {
 			NotePreviewResponse preview = noteMapper.toPreview(note, OVERVIEW_PREVIEW_LIMIT);
 			return new TodayPublishedResponse(false, null, preview);
 		}
 		return new TodayPublishedResponse(true, noteMapper.toResponse(note), null);
 	}

	/**
	 * 지난 노트(ARCHIVED) 목록을 검색 조건과 함께 조회한다.
	 */
	public Page<ArchivedNoteSummaryResponse> getArchivedNoteList(String keyword, Pageable pageable) {
		Page<Note> page = (keyword == null || keyword.isBlank())
			? noteRepository.findAllArchived(pageable)
			: noteRepository.searchArchived(keyword, pageable);
		return page.map(note -> noteMapper.toArchivedSummary(note));
	}

	/**
	 * 유료 구독자 전용으로 아카이브 상세 내용을 제공한다.
	 */
	public NoteResponse getArchivedNoteDetail(Long noteId, Long userId) {
		if (!subscriptionService.isActiveSubscriber(userId)) {
			throw new NoteAccessDeniedException("유료 구독자만 지난 노트 상세를 열람할 수 있습니다.");
		}

		Note note = noteRepository.findById(noteId)
			.orElseThrow(() -> new NoteNotFoundException(noteId));
		if (note.getStatus() != NoteStatus.ARCHIVED) {
			throw new NoteInvalidStatusException("해당 노트는 아카이브 상태가 아닙니다.");
		}

		return noteMapper.toResponse(note);
	}

	private Note findTodayPublishedNote() {
		LocalDate today = LocalDate.now(KST);
		LocalDateTime start = today.atStartOfDay();
		LocalDateTime end = start.plusDays(1);
		return noteRepository.findFirstByStatusAndPublishedAtBetween(
			NoteStatus.PUBLISHED,
			start,
			end
		).orElseThrow(() -> new NoteNotFoundException("오늘 게시된 노트가 없습니다."));
	}
}
