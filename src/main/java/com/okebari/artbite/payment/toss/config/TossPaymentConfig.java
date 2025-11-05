package com.okebari.artbite.payment.toss.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Component
@Getter
public class TossPaymentConfig {

	private final String url;
	private final String testClientApiKey;
	private final String testSecretKey;
	private final String successUrl;
	private final String failUrl;
	private final Long membershipAmount;
	private final String frontendSuccessUrl;
	private final String frontendFailUrl;
	private final String orderName;
	private final int readyTimeoutMinutes;
	private final long readyCheckIntervalMs;

	public TossPaymentConfig(
		@Value("${payment.toss.url}") String url,
		@Value("${payment.toss.test_client_api_key}") String testClientApiKey,
		@Value("${payment.toss.test_secret_api_key}") String testSecretKey,
		@Value("${payment.toss.success_url}") String successUrl,
		@Value("${payment.toss.fail_url}") String failUrl,
		@Value("${payment.toss.membership_amount}") Long membershipAmount,
		@Value("${payment.toss.frontend_success_url}") String frontendSuccessUrl,
		@Value("${payment.toss.frontend_fail_url}") String frontendFailUrl,
		@Value("${payment.toss.order-name}") String orderName,
		@Value("${payment.toss.ready-timeout-minutes}") int readyTimeoutMinutes,
		@Value("${payment.toss.ready-check-interval-ms}") long readyCheckIntervalMs) {
		this.url = url;
		this.testClientApiKey = testClientApiKey;
		this.testSecretKey = testSecretKey;
		this.successUrl = successUrl;
		this.failUrl = failUrl;
		this.membershipAmount = membershipAmount;
		this.frontendSuccessUrl = frontendSuccessUrl;
		this.frontendFailUrl = frontendFailUrl;
		this.orderName = orderName;
		this.readyTimeoutMinutes = readyTimeoutMinutes;
		this.readyCheckIntervalMs = readyCheckIntervalMs;
	}
}
