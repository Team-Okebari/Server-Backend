package com.okebari.artbite.common.exception;

public class TokenExpiredException extends BusinessException {
	public TokenExpiredException() {
		super(ErrorCode.AUTH_TOKEN_EXPIRED);
	}

	public TokenExpiredException(String message) {
		super(ErrorCode.AUTH_TOKEN_EXPIRED, message);
	}
}
