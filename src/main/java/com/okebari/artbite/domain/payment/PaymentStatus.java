package com.okebari.artbite.domain.payment;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
	READY("결제 대기"),
	CONFIRMING("결제 승인 중"),
	SUCCESS("결제 성공"),
	FAILED("결제 실패"),
	CANCELED("결제 취소"),
	PROCESSING_FAILED("내부 처리 실패"); // Toss 결제는 성공했으나, 멤버십 활성화 등 후속 처리 실패

	private final String description;
}
