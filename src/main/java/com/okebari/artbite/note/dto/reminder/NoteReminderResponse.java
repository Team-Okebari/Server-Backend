package com.okebari.artbite.note.dto.reminder;

import java.time.LocalDate;

import com.okebari.artbite.note.domain.NoteReminderPayload;
import com.okebari.artbite.note.domain.ReminderSourceType;

/**
 * 리마인드 배너 API 응답.
 */
public record NoteReminderResponse(
	SurfaceHint surfaceHint,
	Long noteId,
	String title,
	String mainImageUrl,
	ReminderSourceType sourceType,
	LocalDate reminderDate,
	boolean dismissed
) {

	public static NoteReminderResponse of(SurfaceHint surfaceHint, NoteReminderPayload payload,
		ReminderSourceType sourceType, LocalDate reminderDate, boolean dismissed) {
		return new NoteReminderResponse(
			surfaceHint,
			payload != null ? payload.getNoteId() : null,
			payload != null ? payload.getTitle() : null,
			payload != null ? payload.getMainImageUrl() : null,
			sourceType,
			reminderDate,
			dismissed
		);
	}
}
