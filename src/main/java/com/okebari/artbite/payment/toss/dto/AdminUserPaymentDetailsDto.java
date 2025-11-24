package com.okebari.artbite.payment.toss.dto;

import com.okebari.artbite.membership.dto.MembershipStatusResponseDto;
import com.okebari.artbite.tracking.dto.ContentAccessLogDto;

/**
 * 관리자가 사용자 이메일로 조회하는 결제, 멤버십, 콘텐츠 접근 기록 상세 정보 DTO.
 */
public record AdminUserPaymentDetailsDto(
	Long userId,
	String userEmail,
	MembershipStatusResponseDto membershipStatus,
	SliceResponseDto<PaymentHistoryDto> paymentHistory,
	SliceResponseDto<ContentAccessLogDto> contentAccessLogs
) {
}
