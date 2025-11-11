package com.okebari.artbite.payment.toss.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PaymentConfigResponseDto {
	private final String clientKey;
	private final String orderName;
	private final Long amount;
}
