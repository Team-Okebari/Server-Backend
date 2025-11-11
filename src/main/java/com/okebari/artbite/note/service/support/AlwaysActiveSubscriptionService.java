package com.okebari.artbite.note.service.support;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.okebari.artbite.note.service.SubscriptionService;

/**
 * 개발/테스트 편의를 위해 모든 사용자를 활성 구독자로 취급하는 스텁 구현.
 */
@Service
@Profile("stub")
public class AlwaysActiveSubscriptionService implements SubscriptionService {

	@Override
	public boolean isActiveSubscriber(Long userId) {
		return true;
	}
}
