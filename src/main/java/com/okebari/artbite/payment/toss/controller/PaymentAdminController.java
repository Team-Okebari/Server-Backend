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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "[Admin] Payments", description = "관리자용 결제 관리 API")
@RestController
@RequestMapping("/api/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PaymentAdminController {

	private final TossPaymentService tossPaymentService;

	@Operation(summary = "특정 사용자의 결제 내역 조회", description = "관리자가 특정 사용자의 이메일을 통해 간소화된 결제 내역을 페이지네이션하여 조회합니다. (ADMIN 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "결제 내역 조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "Unauthorized", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"),
				mediaType = "application/json")),
		@ApiResponse(responseCode = "403", description = "권한 없음",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "Forbidden", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C003\",\"message\":\"접근 권한이 없습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"),
				mediaType = "application/json"))
	})
	/**
	 * [관리자] 특정 사용자의 모든 결제 내역을 이메일로 조회합니다. (간소화된 버전)
	 * @param email 조회할 사용자의 이메일
	 * @param pageable 페이징 정보
	 * @return 결제 내역 목록
	 */
	@GetMapping("/by-user")
	public CustomApiResponse<SliceResponseDto<PaymentHistoryDto>> getPaymentsByUserEmail(
		@Parameter(description = "조회할 사용자의 이메일", example = "user@example.com", required = true) @RequestParam String email,
		@Parameter(hidden = true) Pageable pageable) {

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

	@Operation(summary = "특정 사용자의 상세 결제 정보 조회", description = "관리자가 특정 사용자의 이메일을 통해 결제, 멤버십, 콘텐츠 접근 기록을 포함한 종합 정보를 조회합니다. (ADMIN 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "사용자 상세 정보 조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "Unauthorized", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"),
				mediaType = "application/json")),
		@ApiResponse(responseCode = "403", description = "권한 없음",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "Forbidden", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C003\",\"message\":\"접근 권한이 없습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"),
				mediaType = "application/json")),
		@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "UserNotFound", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"A005\",\"message\":\"사용자를 찾을 수 없습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"),
				mediaType = "application/json"))
	})
	/**
	 * [관리자] 특정 사용자의 종합 정보를 이메일로 조회합니다. (결제내역, 멤버십, 컨텐츠 접근기록 포함)
	 * @param email 조회할 사용자의 이메일
	 * @param paymentPageable 결제 내역 페이징 정보
	 * @param accessLogPageable 콘텐츠 접근 기록 페이징 정보
	 * @return 사용자의 종합 정보
	 */
	@GetMapping("/details-by-user")
	public CustomApiResponse<AdminUserPaymentDetailsDto> getUserPaymentDetails(
		@Parameter(description = "조회할 사용자의 이메일", example = "user@example.com", required = true) @RequestParam String email,
		@Parameter(hidden = true) @Qualifier("paymentPageable") Pageable paymentPageable,
		@Parameter(hidden = true) @Qualifier("accessLogPageable") Pageable accessLogPageable
	) {
		AdminUserPaymentDetailsDto userDetails = tossPaymentService.getAdminUserPaymentDetails(email, paymentPageable,
			accessLogPageable);
		return CustomApiResponse.success(userDetails);
	}

	@Operation(summary = "특정 결제 강제 환불 처리", description = "관리자가 특정 결제 키를 통해 정책 검증을 우회하여 결제를 강제 환불 처리합니다. (ADMIN 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "환불 처리 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청 또는 이미 취소된 결제",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = {
					@ExampleObject(name = "BadRequest", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C001\",\"message\":\"환불 사유는 필수입니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"),
					@ExampleObject(name = "AlreadyCanceled", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"P004\",\"message\":\"이미 취소된 결제입니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}")
				},
				mediaType = "application/json")),
		@ApiResponse(responseCode = "401", description = "인증 실패",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "Unauthorized", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"),
				mediaType = "application/json")),
		@ApiResponse(responseCode = "403", description = "권한 없음",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "Forbidden", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C003\",\"message\":\"접근 권한이 없습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"),
				mediaType = "application/json")),
		@ApiResponse(responseCode = "404", description = "결제 정보를 찾을 수 없음",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "PaymentNotFound", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"P001\",\"message\":\"결제 정보를 찾을 수 없습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"),
				mediaType = "application/json")),
		@ApiResponse(responseCode = "500", description = "환불 처리 중 서버 내부 오류",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "RefundFailed", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"P005\",\"message\":\"환불 처리 중 오류가 발생했습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"),
				mediaType = "application/json"))
	})
	/**
	 * [관리자] 특정 결제를 강제로 환불 처리합니다. (정책 검증 우회)
	 * @param paymentKey 환불할 결제의 paymentKey
	 * @param refundRequest 환불 사유를 담은 DTO
	 * @return 환불 처리 결과
	 */
	@PostMapping("/{paymentKey}/refund")
	public CustomApiResponse<TossPaymentCancelDto> requestRefund(
		@Parameter(description = "환불할 결제의 paymentKey", example = "5P5M0xK4B1P1A1J1L1T1G", required = true) @PathVariable String paymentKey,
		@Valid @RequestBody RefundRequestDto refundRequest
	) {
		// 관리자 API는 정책 검증을 하는 requestRefundByUser 대신 executeRefund를 직접 호출하도록 변경
		TossPaymentCancelDto refundResult = tossPaymentService.executeRefund(paymentKey, refundRequest.reason());
		return CustomApiResponse.success(refundResult);
	}
}
