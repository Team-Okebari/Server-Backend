package com.okebari.artbite.payment.toss.controller;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.okebari.artbite.common.dto.CustomApiResponse;
import com.okebari.artbite.domain.payment.Payment;
import com.okebari.artbite.payment.toss.dto.AdminUserPaymentDetailsDto;
import com.okebari.artbite.payment.toss.dto.PaymentHistoryDto;
import com.okebari.artbite.payment.toss.dto.RefundRequestDto;
import com.okebari.artbite.payment.toss.dto.SliceInfo;
import com.okebari.artbite.payment.toss.dto.SliceResponseDto;
import com.okebari.artbite.payment.toss.dto.TossPaymentCancelDto;
import com.okebari.artbite.payment.toss.service.TossPaymentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class PaymentAdminController {

	private final TossPaymentService tossPaymentService;

	/**
	 * [관리자] 특정 사용자의 모든 결제 내역을 이메일로 조회합니다. (간소화된 버전)
	 * @param email 조회할 사용자의 이메일
	 * @param pageable 페이징 정보
	 * @return 결제 내역 목록
	 */
	@GetMapping("/by-user")
	public CustomApiResponse<SliceResponseDto<PaymentHistoryDto>> getPaymentsByUserEmail(
		@RequestParam String email,
		Pageable pageable) {

		Slice<Payment> histories = tossPaymentService.findAllPaymentsByUserEmailForAdmin(email, pageable);
		SliceInfo sliceInfo = new SliceInfo(pageable, histories.getNumberOfElements(), histories.hasNext());

		List<PaymentHistoryDto> paymentHistoryDtos = histories.getContent().stream()
			.map(payment -> PaymentHistoryDto.builder()
				.paymentHistoryId(payment.getId())
				.paymentKey(payment.getPaymentKey())
				.amount(payment.getAmount())
				.orderName(payment.getOrderName())
				.createdAt(payment.getCreatedAt())
				.status(payment.getStatus())
				.build())
			.collect(Collectors.toList());

		SliceResponseDto<PaymentHistoryDto> responseDto = new SliceResponseDto<>(paymentHistoryDtos, sliceInfo);
		return CustomApiResponse.success(responseDto);
	}

	/**
	 * [관리자] 특정 사용자의 종합 정보를 이메일로 조회합니다. (결제내역, 멤버십, 컨텐츠 접근기록 포함)
	 * @param email 조회할 사용자의 이메일
	 * @param paymentPageable 결제 내역 페이징 정보
	 * @param accessLogPageable 콘텐츠 접근 기록 페이징 정보
	 * @return 사용자의 종합 정보
	 */
	@GetMapping("/details-by-user")
	public CustomApiResponse<AdminUserPaymentDetailsDto> getUserPaymentDetails(
		@RequestParam String email,
		@Qualifier("paymentPageable") Pageable paymentPageable,
		@Qualifier("accessLogPageable") Pageable accessLogPageable
	) {
		AdminUserPaymentDetailsDto userDetails = tossPaymentService.getAdminUserPaymentDetails(email, paymentPageable,
			accessLogPageable);
		return CustomApiResponse.success(userDetails);
	}

	/**
	 * [관리자] 특정 결제를 강제로 환불 처리합니다. (정책 검증 우회)
	 * @param paymentKey 환불할 결제의 paymentKey
	 * @param refundRequest 환불 사유를 담은 DTO
	 * @return 환불 처리 결과
	 */
	@PostMapping("/{paymentKey}/refund")
	public CustomApiResponse<TossPaymentCancelDto> requestRefund(
		@PathVariable String paymentKey,
		@Valid @RequestBody RefundRequestDto refundRequest
	) {
		// 관리자 API는 정책 검증을 하는 requestRefundByUser 대신 executeRefund를 직접 호출하도록 변경
		TossPaymentCancelDto refundResult = tossPaymentService.executeRefund(paymentKey, refundRequest.reason());
		return CustomApiResponse.success(refundResult);
	}
}
