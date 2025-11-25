package com.okebari.artbite.payment.toss.dto;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "환불 요청 DTO")
public record RefundRequestDto(
	@Schema(description = "환불 사유", example = "고객 변심")
	@NotBlank(message = "환불 사유는 필수입니다.")
	String reason
) {
}
