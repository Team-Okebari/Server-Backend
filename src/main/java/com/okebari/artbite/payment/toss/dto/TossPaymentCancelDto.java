package com.okebari.artbite.payment.toss.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Toss Payments의 결제 취소 API 응답 전체를 매핑하기 위한 DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TossPaymentCancelDto(
	String version,
	String paymentKey,
	String type,
	String orderId,
	String orderName,
	String mId,
	String currency,
	String method,
	Long totalAmount,
	Long balanceAmount,
	String status,
	String requestedAt,
	String approvedAt,
	List<CancelDetailDto> cancels,
	Object easyPay,
	String country
) {
}
