package com.okebari.artbite.note.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NoteStatus {
	IN_PROGRESS("임시 저장"),
	COMPLETED("작성 완료"),
	PUBLISHED("게시"),
	ARCHIVED("아카이빙");

	private final String description;
}
