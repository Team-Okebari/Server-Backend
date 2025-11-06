package com.okebari.artbite.payment.toss.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PayType {
	CARD("카드"),
	VIRTUAL_ACCOUNT("가상계좌"),
	TRANSFER("계좌이체"),
	MOBILE_PHONE("휴대폰");

	private final String description;
}
