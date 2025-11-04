package com.okebari.artbite.domain.membership;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MembershipStatus {
	ACTIVE("활성"),
	CANCELED("취소됨"),
	EXPIRED("만료됨"),
	BANNED("정지됨");

	private final String description;
}
