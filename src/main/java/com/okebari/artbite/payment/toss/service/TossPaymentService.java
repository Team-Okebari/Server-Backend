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

		// Check if the user already has an ACTIVE membership of the requested type
		membershipRepository.findByUserAndStatusAndPlanType(user, MembershipStatus.ACTIVE,
				paymentDto.getMembershipPlanType())
			.ifPresent(membership -> {
				throw new BusinessException(ErrorCode.MEMBERSHIP_ALREADY_ACTIVE);
			});

		// Check if the user has a CANCELED membership of the requested type
		membershipRepository.findByUserAndStatusAndPlanType(user, MembershipStatus.CANCELED,
				paymentDto.getMembershipPlanType())
			.ifPresent(membership -> {
				throw new BusinessException(ErrorCode.MEMBERSHIP_CANCELED_CANNOT_RENEW);
			});

		// Check if the user has a BANNED membership of the requested type
		membershipRepository.findByUserAndStatusAndPlanType(user, MembershipStatus.BANNED,
				paymentDto.getMembershipPlanType())
			.ifPresent(membership -> {
				throw new BusinessException(ErrorCode.MEMBERSHIP_BANNED);
			});

		// 멤버십 플랜 타입에 따른 금액 검증
		Long expectedAmount = paymentDto.getMembershipPlanType().getAmount();
		if (!paymentDto.getAmount().equals(expectedAmount)) {
			throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH, "요청된 결제 금액이 멤버십 플랜의 금액과 일치하지 않습니다.");
		}

		// orderName이 비어있을 경우 설정 파일의 기본값을 사용
		String orderName = (paymentDto.getOrderName() == null || paymentDto.getOrderName().isBlank())
			? tossPaymentConfig.getOrderName()
			: paymentDto.getOrderName();

		Payment payment = Payment.builder()
			.user(user)
			.payType(paymentDto.getPayType())
			.amount(paymentDto.getAmount())
			.orderName(orderName)
			.orderId(UUID.randomUUID().toString()) // 고유한 주문 ID 생성
			.status(PaymentStatus.READY) // 초기에는 결제 대기 상태
			.build();

		return paymentRepository.save(payment);
	}

	public PaymentSuccessDto confirmPayment(String paymentKey, String orderId, Long amount) {
		Payment payment = verifyPayment(orderId, amount); // 금액 검증

		// 멱등성 체크: 이미 결제가 성공적으로 처리되었다면 중복 실행을 방지.
		if (payment.getStatus() == PaymentStatus.SUCCESS) {
			log.warn("이미 처리된 결제입니다. orderId: {}", orderId);
			// 여기서 이미 저장된 결제 성공 정보를 바탕으로 PaymentSuccessDto를 재구성하거나,
			// 간단하게 성공했다는 응답을 보낼 수 있습니다. Toss의 응답을 재현하기는 어려우므로,
			// 클라이언트가 오해하지 않도록 명확한 응답을 주는 것이 좋습니다.
			// 여기서는 예외를 발생시켜 클라이언트에게 이미 처리되었음을 명확히 알립니다.
			throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
		}

		// 1. Toss Payments 결제 승인 API 호출 (트랜잭션 외부에서 실행)
		PaymentSuccessDto tossPaymentResponse = requestPaymentAccept(paymentKey, orderId, amount);

		// 2. DB 업데이트 로직을 별도의 트랜잭션 메소드로 호출
		// 만약 여기서 예외가 발생하면, Toss 결제는 이미 완료되었으므로,
		// 해당 오류를 로깅하고 개발자가 수동으로 처리해야 하는 심각한 상황입니다.
		// (고도화: 재시도 큐에 넣는 방식 등)
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
		// 결제 성공 시 DB 업데이트
		payment.success(paymentKey);
		paymentRepository.save(payment);

		try {
			// 멤버십 활성화 로직 호출
			membershipService.activateMembership(payment.getUser().getId(), payment.getAmount(), payment.getPayType());
		} catch (Exception e) {
			log.error("멤버십 활성화 중 오류 발생: paymentId={}, userId={}, error={}", payment.getId(),
				payment.getUser().getId(), e.getMessage(), e);
			payment.processingFailed(e.getMessage());
			paymentRepository.save(payment);
			// 이 시점에서 클라이언트에게는 결제 성공으로 응답이 갔을 것이므로,
			// 이 오류는 별도의 모니터링 및 수동 처리가 필요합니다.
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
