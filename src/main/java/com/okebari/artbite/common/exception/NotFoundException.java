package com.okebari.artbite.common.exception;

public class NotFoundException extends BusinessException {
	public NotFoundException(String message) {
		super(ErrorCode.COMMON_NOT_FOUND, message);
	}

	public NotFoundException() {
		super(ErrorCode.COMMON_NOT_FOUND);
	}
}
