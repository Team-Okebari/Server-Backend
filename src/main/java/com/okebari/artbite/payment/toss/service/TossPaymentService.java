package com.okebari.artbite.payment.toss.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.okebari.artbite.common.exception.BusinessException;
import com.okebari.artbite.common.exception.ErrorCode;
import com.okebari.artbite.common.service.MdcLogging;
import com.okebari.artbite.domain.membership.Membership;
import com.okebari.artbite.domain.membership.MembershipRepository;
import com.okebari.artbite.domain.membership.MembershipStatus;
import com.okebari.artbite.domain.payment.Payment;
import com.okebari.artbite.domain.payment.PaymentRepository;
import com.okebari.artbite.domain.payment.PaymentStatus;
import com.okebari.artbite.domain.tracking.ContentAccessLogRepository;
import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.domain.user.UserRepository;
import com.okebari.artbite.membership.dto.MembershipStatusResponseDto;
import com.okebari.artbite.membership.service.MembershipService;
import com.okebari.artbite.note.repository.NoteRepository;
import com.okebari.artbite.payment.toss.config.TossPaymentConfig;
import com.okebari.artbite.payment.toss.dto.AdminUserPaymentDetailsDto;
import com.okebari.artbite.payment.toss.dto.PaymentDto;
import com.okebari.artbite.payment.toss.dto.PaymentHistoryDto;
import com.okebari.artbite.payment.toss.dto.PaymentSuccessDto;
import com.okebari.artbite.payment.toss.dto.SliceInfo;
import com.okebari.artbite.payment.toss.dto.SliceResponseDto;
import com.okebari.artbite.payment.toss.dto.TossPaymentCancelDto;
import com.okebari.artbite.tracking.service.ContentAccessLogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TossPaymentService {

	private final UserRepository userRepository;
	private final PaymentRepository paymentRepository;
	private final MembershipRepository membershipRepository;
	private final NoteRepository noteRepository;
	private final ContentAccessLogRepository contentAccessLogRepository;
	private final RestTemplate restTemplate;
	private final TossPaymentConfig tossPaymentConfig;
	private final MembershipService membershipService;
	private final ContentAccessLogService contentAccessLogService;
	private final ObjectMapper objectMapper;
	private TossPaymentService self; // Self-injection for proxy-based transactional calls

	@Autowired
	public TossPaymentService(UserRepository userRepository, PaymentRepository paymentRepository,
		MembershipRepository membershipRepository, NoteRepository noteRepository,
		ContentAccessLogRepository contentAccessLogRepository, RestTemplate restTemplate,
		TossPaymentConfig tossPaymentConfig, @Lazy MembershipService membershipService, ObjectMapper objectMapper,
		@Lazy TossPaymentService self, ContentAccessLogService contentAccessLogService) {
		this.userRepository = userRepository;
		this.paymentRepository = paymentRepository;
		this.membershipRepository = membershipRepository;
		this.noteRepository = noteRepository;
		this.contentAccessLogRepository = contentAccessLogRepository;
		this.restTemplate = restTemplate;
		this.tossPaymentConfig = tossPaymentConfig;
		this.membershipService = membershipService;
		this.contentAccessLogService = contentAccessLogService;
		this.objectMapper = objectMapper;
		this.self = self;
	}

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
		// 1. 결제 데이터를 검증하고, 상태를 CONFIRMING으로 변경 (DB 트랜잭션)
		self.prepareForConfirmation(orderId, amount);

		try {
			// 2. Toss Payments API에 결제 승인 요청 (외부 API 호출)
			PaymentSuccessDto tossPaymentResponse = requestPaymentAccept(paymentKey, orderId, amount);

			// 3. 내부 DB 처리 (DB 트랜잭션)
			self.processPaymentSuccess(orderId, paymentKey);

			return tossPaymentResponse;

		} catch (Exception e) {
			// 2번 또는 3번 과정에서 오류 발생 시, 결제 상태를 FAILED로 변경
			self.failPayment(orderId, e.getMessage());
			// 에러를 다시 던져서 GlobalExceptionHandler가 처리하도록 함
			throw e;
		}
	}

	/**
	 * 결제 승인 전, 결제 데이터를 검증하고 상태를 'CONFIRMING'으로 업데이트합니다.
	 * 이 과정은 단일 트랜잭션으로 처리됩니다.
	 */
	@Transactional
	public void prepareForConfirmation(String orderId, Long amount) {
		Payment payment = paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PAYMENT));

		if (!payment.getAmount().equals(amount)) {
			throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
		}

		if (payment.getStatus() != PaymentStatus.READY) {
			log.warn("이미 처리되었거나 대기 상태가 아닌 결제입니다. orderId: {}, status: {}", orderId, payment.getStatus());
			throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
		}

		payment.confirming();
	}

	/**
	 * 결제 성공 후 DB 상태를 업데이트하고 멤버십을 활성화하는 트랜잭션 메소드입니다.
	 * Spring AOP가 트랜잭션을 적용할 수 있도록 public으로 선언되어야 합니다.
	 * @param orderId 주문 ID
	 * @param paymentKey 토스 페이먼츠의 결제 키
	 */
	@Transactional
	public void processPaymentSuccess(String orderId, String paymentKey) {
		Payment payment = paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PAYMENT));

		// CONFIRMING 상태일 때만 후속 처리를 진행
		if (payment.getStatus() != PaymentStatus.CONFIRMING) {
			log.warn("[결제 승인 후 처리] 이미 처리되었거나 유효하지 않은 상태의 결제입니다. orderId: {}, status: {}", orderId, payment.getStatus());
			// 이 경우, 이미 다른 트랜잭션이나 복구 로직에 의해 처리되었을 가능성이 높으므로 에러를 던지지 않고 종료
			return;
		}

		payment.success(paymentKey);

		try {
			membershipService.activateMembership(payment.getUser().getId(), payment.getAmount(), payment.getPayType());
		} catch (Exception e) {
			log.error("멤버십 활성화 중 오류 발생: paymentId={}, userId={}, error={}", payment.getId(),
				payment.getUser().getId(), e.getMessage(), e);
			payment.processingFailed(e.getMessage());
			// 이 트랜잭션은 롤백되지 않고, payment의 상태가 PROCESSING_FAILED로 커밋됩니다.
			throw new BusinessException(ErrorCode.MEMBERSHIP_ACTIVATION_FAILED,
				"결제는 성공했으나 멤버십 활성화에 실패했습니다. 관리자에게 문의하세요.");
		}
	}

	@Transactional
	public void failPayment(String orderId, String failReason) {
		// findById 대신 findByOrderId 사용
		paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
			// CONFIRMING 또는 READY 상태일 때만 FAILED로 변경
			if (payment.getStatus() == PaymentStatus.CONFIRMING || payment.getStatus() == PaymentStatus.READY) {
				payment.fail(failReason);
			}
		});
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

	/**
	 * [관리자/사용자 요청] 환불 정책을 검증하고 환불을 요청합니다.
	 * @param paymentKey 환불할 결제의 paymentKey
	 * @param cancelReason 환불 사유
	 * @return Toss Payments로부터 받은 취소 완료 응답
	 */
	@Transactional
	public TossPaymentCancelDto requestRefundByUser(String paymentKey, String cancelReason) {
		Payment payment = paymentRepository.findByPaymentKey(paymentKey)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PAYMENT));

		// 1. 환불 기간 정책 검증 (7일 이내)
		if (payment.getCreatedAt().isBefore(LocalDateTime.now().minusDays(7))) {
			throw new BusinessException(ErrorCode.REFUND_PERIOD_EXPIRED);
		}

		// 2. 유료 콘텐츠 이용 여부 확인 정책 검증
		User user = payment.getUser();
		Optional<Membership> membershipOpt = membershipRepository.findTopByUserAndStatusInOrderByStartDateDesc(
			user, List.of(MembershipStatus.ACTIVE, MembershipStatus.CANCELED, MembershipStatus.EXPIRED));

		if (membershipOpt.isPresent()) {
			Membership membership = membershipOpt.get();
			if (contentAccessLogService.hasUserAccessedContentSince(user, membership.getStartDate())) {
				throw new BusinessException(ErrorCode.REFUND_CONTENT_ACCESSED);
			}
		}

		// 정책 검증 통과 후, 실제 환불 처리 호출
		return executeRefund(paymentKey, cancelReason);
	}

	/**
	 * [내부/시스템용] 정책 검증 없이 실제 환불을 실행합니다.
	 * 자동 환불 스케줄러 또는 정책 검증을 통과한 요청에 의해서만 호출되어야 합니다.
	 * @param paymentKey 환불할 결제의 paymentKey
	 * @param cancelReason 환불 사유
	 * @return Toss Payments로부터 받은 취소 완료 응답
	 */
	@Transactional
	public TossPaymentCancelDto executeRefund(String paymentKey, String cancelReason) {
		try (var ignored = MdcLogging.withContext("paymentKey", paymentKey)) {
			Payment payment = paymentRepository.findByPaymentKey(paymentKey)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PAYMENT));

			if (payment.getStatus() == PaymentStatus.CANCELED) {
				log.warn("이미 취소된 결제에 대한 환불 요청입니다.");
				throw new BusinessException(ErrorCode.ALREADY_CANCELED_PAYMENT);
			}

			if (payment.getStatus() != PaymentStatus.SUCCESS
				&& payment.getStatus() != PaymentStatus.PROCESSING_FAILED) {
				log.warn("환불 불가능한 상태의 결제에 대한 환불 요청입니다. 현재 상태: {}", payment.getStatus());
				throw new BusinessException(ErrorCode.PAYMENT_CANNOT_BE_REFUNDED);
			}

			TossPaymentCancelDto cancelResponse = callTossCancelApi(paymentKey, cancelReason);

			payment.cancel(cancelReason);

			// 관리자 환불 시, 해당 사용자의 활성 또는 취소된 멤버십을 EXPIRED 상태로 변경
			User user = payment.getUser();
			List<MembershipStatus> statusesToExpire = List.of(MembershipStatus.ACTIVE, MembershipStatus.CANCELED);
			membershipRepository.findTopByUserAndStatusInOrderByStartDateDesc(user, statusesToExpire)
				.ifPresent(membership -> {
					MembershipStatus oldStatus = membership.getStatus();
					membership.expire(); // 상태를 EXPIRED로 변경
					log.info("관리자 환불로 인해 사용자 '{}'의 멤버십(ID: {})을 EXPIRED로 변경했습니다. (이전 상태: {})",
						user.getEmail(), membership.getId(), oldStatus);
				});

			// @Transactional에 의해 메서드 종료 시 자동으로 save 처리됩니다.

			log.info("결제가 성공적으로 환불되었습니다. reason: {}", cancelReason);
			return cancelResponse;
		}
	}

	/**
	 * Toss Payments API에 결제 취소를 요청하는 외부 API 호출 메서드입니다.
	 * @param paymentKey 취소할 결제의 paymentKey
	 * @param cancelReason 취소 사유
	 * @return Toss Payments로부터 받은 취소 응답
	 */
	private TossPaymentCancelDto callTossCancelApi(String paymentKey, String cancelReason) {
		HttpHeaders headers = getHeaders();
		Map<String, String> requestBodyMap = new HashMap<>();
		requestBodyMap.put("cancelReason", cancelReason);

		HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBodyMap, headers);

		try {
			ResponseEntity<TossPaymentCancelDto> responseEntity = restTemplate.postForEntity(
				tossPaymentConfig.getUrl() + paymentKey + "/cancel",
				requestEntity,
				TossPaymentCancelDto.class
			);

			if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
				return responseEntity.getBody();
			} else {
				String responseBody = "";
				try {
					responseBody = objectMapper.writeValueAsString(responseEntity.getBody());
				} catch (Exception e) {
					// ignore
				}
				log.error("Toss Payments API cancel failed: status={}, body={}", responseEntity.getStatusCode(),
					responseBody);
				throw new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED, "Toss Payments 결제 취소 실패: " + responseBody);
			}
		} catch (Exception e) {
			log.error("Error calling Toss Payments API cancel: {}", e.getMessage(), e);
			throw new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED,
				"Toss Payments 결제 취소 중 오류 발생: " + e.getMessage());
		}
	}

	/**
	 * [외부 API] Toss Payments API를 통해 주문 ID로 결제 정보를 조회합니다.
	 * 복구 스케줄러가 중간 상태의 결제의 최종 상태를 확인할 때 사용합니다.
	 * @param orderId 조회할 주문 ID
	 * @return 조회된 결제 정보
	 */
	public PaymentSuccessDto fetchPaymentByOrderId(String orderId) {
		HttpHeaders headers = getHeaders();
		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

		try {
			ResponseEntity<PaymentSuccessDto> responseEntity = restTemplate.exchange(
				tossPaymentConfig.getUrl() + "orders/" + orderId,
				HttpMethod.GET,
				requestEntity,
				PaymentSuccessDto.class
			);

			if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
				return responseEntity.getBody();
			} else {
				String responseBody = "";
				try {
					responseBody = objectMapper.writeValueAsString(responseEntity.getBody());
				} catch (Exception e) {
					// ignore
				}
				log.error("Toss Payments API fetch by orderId failed: status={}, body={}",
					responseEntity.getStatusCode(), responseBody);
				throw new BusinessException(ErrorCode.PAYMENT_FETCH_FAILED, "Toss 결제 조회 실패: " + responseBody);
			}
		} catch (Exception e) {
			log.error("Error calling Toss Payments API fetch by orderId: {}", e.getMessage(), e);
			throw new BusinessException(ErrorCode.PAYMENT_FETCH_FAILED, "Toss 결제 조회 중 오류 발생: " + e.getMessage());
		}
	}

	/**
	 * [관리자용] 특정 사용자의 모든 결제 내역을 조회합니다.
	 * @param userEmail 조회할 사용자의 이메일
	 * @param pageable 페이징 정보
	 * @return 결제 내역 슬라이스
	 */
	public Slice<Payment> findAllPaymentsByUserEmailForAdmin(String userEmail, Pageable pageable) {
		userRepository.findByEmail(userEmail)
			.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));
		return paymentRepository.findAllByUserEmail(userEmail, pageable);
	}

	public Slice<Payment> findAllChargingHistories(String userEmail, Pageable pageable) {
		userRepository.findByEmail(userEmail)
			.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));
		return paymentRepository.findAllByUserEmailAndStatus(userEmail, PaymentStatus.SUCCESS, pageable);
	}

	@Transactional(readOnly = true)
	public AdminUserPaymentDetailsDto getAdminUserPaymentDetails(String userEmail, Pageable paymentPageable,
		Pageable accessLogPageable) {
		User user = userRepository.findByEmail(userEmail)
			.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

		// 1. 멤버십 상태 조회
		MembershipStatusResponseDto membershipStatus = membershipService.getMembershipInfo(user.getId());

		// 2. 결제 내역 조회
		Slice<Payment> paymentSlice = findAllPaymentsByUserEmailForAdmin(userEmail, paymentPageable);
		List<PaymentHistoryDto> paymentHistoryDtos = paymentSlice.getContent().stream()
			.map(payment -> new PaymentHistoryDto(
				payment.getId(),
				payment.getPaymentKey(),
				payment.getAmount(),
				payment.getOrderName(),
				payment.getStatus(),
				payment.getCreatedAt()
			))
			.toList();
		SliceResponseDto<PaymentHistoryDto> paymentHistoryResponse = new SliceResponseDto<>(paymentHistoryDtos,
			new SliceInfo(paymentPageable, paymentSlice.getNumberOfElements(), paymentSlice.hasNext()));

		// 3. 콘텐츠 접근 기록 조회
		Slice<com.okebari.artbite.domain.tracking.ContentAccessLog> accessLogSlice = contentAccessLogRepository.findByUserOrderByAccessedAtDesc(
			user, accessLogPageable);
		List<com.okebari.artbite.tracking.dto.ContentAccessLogDto> accessLogDtos = accessLogSlice.getContent()
			.stream()
			.map(log -> new com.okebari.artbite.tracking.dto.ContentAccessLogDto(
				log.getNote().getId(),
				log.getNote().getCover().getTitle(),
				log.getAccessedAt()
			))
			.toList();
		SliceResponseDto<com.okebari.artbite.tracking.dto.ContentAccessLogDto> accessLogResponse = new SliceResponseDto<>(
			accessLogDtos,
			new SliceInfo(accessLogPageable, accessLogSlice.getNumberOfElements(), accessLogSlice.hasNext()));

		// 4. 종합 DTO 생성
		return new com.okebari.artbite.payment.toss.dto.AdminUserPaymentDetailsDto(
			user.getId(),
			user.getEmail(),
			membershipStatus,
			paymentHistoryResponse,
			accessLogResponse
		);
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
