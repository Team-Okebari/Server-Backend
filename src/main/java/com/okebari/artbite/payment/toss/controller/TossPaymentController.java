package com.okebari.artbite.payment.toss.controller;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.okebari.artbite.auth.vo.CustomUserDetails;
import com.okebari.artbite.common.dto.CustomApiResponse;
import com.okebari.artbite.domain.payment.Payment;
import com.okebari.artbite.payment.toss.config.TossPaymentConfig;
import com.okebari.artbite.payment.toss.dto.PaymentDto;
import com.okebari.artbite.payment.toss.dto.PaymentHistoryDto;
import com.okebari.artbite.payment.toss.dto.PaymentResDto;
import com.okebari.artbite.payment.toss.dto.PaymentSuccessDto;
import com.okebari.artbite.payment.toss.dto.SliceInfo;
import com.okebari.artbite.payment.toss.dto.SliceResponseDto;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag(name = "Toss Payments", description = "토스페이먼츠 연동 API")
@RestController
@RequestMapping("/api/payments/toss")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TossPaymentController {

	private final TossPaymentService tossPaymentService;
	private final TossPaymentConfig tossPaymentConfig;

	@Operation(summary = "결제 요청 생성", description = "토스페이먼츠 결제창 호출에 필요한 정보를 생성하고 반환합니다. (USER, ADMIN 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "결제 요청 정보 생성 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청 또는 이미 결제 대기 중",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = {
					@ExampleObject(name = "BadRequest", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C001\",\"message\":\"결제 금액은 100원 이상이어야 합니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"),
					@ExampleObject(name = "PaymentPending", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"P006\",\"message\":\"이미 결제 대기 중인 멤버십이 있습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}")
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
		@ApiResponse(responseCode = "500", description = "멤버십 활성화 실패 또는 서버 내부 오류",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "MembershipActivationFailed", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"M005\",\"message\":\"멤버십 활성화에 실패했습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"),
				mediaType = "application/json"))
	})
	@PostMapping
	@ResponseStatus(HttpStatus.OK)
	public CustomApiResponse<PaymentResDto> requestTossPayment(
		@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
		@Valid @RequestBody PaymentDto paymentDto) {

		Payment payment = tossPaymentService.requestTossPayment(paymentDto, customUserDetails.getUsername());

		// 프론트엔드에서 요청한 최종 리다이렉션 URL (없으면 기본값 사용)
		String finalFrontendSuccessUrl =
			(paymentDto.getYourSuccessUrl() != null && !paymentDto.getYourSuccessUrl().isBlank())
				? paymentDto.getYourSuccessUrl()
				: tossPaymentConfig.getFrontendSuccessUrl(); // 백엔드 설정의 프론트엔드 성공 URL

		String finalFrontendFailUrl = (paymentDto.getYourFailUrl() != null && !paymentDto.getYourFailUrl().isBlank())
			? paymentDto.getYourFailUrl()
			: tossPaymentConfig.getFrontendFailUrl(); // 백엔드 설정의 프론트엔드 실패 URL

		// Toss Payments에 전달할 백엔드 콜백 URL 구성
		// 여기에 프론트엔드의 최종 리다이렉션 URL을 쿼리 파라미터로 추가
		String tossCallbackSuccessUrl =
			tossPaymentConfig.getSuccessUrl() + "?frontendSuccessUrl=" + finalFrontendSuccessUrl + "&frontendFailUrl="
				+ finalFrontendFailUrl;
		String tossCallbackFailUrl =
			tossPaymentConfig.getFailUrl() + "?frontendSuccessUrl=" + finalFrontendSuccessUrl + "&frontendFailUrl="
				+ finalFrontendFailUrl;

		PaymentResDto paymentResDto = PaymentResDto.builder()
			.payType(payment.getPayType().getDescription())
			.amount(payment.getAmount())
			.orderName(payment.getOrderName())
			.orderId(payment.getOrderId())
			.customerEmail(customUserDetails.getUsername())
			.customerName(customUserDetails.getUser().getUsername())
			.successUrl(tossCallbackSuccessUrl) // Toss Payments에 전달할 백엔드 콜백 URL
			.failUrl(tossCallbackFailUrl)     // Toss Payments에 전달할 백엔드 콜백 URL
			.frontendSuccessUrl(finalFrontendSuccessUrl) // 프론트엔드에서 요청한 최종 성공 URL
			.frontendFailUrl(finalFrontendFailUrl)     // 프론트엔드에서 요청한 최종 실패 URL
			.createdAt(payment.getCreatedAt().toString())
			.build();

		return CustomApiResponse.success(paymentResDto);
	}

	@Operation(summary = "결제 성공 콜백", description = "토스페이먼츠로부터 결제 성공 알림을 받고 최종 승인 처리합니다. 이후 프론트엔드 성공 URL로 리다이렉트됩니다. (내부용)",
		externalDocs = @io.swagger.v3.oas.annotations.ExternalDocumentation(description = "토스페이먼츠 결제 성공 연동 가이드", url = "https://docs.tosspayments.com/reference/connectpay-api/api-v1-payments-key-confirm"))
	@ApiResponses({
		@ApiResponse(responseCode = "302", description = "프론트엔드 성공 URL로 리다이렉트"),
		@ApiResponse(responseCode = "500", description = "결제 승인 처리 중 오류 발생",
			content = @Content(schema = @Schema(hidden = true),
				examples = @ExampleObject(name = "InternalServerError", value = "리다이렉트 시 에러 메시지 포함"),
				mediaType = "text/html"))
	})
	@GetMapping("/success")
	public ModelAndView tossPaymentSuccess(
		@Parameter(description = "토스페이먼츠 결제 고유 키", required = true) @RequestParam String paymentKey,
		@Parameter(description = "주문 ID", required = true) @RequestParam String orderId,
		@Parameter(description = "결제 금액", required = true) @RequestParam Long amount,
		@Parameter(description = "결제 성공 시 최종 리다이렉트될 프론트엔드 URL", required = true) @RequestParam String frontendSuccessUrl,
		@Parameter(description = "결제 실패 시 최종 리다이렉트될 프론트엔드 URL", required = true) @RequestParam String frontendFailUrl) {
		try {
			PaymentSuccessDto paymentSuccessDto = tossPaymentService.confirmPayment(paymentKey, orderId, amount);
			log.info("Toss Payments 결제 성공: paymentKey={}, orderId={}, amount={}, status={}",
				paymentSuccessDto.getPaymentKey(), paymentSuccessDto.getOrderId(), paymentSuccessDto.getTotalAmount(),
				paymentSuccessDto.getStatus());
			// 결제 성공 시 프론트엔드의 성공 페이지로 리다이렉트
			return new ModelAndView("redirect:" + frontendSuccessUrl); // 프론트엔드 경로
		} catch (Exception e) {
			log.error("Toss Payments 결제 성공 처리 중 오류 발생: {}", e.getMessage(), e);
			// 결제 실패 시 프론트엔드의 실패 페이지로 리다이렉트
			ModelAndView modelAndView = new ModelAndView(
				"redirect:" + frontendFailUrl); // 프론트엔드 경로
			modelAndView.addObject("message", e.getMessage());
			return modelAndView;
		}
	}

	@Operation(summary = "결제 실패 콜백", description = "토스페이먼츠로부터 결제 실패 알림을 받고 실패 처리합니다. 이후 프론트엔드 실패 URL로 리다이렉트됩니다. (내부용)",
		externalDocs = @io.swagger.v3.oas.annotations.ExternalDocumentation(description = "토스페이먼츠 결제 실패 연동 가이드", url = "https://docs.tosspayments.com/reference/connectpay-api/api-v1-payments-key-confirm"))
	@ApiResponses({
		@ApiResponse(responseCode = "302", description = "프론트엔드 실패 URL로 리다이렉트"),
		@ApiResponse(responseCode = "500", description = "결제 실패 처리 중 오류 발생",
			content = @Content(schema = @Schema(hidden = true),
				examples = @ExampleObject(name = "InternalServerError", value = "리다이렉트 시 에러 메시지 포함"),
				mediaType = "text/html"))
	})
	@GetMapping("/fail")
	public ModelAndView tossPaymentFail(
		@Parameter(description = "토스페이먼츠 에러 코드", required = true) @RequestParam String code,
		@Parameter(description = "토스페이먼츠 에러 메시지", required = true) @RequestParam String message,
		@Parameter(description = "주문 ID", required = true) @RequestParam String orderId,
		@Parameter(description = "결제 성공 시 최종 리다이렉트될 프론트엔드 URL", required = true) @RequestParam String frontendSuccessUrl,
		@Parameter(description = "결제 실패 시 최종 리다이렉트될 프론트엔드 URL", required = true) @RequestParam String frontendFailUrl) {
		log.error("Toss Payments 결제 실패: code={}, message={}, orderId={}", code, message, orderId);
		tossPaymentService.failPayment(orderId, message); // DB에 실패 정보 저장

		// 결제 실패 시 프론트엔드의 실패 페이지로 리다이렉트
		ModelAndView modelAndView = new ModelAndView("redirect:" + frontendFailUrl); // 프론트엔드 경로
		modelAndView.addObject("code", code);
		modelAndView.addObject("message", message);
		modelAndView.addObject("orderId", orderId);
		return modelAndView;
	}

	@Operation(summary = "내 결제 내역 조회", description = "현재 로그인한 사용자의 결제 내역을 페이지네이션하여 조회합니다. (USER, ADMIN 권한 필요)")
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
	@GetMapping("/history")
	public CustomApiResponse<SliceResponseDto<PaymentHistoryDto>> getChargingHistory(
		@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
		@Parameter(hidden = true) Pageable pageable) {

		Slice<Payment> histories = tossPaymentService.findAllChargingHistories(customUserDetails.getUsername(),
			pageable);
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
}
