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
public class PaymentSuccessCardDto {
	private String company; // 회사명
	private String number; // 카드번호
	private String installmentPlanMonths; // 할부 개월
	private String isInterestFree;
	private String approveNo; // 승인번호
	private String useCardPoint; // 카드 포인트 사용 여부
	private String cardType; // 카드 타입
	private String ownerType; // 소유자 타입
	private String acquireStatus; // 승인 상태
	private String receiptUrl; // 영수증 URL
}
