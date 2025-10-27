package com.okebari.artbite.common.exception;

public class EmailAlreadyExistsException extends BusinessException {
	public EmailAlreadyExistsException() {
		super(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS);
	}

	public EmailAlreadyExistsException(String message) {
		super(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS, message);
	}
}
