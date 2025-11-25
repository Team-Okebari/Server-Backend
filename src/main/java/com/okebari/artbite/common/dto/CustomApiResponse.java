package com.okebari.artbite.common.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.okebari.artbite.common.exception.ErrorCode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(Include.NON_NULL) // null 값은 응답에 포함하지 않음
@Schema(description = "API 응답의 표준 형식. 성공 시에는 data 필드를, 실패 시에는 error 필드를 포함합니다.")
public class CustomApiResponse<T> {

	@Schema(description = "요청 성공 여부")
	private final boolean success;
	@Schema(description = "성공 시 반환되는 데이터", nullable = true)
	private final T data;
	@Schema(description = "실패 시 반환되는 에러 정보", nullable = true)
	private final Error error;
	@Schema(description = "응답 생성 시간")
	private final LocalDateTime timestamp;

	private CustomApiResponse(boolean success, T data, Error error, LocalDateTime timestamp) {
		this.success = success;
		this.data = data;
		this.error = error;
		this.timestamp = timestamp;
	}

	// 성공 응답 팩토리

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

	// 에러 응답 팩토리

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
	public static <T> CustomApiResponse<T> error(ErrorCode errorCode,
		String message) { // T type parameter for error method
		return CustomApiResponse.<T>builder()
			.success(false)
			.error(Error.builder()
				.code(errorCode.getCode())
				.message(message)
				.build())
			.timestamp(LocalDateTime.now())
			.build();
	}

	// 에러 응답 (데이터 포함)
	public static <T> CustomApiResponse<T> error(ErrorCode errorCode, String message, T data) {
		return CustomApiResponse.<T>builder()
			.success(false)
			.data(data)
			.error(Error.builder()
				.code(errorCode.getCode())
				.message(message)
				.build())
			.timestamp(LocalDateTime.now())
			.build();
	}

	@Getter
	@Builder
	@Schema(description = "API 에러 상세 정보")
	public static class Error {
		@Schema(description = "애플리케이션 고유 에러 코드")
		private final String code;
		@Schema(description = "에러 메시지")
		private final String message;

		private Error(String code, String message) {
			this.code = code;
			this.message = message;
		}
	}
}
