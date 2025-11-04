package com.okebari.artbite.membership.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.okebari.artbite.common.exception.BusinessException;
import com.okebari.artbite.common.exception.ErrorCode;
import com.okebari.artbite.domain.membership.Membership;
import com.okebari.artbite.domain.membership.MembershipPlanType;
import com.okebari.artbite.domain.membership.MembershipRepository;
import com.okebari.artbite.domain.membership.MembershipStatus;
import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.domain.user.UserRepository;
import com.okebari.artbite.membership.dto.EnrollMembershipRequestDto;
import com.okebari.artbite.membership.dto.MembershipStatusResponseDto;
import com.okebari.artbite.payment.toss.dto.PayType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipService {

	private final MembershipRepository membershipRepository;
	private final UserRepository userRepository;

	// 기존 enrollMembership은 결제 전 요청을 처리하도록 변경하거나 제거될 수 있음
	// 현재는 임시로 남겨두고 activateMembership을 추가
	@Transactional
	public MembershipStatusResponseDto enrollMembership(Long userId, EnrollMembershipRequestDto requestDto) {
		// 이 메서드는 이제 결제 로직을 직접 수행하지 않고, 결제 서비스에서 호출될 activateMembership을 준비하는 역할로 변경될 수 있습니다.
		// 또는 이 엔드포인트 자체가 결제 전 주문 생성 역할로 변경될 수 있습니다.
		// 현재는 기존 로직을 유지하되, 실제 결제는 TossPaymentService에서 처리하도록 합니다.
		// TODO: 이 메서드의 역할 재정의 필요 (결제 전 주문 생성 또는 제거)
		return activateMembership(userId, 0L, PayType.CARD); // 임시 호출, 실제 결제 금액과 타입은 TossPaymentService에서 전달
	}

	@Transactional
	public MembershipStatusResponseDto activateMembership(Long userId, Long amount, PayType payType) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

		// 기존 활성 멤버십 확인
		Optional<Membership> activeMembership = membershipRepository.findTopByUserAndStatusOrderByStartDateDesc(user,
			MembershipStatus.ACTIVE);
		if (activeMembership.isPresent()) {
			throw new BusinessException(ErrorCode.MEMBERSHIP_ALREADY_ACTIVE);
		}

		// 재활성화를 위해 기존 EXPIRED 또는 CANCELED 멤버십 확인
		Optional<Membership> existingMembership = membershipRepository.findTopByUserAndStatusInOrderByStartDateDesc(
			user, List.of(MembershipStatus.EXPIRED, MembershipStatus.CANCELED));

		Membership membership;
		LocalDateTime now = LocalDateTime.now().toLocalDate().atStartOfDay();
		LocalDateTime endDate = now.plusMonths(1).toLocalDate().atStartOfDay(); // 기본 1개월 멤버십, 다음 달 00시 00분으로 설정

		if (existingMembership.isPresent()) {
			membership = existingMembership.get();
			// 기존 멤버십 업데이트
			membership.activate(now, endDate, membership.getConsecutiveMonths() + 1, true); // 자동 갱신 여부는 결제 시점에 결정
		} else {
			// 새 멤버십 생성
			membership = Membership.builder()
				.user(user)
				.status(MembershipStatus.ACTIVE)
				.planType(MembershipPlanType.DEFAULT_MEMBER_PLAN)
				.startDate(now)
				.endDate(endDate)
				.consecutiveMonths(1)
				.autoRenew(true) // 자동 갱신 여부는 결제 시점에 결정
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

		membership.cancel();
		membershipRepository.save(membership);
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
