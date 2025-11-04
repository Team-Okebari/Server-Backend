package com.okebari.artbite.payment.toss.dto;

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
public class PaymentSuccessDto {
	private String mId; // 가맹점 Id -> tosspayments
	private String version; // Payment 객체 응답 버전
	private String paymentKey;
	private String orderId;
	private String orderName;
	private String currency; // "KRW"
	private String method; // 결제 수단
	private Long totalAmount;
	private Long balanceAmount;
	private Long suppliedAmount;
	private Long vat; // 부가가치세
	private String status; // 결제 처리 상태
	private String requestedAt;
	private String approvedAt;
	private Boolean useEscrow; // false
	private Boolean cultureExpense; // false
	private PaymentSuccessCardDto card; // 결제 카드 정보
	private String type; // 결제 타입 정보 (NOMAL / BILLING / CONNECTPAY)
}
