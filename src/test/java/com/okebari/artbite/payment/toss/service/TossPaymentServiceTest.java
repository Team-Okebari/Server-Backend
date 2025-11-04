package com.okebari.artbite.payment.toss.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.okebari.artbite.AbstractContainerBaseTest;
import com.okebari.artbite.common.exception.BusinessException;
import com.okebari.artbite.common.exception.ErrorCode;
import com.okebari.artbite.domain.membership.Membership;
import com.okebari.artbite.domain.membership.MembershipRepository;
import com.okebari.artbite.domain.membership.MembershipStatus;
import com.okebari.artbite.domain.payment.Payment;
import com.okebari.artbite.domain.payment.PaymentRepository;
import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.domain.user.UserRepository;
import com.okebari.artbite.domain.user.UserRole;
import com.okebari.artbite.payment.toss.config.TossPaymentConfig;
import com.okebari.artbite.payment.toss.dto.PayType;
import com.okebari.artbite.payment.toss.dto.PaymentSuccessDto;

@SpringBootTest
@Transactional
class TossPaymentServiceTest extends AbstractContainerBaseTest {

	@Autowired
	private TossPaymentService tossPaymentService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private MembershipRepository membershipRepository;

	@Autowired
	private TossPaymentConfig tossPaymentConfig;

	@MockitoBean
	private RestTemplate restTemplate;

	private User testUser;

	@BeforeEach
	void setUp() {
		// Clean up database before each test
		paymentRepository.deleteAll();
		membershipRepository.deleteAll();
		userRepository.deleteAll();

		// Create and save a test user
		testUser = User.builder()
			.email("testuser@example.com")
			.password("password")
			.username("Test User")
			.role(UserRole.USER)
			.build();
		userRepository.saveAndFlush(testUser);
	}

	@Test
	@DisplayName("결제 승인 성공 시, 결제와 멤버십 상태가 정상적으로 업데이트된다")
	void confirmPayment_success() {
		// Given: 결제 대기중인 Payment 엔티티 생성
		String orderId = UUID.randomUUID().toString();
		String paymentKey = "test_payment_key_12345";
		long amount = tossPaymentConfig.getMembershipAmount();

		Payment pendingPayment = Payment.builder()
			.user(testUser)
			.payType(PayType.CARD)
			.amount(amount)
			.orderName("ArtBite 월간 구독")
			.orderId(orderId)
			.paySuccessYN(false)
			.build();
		paymentRepository.save(pendingPayment);

		// Mocking: Toss Payments API의 성공적인 응답을 모의 설정
		PaymentSuccessDto mockTossResponse = new PaymentSuccessDto();
		// 필요한 경우 mockTossResponse에 필드 설정

		when(restTemplate.postForEntity(
			eq(tossPaymentConfig.getUrl() + "confirm"),
			any(HttpEntity.class),
			eq(PaymentSuccessDto.class)
		)).thenReturn(ResponseEntity.ok(mockTossResponse));

		// When: 결제 승인 서비스 호출
		tossPaymentService.confirmPayment(paymentKey, orderId, amount);

		// Then: 결제 상태 검증
		Payment confirmedPayment = paymentRepository.findByOrderId(orderId).orElseThrow();
		assertThat(confirmedPayment.isPaySuccessYN()).isTrue();
		assertThat(confirmedPayment.getPaymentKey()).isEqualTo(paymentKey);

		// Then: 멤버십 상태 검증
		Membership activatedMembership = membershipRepository.findTopByUserAndStatusOrderByStartDateDesc(testUser,
			MembershipStatus.ACTIVE).orElseThrow();
		assertThat(activatedMembership).isNotNull();
		assertThat(activatedMembership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);

		// Verify: 외부 API가 정확히 1번 호출되었는지 확인
		verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(PaymentSuccessDto.class));
	}

	@Test
	@DisplayName("결제 승인 실패 - 이미 처리된 결제")
	void confirmPayment_fail_alreadyProcessed() {
		// Given: 이미 성공적으로 처리된 Payment 엔티티 생성
		String orderId = UUID.randomUUID().toString();
		String paymentKey = "test_payment_key_processed";
		long amount = tossPaymentConfig.getMembershipAmount();

		Payment successfulPayment = Payment.builder()
			.user(testUser)
			.payType(PayType.CARD)
			.amount(amount)
			.orderName("ArtBite 월간 구독")
			.orderId(orderId)
			.paymentKey(paymentKey)
			.paySuccessYN(true) // 이미 성공한 상태
			.build();
		paymentRepository.save(successfulPayment);

		// When & Then: 동일한 결제를 다시 승인하려고 할 때 예외가 발생하는지 검증
		BusinessException exception = assertThrows(BusinessException.class, () -> {
			tossPaymentService.confirmPayment(paymentKey, orderId, amount);
		});

		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_ALREADY_PROCESSED);

		// Verify: 외부 API가 호출되지 않았는지 확인
		verify(restTemplate, never()).postForEntity(anyString(), any(), any());
	}

	@Test
	@DisplayName("결제 승인 실패 - 금액 불일치")
	void confirmPayment_fail_amountMismatch() {
		// Given: 결제 대기중인 Payment 엔티티 생성
		String orderId = UUID.randomUUID().toString();
		String paymentKey = "test_payment_key_mismatch";
		long originalAmount = tossPaymentConfig.getMembershipAmount();
		long wrongAmount = originalAmount + 100L;

		Payment pendingPayment = Payment.builder()
			.user(testUser)
			.payType(PayType.CARD)
			.amount(originalAmount)
			.orderName("ArtBite 월간 구독")
			.orderId(orderId)
			.paySuccessYN(false)
			.build();
		paymentRepository.save(pendingPayment);

		// When & Then: 다른 금액으로 승인을 시도할 때 예외가 발생하는지 검증
		BusinessException exception = assertThrows(BusinessException.class, () -> {
			tossPaymentService.confirmPayment(paymentKey, orderId, wrongAmount);
		});

		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
		verify(restTemplate, never()).postForEntity(anyString(), any(), any());
	}

	@Test
	@DisplayName("결제 승인 실패 - 존재하지 않는 주문")
	void confirmPayment_fail_paymentNotFound() {
		// Given: 존재하지 않는 주문 정보
		String nonExistentOrderId = "non_existent_order_id";
		String paymentKey = "test_payment_key_not_found";
		long amount = tossPaymentConfig.getMembershipAmount();

		// When & Then: 존재하지 않는 주문 ID로 승인을 시도할 때 예외가 발생하는지 검증
		BusinessException exception = assertThrows(BusinessException.class, () -> {
			tossPaymentService.confirmPayment(paymentKey, nonExistentOrderId, amount);
		});

		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND_PAYMENT);

		// Verify: 외부 API가 호출되지 않았는지 확인
		verify(restTemplate, never()).postForEntity(anyString(), any(), any());
	}

	@Test
	@DisplayName("결제 승인 실패 - Toss API 오류")
	void confirmPayment_fail_tossApiError() {
		// Given: 결제 대기중인 Payment 엔티티 생성
		String orderId = UUID.randomUUID().toString();
		String paymentKey = "test_payment_key_api_error";
		long amount = tossPaymentConfig.getMembershipAmount();

		Payment pendingPayment = Payment.builder()
			.user(testUser)
			.payType(PayType.CARD)
			.amount(amount)
			.orderName("ArtBite 월간 구독")
			.orderId(orderId)
			.paySuccessYN(false)
			.build();
		paymentRepository.save(pendingPayment);

		// Mocking: Toss Payments API의 실패 응답을 모의 설정
		when(restTemplate.postForEntity(
			eq(tossPaymentConfig.getUrl() + "confirm"),
			any(HttpEntity.class),
			eq(PaymentSuccessDto.class)
		)).thenThrow(new RuntimeException("Toss API call failed"));

		// When & Then: Toss API 호출 실패 시 예외가 발생하는지 검증
		BusinessException exception = assertThrows(BusinessException.class, () -> {
			tossPaymentService.confirmPayment(paymentKey, orderId, amount);
		});

		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_CONFIRM_FAILED);

		// Then: DB 상태가 변경되지 않았는지 확인
		Payment paymentAfterFail = paymentRepository.findByOrderId(orderId).orElseThrow();
		assertThat(paymentAfterFail.isPaySuccessYN()).isFalse();

		long membershipCount = membershipRepository.count();
		assertThat(membershipCount).isZero();
	}
}
