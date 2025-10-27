package com.okebari.artbite.common.exception;

public class InvalidTokenException extends BusinessException {
	public InvalidTokenException() {
		super(ErrorCode.AUTH_INVALID_TOKEN);
	}

	public InvalidTokenException(String message) {
		super(ErrorCode.AUTH_INVALID_TOKEN, message);
	}
}
