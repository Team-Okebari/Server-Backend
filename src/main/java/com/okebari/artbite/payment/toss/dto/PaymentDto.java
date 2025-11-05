package com.okebari.artbite.payment.toss.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.okebari.artbite.domain.membership.MembershipPlanType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {

	@NotNull(message = "결제 타입은 필수입니다.")
	private PayType payType;

	@Min(value = 100, message = "결제 금액은 100원 이상이어야 합니다.")
	@NotNull(message = "결제 금액은 필수입니다.")
	private Long amount;

	@NotBlank(message = "주문명은 필수입니다.")
	private String orderName;

	@NotNull(message = "멤버십 플랜 타입은 필수입니다.")
	private MembershipPlanType membershipPlanType;

	// 프론트엔드에서 오버라이드할 수 있는 성공/실패 URL (선택 사항)
	private String yourSuccessUrl;
	private String yourFailUrl;
}
