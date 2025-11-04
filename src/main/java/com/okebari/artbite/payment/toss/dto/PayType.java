package com.okebari.artbite.payment.toss.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PayType {
	CARD("카드"),
	CASH("현금"),
	POINT("포인트");

	private final String description;
}
