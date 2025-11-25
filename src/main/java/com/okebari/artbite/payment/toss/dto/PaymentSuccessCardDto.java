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
@Schema(description = "토스페이먼츠 결제 성공 카드 정보 DTO")
public class PaymentSuccessCardDto {
	@Schema(description = "카드 회사명", example = "현대")
	private String company; // 회사명
	@Schema(description = "카드 번호", example = "4092**********01")
	private String number; // 카드번호
	@Schema(description = "할부 개월 수", example = "0")
	private String installmentPlanMonths; // 할부 개월
	@Schema(description = "무이자 할부 여부", example = "false")
	private String isInterestFree;
	@Schema(description = "승인 번호", example = "00000000")
	private String approveNo; // 승인번호
	@Schema(description = "카드 포인트 사용 여부", example = "false")
	private String useCardPoint; // 카드 포인트 사용 여부
	@Schema(description = "카드 타입 (신용, 체크 등)", example = "신용")
	private String cardType; // 카드 타입
	@Schema(description = "카드 소유자 타입 (개인, 법인 등)", example = "개인")
	private String ownerType; // 소유자 타입
	@Schema(description = "카드 승인 상태", example = "DONE")
	private String acquireStatus; // 승인 상태
	@Schema(description = "영수증 URL", example = "https://example.com/receipt")
	private String receiptUrl; // 영수증 URL
}
