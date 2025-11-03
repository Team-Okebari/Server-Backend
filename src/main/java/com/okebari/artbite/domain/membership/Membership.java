package com.okebari.artbite.domain.membership;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.okebari.artbite.domain.common.BaseTimeEntity;
import com.okebari.artbite.domain.user.User;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "memberships")
public class Membership extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MembershipStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private MembershipPlanType planType;

	@Column(nullable = false)
	private LocalDateTime startDate;

	@Column
	private LocalDateTime endDate; // 계획에 따라 null 허용

	@Column(nullable = false)
	private int consecutiveMonths;

	@Column(nullable = false)
	private boolean autoRenew;

	@Builder
	public Membership(User user, MembershipStatus status, MembershipPlanType planType,
		LocalDateTime startDate, LocalDateTime endDate, int consecutiveMonths, boolean autoRenew) {
		this.user = user;
		this.status = status;
		this.planType = planType;
		this.startDate = startDate;
		this.endDate = endDate;
		this.consecutiveMonths = consecutiveMonths;
		this.autoRenew = autoRenew;
	}

	// --- 상태 변경을 위한 비즈니스 메소드 ---
	public void activate(LocalDateTime startDate, LocalDateTime endDate, int consecutiveMonths, boolean autoRenew) {
		this.status = MembershipStatus.ACTIVE;
		this.startDate = startDate;
		this.endDate = endDate;
		this.consecutiveMonths = consecutiveMonths;
		this.autoRenew = autoRenew;
	}

	public void cancel() {
		this.status = MembershipStatus.CANCELED;
		this.autoRenew = false;
	}

	public void expire() {
		this.status = MembershipStatus.EXPIRED;
		this.autoRenew = false; // 만료 시 자동 갱신 비활성화
	}

	public void ban() {
		this.status = MembershipStatus.BANNED;
		this.autoRenew = false; // 정지 시 자동 갱신 비활성화
	}

	public void unban() {
		this.status = MembershipStatus.EXPIRED; // 계획에 따라 밴 해제 시 EXPIRED로 설정
	}

	public void renew(LocalDateTime newEndDate, int newConsecutiveMonths) {
		this.endDate = newEndDate;
		this.consecutiveMonths = newConsecutiveMonths;
		this.status = MembershipStatus.ACTIVE; // 성공적인 갱신 시 상태를 ACTIVE로 설정
	}

	public void updateAutoRenew(boolean autoRenew) {
		this.autoRenew = autoRenew;
	}
}
