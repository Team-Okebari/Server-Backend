package com.okebari.artbite.payment.toss.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "프론트엔드용 결제 설정 응답 DTO")
public class PaymentConfigResponseDto {
	@Schema(description = "Toss Payments 클라이언트 키", example = "test_ck_...")
	private final String clientKey;

	@Schema(description = "기본 주문명", example = "Sparki 월간 구독")
	private final String orderName;

	@Schema(description = "기본 결제 금액", example = "3900")
	private final Long amount;
}
