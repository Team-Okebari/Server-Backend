package com.okebari.artbite.membership.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnrollMembershipRequestDto {
	@NotNull
	private boolean autoRenew;
	// 실제 구현에서는 여기에 결제 상세 정보가 포함됩니다.
}
