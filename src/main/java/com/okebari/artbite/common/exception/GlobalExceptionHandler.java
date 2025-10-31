package com.okebari.artbite.common.exception;

import java.util.stream.Collectors;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.okebari.artbite.common.dto.CustomApiResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	// Custom Business Exception 처리
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<CustomApiResponse<?>> handleBusinessException(BusinessException ex) {
		log.error("BusinessException occurred: {}", ex.getMessage(), ex);
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
		return ResponseEntity
			.status(ex.getErrorCode().getHttpStatus())
			.headers(headers)
			.body(CustomApiResponse.error(ex.getErrorCode()));
	}

	// @Valid 검증 실패 시 처리
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<CustomApiResponse<?>> handleValidationExceptions(MethodArgumentNotValidException ex) {
		String errorMessages = ex.getBindingResult().getAllErrors().stream()
			.map(DefaultMessageSourceResolvable::getDefaultMessage)
			.collect(Collectors.joining(", "));

		log.error("ValidationException occurred: {}", errorMessages);
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.headers(headers)
			.body(CustomApiResponse.error(ErrorCode.COMMON_BAD_REQUEST, errorMessages));
	}

	// 모든 예상치 못한 예외 처리
	@ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
	public ResponseEntity<CustomApiResponse<?>> handleAuthenticationException(
		org.springframework.security.core.AuthenticationException ex) {
		log.error("AuthenticationException occurred: {}", ex.getMessage(), ex);
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
		return ResponseEntity
			.status(HttpStatus.UNAUTHORIZED)
			.headers(headers)
			.body(CustomApiResponse.error(ErrorCode.AUTH_INVALID_CREDENTIALS, ex.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<CustomApiResponse<?>> handleAllExceptions(Exception ex) {
		log.error("An unexpected error occurred: {}", ex.getMessage(), ex);
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
		return ResponseEntity
			.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.headers(headers)
			.body(CustomApiResponse.error(ErrorCode.COMMON_INTERNAL_SERVER_ERROR));
	}
}
