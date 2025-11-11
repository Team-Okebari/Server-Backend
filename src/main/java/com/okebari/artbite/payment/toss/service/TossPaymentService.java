package com.okebari.artbite.payment.toss.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.okebari.artbite.common.exception.BusinessException;
import com.okebari.artbite.common.exception.ErrorCode;
import com.okebari.artbite.domain.membership.MembershipRepository;
import com.okebari.artbite.domain.membership.MembershipStatus;
import com.okebari.artbite.domain.payment.Payment;
import com.okebari.artbite.domain.payment.PaymentRepository;
import com.okebari.artbite.domain.payment.PaymentStatus;
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
	private final MembershipRepository membershipRepository;
	private final RestTemplate restTemplate;
	private final TossPaymentConfig tossPaymentConfig;
	private final MembershipService membershipService;
	private final ObjectMapper objectMapper;

	@Transactional
	public Payment requestTossPayment(PaymentDto paymentDto, String userEmail) {
		User user = userRepository.findByEmail(userEmail)
			.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

		membershipRepository.findByUserAndStatusAndPlanType(user, MembershipStatus.ACTIVE,
				paymentDto.getMembershipPlanType())
			.ifPresent(membership -> {
				throw new BusinessException(ErrorCode.MEMBERSHIP_ALREADY_ACTIVE);
			});

		membershipRepository.findByUserAndStatusAndPlanType(user, MembershipStatus.CANCELED,
				paymentDto.getMembershipPlanType())
			.ifPresent(membership -> {
				throw new BusinessException(ErrorCode.MEMBERSHIP_CANCELED_CANNOT_RENEW);
			});

		membershipRepository.findByUserAndStatusAndPlanType(user, MembershipStatus.BANNED,
				paymentDto.getMembershipPlanType())
			.ifPresent(membership -> {
				throw new BusinessException(ErrorCode.MEMBERSHIP_BANNED);
			});

		Long expectedAmount = tossPaymentConfig.getMembershipAmount();
		if (!paymentDto.getAmount().equals(expectedAmount)) {
			throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH, "요청된 결제 금액이 멤버십 플랜의 금액과 일치하지 않습니다.");
		}

		String orderName = (paymentDto.getOrderName() == null || paymentDto.getOrderName().isBlank())
			? tossPaymentConfig.getOrderName()
			: paymentDto.getOrderName();

		Payment payment = Payment.builder()
			.user(user)
			.payType(paymentDto.getPayType())
			.amount(paymentDto.getAmount())
			.orderName(orderName)
			.orderId(UUID.randomUUID().toString())
			.status(PaymentStatus.READY)
			.build();

		return paymentRepository.save(payment);
	}

	public PaymentSuccessDto confirmPayment(String paymentKey, String orderId, Long amount) {
		Payment payment = verifyPayment(orderId, amount);

		if (payment.getStatus() == PaymentStatus.SUCCESS) {
			log.warn("이미 처리된 결제입니다. orderId: {}", orderId);
			throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
		}

		PaymentSuccessDto tossPaymentResponse = requestPaymentAccept(paymentKey, orderId, amount);

		processPaymentSuccess(payment, paymentKey);

		return tossPaymentResponse;
	}

	/**
	 * 결제 성공 후 DB 상태를 업데이트하고 멤버십을 활성화하는 트랜잭션 메소드입니다.
	 * Spring AOP가 트랜잭션을 적용할 수 있도록 public으로 선언되어야 합니다.
	 * @param payment 결제 엔티티
	 * @param paymentKey 토스 페이먼츠의 결제 키
	 */
	@Transactional
	public void processPaymentSuccess(Payment payment, String paymentKey) {
		payment.success(paymentKey);
		paymentRepository.save(payment);

		try {
			membershipService.activateMembership(payment.getUser().getId(), payment.getAmount(), payment.getPayType());
		} catch (Exception e) {
			log.error("멤버십 활성화 중 오류 발생: paymentId={}, userId={}, error={}", payment.getId(),
				payment.getUser().getId(), e.getMessage(), e);
			payment.processingFailed(e.getMessage());
			paymentRepository.save(payment);
			throw new BusinessException(ErrorCode.MEMBERSHIP_ACTIVATION_FAILED,
				"결제는 성공했으나 멤버십 활성화에 실패했습니다. 관리자에게 문의하세요.");
		}
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
				tossPaymentConfig.getUrl() + "confirm",
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
		// 환불 기능은 현재 일시적으로 비활성화되어 있습니다.
		throw new BusinessException(ErrorCode.REFUND_TEMPORARILY_DISABLED);
	}

	private PaymentSuccessDto requestTossCancelApi(String paymentKey, String cancelReason) {
		HttpHeaders headers = getHeaders();
		Map<String, String> requestBodyMap = new HashMap<>();
		requestBodyMap.put("cancelReason", cancelReason);

		HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBodyMap, headers);

		try {
			ResponseEntity<PaymentSuccessDto> responseEntity = restTemplate.postForEntity(
				tossPaymentConfig.getUrl() + paymentKey + "/cancel",
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
		return paymentRepository.findAllByUserEmailAndStatus(userEmail, PaymentStatus.SUCCESS, pageable);
	}

	@Scheduled(fixedRateString = "${payment.toss.ready-check-interval-ms}")
	@Transactional
	public void processStaleReadyPayments() {
		LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(tossPaymentConfig.getReadyTimeoutMinutes());
		List<Payment> staleReadyPayments = paymentRepository.findByStatusAndCreatedAtBefore(PaymentStatus.READY,
			timeoutThreshold);

		if (!staleReadyPayments.isEmpty()) {
			log.warn("{}개의 오래된 READY 상태 결제를 FAILED로 전환합니다.", staleReadyPayments.size());
		}

		for (Payment payment : staleReadyPayments) {
			payment.fail("결제 시간 초과 또는 미완료");
			paymentRepository.save(payment);
			log.info("Payment {} (orderId: {}) 상태를 READY에서 FAILED로 전환했습니다.", payment.getId(),
				payment.getOrderId());
		}
	}
}
