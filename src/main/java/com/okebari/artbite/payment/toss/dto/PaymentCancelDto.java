package com.okebari.artbite.payment.toss.dto;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "토스페이먼츠 결제 취소 요청 DTO")
public class PaymentCancelDto {

	@Schema(description = "결제 고유 키", example = "5P5M0xK4B1P1A1J1L1T1G")
	@NotBlank(message = "결제 키는 필수입니다.")
	private String paymentKey;

	@Schema(description = "취소 사유", example = "고객 변심")
	@NotBlank(message = "취소 사유는 필수입니다.")
	private String cancelReason;
}
