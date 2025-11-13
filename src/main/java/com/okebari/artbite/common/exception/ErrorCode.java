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
	AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "유효하지 않은 토큰입니다."),
	AUTH_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "A005", "사용자를 찾을 수 없습니다."),


	// Note Errors
	NOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "N001", "노트를 찾을 수 없습니다."),
	NOTE_INVALID_STATUS(HttpStatus.BAD_REQUEST, "N002", "허용되지 않은 노트 상태입니다."),
	NOTE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "N003", "노트에 접근할 권한이 없습니다."),

	REMINDER_NOT_FOUND(HttpStatus.NOT_FOUND, "NR001", "리마인드 정보를 찾을 수 없습니다."),

	// Creator Errors
	CREATOR_NOT_FOUND(HttpStatus.NOT_FOUND, "CR001", "작가 정보를 찾을 수 없습니다."),
	// Membership Errors
	MEMBERSHIP_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "멤버십을 찾을 수 없습니다."),
	MEMBERSHIP_ALREADY_ACTIVE(HttpStatus.CONFLICT, "M002", "이미 활성 멤버십을 가지고 있습니다."),
	MEMBERSHIP_BANNED(HttpStatus.FORBIDDEN, "M003", "정지된 멤버십입니다."),
	MEMBERSHIP_INVALID_STATUS(HttpStatus.BAD_REQUEST, "M004", "유효하지 않은 멤버십 상태입니다."),
	MEMBERSHIP_ACTIVATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "M005", "멤버십 활성화에 실패했습니다."),
	MEMBERSHIP_CANCELED_CANNOT_RENEW(HttpStatus.CONFLICT, "M006", "취소된 멤버십이 존재하여 신규 결제를 진행할 수 없습니다. 멤버십 재활성화를 이용해주세요."),
	PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "M007", "결제에 실패했습니다."),

	// Payment Errors
	NOT_FOUND_PAYMENT(HttpStatus.NOT_FOUND, "P001", "결제 정보를 찾을 수 없습니다."),
	PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "P002", "결제 금액이 일치하지 않습니다."),
	FAILED_PAYMENT_INTERNAL_SYSTEM_PROCESSING(HttpStatus.INTERNAL_SERVER_ERROR, "P003", "결제 시스템 내부 처리 오류가 발생했습니다."),
	ALREADY_CANCELED_PAYMENT(HttpStatus.BAD_REQUEST, "P004", "이미 취소된 결제입니다."),
	FAILED_REFUND_PROCESS(HttpStatus.INTERNAL_SERVER_ERROR, "P005", "환불 처리 중 오류가 발생했습니다."),
	PAYMENT_PENDING(HttpStatus.CONFLICT, "P006", "이미 결제 대기 중인 멤버십이 있습니다."),
	PAYMENT_CONFIRM_FAILED(HttpStatus.BAD_REQUEST, "P007", "결제 승인에 실패했습니다."),
	PAYMENT_ALREADY_PROCESSED(HttpStatus.CONFLICT, "P008", "이미 처리된 결제입니다."),
	REFUND_TEMPORARILY_DISABLED(HttpStatus.SERVICE_UNAVAILABLE, "P009", "현재 환불 기능은 일시적으로 비활성화되어 있습니다. 관리자에게 문의해주세요.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;
}
