package com.okebari.artbite.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// Common Errors
	COMMON_BAD_REQUEST(HttpStatus.BAD_REQUEST, "C001", "잘못된 요청입니다."),
	COMMON_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C002", "인증되지 않은 사용자입니다."),
	COMMON_FORBIDDEN(HttpStatus.FORBIDDEN, "C003", "접근 권한이 없습니다."),
	COMMON_NOT_FOUND(HttpStatus.NOT_FOUND, "C004", "리소스를 찾을 수 없습니다."),
	COMMON_INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C005", "서버 내부 오류가 발생했습니다."),

	// Auth Errors
	AUTH_EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "A001", "이미 가입된 이메일입니다."),
	AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A002", "이메일 또는 비밀번호가 일치하지 않습니다."),
	AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A003", "토큰이 만료되었습니다."),
	AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "유효하지 않은 토큰입니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;
}
