package com.okebari.artbite.membership.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.okebari.artbite.AbstractContainerBaseTest;
import com.okebari.artbite.auth.vo.CustomUserDetails;
import com.okebari.artbite.domain.membership.Membership;
import com.okebari.artbite.domain.membership.MembershipPlanType;
import com.okebari.artbite.domain.membership.MembershipRepository;
import com.okebari.artbite.domain.membership.MembershipStatus;
import com.okebari.artbite.domain.payment.Payment;
import com.okebari.artbite.domain.payment.PaymentRepository;
import com.okebari.artbite.domain.payment.PaymentStatus;
import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.domain.user.UserRepository;
import com.okebari.artbite.domain.user.UserRole;
import com.okebari.artbite.membership.service.MembershipService;
import com.okebari.artbite.payment.toss.config.TossPaymentConfig;
import com.okebari.artbite.payment.toss.dto.PayType;
import com.okebari.artbite.payment.toss.dto.PaymentDto;
import com.okebari.artbite.payment.toss.dto.PaymentSuccessDto;
import com.okebari.artbite.payment.toss.dto.TossPaymentCancelDto;
import com.okebari.artbite.tracking.service.ContentAccessLogService;

import software.amazon.awssdk.services.s3.S3Client;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "cloud.aws.s3.bucket=dummy-bucket")
class MembershipControllerTest extends AbstractContainerBaseTest {

	@MockitoBean
	private S3Client s3Client;
	@MockitoBean
	private ContentAccessLogService contentAccessLogService; // Mock for new dependency

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private MembershipService membershipService;

	@Autowired
	private MembershipRepository membershipRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private TossPaymentConfig tossPaymentConfig;

	@MockitoBean // 외부 API 호출을 Mocking
	private RestTemplate restTemplate;

	// Test users
	private User regularUser;
	private User adminUser;
	private UserDetails regularUserDetails;
	private UserDetails adminUserDetails;

	@BeforeEach
	void setUp() {
		// 참조 무결성을 위해 자식 테이블부터 삭제
		paymentRepository.deleteAll();
		membershipRepository.deleteAll();
		userRepository.deleteAll();

		regularUser = User.builder()
			.email("testuser@example.com")
			.password("password")
			.username("Test User")
			.role(UserRole.USER)
			.enabled(true)
			.accountNonExpired(true)
			.accountNonLocked(true)
			.credentialsNonExpired(true)
			.tokenVersion(1)
			.build();
		userRepository.saveAndFlush(regularUser);
		regularUserDetails = new CustomUserDetails(regularUser);

		adminUser = User.builder()
			.email("admin@example.com")
			.password("password")
			.username("Admin User")
			.role(UserRole.ADMIN)
			.enabled(true)
			.accountNonExpired(true)
			.accountNonLocked(true)
			.credentialsNonExpired(true)
			.tokenVersion(1)
			.build();
		userRepository.saveAndFlush(adminUser);
		adminUserDetails = new CustomUserDetails(adminUser);
	}

	@Test
	@DisplayName("Toss Payments 연동 멤버십 가입 성공 통합 테스트")
	void enrollMembershipWithTossPayment_Success() throws Exception {
		// Given: 결제 승인 성공 상황 Mocking
		long amount = tossPaymentConfig.getMembershipAmount();
		String paymentKey = "test_payment_key_12345";
		PaymentSuccessDto mockTossResponse = PaymentSuccessDto.builder()
			.status("DONE")
			.totalAmount(amount)
			.build();

		when(restTemplate.postForEntity(
			eq("https://api.tosspayments.com/v1/payments/confirm"),
			any(HttpEntity.class),
			eq(PaymentSuccessDto.class)
		)).thenReturn(ResponseEntity.ok(mockTossResponse));

		// When: 1. 프론트엔드가 백엔드에 결제 정보 요청
		PaymentDto paymentDto = new PaymentDto();
		paymentDto.setPayType(PayType.CARD);
		paymentDto.setAmount(amount);
		paymentDto.setOrderName("ArtBite 월간 구독");
		paymentDto.setMembershipPlanType(MembershipPlanType.DEFAULT_MEMBER_PLAN);

		String responseContent = mockMvc.perform(post("/api/payments/toss")
				.with(user(regularUserDetails)) // 인증된 사용자
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(paymentDto)))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();

		JsonNode responseNode = objectMapper.readTree(responseContent);
		String orderId = responseNode.get("data").get("orderId").asText();

		// When: 2. Toss Payments가 백엔드의 successUrl로 리다이렉트 (인증 없이 호출)
		mockMvc.perform(get("/api/payments/toss/success")
				.param("paymentKey", paymentKey)
				.param("orderId", orderId)
				.param("amount", String.valueOf(amount))
				.param("frontendSuccessUrl", tossPaymentConfig.getFrontendSuccessUrl()) // 추가
				.param("frontendFailUrl", tossPaymentConfig.getFrontendFailUrl()))     // 추가
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl(tossPaymentConfig.getFrontendSuccessUrl())); // 성공 URL로 리다이렉트 검증

		// Then: 멤버십 상태가 ACTIVE로 변경되었는지 검증
		LocalDateTime expectedStartDate = LocalDateTime.now().toLocalDate().atStartOfDay();
		Membership membership = membershipRepository.findTopByUserAndStatusOrderByStartDateDesc(regularUser,
				MembershipStatus.ACTIVE)
			.orElseThrow(() -> new AssertionError("활성화된 멤버십을 찾을 수 없습니다."));

		assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		assertThat(membership.getStartDate().toLocalDate()).isEqualTo(expectedStartDate.toLocalDate());
	}

	@Test
	@DisplayName("스마트 취소 - 일반 취소 성공 (콘텐츠 이용)")
	void cancelMembership_asStandardCancel_whenContentAccessed() throws Exception {
		// Given: 활성 멤버십과 최근 결제 내역 생성
		activateAndPay(regularUser);
		when(contentAccessLogService.hasUserAccessedContentSince(any(), any())).thenReturn(true); // 콘텐츠 이용함
		// When & Then
		mockMvc.perform(post("/api/memberships/cancel")
				.with(user(regularUserDetails)))
			.andExpect(status().isOk());
		Membership canceled = membershipRepository.findTopByUserAndStatusOrderByStartDateDesc(regularUser,
				MembershipStatus.CANCELED)
			.orElseThrow(() -> new AssertionError("Canceled membership not found"));
		assertThat(canceled.getStatus()).isEqualTo(MembershipStatus.CANCELED);
	}

	@Test
	@DisplayName("스마트 취소 - 환불 성공 (7일 이내, 콘텐츠 미이용)")
	void cancelMembership_asRefund_whenNotAccessed() throws Exception {
		// Given: 활성 멤버십과 최근 결제 내역 생성
		Payment payment = activateAndPay(regularUser);
		when(contentAccessLogService.hasUserAccessedContentSince(any(), any())).thenReturn(false); // 콘텐츠 미이용

		// Mocking: Toss Payments 환불 API의 성공 응답 모의 설정
		when(restTemplate.postForEntity(
			eq("https://api.tosspayments.com/v1/payments/" + payment.getPaymentKey() + "/cancel"),
			any(HttpEntity.class),
			eq(TossPaymentCancelDto.class)
		)).thenReturn(ResponseEntity.ok(
			new TossPaymentCancelDto(null, null, null, null, null, null, null, null, 0L, 0L, "CANCELED", null, null,
				null, null, null)));
		// When & Then
		mockMvc.perform(post("/api/memberships/cancel")
				.with(user(regularUserDetails)))
			.andExpect(status().isOk());
		// Then
		Membership expired = membershipRepository.findTopByUserAndStatusOrderByStartDateDesc(regularUser,
				MembershipStatus.EXPIRED)
			.orElseThrow(() -> new AssertionError("Expired membership not found"));
		assertThat(expired.getStatus()).isEqualTo(MembershipStatus.EXPIRED); // 환불 시 즉시 만료
	}

	@Test
	@DisplayName("멤버십 상태 조회 성공: 활성 멤버십")
	void getMembershipStatus_active() throws Exception {
		// 활성 멤버십 생성
		membershipService.activateMembership(regularUser.getId(), 1500L, PayType.CARD);

		mockMvc.perform(get("/api/memberships/status")
				.with(user(regularUserDetails)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.status").value("ACTIVE"));
	}

	@Test
	@DisplayName("멤버십 상태 조회 성공: 취소된 멤버십")
	void getMembershipStatus_canceled() throws Exception {
		// Given: 취소된 멤버십을 직접 생성
		Membership canceledMembership = Membership.builder()
			.user(regularUser)
			.status(MembershipStatus.CANCELED)
			.planType(MembershipPlanType.DEFAULT_MEMBER_PLAN)
			.startDate(LocalDateTime.now().minusMonths(2))
			.endDate(LocalDateTime.now().plusMonths(1))
			.consecutiveMonths(1)
			.autoRenew(false)
			.build();
		membershipRepository.save(canceledMembership);

		// When & Then
		mockMvc.perform(get("/api/memberships/status")
				.with(user(regularUserDetails)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.status").value("CANCELED"));
	}

	@Test
	@DisplayName("멤버십 상태 조회 성공: 만료된 멤버십")
	void getMembershipStatus_expired() throws Exception {
		// 만료된 멤버십 생성
		Membership expiredMembership = Membership.builder()
			.user(regularUser)
			.status(MembershipStatus.EXPIRED)
			.planType(MembershipPlanType.DEFAULT_MEMBER_PLAN)
			.startDate(LocalDateTime.now().minusMonths(2))
			.endDate(LocalDateTime.now().minusMonths(1))
			.consecutiveMonths(1)
			.autoRenew(false)
			.build();
		membershipRepository.save(expiredMembership);

		mockMvc.perform(get("/api/memberships/status")
				.with(user(regularUserDetails)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.status").value("EXPIRED"));
	}

	@Test
	@DisplayName("멤버십 상태 조회 성공: 정지된 멤버십")
	void getMembershipStatus_banned() throws Exception {
		// 정지된 멤버십 생성 (activateMembership 후 banMembership 호출)
		activateAndPay(regularUser);
		membershipService.banMembership(regularUser.getId());

		mockMvc.perform(get("/api/memberships/status")
				.with(user(regularUserDetails)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.status").value("BANNED"));
	}

	private Payment activateAndPay(User user) {
		return transactionTemplate.execute(status -> {
			membershipService.activateMembership(user.getId(), 1500L, PayType.CARD);
			Payment payment = Payment.builder()
				.user(user)
				.status(PaymentStatus.SUCCESS)
				.paymentKey("test_key_" + UUID.randomUUID())
				.amount(1500L)
				.orderId("order_" + UUID.randomUUID())
				.orderName("test")
				.payType(PayType.CARD)
				.build();
			return paymentRepository.save(payment);
		});
	}
}
