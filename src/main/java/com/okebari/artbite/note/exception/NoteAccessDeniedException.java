package com.okebari.artbite.note.exception;

import com.okebari.artbite.common.exception.BusinessException;
import com.okebari.artbite.common.exception.ErrorCode;

/**
 * 노트 접근 권한이 없는 경우 발생시키는 예외.
 */
public class NoteAccessDeniedException extends BusinessException {

	public NoteAccessDeniedException(String message) {
		super(ErrorCode.NOTE_ACCESS_DENIED, message);
	}

	public NoteAccessDeniedException() {
		this("노트에 접근할 권한이 없습니다.");
	}
}
