package com.okebari.artbite.membership.dto;

import java.time.LocalDateTime;

import com.okebari.artbite.domain.membership.MembershipPlanType;
import com.okebari.artbite.domain.membership.MembershipStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MembershipStatusResponseDto {
	private MembershipStatus status;
	private MembershipPlanType planType;
	private LocalDateTime startDate;
	private LocalDateTime endDate;
	private int consecutiveMonths;
	private boolean autoRenew;
}
