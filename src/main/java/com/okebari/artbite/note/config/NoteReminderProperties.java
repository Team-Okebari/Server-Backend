package com.okebari.artbite.note.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "note.reminder")
public class NoteReminderProperties {

	/**
	 * 사용자별 후보 선정에 실패했을 때 재시도 횟수.
	 */
	private int maxRetry = 3;

	/**
	 * 배치에서 사용자 ID를 나누어 처리할 청크 크기.
	 */
	private int chunkSize = 1000;

	/**
	 * Redis TTL(시간 단위).
	 */
	private int redisTtlHours = 24;

	/**
	 * Redis 파이프라인 단위 저장 크기.
	 */
	private int redisBatchSize = 5000;

	private String alarmChannel = "note-reminder-alert";
}
