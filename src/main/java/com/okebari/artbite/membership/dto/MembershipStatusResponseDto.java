package com.okebari.artbite.membership.dto;

import java.time.LocalDateTime;

import com.okebari.artbite.domain.membership.MembershipPlanType;
import com.okebari.artbite.domain.membership.MembershipStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "멤버십 상태 응답 DTO")
public class MembershipStatusResponseDto {
	@Schema(description = "멤버십 상태 (ACTIVE, CANCELED, EXPIRED, BANNED)")
	private MembershipStatus status;

	@Schema(description = "멤버십 플랜 유형")
	private MembershipPlanType planType;

	@Schema(description = "멤버십 시작일")
	private LocalDateTime startDate;

	@Schema(description = "멤버십 종료 예정일")
	private LocalDateTime endDate;

	@Schema(description = "연속 구독 개월 수")
	private int consecutiveMonths;

	@Schema(description = "자동 갱신 여부")
	private boolean autoRenew;
}
