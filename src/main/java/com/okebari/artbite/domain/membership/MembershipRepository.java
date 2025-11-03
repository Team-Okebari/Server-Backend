package com.okebari.artbite.domain.membership;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.okebari.artbite.domain.user.User;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

	// 사용자의 최신 멤버십을 상태별로 찾고, 시작일 내림차순으로 정렬
	Optional<Membership> findTopByUserAndStatusOrderByStartDateDesc(User user, MembershipStatus status);

	// 사용자의 모든 멤버십을 상태별로 찾고, 시작일 내림차순으로 정렬
	List<Membership> findByUserAndStatusOrderByStartDateDesc(User user, MembershipStatus status);

	// 사용자와 상태로 멤버십 찾기
	Optional<Membership> findByUserAndStatus(User user, MembershipStatus status);

	// 사용자와 상태(재가입을 위해 EXPIRED 또는 CANCELED 상태를 찾음)로 멤버십 찾기
	Optional<Membership> findTopByUserAndStatusInOrderByStartDateDesc(User user, List<MembershipStatus> statuses);

	// --- 스케줄러를 위한 메소드 ---
	// ACTIVE 또는 CANCELED 상태이며 end_date가 지난 멤버십 찾기
	List<Membership> findByStatusInAndEndDateBefore(List<MembershipStatus> statuses, LocalDateTime endDate);

	// ACTIVE 상태이고 autoRenew=true이며 end_date가 갱신 기간(예: 만료 1일 전) 내에 있는 멤버십 찾기
	List<Membership> findByStatusAndAutoRenewTrueAndEndDateBefore(MembershipStatus status, LocalDateTime endDate);
}
