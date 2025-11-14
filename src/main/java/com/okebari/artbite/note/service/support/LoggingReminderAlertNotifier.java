package com.okebari.artbite.note.service.support;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LoggingReminderAlertNotifier implements ReminderAlertNotifier {

	@Override
	public void notifyFailure(String channel, String title, String message) {
		log.warn("[ReminderAlert][{}] {} - {}", channel, title, message);
	}
}
