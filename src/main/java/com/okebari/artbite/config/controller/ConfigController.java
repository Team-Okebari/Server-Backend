package com.okebari.artbite.config.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.okebari.artbite.common.dto.CustomApiResponse;
import com.okebari.artbite.payment.toss.config.TossPaymentConfig;
import com.okebari.artbite.payment.toss.dto.PaymentConfigResponseDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

	private final TossPaymentConfig tossPaymentConfig;

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
