package com.okebari.artbite.note.service.support;

public interface ReminderAlertNotifier {

	void notifyFailure(String channel, String title, String message);
}
