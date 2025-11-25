package com.okebari.artbite.config.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.okebari.artbite.common.dto.CustomApiResponse;
import com.okebari.artbite.payment.toss.config.TossPaymentConfig;
import com.okebari.artbite.payment.toss.dto.PaymentConfigResponseDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Config", description = "설정 정보 API")
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

	private final TossPaymentConfig tossPaymentConfig;

	@Operation(summary = "결제 설정 정보 조회", description = "프론트엔드에서 사용할 토스 페이먼츠 등 결제 관련 설정 정보를 조회합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "결제 설정 정보 조회 성공")
	})
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping("/payment")
	public ResponseEntity<CustomApiResponse<PaymentConfigResponseDto>> getPaymentConfig() {
		PaymentConfigResponseDto paymentConfig = new PaymentConfigResponseDto(
			tossPaymentConfig.getTestClientApiKey(),
			tossPaymentConfig.getOrderName(),
			tossPaymentConfig.getMembershipAmount()
		);
		return ResponseEntity.ok(CustomApiResponse.success(paymentConfig));
	}
}
