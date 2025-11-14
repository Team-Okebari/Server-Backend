package com.okebari.artbite.note.service.support;

import java.time.LocalDate;

import com.okebari.artbite.note.domain.NoteReminder;
import com.okebari.artbite.note.domain.NoteReminderPayload;
import com.okebari.artbite.note.domain.ReminderSourceType;

public record NoteReminderCacheValue(
	Long id,
	Long userId,
	Long noteId,
	ReminderSourceType sourceType,
	LocalDate reminderDate,
	NoteReminderPayload payload,
	ReminderStateSnapshot state
) {

	public static NoteReminderCacheValue from(NoteReminder reminder) {
		return new NoteReminderCacheValue(
			reminder.getId(),
			reminder.getUserId(),
			reminder.getNoteId(),
			reminder.getSourceType(),
			reminder.getReminderDate(),
			reminder.getPayload(),
			ReminderStateSnapshot.from(reminder)
		);
	}

	public boolean hasFirstVisit() {
		return state.hasFirstVisit();
	}

	public boolean hasBannerSeen() {
		return state.hasBannerSeen();
	}

	public boolean dismissed() {
		return state.dismissed();
	}
}
