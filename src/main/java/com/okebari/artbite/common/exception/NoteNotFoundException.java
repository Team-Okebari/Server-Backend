package com.okebari.artbite.common.exception;

/**
 * 노트를 찾을 수 없을 때 발생시키는 도메인 예외.
 */
public class NoteNotFoundException extends BusinessException {

	public NoteNotFoundException(Long noteId) {
		super(ErrorCode.NOTE_NOT_FOUND, "요청한 노트(" + noteId + ")를 찾을 수 없습니다.");
	}

	public NoteNotFoundException(String message) {
		super(ErrorCode.NOTE_NOT_FOUND, message);
	}
}
