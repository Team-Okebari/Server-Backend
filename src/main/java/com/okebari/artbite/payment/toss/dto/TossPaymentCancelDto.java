package com.okebari.artbite.payment.toss.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Toss Payments의 결제 취소 API 응답 전체를 매핑하기 위한 DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "토스페이먼츠 결제 취소 API 응답 DTO")
public record TossPaymentCancelDto(
	@Schema(description = "API 응답 버전", example = "v1")
	String version,
	@Schema(description = "결제 고유 키", example = "5P5M0xK4B1P1A1J1L1T1G")
	String paymentKey,
	@Schema(description = "결제 타입 (NORMAL, BILLING, CONNECTPAY)", example = "NORMAL")
	String type,
	@Schema(description = "주문 ID", example = "orderId_12345")
	String orderId,
	@Schema(description = "주문명", example = "Sparki 월간 구독")
	String orderName,
	@Schema(description = "상점 ID", example = "mId_XXXXXXXXXX")
	String mId,
	@Schema(description = "통화", example = "KRW")
	String currency,
	@Schema(description = "결제 수단", example = "카드")
	String method,
	@Schema(description = "총 결제 금액", example = "3900")
	Long totalAmount,
	@Schema(description = "잔여 승인 금액", example = "0")
	Long balanceAmount,
	@Schema(description = "결제 상태 (DONE, CANCELED 등)", example = "CANCELED")
	String status,
	@Schema(description = "요청 시간", example = "2025-11-25T10:00:00+09:00")
	String requestedAt,
	@Schema(description = "승인 시간", example = "2025-11-25T10:01:00+09:00")
	String approvedAt,
	@Schema(description = "취소 상세 정보 목록")
	List<CancelDetailDto> cancels,
	@Schema(description = "간편결제 정보 (JSON 객체)", implementation = Object.class)
	Object easyPay,
	@Schema(description = "결제가 발생한 국가", example = "KR")
	String country
) {
}
