package com.okebari.artbite.note.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.okebari.artbite.common.exception.BusinessException;
import com.okebari.artbite.common.exception.ErrorCode;
import com.okebari.artbite.common.exception.NoteInvalidStatusException;
import com.okebari.artbite.common.exception.NoteNotFoundException;
import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.domain.user.UserRepository;
import com.okebari.artbite.note.domain.Note;
import com.okebari.artbite.note.domain.NoteStatus;
import com.okebari.artbite.note.dto.answer.NoteAnswerDto;
import com.okebari.artbite.note.dto.note.ArchivedNoteViewResponse;
import com.okebari.artbite.note.dto.note.NoteCoverResponse;
import com.okebari.artbite.note.dto.note.NotePreviewResponse;
import com.okebari.artbite.note.dto.note.TodayPublishedResponse;
import com.okebari.artbite.note.dto.summary.ArchivedNoteSummaryResponse;
import com.okebari.artbite.note.mapper.NoteMapper;
import com.okebari.artbite.note.repository.NoteBookmarkRepository;
import com.okebari.artbite.note.repository.NoteRepository;
import com.okebari.artbite.tracking.service.ContentAccessLogService;

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
	private final UserRepository userRepository;
	private final NoteBookmarkRepository noteBookmarkRepository;
	private final NoteMapper noteMapper;
	private final SubscriptionService subscriptionService;
	private final NoteAnswerService noteAnswerService;
	private final ContentAccessLogService contentAccessLogService;

	/**
	 * 로그인 사용자에게 제공하는 노트 미리보기.
	 * 개요 본문은 100자까지만 잘라서 내려준다.
	 */
	public NotePreviewResponse getTodayPreview(Long userId) {
		Note note = findTodayPublishedNote();
		boolean isBookmarked = noteBookmarkRepository.findByNoteIdAndUserId(note.getId(), userId).isPresent();
		return noteMapper.toPreviewWithCategory(note, OVERVIEW_PREVIEW_LIMIT, isBookmarked);
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
		boolean isBookmarked = noteBookmarkRepository.findByNoteIdAndUserId(note.getId(), userId).isPresent();
		boolean accessible = subscriptionService.isActiveSubscriber(userId);

		if (!accessible) {
			NotePreviewResponse preview = noteMapper.toPreviewWithCategory(note, OVERVIEW_PREVIEW_LIMIT, isBookmarked);
			return new TodayPublishedResponse(false, null, preview);
		}

		// 유료 콘텐츠 접근 기록
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));
		contentAccessLogService.logNoteAccess(user, note);

		// Fetch user's answer if question exists
		NoteAnswerDto userAnswer = null;
		if (note.getQuestion() != null) {
			userAnswer = noteAnswerService.getAnswer(note.getQuestion().getId(), userId);
		}
		return new TodayPublishedResponse(true, noteMapper.toResponseWithCoverCategory(note, userAnswer, isBookmarked),
			null);
	}

	/**
	 * 지난 노트(PUBLISHED, ARCHIVED) 목록을 검색 조건과 함께 조회한다.
	 */
	public Page<ArchivedNoteSummaryResponse> getArchivedNoteList(String keyword, Pageable pageable) {
		List<NoteStatus> statuses = List.of(NoteStatus.PUBLISHED, NoteStatus.ARCHIVED);
		Page<Note> page = (keyword == null || keyword.isBlank())
			? noteRepository.findAllByStatusIn(statuses, pageable)
			: noteRepository.searchByStatusIn(keyword, statuses, pageable);
		return page.map(note -> noteMapper.toArchivedSummary(note));
	}

	/**
	 * 구독 상태에 따라 지난 노트 전체/프리뷰를 제공한다.
	 * PUBLISHED, ARCHIVED 상태의 노트를 조회할 수 있다.
	 */
	public ArchivedNoteViewResponse getArchivedNoteView(Long noteId, Long userId) {
		Note note = noteRepository.findById(noteId)
			.orElseThrow(() -> new NoteNotFoundException(noteId));

		if (note.getStatus() != NoteStatus.ARCHIVED && note.getStatus() != NoteStatus.PUBLISHED) {
			throw new NoteInvalidStatusException("해당 노트는 열람할 수 있는 상태가 아닙니다.");
		}

		boolean isBookmarked = noteBookmarkRepository.findByNoteIdAndUserId(noteId, userId).isPresent();
		boolean subscribed = subscriptionService.isActiveSubscriber(userId);

		if (subscribed) {
			// 유료 콘텐츠 접근 기록
			User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));
			contentAccessLogService.logNoteAccess(user, note);

			NoteAnswerDto userAnswer = null;
			if (note.getQuestion() != null) {
				userAnswer = noteAnswerService.getAnswer(note.getQuestion().getId(), userId);
			}
			return new ArchivedNoteViewResponse(true,
				noteMapper.toResponseWithCoverCategory(note, userAnswer, isBookmarked), null);
		}
		NotePreviewResponse preview = noteMapper.toPreviewWithCategory(note, OVERVIEW_PREVIEW_LIMIT, isBookmarked);
		return new ArchivedNoteViewResponse(false, null, preview);
	}

	private Note findTodayPublishedNote() {
		LocalDate today = LocalDate.now(KST);
		LocalDateTime start = today.atStartOfDay();
		LocalDateTime end = start.plusDays(1);
		return noteRepository.findFirstByStatusAndPublishedAtBetweenOrderByPublishedAtDesc(
			NoteStatus.PUBLISHED,
			start,
			end
		).orElseThrow(() -> new NoteNotFoundException("오늘 게시된 노트가 없습니다."));
	}
}

