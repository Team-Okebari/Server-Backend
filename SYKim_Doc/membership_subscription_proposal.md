# SubscriptionService Membership 연동 제안 (2025-11-06)

## 요구 사항
- 오늘 노트 상세 조회(`NoteQueryService#getTodayPublishedDetail`)는 **멤버십 상태가 ACTIVE인 사용자**에게만 전체 본문을 제공해야 합니다.
- 현재 구현(`AlwaysActiveSubscriptionService`)은 모든 사용자를 구독자로 처리하므로 조건을 만족하지 못합니다.

## 제안 변경 사항

### 1. `SubscriptionService` 실제 구현 교체
- 기존 스텁 `AlwaysActiveSubscriptionService` 제거 (또는 `@Profile("stub")` 지정).
- 멤버십 저장소를 조회해 ACTIVE 상태를 확인하는 새 구현을 추가.

```java
// src/main/java/com/okebari/artbite/note/service/support/MembershipSubscriptionService.java

package com.okebari.artbite.note.service.support;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.okebari.artbite.domain.membership.MembershipRepository;
import com.okebari.artbite.domain.membership.MembershipStatus;
import com.okebari.artbite.domain.user.UserRepository;
import com.okebari.artbite.note.service.SubscriptionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MembershipSubscriptionService implements SubscriptionService {

	private final MembershipRepository membershipRepository;
	private final UserRepository userRepository;

	@Override
	public boolean isActiveSubscriber(Long userId) {
		if (userId == null) {
			return false;
		}

		return userRepository.findById(userId)
			.flatMap(user -> membershipRepository.findTopByUserAndStatusOrderByStartDateDesc(user, MembershipStatus.ACTIVE))
			.filter(membership -> !membership.getEndDate().isBefore(LocalDateTime.now()))
			.isPresent();
	}
}
```

> 만약 `AlwaysActiveSubscriptionService`를 유지하고 싶다면 `@Primary`를 이 클래스에 부여하거나, 스텁 클래스에 `@Profile("stub")`를 적용해 환경별로 선택할 수 있습니다.

### 2. `NoteQueryService`는 추가 수정 불필요
- `SubscriptionService`가 실제 구독 여부를 반환하면 기존 로직으로도 `TodayPublishedResponse`가 자동으로 분기합니다.

## 기대 효과
- 멤버십이 활성 상태인 사용자만 전체 본문에 접근하게 되어 정책을 준수합니다.
- 향후 결제/멤버십 로직이 변경되더라도 `SubscriptionService` 구현만 교체하면 되므로 책임 분리가 명확해집니다.
