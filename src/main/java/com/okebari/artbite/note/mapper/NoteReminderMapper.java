package com.okebari.artbite.note.mapper;

import org.springframework.stereotype.Component;

import com.okebari.artbite.note.domain.Note;
import com.okebari.artbite.note.domain.NoteCover;
import com.okebari.artbite.note.domain.NoteReminder;
import com.okebari.artbite.note.domain.NoteReminderPayload;
import com.okebari.artbite.note.dto.reminder.NoteReminderResponse;
import com.okebari.artbite.note.dto.reminder.SurfaceHint;
import com.okebari.artbite.note.service.support.NoteReminderCacheValue;

@Component
public class NoteReminderMapper {

	public NoteReminderPayload toPayload(Note note) {
		NoteCover cover = note.getCover();
		return NoteReminderPayload.builder()
			.noteId(note.getId())
			.title(cover != null ? cover.getTitle() : null)
			.mainImageUrl(cover != null ? cover.getMainImageUrl() : null)
			.build();
	}

	/**
	 * DB에서 막 조회한 `NoteReminder` 엔티티를 바로 응답으로 내보낼 때 사용.
	 * (예: 캐시에 값이 없어서 DB에서 읽은 뒤, 상태 전이를 수행하고 다시 저장할 때)
	 */
	public NoteReminderResponse toResponse(NoteReminder reminder, SurfaceHint hint) {
		return NoteReminderResponse.of(
			hint,
			reminder.getPayload(),
			reminder.getSourceType(),
			reminder.getReminderDate(),
			reminder.isDismissed()
		);
	}

	/**
	 * Redis 캐시에서 꺼낸 스냅샷(`NoteReminderCacheValue`)을 응답으로 만들 때 사용.
	 * 캐시를 먼저 확인해 빠르게 응답할 때 호출된다.
	 */
	public NoteReminderResponse toResponse(NoteReminderCacheValue cacheValue, SurfaceHint hint) {
		return NoteReminderResponse.of(
			hint,
			cacheValue.payload(),
			cacheValue.sourceType(),
			cacheValue.reminderDate(),
			cacheValue.dismissed()
		);
	}
}
