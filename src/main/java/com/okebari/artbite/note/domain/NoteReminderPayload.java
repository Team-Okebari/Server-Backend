package com.okebari.artbite.note.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NoteReminderPayload {

	@Column(name = "payload_note_id")
	private Long noteId;

	@Column(name = "payload_title", length = 60)
	private String title;

	@Column(name = "payload_main_image_url", length = 500)
	private String mainImageUrl;

	public NoteReminderPayload copyWithNoteId(Long noteId) {
		return NoteReminderPayload.builder()
			.noteId(noteId)
			.title(this.title)
			.mainImageUrl(this.mainImageUrl)
			.build();
	}
}
