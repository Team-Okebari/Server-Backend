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
import com.okebari.artbite.payment.toss.dto.PaymentCancelDto;
import com.okebari.artbite.payment.toss.dto.PaymentDto;
import com.okebari.artbite.payment.toss.dto.PaymentHistoryDto;
import com.okebari.artbite.payment.toss.dto.PaymentResDto;
import com.okebari.artbite.payment.toss.dto.PaymentSuccessDto;
import com.okebari.artbite.payment.toss.dto.SliceInfo;
import com.okebari.artbite.payment.toss.dto.SliceResponseDto;
import com.okebari.artbite.payment.toss.service.TossPaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/payments/toss")
@RequiredArgsConstructor
public class TossPaymentController {

	private final TossPaymentService tossPaymentService;
	private final TossPaymentConfig tossPaymentConfig;

	@PostMapping
	@ResponseStatus(HttpStatus.OK)
	public CustomApiResponse<PaymentResDto> requestTossPayment(
		@AuthenticationPrincipal CustomUserDetails customUserDetails,
		@Valid @RequestBody PaymentDto paymentDto) {

		Payment payment = tossPaymentService.requestTossPayment(paymentDto, customUserDetails.getUsername());

		PaymentResDto paymentResDto = PaymentResDto.builder()
			.payType(payment.getPayType().getDescription())
			.amount(payment.getAmount())
			.orderName(payment.getOrderName())
			.orderId(payment.getOrderId())
			.customerEmail(customUserDetails.getUsername())
			.customerName(customUserDetails.getUser().getUsername())
			.successUrl(tossPaymentConfig.getSuccessUrl())
			.failUrl(tossPaymentConfig.getFailUrl())
			.createdAt(payment.getCreatedAt().toString())
			.build();

		return CustomApiResponse.success(paymentResDto);
	}

	@GetMapping("/success")
	public ModelAndView tossPaymentSuccess(
		@RequestParam String paymentKey,
		@RequestParam String orderId,
		@RequestParam Long amount) {
		try {
			PaymentSuccessDto paymentSuccessDto = tossPaymentService.confirmPayment(paymentKey, orderId, amount);
			log.info("Toss Payments 결제 성공: paymentKey={}, orderId={}, amount={}, status={}",
				paymentSuccessDto.getPaymentKey(), paymentSuccessDto.getOrderId(), paymentSuccessDto.getTotalAmount(),
				paymentSuccessDto.getStatus());
			// 결제 성공 시 프론트엔드의 성공 페이지로 리다이렉트
			return new ModelAndView("redirect:" + tossPaymentConfig.getFrontendSuccessUrl()); // 프론트엔드 경로
		} catch (Exception e) {
			log.error("Toss Payments 결제 성공 처리 중 오류 발생: {}", e.getMessage(), e);
			// 결제 실패 시 프론트엔드의 실패 페이지로 리다이렉트
			ModelAndView modelAndView = new ModelAndView(
				"redirect:" + tossPaymentConfig.getFrontendFailUrl()); // 프론트엔드 경로
			modelAndView.addObject("message", e.getMessage());
			return modelAndView;
		}
	}

	@GetMapping("/fail")
	public ModelAndView tossPaymentFail(
		@RequestParam String code,
		@RequestParam String message,
		@RequestParam String orderId) {
		log.error("Toss Payments 결제 실패: code={}, message={}, orderId={}", code, message, orderId);
		tossPaymentService.failPayment(orderId, message); // DB에 실패 정보 저장

		// 결제 실패 시 프론트엔드의 실패 페이지로 리다이렉트
		ModelAndView modelAndView = new ModelAndView("redirect:" + tossPaymentConfig.getFrontendFailUrl()); // 프론트엔드 경로
		modelAndView.addObject("code", code);
		modelAndView.addObject("message", message);
		modelAndView.addObject("orderId", orderId);
		return modelAndView;
	}

	@PostMapping("/cancel")
	public CustomApiResponse<PaymentSuccessDto> tossPaymentCancel(
		@AuthenticationPrincipal CustomUserDetails customUserDetails,
		@Valid @RequestBody PaymentCancelDto paymentCancelDto) {

		PaymentSuccessDto cancelResponse = tossPaymentService.cancelPayment(
			customUserDetails.getUsername(),
			paymentCancelDto.getPaymentKey(),
			paymentCancelDto.getCancelReason()
		);

		return CustomApiResponse.success(cancelResponse);
	}

	@GetMapping("/history")
	public CustomApiResponse<SliceResponseDto<PaymentHistoryDto>> getChargingHistory(
		@AuthenticationPrincipal CustomUserDetails customUserDetails,
		Pageable pageable) {

		Slice<Payment> histories = tossPaymentService.findAllChargingHistories(customUserDetails.getUsername(),
			pageable);
		SliceInfo sliceInfo = new SliceInfo(pageable, histories.getNumberOfElements(), histories.hasNext());

		List<PaymentHistoryDto> paymentHistoryDtos = histories.getContent().stream()
			.map(payment -> PaymentHistoryDto.builder()
				.paymentHistoryId(payment.getId())
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
