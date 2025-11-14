package com.okebari.artbite.note.service.support;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import com.okebari.artbite.note.domain.NoteReminder;

/**
 * 캐시 접근을 인터페이스로 추상화해 두면,
 * Redis 세부 명령(SETNX, TTL, 파이프라인)을 유지하되
 * 향후 다른 캐시 구현으로 손쉽게 교체할 수 있다.
 */
public interface ReminderCacheClient {

	Optional<NoteReminderCacheValue> get(Long userId, LocalDate date);

	void save(NoteReminder reminder);

	void saveAll(Collection<NoteReminder> reminders);

	void evict(Long userId, LocalDate date);
}
