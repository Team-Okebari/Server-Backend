package com.okebari.artbite.note.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.okebari.artbite.common.exception.ReminderNotFoundException;
import com.okebari.artbite.note.domain.NoteReminder;
import com.okebari.artbite.note.dto.reminder.NoteReminderResponse;
import com.okebari.artbite.note.dto.reminder.SurfaceHint;
import com.okebari.artbite.note.mapper.NoteReminderMapper;
import com.okebari.artbite.note.repository.NoteReminderRepository;
import com.okebari.artbite.note.service.support.ReminderCacheClient;
import com.okebari.artbite.note.service.support.NoteReminderCacheValue;
import com.okebari.artbite.note.service.support.ReminderStateMachine;
import com.okebari.artbite.note.service.support.ReminderStateSnapshot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NoteReminderService {

	private final NoteReminderRepository reminderRepository;
	// 공통 캐시 추상화를 주입받는다. (Redis 외 다른 구현으로도 교체 가능)
	private final ReminderCacheClient cacheClient;
	private final ReminderStateMachine reminderStateMachine;
	private final NoteReminderMapper reminderMapper;
	private final Clock clock;

	/**
	 * GET /api/notes/reminder/today
	 * - 1) Redis에서 오늘자 리마인드 데이터를 찾고, 없으면 DB에서 가져온 뒤 캐시에 저장한다.
	 * - 2) 상태 전이를 수행(첫 방문/두 번째 방문/숨김)하고 SurfaceHint에 맞는 응답을 반환한다.
	 */
	public Optional<NoteReminderResponse> getTodayReminder(Long userId) {
		LocalDate today = today();
		NoteReminderCacheValue cached = fetchCacheSafely(userId, today);
		if (cached == null) {
			NoteReminder reminder = reminderRepository.findByUserIdAndReminderDate(userId, today)
				.orElse(null);
			if (reminder == null) {
				return Optional.empty();
			}
			tryCacheSave(reminder);
			return Optional.of(applyStateTransition(reminder));
		}
		return Optional.of(applyStateTransition(cached, userId, today));
	}

	/**
	 * POST /api/notes/reminder/dismiss
	 * - “오늘은 그만 보기”를 선택했을 때 호출.
	 * - DB와 캐시를 모두 dismissed=true 로 갱신해 당일 배너를 숨긴다.
	 */
	public void dismissToday(Long userId) {
		LocalDate today = today();
		NoteReminder reminder = loadReminder(userId, today);
		reminder.dismiss(null, now());
		persistAndCache(reminder);
	}

	/**
	 * (선택) X 버튼 모달에서 ‘취소’를 눌렀다는 기록만 남기는 API.
	 */
	public void markModalClosed(Long userId) {
		LocalDate today = today();
		NoteReminder reminder = loadReminder(userId, today);
		reminder.markModalClosed(now());
		persistAndCache(reminder);
	}

	private NoteReminderResponse applyStateTransition(NoteReminder reminder) {
		ReminderStateSnapshot snapshot = ReminderStateSnapshot.from(reminder);
		ReminderStateMachine.TransitionDecision decision = reminderStateMachine.decide(snapshot);
		return executeStateAction(reminder, decision);
	}

	private NoteReminderResponse applyStateTransition(NoteReminderCacheValue cacheValue, Long userId, LocalDate date) {
		ReminderStateMachine.TransitionDecision decision = reminderStateMachine.decide(cacheValue);
		if (!decision.requiresPersistence()) {
			return reminderMapper.toResponse(cacheValue, decision.hint());
		}
		NoteReminder reminder = loadReminder(userId, date);
		return executeStateAction(reminder, decision);
	}

	private NoteReminderResponse executeStateAction(NoteReminder reminder,
		ReminderStateMachine.TransitionDecision decision) {
		return switch (decision.action()) {
			case MARK_FIRST_VISIT -> {
				reminder.markFirstVisit(now());
				NoteReminder saved = persistAndCache(reminder);
				yield reminderMapper.toResponse(saved, decision.hint());
			}
			case MARK_BANNER_SEEN -> {
				reminder.markBannerSeen(now());
				NoteReminder saved = persistAndCache(reminder);
				yield reminderMapper.toResponse(saved, decision.hint());
			}
			case NONE -> reminderMapper.toResponse(reminder, decision.hint());
		};
	}

	/**
	 * 사용자와 날짜로 DB에서 리마인드 엔티티를 가져온다.
	 * 없으면 404(ReminderNotFoundException)로 처리.
	 */
	private NoteReminder loadReminder(Long userId, LocalDate date) {
		return reminderRepository.findByUserIdAndReminderDate(userId, date)
			.orElseThrow(() -> new ReminderNotFoundException(userId, date));
	}

	private NoteReminder persistAndCache(NoteReminder reminder) {
		NoteReminder saved = reminderRepository.save(reminder);
		tryCacheSave(saved);
		return saved;
	}

	private NoteReminderCacheValue fetchCacheSafely(Long userId, LocalDate date) {
		try {
			return cacheClient.get(userId, date).orElse(null);
		} catch (RuntimeException ex) {
			// Q7 응답: 캐시 장애 시 DB만으로도 응답할 수 있도록 로그만 남기고 흐름을 이어간다.
			log.warn("Failed to load reminder cache userId={} date={}", userId, date, ex);
			return null;
		}
	}

	private void tryCacheSave(NoteReminder reminder) {
		try {
			cacheClient.save(reminder);
		} catch (RuntimeException ex) {
			log.warn("Failed to write reminder cache userId={} date={}", reminder.getUserId(),
				reminder.getReminderDate(), ex);
		}
	}

	private LocalDate today() {
		return LocalDate.now(clock);
	}

	private LocalDateTime now() {
		return LocalDateTime.now(clock);
	}
}
