package com.okebari.artbite.payment.toss.dto;

import java.time.LocalDateTime;

import com.okebari.artbite.domain.payment.PaymentStatus;

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
@Schema(description = "결제 내역 요약 DTO")
public class PaymentHistoryDto {
	@Schema(description = "결제 내역 ID", example = "1")
	private Long paymentHistoryId;
	@Schema(description = "토스 결제 고유 키", example = "5P5M0xK4B1P1A1J1L1T1G")
	private String paymentKey;
	@Schema(description = "결제 금액", example = "3900")
	private Long amount;
	@Schema(description = "주문명", example = "Sparki 월간 구독")
	private String orderName;
	@Schema(description = "결제 상태", example = "SUCCESS")
	private PaymentStatus status;
	@Schema(description = "결제 생성 시간", example = "2025-11-25T10:00:00")
	private LocalDateTime createdAt;
}
