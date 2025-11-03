package com.okebari.artbite.domain.membership;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MembershipStatus {
	ACTIVE("활성"),
	PENDING_PAYMENT("결제 대기중"), // Add PENDING_PAYMENT
	CANCELED("취소됨"),
	EXPIRED("만료됨"),
	BANNED("정지됨");

	private final String description;
}
