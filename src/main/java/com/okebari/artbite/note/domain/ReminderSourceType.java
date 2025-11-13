package com.okebari.artbite.note.domain;

/**
 * 리마인드 후보의 출처.
 * 북마크 기반인지, 질문 답변 기반인지 구분해 분석/필터링에 활용한다.
 */
public enum ReminderSourceType {
	BOOKMARK,
	ANSWER
}
