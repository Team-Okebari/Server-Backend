package com.okebari.artbite.payment.toss.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.okebari.artbite.domain.membership.MembershipPlanType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "토스 결제 요청 DTO")
public class PaymentDto {

	@Schema(description = "결제 타입", example = "CARD", allowableValues = {"CARD", "VIRTUAL_ACCOUNT", "TRANSFER",
		"MOBILE_PHONE" })
	@NotNull(message = "결제 타입은 필수입니다.")
	private PayType payType;

	@Schema(description = "결제 금액", example = "3900")
	@Min(value = 100, message = "결제 금액은 100원 이상이어야 합니다.")
	@NotNull(message = "결제 금액은 필수입니다.")
	private Long amount;

	@Schema(description = "주문명", example = "Sparki 월간 구독")
	@NotBlank(message = "주문명은 필수입니다.")
	private String orderName;

	@Schema(description = "멤버십 플랜 타입", example = "DEFAULT_MEMBER_PLAN", allowableValues = {"DEFAULT_MEMBER_PLAN" })
	@NotNull(message = "멤버십 플랜 타입은 필수입니다.")
	private MembershipPlanType membershipPlanType;

	// 프론트엔드에서 오버라이드할 수 있는 성공/실패 URL (선택 사항)
	@Schema(description = "성공 시 리다이렉트 될 프론트엔드 URL (선택 사항)", example = "https://your-frontend.com/success")
	private String yourSuccessUrl;
	@Schema(description = "실패 시 리다이렉트 될 프론트엔드 URL (선택 사항)", example = "https://your-frontend.com/fail")
	private String yourFailUrl;
}
