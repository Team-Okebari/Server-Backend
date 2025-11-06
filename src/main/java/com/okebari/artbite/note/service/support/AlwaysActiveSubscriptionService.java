package com.okebari.artbite.note.service.support;

import org.springframework.stereotype.Service;

import com.okebari.artbite.note.service.SubscriptionService;

/**
 * 현재는 결제 모듈이 준비되지 않았으므로 모든 사용자를 활성 구독자로 간주한다.
 * 추후 실제 구독 서비스와 연동되면 해당 구현을 교체한다.
 */
@Service
public class AlwaysActiveSubscriptionService implements SubscriptionService {

	@Override
	public boolean isActiveSubscriber(Long userId) {
		return true;
	}
}
