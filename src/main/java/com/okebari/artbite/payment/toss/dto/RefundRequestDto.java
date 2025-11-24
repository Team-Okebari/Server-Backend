package com.okebari.artbite.payment.toss.dto;

import jakarta.validation.constraints.NotBlank;

public record RefundRequestDto(
	@NotBlank(message = "환불 사유는 필수입니다.")
	String reason
) {
}
