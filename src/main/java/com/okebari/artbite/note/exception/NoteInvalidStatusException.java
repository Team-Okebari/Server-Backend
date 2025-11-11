package com.okebari.artbite.note.exception;

import com.okebari.artbite.common.exception.BusinessException;
import com.okebari.artbite.common.exception.ErrorCode;

/**
 * 노트 상태가 허용 범위를 벗어났을 때 발생시키는 예외.
 */
public class NoteInvalidStatusException extends BusinessException {

	public NoteInvalidStatusException(String message) {
		super(ErrorCode.NOTE_INVALID_STATUS, message);
	}

	public NoteInvalidStatusException() {
		this("허용되지 않은 노트 상태입니다.");
	}
}
