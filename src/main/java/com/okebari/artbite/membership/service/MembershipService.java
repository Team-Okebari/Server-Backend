package com.okebari.artbite.membership.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.okebari.artbite.common.exception.BusinessException;
import com.okebari.artbite.common.exception.ErrorCode;
import com.okebari.artbite.domain.membership.Membership;
import com.okebari.artbite.domain.membership.MembershipPlanType;
import com.okebari.artbite.domain.membership.MembershipRepository;
import com.okebari.artbite.domain.membership.MembershipStatus;
import com.okebari.artbite.domain.payment.PaymentRepository;
import com.okebari.artbite.domain.payment.PaymentStatus;
import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.domain.user.UserRepository;
import com.okebari.artbite.membership.dto.MembershipStatusResponseDto;
import com.okebari.artbite.payment.toss.dto.PayType;
import com.okebari.artbite.payment.toss.service.TossPaymentService;
import com.okebari.artbite.tracking.service.ContentAccessLogService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MembershipService {

	private final MembershipRepository membershipRepository;
	private final UserRepository userRepository;
	private final PaymentRepository paymentRepository;
	private final ContentAccessLogService contentAccessLogService;
	private final TossPaymentService tossPaymentService;

	@Value("${membership.default-plan-type}")
	private String defaultPlanType;

	@Value("${membership.default-duration-months}")
	private int defaultDurationMonths;

	@Value("${membership.default-auto-renew}")
	private boolean defaultAutoRenew;

	@Autowired
	public MembershipService(MembershipRepository membershipRepository, UserRepository userRepository,
		PaymentRepository paymentRepository, ContentAccessLogService contentAccessLogService,
		@Lazy TossPaymentService tossPaymentService) {
		this.membershipRepository = membershipRepository;
		this.userRepository = userRepository;
		this.paymentRepository = paymentRepository;
		this.contentAccessLogService = contentAccessLogService;
		this.tossPaymentService = tossPaymentService;
	}

	@Transactional
	public MembershipStatusResponseDto activateMembership(Long userId, Long amount, PayType payType) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

		// BANNED 상태의 멤버십이 있는지 확인
		Optional<Membership> bannedMembership = membershipRepository.findByUserAndStatus(user, MembershipStatus.BANNED);
		if (bannedMembership.isPresent()) {
			throw new BusinessException(ErrorCode.MEMBERSHIP_BANNED);
		}

		// 기존 활성 멤버십 확인
		Optional<Membership> activeMembership = membershipRepository.findTopByUserAndStatusOrderByStartDateDesc(user,
			MembershipStatus.ACTIVE);
		if (activeMembership.isPresent()) {
			throw new BusinessException(ErrorCode.MEMBERSHIP_ALREADY_ACTIVE);
		}

		// 재활성화를 위해 기존 EXPIRED 멤버십 확인 (CANCELED는 결제 없이 재활성화되므로 제외)
		Optional<Membership> existingMembership = membershipRepository.findTopByUserAndStatusInOrderByStartDateDesc(
			user, List.of(MembershipStatus.EXPIRED));

		Membership membership;
		LocalDateTime now = LocalDateTime.now().toLocalDate().atStartOfDay();
		LocalDateTime endDate = now.plusMonths(defaultDurationMonths).toLocalDate().atStartOfDay();

		if (existingMembership.isPresent()) {
			membership = existingMembership.get();
			// 기존 멤버십 업데이트
			membership.activate(now, endDate, membership.getConsecutiveMonths() + 1, defaultAutoRenew);
		} else {
			// 새 멤버십 생성
			membership = Membership.builder()
				.user(user)
				.status(MembershipStatus.ACTIVE)
				.planType(MembershipPlanType.valueOf(defaultPlanType))
				.startDate(now)
				.endDate(endDate)
				.consecutiveMonths(1)
				.autoRenew(defaultAutoRenew)
				.build();
		}

		Membership savedMembership = membershipRepository.save(membership);
		return MembershipStatusResponseDto.builder()
			.status(savedMembership.getStatus())
			.planType(savedMembership.getPlanType())
			.startDate(savedMembership.getStartDate())
			.endDate(savedMembership.getEndDate())
			.consecutiveMonths(savedMembership.getConsecutiveMonths())
			.autoRenew(savedMembership.isAutoRenew())
			.build();
	}

	@Transactional
	public void cancelMembership(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

		Membership membership = membershipRepository.findTopByUserAndStatusOrderByStartDateDesc(user,
				MembershipStatus.ACTIVE)
			.orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));

		// 가장 최근의 성공한 결제 건을 찾음
		Optional<com.okebari.artbite.domain.payment.Payment> lastPaymentOpt = paymentRepository.findTopByUserAndStatusOrderByCreatedAtDesc(
			user, PaymentStatus.SUCCESS);

		boolean isRefundable = false;
		if (lastPaymentOpt.isPresent()) {
			com.okebari.artbite.domain.payment.Payment lastPayment = lastPaymentOpt.get();
			// 1. 7일 이내 결제 건인지 확인
			boolean within7Days = lastPayment.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7));
			// 2. 콘텐츠 이용 내역이 없는지 확인
			boolean contentNotAccessed = !contentAccessLogService.hasUserAccessedContentSince(user,
				membership.getStartDate());

			if (within7Days && contentNotAccessed) {
				isRefundable = true;
			}
		}

		if (isRefundable) {
			// --- 청약철회(환불) 처리 ---
			log.info("청약철회 조건을 만족하여 환불 및 멤버십 즉시 만료 처리를 시작합니다. userId: {}", userId);
			tossPaymentService.executeRefund(lastPaymentOpt.get().getPaymentKey(), "7일 이내 청약철회");
			membership.expire(); // 즉시 만료 처리
		} else {
			// --- 일반 구독 취소(자동 연장 해지) 처리 ---
			log.info("일반 구독 취소(자동 연장 해지) 처리를 시작합니다. userId: {}", userId);
			membership.cancel();
		}

		membershipRepository.save(membership);
	}

	@Transactional
	public MembershipStatusResponseDto reactivateCanceledMembership(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

		Membership membership = membershipRepository.findTopByUserAndStatusOrderByStartDateDesc(user,
				MembershipStatus.CANCELED)
			.orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND, "취소된 멤버십을 찾을 수 없습니다."));

		// CANCELED 멤버십을 ACTIVE로 변경하고 자동 갱신 설정
		membership.activate(membership.getStartDate(), membership.getEndDate(), membership.getConsecutiveMonths(),
			defaultAutoRenew);
		Membership savedMembership = membershipRepository.save(membership);

		return MembershipStatusResponseDto.builder()
			.status(savedMembership.getStatus())
			.planType(savedMembership.getPlanType())
			.startDate(savedMembership.getStartDate())
			.endDate(savedMembership.getEndDate())
			.consecutiveMonths(savedMembership.getConsecutiveMonths())
			.autoRenew(savedMembership.isAutoRenew())
			.build();
	}

	@Transactional(readOnly = true)
	public MembershipStatusResponseDto getMembershipInfo(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

		Membership membership = membershipRepository.findTopByUserAndStatusInOrderByStartDateDesc(
				user, List.of(MembershipStatus.ACTIVE, MembershipStatus.CANCELED, MembershipStatus.EXPIRED,
					MembershipStatus.BANNED))
			.orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));

		return MembershipStatusResponseDto.builder()
			.status(membership.getStatus())
			.planType(membership.getPlanType())
			.startDate(membership.getStartDate())
			.endDate(membership.getEndDate())
			.consecutiveMonths(membership.getConsecutiveMonths())
			.autoRenew(membership.isAutoRenew())
			.build();
	}

	private void renewMembership(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

		Membership membership = membershipRepository.findTopByUserAndStatusOrderByStartDateDesc(user,
				MembershipStatus.ACTIVE)
			.orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));

		if (!membership.isAutoRenew()) {
			log.warn("Membership {} for user {} is not set for auto-renewal. Skipping.", membership.getId(), userId);
			return; // 스케줄러가 올바르게 필터링하면 발생하지 않아야 함
		}

		// TODO: 멤버십 갱신 시 자동 결제 연동은 PG사 시스템(예: 정기 결제 API)과의 연동 방식에 따라 구현해야 합니다.
		boolean paymentSuccess = true; // 결제 성공 목킹

		if (!paymentSuccess) {
			membership.expire(); // 결제 실패 시 EXPIRED로 전환
			membershipRepository.save(membership);
			throw new BusinessException(ErrorCode.PAYMENT_FAILED);
		}

		LocalDateTime newEndDate = membership.getEndDate().plusMonths(1);
		membership.renew(newEndDate, membership.getConsecutiveMonths() + 1);
		membershipRepository.save(membership);
	}

	@Transactional
	public void banMembership(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

		Membership membership = membershipRepository.findTopByUserAndStatusInOrderByStartDateDesc(
				user, List.of(MembershipStatus.ACTIVE, MembershipStatus.CANCELED,
					MembershipStatus.EXPIRED)) // 활성, 취소 또는 만료된 멤버십을 정지할 수 있음
			.orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));

		membership.ban();
		membershipRepository.save(membership);
	}

	@Transactional
	public void unbanMembership(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

		Membership membership = membershipRepository.findByUserAndStatus(user, MembershipStatus.BANNED)
			.orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND, "정지된 멤버십이 아닙니다."));

		membership.unban(); // 상태를 EXPIRED로 설정
		membershipRepository.save(membership);
	}

	// 이 메소드는 스케줄러에 의해 호출됩니다.
	@Transactional
	public void processMembershipExpirations() {
		LocalDateTime now = LocalDateTime.now();
		// ACTIVE 또는 CANCELED 상태이며 종료일이 지난 멤버십 찾기
		List<Membership> membershipsToExpire = membershipRepository.findByStatusInAndEndDateBefore(
			List.of(MembershipStatus.ACTIVE, MembershipStatus.CANCELED), now);

		for (Membership membership : membershipsToExpire) {
			membership.expire();
			membershipRepository.save(membership);
			log.info("Membership {} for user {} has expired.", membership.getId(), membership.getUser().getId());
		}
	}

	// 이 메소드는 스케줄러에 의해 호출됩니다.
	@Transactional
	public void processMembershipRenewals() {
		LocalDateTime now = LocalDateTime.now();
		// ACTIVE 상태이고 자동 갱신이 true이며, 종료일이 갱신 기간(예: 1일 전) 내에 있거나 종료일인 멤버십 찾기
		LocalDateTime renewalWindowEnd = now.plusDays(1); // 예시: 만료 1일 전 갱신
		List<Membership> membershipsToRenew = membershipRepository.findByStatusAndAutoRenewTrueAndEndDateBefore(
			// Before 또는 Equal로 변경됨
			MembershipStatus.ACTIVE, renewalWindowEnd);

		for (Membership membership : membershipsToRenew) {
			try {
				// renewMembership 로직 호출 (결제 포함)
				renewMembership(membership.getUser().getId());
				log.info("Membership {} for user {} successfully renewed.", membership.getId(),
					membership.getUser().getId());
			} catch (BusinessException e) {
				log.error("Failed to renew membership {} for user {}: {}", membership.getId(),
					membership.getUser().getId(), e.getMessage());
				// renewMembership은 이미 결제 실패 시 EXPIRED로 설정하는 것을 처리함
			}
		}
	}
}
