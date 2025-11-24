package com.okebari.artbite.payment.toss.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Toss Payments 취소 응답의 'cancels' 배열 내 객체에 대한 DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CancelDetailDto(
	Long cancelAmount,
	String cancelReason,
	String receiptKey,
	String canceledAt,
	String transactionKey,
	String easyPayDiscountAmount // 간편결제 할인 금액 (있을 경우)
) {
}
