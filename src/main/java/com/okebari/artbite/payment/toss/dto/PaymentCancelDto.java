package com.okebari.artbite.payment.toss.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCancelDto {

	@NotBlank(message = "결제 키는 필수입니다.")
	private String paymentKey;

	@NotBlank(message = "취소 사유는 필수입니다.")
	private String cancelReason;
}
