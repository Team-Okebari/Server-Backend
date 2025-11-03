package com.okebari.artbite.domain.membership;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MembershipPlanType {
	DEFAULT_MEMBER_PLAN("기본 멤버십");

	private final String description;
}
