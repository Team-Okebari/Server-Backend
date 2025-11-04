package com.okebari.artbite.payment.toss.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.okebari.artbite.common.exception.BusinessException;
import com.okebari.artbite.common.exception.ErrorCode;
import com.okebari.artbite.domain.payment.Payment;
import com.okebari.artbite.domain.payment.PaymentRepository;
import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.domain.user.UserRepository;
import com.okebari.artbite.membership.service.MembershipService;
import com.okebari.artbite.payment.toss.config.TossPaymentConfig;
import com.okebari.artbite.payment.toss.dto.PaymentDto;
import com.okebari.artbite.payment.toss.dto.PaymentSuccessDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TossPaymentService {

	private final UserRepository userRepository;
	private final PaymentRepository paymentRepository;
	private final RestTemplate restTemplate;
	private final TossPaymentConfig tossPaymentConfig;
	private final MembershipService membershipService;
	private final ObjectMapper objectMapper;

	@Transactional
	public Payment requestTossPayment(PaymentDto paymentDto, String userEmail) {
		User user = userRepository.findByEmail(userEmail)
			.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

		// 고정된 구독 금액과 요청된 금액이 일치하는지 확인
		if (!paymentDto.getAmount().equals(tossPaymentConfig.getMembershipAmount())) {
			throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH, "구독 금액이 올바르지 않습니다.");
		}

		Payment payment = Payment.builder()
			.user(user)
			.payType(paymentDto.getPayType())
			.amount(paymentDto.getAmount())
			.orderName(paymentDto.getOrderName())
			.orderId(UUID.randomUUID().toString()) // 고유한 주문 ID 생성
			.paySuccessYN(false) // 초기에는 결제 미승인 상태
			.build();

		return paymentRepository.save(payment);
	}

	@Transactional
	public PaymentSuccessDto confirmPayment(String paymentKey, String orderId, Long amount) {
		Payment payment = verifyPayment(orderId, amount); // 금액 검증

		PaymentSuccessDto tossPaymentResponse = requestPaymentAccept(paymentKey, orderId,
			amount); // Toss API 호출 및 응답 파싱

		// 5. 결제 성공 시 DB 업데이트
		payment.success(paymentKey);
		paymentRepository.save(payment);

		// 6. 멤버십 활성화 로직 호출
		membershipService.activateMembership(payment.getUser().getId(), payment.getAmount(), payment.getPayType());

		return tossPaymentResponse;
	}

	@Transactional
	public void failPayment(String orderId, String failReason) {
		Payment payment = paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PAYMENT));
		payment.fail(failReason);
		paymentRepository.save(payment);
	}

	// --- 헬퍼 메서드 --- //

	private HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		String secretKey = tossPaymentConfig.getTestSecretKey() + ":";
		String encodedAuth = Base64.getEncoder().encodeToString(secretKey.getBytes(StandardCharsets.UTF_8));
		headers.setBasicAuth(encodedAuth);
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		return headers;
	}

	private Payment verifyPayment(String orderId, Long amount) {
		Payment payment = paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PAYMENT));

		if (!payment.getAmount().equals(amount)) {
			throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
		}
		return payment;
	}

	private PaymentSuccessDto requestPaymentAccept(String paymentKey, String orderId, Long amount) {
		HttpHeaders headers = getHeaders();
		Map<String, Object> requestBodyMap = new HashMap<>();
		requestBodyMap.put("orderId", orderId);
		requestBodyMap.put("amount", amount);
		requestBodyMap.put("paymentKey", paymentKey);
		HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBodyMap, headers);
		try {
			ResponseEntity<PaymentSuccessDto> responseEntity = restTemplate.postForEntity(
				tossPaymentConfig.URL + "confirm",
				requestEntity,
				PaymentSuccessDto.class
			);
			if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
				return responseEntity.getBody();
			} else {
				log.error("Toss Payments API confirm failed: status={}, body={}", responseEntity.getStatusCode(),
					responseEntity.getBody());
				throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED,
					"Toss Payments 결제 승인 실패: " + responseEntity.getBody());
			}
		} catch (Exception e) {
			log.error("Error calling Toss Payments API confirm: {}", e.getMessage(), e);
			throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED,
				"Toss Payments 결제 승인 중 오류 발생: " + e.getMessage());
		}
	}

	@Transactional
	public PaymentSuccessDto cancelPayment(String userEmail, String paymentKey, String cancelReason) {
		Payment payment = paymentRepository.findByPaymentKeyAndUserEmail(paymentKey, userEmail)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PAYMENT, "결제 정보를 찾을 수 없거나 취소 권한이 없습니다."));

		// Toss Payments API 호출
		PaymentSuccessDto tossCancelResponse = requestTossCancelApi(paymentKey, cancelReason);

		// DB에 취소 상태 업데이트
		payment.cancel(cancelReason);
		paymentRepository.save(payment);

		// TODO: 포인트 관련 로직이 있다면 여기서 처리. (현재는 멤버십 모델이므로 생략)

		return tossCancelResponse;
	}

	private PaymentSuccessDto requestTossCancelApi(String paymentKey, String cancelReason) {
		HttpHeaders headers = getHeaders();
		Map<String, String> requestBodyMap = new HashMap<>();
		requestBodyMap.put("cancelReason", cancelReason);

		HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBodyMap, headers);

		try {
			ResponseEntity<PaymentSuccessDto> responseEntity = restTemplate.postForEntity(
				TossPaymentConfig.URL + paymentKey + "/cancel",
				requestEntity,
				PaymentSuccessDto.class
			);

			if (responseEntity.getStatusCode().is2xxSuccessful()) {
				return responseEntity.getBody();
			} else {
				log.error("Toss Payments API cancel failed: status={}, body={}", responseEntity.getStatusCode(),
					responseEntity.getBody());
				throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED,
					"Toss Payments 결제 취소 실패: " + responseEntity.getBody());
			}
		} catch (Exception e) {
			log.error("Error calling Toss Payments API cancel: {}", e.getMessage(), e);
			throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED,
				"Toss Payments 결제 취소 중 오류 발생: " + e.getMessage());
		}
	}

	public Slice<Payment> findAllChargingHistories(String userEmail, Pageable pageable) {
		userRepository.findByEmail(userEmail)
			.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));
		return paymentRepository.findAllByUserEmail(userEmail, pageable);
	}
}
