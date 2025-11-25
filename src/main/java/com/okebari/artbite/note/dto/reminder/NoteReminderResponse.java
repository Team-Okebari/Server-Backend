package com.okebari.artbite.note.dto.reminder;

import java.time.LocalDate;

import com.okebari.artbite.note.domain.NoteReminderPayload;
import com.okebari.artbite.note.domain.ReminderSourceType;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 리마인드 배너 API 응답.
 */
@Schema(description = "노트 리마인더 응답 DTO")
public record NoteReminderResponse(
	@Schema(description = "프론트에 리마인더 노출 방향 힌트", example = "BANNER", allowableValues = {"DEFERRED", "BANNER", "NONE" })
	SurfaceHint surfaceHint,
	@Schema(description = "리마인더 노트 ID (surfaceHint가 BANNER일 경우)", example = "10")
	Long noteId,
	@Schema(description = "리마인더 노트 제목 (surfaceHint가 BANNER일 경우)", example = "오늘의 영감: 새로운 디자인")
	String title,
	@Schema(description = "리마인더 노트 대표 이미지 URL (surfaceHint가 BANNER일 경우)", example = "https://example.com/reminder_image.jpg")
	String mainImageUrl,
	@Schema(description = "리마인더 소스 타입 (어떤 기준으로 리마인더가 발생했는지)", example = "RANDOM", allowableValues = {"RANDOM",
		"RECOMMEND", "PERSONAL" })
	ReminderSourceType sourceType,
	@Schema(description = "리마인더 날짜", example = "2025-11-25")
	LocalDate reminderDate,
	@Schema(description = "오늘 리마인더가 숨김 처리되었는지 여부", example = "false")
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
