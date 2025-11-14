package com.okebari.artbite.note.scheduler;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.okebari.artbite.note.config.NoteReminderProperties;
import com.okebari.artbite.note.domain.NoteReminder;
import com.okebari.artbite.note.repository.NoteReminderRepository;
import com.okebari.artbite.note.service.support.NoteReminderSelector;
import com.okebari.artbite.note.service.support.NoteReminderSelector.ReminderCandidate;
import com.okebari.artbite.note.service.support.ReminderAlertNotifier;
import com.okebari.artbite.note.service.support.ReminderCacheClient;
import com.okebari.artbite.note.service.support.ReminderTargetUserReader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoteReminderScheduler {

	// 1) 모든 가입 사용자 ID를 스트리밍으로 가져오는 리더
	private final ReminderTargetUserReader targetUserReader;

	// 2) 사용자별 후보 노트 중에서 1건을 결정하는 셀렉터
	private final NoteReminderSelector selector;

	// 3) `note_reminder_pot` JPA 레포지토리 (23시 upsert, 00시 워밍)
	private final NoteReminderRepository reminderRepository;

	// 4) Redis 캐시 접근을 캡슐화한 포트 (save/get/evict)
	private final ReminderCacheClient cacheClient;

	// 5) 재시도 실패 시 Slack 등으로 알람을 보내는 노티파이어
	private final ReminderAlertNotifier alertNotifier;

	// 6) `note.reminder.*` 설정값(재시도 횟수, 청크 크기, Redis batch 등)
	private final NoteReminderProperties properties;

	// 7) KST 기준 시각 계산용 Clock (테스트 주입 가능)
	private final Clock clock;

	/**
	 * 23시: 다음날 노출분을 미리 확정한다.
	 */
	@Scheduled(cron = "0 0 23 * * *", zone = "Asia/Seoul")
	public void snapshotNextDay() {
		LocalDate targetDate = today().plusDays(1);
		// 정책상 “모든 사용자에게 리마인드 제공”이므로 전체 ID를 가져온다.
		List<Long> userIds = targetUserReader.fetchAllUserIds();
		log.info("[Reminder] snapshot start targetDate={} totalUsers={}", targetDate, userIds.size());
		runInChunks(userIds, chunk -> chunk.forEach(userId -> assignWithRetry(userId, targetDate)));
		log.info("[Reminder] snapshot completed targetDate={}", targetDate);
	}

	/**
	 * 00시: 당일 데이터를 Redis에 워밍한다.
	 */
	@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
	public void warmupTodayCache() {
		LocalDate today = today();
		log.info("[Reminder] cache warmup start date={}", today);
		try (Stream<NoteReminder> stream = reminderRepository.streamAllByReminderDate(today)) {
			List<NoteReminder> batch = new ArrayList<>();
			final int batchSize = Math.max(1, properties.getRedisBatchSize());
			stream.forEach(reminder -> {
				batch.add(reminder);
				if (batch.size() >= batchSize) {
					cacheClient.saveAll(List.copyOf(batch));
					batch.clear();
				}
			});
			if (!batch.isEmpty()) {
				cacheClient.saveAll(batch);
			}
		}
		log.info("[Reminder] cache warmup completed date={}", today);
	}

	private void assignWithRetry(Long userId, LocalDate targetDate) {
		int attempts = Math.max(1, properties.getMaxRetry());
		for (int attempt = 1; attempt <= attempts; attempt++) {
			try {
				selector.pickCandidate(userId, targetDate)
					.ifPresentOrElse(
						candidate -> upsertCandidate(userId, targetDate, candidate),
						() -> clearCandidate(userId, targetDate)
					);
				return;
			} catch (Exception e) {
				log.warn("Reminder pick failed userId={} attempt={}/{} date={}", userId, attempt, attempts, targetDate,
					e);
			}
		}
		alertNotifier.notifyFailure(
			properties.getAlarmChannel(),
			"Reminder pick 실패",
			"userId=%d date=%s".formatted(userId, targetDate)
		);
	}

	private void upsertCandidate(Long userId, LocalDate targetDate, ReminderCandidate candidate) {
		NoteReminder reminder = reminderRepository.findByUserIdAndReminderDate(userId, targetDate)
			.orElseGet(() -> NoteReminder.create(userId, targetDate,
				candidate.sourceType(), candidate.noteId(), candidate.payload()));

		if (reminder.getId() == null) {
			reminder = reminderRepository.save(reminder);
		} else {
			reminder.replaceCandidate(candidate.sourceType(), candidate.noteId(), candidate.payload());
			reminder = reminderRepository.save(reminder);
		}
		cacheClient.evict(userId, targetDate);
	}

	private void clearCandidate(Long userId, LocalDate date) {
		reminderRepository.deleteByUserIdAndReminderDate(userId, date);
		cacheClient.evict(userId, date);
	}

	private void runInChunks(List<Long> userIds, Consumer<List<Long>> chunkConsumer) {
		if (userIds.isEmpty()) {
			return;
		}
		int chunkSize = Math.max(1, properties.getChunkSize());
		for (int start = 0; start < userIds.size(); start += chunkSize) {
			int end = Math.min(userIds.size(), start + chunkSize);
			List<Long> chunk = userIds.subList(start, end);
			chunkConsumer.accept(chunk);
		}
	}

	private LocalDate today() {
		return LocalDate.now(clock);
	}
}
