package com.okebari.artbite.payment.toss.dto;

import com.okebari.artbite.membership.dto.MembershipStatusResponseDto;
import com.okebari.artbite.tracking.dto.ContentAccessLogDto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 관리자가 사용자 이메일로 조회하는 결제, 멤버십, 콘텐츠 접근 기록 상세 정보 DTO.
 */
@Schema(description = "관리자용 사용자 결제 및 활동 상세 정보 DTO")
public record AdminUserPaymentDetailsDto(
	@Schema(description = "사용자 ID", example = "1")
	Long userId,
	@Schema(description = "사용자 이메일", example = "user@example.com")
	String userEmail,
	@Schema(description = "멤버십 상태 정보")
	MembershipStatusResponseDto membershipStatus,
	@Schema(description = "결제 내역 슬라이스 정보")
	SliceResponseDto<PaymentHistoryDto> paymentHistory,
	@Schema(description = "콘텐츠 접근 로그 슬라이스 정보")
	SliceResponseDto<ContentAccessLogDto> contentAccessLogs
) {
}
