package com.okebari.artbite.membership.dto;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "멤버십 가입 요청 DTO")
public class EnrollMembershipRequestDto {
	@Schema(description = "자동 갱신 여부", example = "true")
	@NotNull
	private boolean autoRenew;
	// 실제 구현에서는 여기에 결제 상세 정보가 포함됩니다.
}
