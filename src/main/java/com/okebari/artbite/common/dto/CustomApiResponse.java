package com.okebari.artbite.common.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.okebari.artbite.common.exception.ErrorCode;

import lombok.Builder;
import lombok.Getter;

@Getter // Added
@Builder // Added
@JsonInclude(Include.NON_NULL) // null 값은 응답에 포함하지 않음
public class CustomApiResponse<T> { // Changed from record to class

	private final boolean success;
	private final T data;
	private final Error error; // Changed type from int status, String message
	private final LocalDateTime timestamp; // Changed from Instant

	// Constructor for @Builder
	private CustomApiResponse(boolean success, T data, Error error, LocalDateTime timestamp) {
		this.success = success;
		this.data = data;
		this.error = error;
		this.timestamp = timestamp;
	}

	/* ---------- Factory: Success ---------- */

	public static <T> CustomApiResponse<T> success(T data) {
		return CustomApiResponse.<T>builder()
			.success(true)
			.data(data)
			.timestamp(LocalDateTime.now())
			.build();
	}

	// 성공 응답 (데이터 없음)
	public static CustomApiResponse<?> success() {
		return CustomApiResponse.builder()
			.success(true)
			.timestamp(LocalDateTime.now())
			.build();
	}

	/* ---------- Factory: Error ---------- */

	public static CustomApiResponse<?> error(ErrorCode errorCode) {
		return CustomApiResponse.builder()
			.success(false)
			.error(Error.builder()
				.code(errorCode.getCode())
				.message(errorCode.getMessage())
				.build())
			.timestamp(LocalDateTime.now())
			.build();
	}

	// 에러 응답 (커스텀 메시지)
	public static CustomApiResponse<?> error(ErrorCode errorCode, String message) {
		return CustomApiResponse.builder()
			.success(false)
			.error(Error.builder()
				.code(errorCode.getCode())
				.message(message)
				.build())
			.timestamp(LocalDateTime.now())
			.build();
	}

	@Getter // Added
	@Builder // Added
	public static class Error {
		private final String code;
		private final String message;

		// Constructor for @Builder
		private Error(String code, String message) {
			this.code = code;
			this.message = message;
		}
	}
}
