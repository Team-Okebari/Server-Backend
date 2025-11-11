package com.okebari.artbite.note.service;

/**
 * 구독 여부를 확인하는 서비스 인터페이스.
 * 실제 결제/구독 모듈과 연동될 예정이며, 현재 구현은 스텁으로 대체한다.
 */
public interface SubscriptionService {

	boolean isActiveSubscriber(Long userId);
}
