package com.okebari.artbite.common.exception;

public class UserNotFoundException extends BusinessException {

	public UserNotFoundException() {
		super(ErrorCode.AUTH_USER_NOT_FOUND);
	}

	public UserNotFoundException(String message) {
		super(ErrorCode.AUTH_USER_NOT_FOUND, message);
	}
}
