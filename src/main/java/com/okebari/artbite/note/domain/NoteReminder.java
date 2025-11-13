package com.okebari.artbite.note.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.okebari.artbite.domain.common.BaseTimeEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "note_reminder_pot",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_note_reminder_user_date", columnNames = {"user_id", "reminder_date"})
	})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoteReminder extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "note_id", nullable = false)
	private Long noteId;

	@Column(name = "reminder_date", nullable = false)
	private LocalDate reminderDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_type", nullable = false, length = 30)
	private ReminderSourceType sourceType;

	@Embedded
	private NoteReminderPayload payload;

	@Column(name = "first_visit_at")
	private LocalDateTime firstVisitAt;

	@Column(name = "banner_seen_at")
	private LocalDateTime bannerSeenAt;

	@Column(name = "modal_closed_at")
	private LocalDateTime modalClosedAt;

	@Column(name = "dismissed")
	private boolean dismissed;

	@Column(name = "dismissed_at")
	private LocalDateTime dismissedAt;

	private NoteReminder(Long userId, LocalDate reminderDate, ReminderSourceType sourceType,
		Long noteId, NoteReminderPayload payload) {
		this.userId = userId;
		this.reminderDate = reminderDate;
		this.sourceType = sourceType;
		this.noteId = noteId;
		this.payload = payload != null ? payload.copyWithNoteId(noteId) : null;
	}

	public static NoteReminder create(Long userId, LocalDate reminderDate,
		ReminderSourceType sourceType, Long noteId, NoteReminderPayload payload) {
		NoteReminder reminder = new NoteReminder(userId, reminderDate, sourceType, noteId, payload);
		reminder.resetRuntimeState();
		return reminder;
	}

	public void replaceCandidate(ReminderSourceType sourceType, Long noteId, NoteReminderPayload payload) {
		this.sourceType = sourceType;
		this.noteId = noteId;
		this.payload = payload != null ? payload.copyWithNoteId(noteId) : null;
		resetRuntimeState();
	}

	public void markFirstVisit(LocalDateTime now) {
		if (this.firstVisitAt == null) {
			this.firstVisitAt = now;
		}
	}

	public void markBannerSeen(LocalDateTime now) {
		if (this.bannerSeenAt == null) {
			this.bannerSeenAt = now;
		}
	}

	public void markModalClosed(LocalDateTime now) {
		this.modalClosedAt = now;
	}

	public void dismiss(String reason, LocalDateTime now) {
		this.dismissed = true;
		this.dismissedAt = now;
	}

	public void resetRuntimeState() {
		this.firstVisitAt = null;
		this.bannerSeenAt = null;
		this.modalClosedAt = null;
		this.dismissed = false;
		this.dismissedAt = null;
	}

	public boolean isFor(LocalDate date) {
		return this.reminderDate.equals(date);
	}
}
