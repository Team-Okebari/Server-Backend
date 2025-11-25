package com.okebari.artbite.payment.toss.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "토스 결제 요청 후 응답 DTO")
public class PaymentResDto {
	@Schema(description = "결제 타입 - 카드/가상계좌/계좌이체/휴대폰", example = "CARD", allowableValues = {"CARD", "VIRTUAL_ACCOUNT",
		"TRANSFER", "MOBILE_PHONE" })
	private String payType; // 결제 타입 - 카드/가상계좌/계좌이체/휴대폰
	@Schema(description = "가격 정보", example = "3900")
	private Long amount; // 가격 정보
	@Schema(description = "주문명", example = "Sparki 월간 구독")
	private String orderName; // 주문명
	@Schema(description = "주문 ID", example = "orderId_12345")
	private String orderId; // 주문 Id
	@Schema(description = "고객 이메일", example = "user@example.com")
	private String customerEmail; // 고객 이메일
	@Schema(description = "고객 이름", example = "홍길동")
	private String customerName; // 고객 이름
	@Schema(description = "결제 성공 시 리다이렉트 될 백엔드 콜백 URL", example = "https://your-backend.com/api/payments/toss/success")
	private String successUrl; // 성공 시 리다이렉트 될 URL
	@Schema(description = "결제 실패 시 리다이렉트 될 백엔드 콜백 URL", example = "https://your-backend.com/api/payments/toss/fail")
	private String failUrl; // 실패 시 리다이렉트 될 URL

	@Schema(description = "프론트엔드에서 요청한 성공 시 리다이렉트 될 최종 URL", example = "https://your-frontend.com/success")
	private String frontendSuccessUrl; // 프론트엔드에서 요청한 성공 시 리다이렉트 될 URL
	@Schema(description = "프론트엔드에서 요청한 실패 시 리다이렉트 될 최종 URL", example = "https://your-frontend.com/fail")
	private String frontendFailUrl; // 프론트엔드에서 요청한 실패 시 리다이렉트 될 URL

	@Schema(description = "실패 이유", example = "잔액 부족", nullable = true)
	private String failReason; // 실패 이유
	@Schema(description = "취소 여부", example = "false")
	private boolean cancelYN; // 취소 YN
	@Schema(description = "취소 이유", example = "사용자 요청", nullable = true)
	private String cancelReason; // 취소 이유
	@Schema(description = "결제가 이루어진 시간", example = "2025-11-25T10:00:00")
	private String createdAt; // 결제가 이루어진 시간
}
