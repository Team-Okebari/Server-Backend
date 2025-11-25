package com.okebari.artbite.membership.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.okebari.artbite.common.exception.BusinessException;
import com.okebari.artbite.common.exception.ErrorCode;
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
import com.okebari.artbite.membership.dto.MembershipStatusResponseDto;
import com.okebari.artbite.payment.toss.service.TossPaymentService;
import com.okebari.artbite.tracking.service.ContentAccessLogService;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

	@Mock
	private MembershipRepository membershipRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private PaymentRepository paymentRepository;
	@Mock
	private ContentAccessLogService contentAccessLogService;
	@Mock
	private TossPaymentService tossPaymentService;

	private MembershipService membershipService;

	private User testUser;

	@BeforeEach
	void setUp() {
		// Manual instantiation to handle @Lazy injection
		membershipService = new MembershipService(
			membershipRepository,
			userRepository,
			paymentRepository,
			contentAccessLogService,
			tossPaymentService // Pass the mock directly for @Lazy
		);

		testUser = User.builder()
			.email("test@example.com")
			.password("password")
			.role(UserRole.USER)
			.enabled(true)
			.accountNonExpired(true)
			.accountNonLocked(true)
			.credentialsNonExpired(true)
			.tokenVersion(0)
			.build();
		ReflectionTestUtils.setField(testUser, "id", 1L);

		// Set default values for @Value fields
		ReflectionTestUtils.setField(membershipService, "defaultPlanType", "DEFAULT_MEMBER_PLAN");
		ReflectionTestUtils.setField(membershipService, "defaultDurationMonths", 1);
		ReflectionTestUtils.setField(membershipService, "defaultAutoRenew", true);
	}

	@Test
	@DisplayName("스마트 취소 - 환불 경로 성공 (7일 이내, 콘텐츠 미사용)")
	void cancelMembership_refundPath_success() {
		// Given
		Membership activeMembership = new Membership(testUser, MembershipStatus.ACTIVE,
			MembershipPlanType.DEFAULT_MEMBER_PLAN,
			LocalDateTime.now().minusDays(1), LocalDateTime.now().plusMonths(1), 1, true);
		Payment recentPayment = Payment.builder().user(testUser).status(PaymentStatus.SUCCESS).build();
		ReflectionTestUtils.setField(recentPayment, "createdAt", LocalDateTime.now().minusDays(1)); // 1일 전 결제

		when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
		when(membershipRepository.findTopByUserAndStatusOrderByStartDateDesc(testUser, MembershipStatus.ACTIVE))
			.thenReturn(Optional.of(activeMembership));
		when(paymentRepository.findTopByUserAndStatusOrderByCreatedAtDesc(testUser, PaymentStatus.SUCCESS))
			.thenReturn(Optional.of(recentPayment));
		when(contentAccessLogService.hasUserAccessedContentSince(testUser, activeMembership.getStartDate()))
			.thenReturn(false); // 콘텐츠 미사용

		// When
		membershipService.cancelMembership(testUser.getId());

		// Then
		verify(tossPaymentService, times(1)).executeRefund(any(), any());
		assertThat(activeMembership.getStatus()).isEqualTo(MembershipStatus.EXPIRED); // 즉시 만료 처리 확인
		verify(membershipRepository, times(1)).save(activeMembership);
	}

	@Test
	@DisplayName("스마트 취소 - 일반 취소 경로 성공 (콘텐츠 사용)")
	void cancelMembership_standardPath_contentAccessed() {
		// Given
		Membership activeMembership = new Membership(testUser, MembershipStatus.ACTIVE,
			MembershipPlanType.DEFAULT_MEMBER_PLAN,
			LocalDateTime.now().minusDays(1), LocalDateTime.now().plusMonths(1), 1, true);
		Payment recentPayment = Payment.builder().user(testUser).status(PaymentStatus.SUCCESS).build();
		ReflectionTestUtils.setField(recentPayment, "createdAt", LocalDateTime.now().minusDays(1)); // 1일 전 결제

		when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
		when(membershipRepository.findTopByUserAndStatusOrderByStartDateDesc(testUser, MembershipStatus.ACTIVE))
			.thenReturn(Optional.of(activeMembership));
		when(paymentRepository.findTopByUserAndStatusOrderByCreatedAtDesc(testUser, PaymentStatus.SUCCESS))
			.thenReturn(Optional.of(recentPayment));
		when(contentAccessLogService.hasUserAccessedContentSince(testUser, activeMembership.getStartDate()))
			.thenReturn(true); // 콘텐츠 사용

		// When
		membershipService.cancelMembership(testUser.getId());

		// Then
		verify(tossPaymentService, never()).executeRefund(any(), any());
		assertThat(activeMembership.getStatus()).isEqualTo(MembershipStatus.CANCELED); // 자동 연장만 해지
		assertThat(activeMembership.isAutoRenew()).isFalse();
		verify(membershipRepository, times(1)).save(activeMembership);
	}

	@Test
	@DisplayName("스마트 취소 - 일반 취소 경로 성공 (7일 경과)")
	void cancelMembership_standardPath_7daysPassed() {
		// Given
		Membership activeMembership = new Membership(testUser, MembershipStatus.ACTIVE,
			MembershipPlanType.DEFAULT_MEMBER_PLAN,
			LocalDateTime.now().minusDays(10), LocalDateTime.now().plusMonths(1), 1, true);
		Payment oldPayment = Payment.builder().user(testUser).status(PaymentStatus.SUCCESS).build();
		ReflectionTestUtils.setField(oldPayment, "createdAt", LocalDateTime.now().minusDays(8)); // 8일 전 결제

		when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
		when(membershipRepository.findTopByUserAndStatusOrderByStartDateDesc(testUser, MembershipStatus.ACTIVE))
			.thenReturn(Optional.of(activeMembership));
		when(paymentRepository.findTopByUserAndStatusOrderByCreatedAtDesc(testUser, PaymentStatus.SUCCESS))
			.thenReturn(Optional.of(oldPayment));

		// When
		membershipService.cancelMembership(testUser.getId());

		// Then
		verify(tossPaymentService, never()).executeRefund(any(), any());
		assertThat(activeMembership.getStatus()).isEqualTo(MembershipStatus.CANCELED);
		assertThat(activeMembership.isAutoRenew()).isFalse();
		verify(membershipRepository, times(1)).save(activeMembership);
	}

	@Test
	@DisplayName("멤버십 취소 실패 - 활성 멤버십 없음")
	void cancelMembership_fail_noActiveMembership() {
		// Given
		when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
		when(membershipRepository.findTopByUserAndStatusOrderByStartDateDesc(testUser, MembershipStatus.ACTIVE))
			.thenReturn(Optional.empty());

		// When & Then
		BusinessException exception = assertThrows(BusinessException.class,
			() -> membershipService.cancelMembership(testUser.getId()));
		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBERSHIP_NOT_FOUND);
		verify(membershipRepository, never()).save(any(Membership.class));
	}

	@Test
	@DisplayName("멤버십 정보 조회 성공 - 활성 멤버십 존재")
	void getMembershipInfo_success_activeMembership() {
		// Given
		Membership activeMembership = Membership.builder()
			.user(testUser)
			.status(MembershipStatus.ACTIVE)
			.planType(MembershipPlanType.DEFAULT_MEMBER_PLAN)
			.startDate(LocalDateTime.now().minusMonths(1))
			.endDate(LocalDateTime.now().plusMonths(1))
			.autoRenew(true)
			.build();

		when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
		when(membershipRepository.findTopByUserAndStatusInOrderByStartDateDesc(eq(testUser), anyList()))
			.thenReturn(Optional.of(activeMembership));

		// When
		MembershipStatusResponseDto result = membershipService.getMembershipInfo(testUser.getId());

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		assertThat(result.getPlanType()).isEqualTo(MembershipPlanType.DEFAULT_MEMBER_PLAN);
	}

	@Test
	@DisplayName("멤버십 정보 조회 성공 - 활성 멤버십 없음")
	void getMembershipInfo_success_noActiveMembership() {
		// Given
		when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
		when(membershipRepository.findTopByUserAndStatusInOrderByStartDateDesc(eq(testUser), anyList()))
			.thenReturn(Optional.empty());

		// When & Then
		BusinessException exception = assertThrows(BusinessException.class,
			() -> membershipService.getMembershipInfo(testUser.getId()));
		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBERSHIP_NOT_FOUND);
	}

	@Test
	@DisplayName("멤버십 정보 조회 실패 - 사용자 없음")
	void getMembershipInfo_fail_userNotFound() {
		// Given
		when(userRepository.findById(testUser.getId())).thenReturn(Optional.empty());

		// When & Then
		BusinessException exception = assertThrows(BusinessException.class,
			() -> membershipService.getMembershipInfo(testUser.getId()));
		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_USER_NOT_FOUND);
	}
}
