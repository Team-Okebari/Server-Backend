package com.okebari.artbite.payment.toss.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "토스페이먼츠 결제 성공 응답 DTO")
public class PaymentSuccessDto {
	@Schema(description = "가맹점 ID", example = "tosspayments")
	private String mId; // 가맹점 Id -> tosspayments
	@Schema(description = "Payment 객체 응답 버전", example = "v1")
	private String version; // Payment 객체 응답 버전
	@Schema(description = "결제 고유 키", example = "5P5M0xK4B1P1A1J1L1T1G")
	private String paymentKey;
	@Schema(description = "주문 ID", example = "orderId_12345")
	private String orderId;
	@Schema(description = "주문명", example = "Sparki 월간 구독")
	private String orderName;
	@Schema(description = "통화", example = "KRW")
	private String currency; // "KRW"
	@Schema(description = "결제 수단", example = "카드")
	private String method; // 결제 수단
	@Schema(description = "총 결제 금액", example = "3900")
	private Long totalAmount;
	@Schema(description = "잔액 금액", example = "0")
	private Long balanceAmount;
	@Schema(description = "공급 금액", example = "3545")
	private Long suppliedAmount;
	@Schema(description = "부가가치세", example = "355")
	private Long vat; // 부가가치세
	@Schema(description = "결제 처리 상태", example = "DONE")
	private String status; // 결제 처리 상태
	@Schema(description = "요청 시간", example = "2025-11-25T10:00:00+09:00")
	private String requestedAt;
	@Schema(description = "승인 시간", example = "2025-11-25T10:01:00+09:00")
	private String approvedAt;
	@Schema(description = "에스크로 사용 여부", example = "false")
	private Boolean useEscrow; // false
	@Schema(description = "문화비 소득공제 가능 여부", example = "false")
	private Boolean cultureExpense; // false
	@Schema(description = "결제 카드 정보")
	private PaymentSuccessCardDto card; // 결제 카드 정보
	@Schema(description = "결제 타입 정보 (NORMAL / BILLING / CONNECTPAY)", example = "NORMAL")
	private String type; // 결제 타입 정보 (NORMAL / BILLING / CONNECTPAY)
}
