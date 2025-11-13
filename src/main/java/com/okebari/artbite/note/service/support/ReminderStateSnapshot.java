package com.okebari.artbite.note.service.support;

import java.time.LocalDateTime;

import com.okebari.artbite.note.domain.NoteReminder;

/**
 * 상태 전이를 계산할 때 필요한 타임스탬프 묶음을 한 곳에 모은 스냅샷.
 * - Q3 응답: 상태 정보가 여러 DTO에 흩어져 있어 수정 범위가 넓다는 피드백을 반영해 도입했다.
 */
public record ReminderStateSnapshot(
	LocalDateTime firstVisitAt,
	LocalDateTime bannerSeenAt,
	LocalDateTime modalClosedAt,
	boolean dismissed,
	LocalDateTime dismissedAt
) {

	public static ReminderStateSnapshot from(NoteReminder reminder) {
		return new ReminderStateSnapshot(
			reminder.getFirstVisitAt(),
			reminder.getBannerSeenAt(),
			reminder.getModalClosedAt(),
			reminder.isDismissed(),
			reminder.getDismissedAt()
		);
	}

	public boolean hasFirstVisit() {
		return firstVisitAt != null;
	}

	public boolean hasBannerSeen() {
		return bannerSeenAt != null;
	}
}
