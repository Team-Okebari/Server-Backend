package com.okebari.artbite.payment.toss.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Component
@Getter
public class TossPaymentConfig {

	@Value("${payment.toss.url}")
	private String url;

	@Value("${payment.toss.test_client_api_key}")
	private String testClientApiKey;

	@Value("${payment.toss.test_secret_api_key}")
	private String testSecretKey;

	@Value("${payment.toss.success_url}")
	private String successUrl;

	@Value("${payment.toss.fail_url}")
	private String failUrl;

	@Value("${payment.toss.membership_amount}")
	private Long membershipAmount;

	@Value("${payment.toss.frontend_success_url}")
	private String frontendSuccessUrl;

	@Value("${payment.toss.frontend_fail_url}")
	private String frontendFailUrl;

	@Value("${payment.toss.order-name}")
	private String orderName;
}
