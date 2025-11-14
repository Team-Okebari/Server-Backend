package com.okebari.artbite.common.exception;

import java.time.LocalDate;

public class ReminderNotFoundException extends BusinessException {

	public ReminderNotFoundException(Long userId, LocalDate date) {
		super(ErrorCode.REMINDER_NOT_FOUND,
			"해당 일자의 리마인드가 존재하지 않습니다. userId=%d, date=%s".formatted(userId, date));
	}
}
