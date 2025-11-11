package com.okebari.artbite.note.scheduler;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.okebari.artbite.note.domain.Note;
import com.okebari.artbite.note.repository.NoteRepository;

import lombok.RequiredArgsConstructor;

/**
 * 노트 상태를 자정에 자동 전환하는 스케줄러.
 */
@Component
@RequiredArgsConstructor
public class NoteStatusScheduler {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final NoteRepository noteRepository;

	/**
	 * 매일 자정에 작성 완료 노트 중 가장 오래된 한 건을 게시 상태로 전환한다.
	 */
	@Transactional
	@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
	public void publishCompletedNotes() {
		List<Note> candidates = noteRepository.findCompletedOrderByUpdatedAtAsc();
		if (candidates.isEmpty()) {
			return;
		}
		Note noteToPublish = candidates.get(0);
		noteToPublish.markPublished(LocalDateTime.now(KST));
	}

	/**
	 * 게시 후 24시간이 지난 노트를 아카이브 상태로 전환한다.
	 */
	@Transactional
	@Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
	public void archiveExpiredNotes() {
		LocalDateTime now = LocalDateTime.now(KST);
		LocalDateTime threshold = now.minusHours(24);
		noteRepository.findPublishedBefore(threshold)
			.forEach(note -> note.markArchived(now));
	}
}
