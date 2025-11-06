package com.okebari.artbite.note.service.support;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.okebari.artbite.domain.membership.MembershipRepository;
import com.okebari.artbite.domain.membership.MembershipStatus;
import com.okebari.artbite.domain.user.UserRepository;
import com.okebari.artbite.note.service.SubscriptionService;

import lombok.RequiredArgsConstructor;

/**
 * 멤버십 ACTIVE 상태 여부를 조회해 노트 접근 가능 여부를 결정하는 실제 구현체.
 */
@Service
@Profile("!stub")
@RequiredArgsConstructor
public class MembershipSubscriptionService implements SubscriptionService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final MembershipRepository membershipRepository;
	private final UserRepository userRepository;

	@Override
	public boolean isActiveSubscriber(Long userId) {
		if (userId == null) {
			return false;
		}

		LocalDateTime now = LocalDateTime.now(KST);

		return userRepository.findById(userId)
			.flatMap(user ->
				membershipRepository.findTopByUserAndStatusOrderByStartDateDesc(
					user, MembershipStatus.ACTIVE))
			.filter(membership -> !membership.getEndDate().isBefore(now))
			.isPresent();
	}
}
