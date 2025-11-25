package com.okebari.artbite.payment.toss.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Toss Payments 취소 응답의 'cancels' 배열 내 객체에 대한 DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "토스페이먼츠 결제 취소 상세 정보 DTO")
public record CancelDetailDto(
	@Schema(description = "취소 금액", example = "3900")
	Long cancelAmount,
	@Schema(description = "취소 사유", example = "고객 변심")
	String cancelReason,
	@Schema(description = "영수증 키", example = "receipt_XXXXXXXXXXXX")
	String receiptKey,
	@Schema(description = "취소된 시간", example = "2025-11-25T10:00:00")
	String canceledAt,
	@Schema(description = "거래 키", example = "trx_XXXXXXXXXXXX")
	String transactionKey,
	@Schema(description = "간편결제 할인 금액 (있을 경우)", example = "0")
	String easyPayDiscountAmount // 간편결제 할인 금액 (있을 경우)
) {
}
