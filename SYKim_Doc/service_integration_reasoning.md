# 서비스 통합 시 로직 설계 사고 정리 (2025-11-06)

본 문서는 **노트 서비스**가 “멤버십 ACTIVE 사용자만 본문 열람 허용”이라는 요구 사항을 충족하기 위해 **멤버십 모듈과 어떻게 연동되어야 하는지** 단계별로 설명합니다. 각 단계마다 실제 파일 경로나 메서드명을 명시하고, 필요한 자리에는 예시 코드를 첨부했습니다.

---

## 1. 도메인 역할과 책임 경계

- `com.okebari.artbite.note` 패키지의 서비스는 **노트를 열람 가능한지 여부를 판별**해야 합니다.
- `com.okebari.artbite.membership` 및 `com.okebari.artbite.domain.membership` 패키지는 **멤버십 상태 관리**에 집중하고, “어떤 조건이 ACTIVE인지” 같은 규칙을 보유합니다.

> 핵심 아이디어: Note 서비스는 **“멤버십이 유효한가?”라는 답**만 필요하며, 그 답을 계산하는 규칙은 멤버십 도메인에 두어야 SOLID(특히 SRP, DIP)를 위반하지 않습니다.

---

## 2. 기존 Stub(`AlwaysActiveSubscriptionService`)이 가진 한계

현재 프로젝트에는 아래와 같이 항상 `true`를 반환하는 스텁 구현이 존재합니다.

```java
// src/main/java/com/okebari/artbite/note/subscription/AlwaysActiveSubscriptionService.java
@Service
public class AlwaysActiveSubscriptionService implements SubscriptionService {
    @Override
    public boolean isActiveSubscriber(Long userId) {
        return true; // 모든 사용자를 구독자로 취급
    }
}
```

- 장점: 초기 개발 속도가 빠릅니다.
- 단점: 운영 환경에서 **멤버십이 없는 사용자도 프리미엄 노트 전체를 열람**하는 문제가 발생합니다.
- 테스트 관점: 멤버십 상태가 바뀌어도 노트 테스트가 실패하지 않아 **결함을 조기에 발견하기 어렵습니다.**

따라서 스텁은 로컬 개발이나 특정 테스트 프로파일에만 남기고, 운영·통합 환경에서는 실제 멤버십 조회가 이루어져야 합니다.

---

## 3. 설계 원칙과 변경 전략

1. **인터페이스 유지 (`SubscriptionService`)**  
   - 기존 노트 서비스는 이 인터페이스에만 의존하므로, 구현체만 교체하면 됩니다.
2. **실제 멤버십 조회 구현 추가 (`MembershipSubscriptionService`)**  
   - 멤버십 리포지토리/서비스를 활용해 ACTIVE 상태 여부를 판단합니다.
3. **스텁 구현 정리**  
   - 운영에서 제외하거나, `@Profile("stub")`과 같이 명시적으로 격리합니다.

---

## 4. 필요한 도메인 정보 정리

멤버십 상태 판별에 활용할 주요 클래스들의 필드 요약입니다.

```java
// src/main/java/com/okebari/artbite/domain/membership/Membership.java
@Entity
public class Membership {
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Enumerated(EnumType.STRING)
    private MembershipStatus status; // ACTIVE, CANCELED, EXPIRED, BANNED ...

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean autoRenew;

    public boolean isExpiredAt(LocalDateTime moment) {
        return endDate.isBefore(moment);
    }
}
```

```java
// src/main/java/com/okebari/artbite/domain/membership/MembershipRepository.java
public interface MembershipRepository extends JpaRepository<Membership, Long> {
    Optional<Membership> findTopByUserAndStatusOrderByStartDateDesc(
        User user,
        MembershipStatus status
    );
}
```

- 위 메서드는 특정 사용자에 대해 **가장 최근의 ACTIVE 멤버십**을 바로 조회할 수 있으므로, 노트 접근 판단에 적합합니다.

---

## 5. SubscriptionService 실제 구현 예시

### 5.1. 인터페이스 재확인

```java
// src/main/java/com/okebari/artbite/note/subscription/SubscriptionService.java
public interface SubscriptionService {
    boolean isActiveSubscriber(Long userId);
}
```

### 5.2. 새 구현체 작성

```java
// src/main/java/com/okebari/artbite/note/subscription/MembershipSubscriptionService.java
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
            .flatMap(user ->
                membershipRepository.findTopByUserAndStatusOrderByStartDateDesc(
                    user, MembershipStatus.ACTIVE)) // 가장 최근 ACTIVE 멤버십
            .filter(membership ->
                !membership.isExpiredAt(LocalDateTime.now(ZoneId.of("Asia/Seoul"))))
            .isPresent();
    }
}
```

설명:
- 사용자 ID로 `User` 엔티티를 찾습니다. 존재하지 않으면 구독자가 아닙니다.
- 가장 최근의 ACTIVE 멤버십을 가져옵니다.
- `endDate`가 현재(KST 기준)보다 이전이면 만료된 것으로 판단합니다.

### 5.3. 스텁은 개발용 프로파일로 한정

```java
// src/main/java/com/okebari/artbite/note/subscription/AlwaysActiveSubscriptionService.java
@Service
@Profile("stub")
public class AlwaysActiveSubscriptionService implements SubscriptionService {
    @Override
    public boolean isActiveSubscriber(Long userId) {
        return true;
    }
}
```

위와 같이 프로파일을 붙이면 `--spring.profiles.active=stub`로 실행한 경우에만 스텁이 빈으로 등록됩니다.

---

## 6. Note 서비스는 인터페이스에만 의존

```java
// src/main/java/com/okebari/artbite/note/service/NoteQueryService.java
@Service
@RequiredArgsConstructor
public class NoteQueryService {

    private final SubscriptionService subscriptionService;
    private final NoteRepository noteRepository;
    private final NoteMapper noteMapper;

    public TodayPublishedResponse getTodayPublishedDetail(Long userId) {
        Note note = findTodayPublishedNote();
        boolean accessible = subscriptionService.isActiveSubscriber(userId);
        if (!accessible) {
            NotePreviewResponse preview = noteMapper.toPreview(note, OVERVIEW_PREVIEW_LIMIT);
            return new TodayPublishedResponse(false, null, preview);
        }
        return new TodayPublishedResponse(true, noteMapper.toResponse(note), null);
    }
}
```

- 노트 서비스는 “구독 여부를 물어보고” 그 결과에 따라 `NoteResponse` 또는 `NotePreviewResponse`를 선택합니다.
- 멤버십 판정 규칙이 바뀌더라도 `SubscriptionService` 구현체만 교체하면 되므로, Note 서비스는 영향이 최소화됩니다.

---

## 7. 테스트 시나리오와 예시 코드

### 7.1. 멤버십 서비스 통합 테스트

```java
@SpringBootTest
@Transactional
class MembershipSubscriptionServiceTest {

    @Autowired
    private MembershipSubscriptionService subscriptionService;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void activeMembershipMakesUserAccessible() {
        User user = userRepository.save(UserFixture.activeAdmin()); // 테스트용 사용자 생성
        membershipRepository.save(MembershipFixture.active(user, LocalDateTime.now(), LocalDateTime.now().plusDays(1)));

        boolean result = subscriptionService.isActiveSubscriber(user.getId());

        assertThat(result).isTrue();
    }
}
```

- `MembershipFixture`는 테스트에서 사용할 엔티티 생성을 담당하는 헬퍼 객체로 가정했습니다.
- ACTIVE 멤버십이 있을 때 `true`가 반환되는지 검증합니다.

### 7.2. 노트 서비스 단위 테스트

```java
@ExtendWith(MockitoExtension.class)
class NoteQueryServiceTest {

    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private NoteRepository noteRepository;
    @Mock
    private NoteMapper noteMapper;

    @InjectMocks
    private NoteQueryService noteQueryService;

    @Test
    void returnsPreviewWhenSubscriptionInactive() {
        Note note = NoteFixture.published(); // 오늘 게시된 노트라고 가정
        when(noteRepository.findTodayPublished()).thenReturn(Optional.of(note));
        when(subscriptionService.isActiveSubscriber(1L)).thenReturn(false);
        when(noteMapper.toPreview(note, 3)).thenReturn(new NotePreviewResponse(...));

        TodayPublishedResponse response = noteQueryService.getTodayPublishedDetail(1L);

        assertThat(response.accessible()).isFalse();
        assertThat(response.preview()).isNotNull();
        assertThat(response.full()).isNull();
    }
}
```

- `SubscriptionService`만 Mock으로 제어하면 노트 서비스 로직을 쉽게 검증할 수 있습니다.

---

## 8. 운영/배포에서 확인할 체크리스트

- **빈 구성**: 운영 프로파일(application.yml)에서 `AlwaysActiveSubscriptionService`가 등록되지 않는지 확인합니다.
- **시간대**: 만료 판정에 사용하는 `LocalDateTime`에 `ZoneId.of("Asia/Seoul")`을 적용해 자정 배포/아카이브 시점과 맞춥니다.
- **로그 모니터링**: 멤버십 조회 실패나 만료 로직이 자주 발생하는지, 노트 접근 차단이 필요한 케이스가 정상 동작하는지 로그로 관찰합니다.
- **회귀 테스트**: `./gradlew clean test` 혹은 CI 파이프라인에서 `checkstyleMain`까지 실행해 스타일/테스트 모두 통과하는지 확인합니다.

---

## 9. 의사결정 요약

| 항목 | 선택한 접근 | 이유 |
|------|-------------|------|
| 접근 제어 책임 | `MembershipSubscriptionService` | 멤버십 정책 변경에도 Note 도메인을 수정하지 않기 위함 |
| Stub 처리 | `@Profile("stub")` | 개발 편의성을 유지하면서 운영 안전성 확보 |
| 시간대 처리 | `ZoneId.of("Asia/Seoul")` | 자정 배포, 24시간 후 아카이빙 규칙과 일치 |
| 테스트 전략 | 통합 + 단위 테스트 분리 | 멤버십 규칙 검증과 노트 응답 검증을 각각 명확히 수행 |

이 구조를 따르면 **구독자 전용 콘텐츠 보호**와 **코드 유지보수성**을 동시에 달성할 수 있습니다. 추후 멤버십 정책(예: 체험 기간, 정지 상태 추가)이 바뀌더라도, `SubscriptionService` 구현만 수정하면 되어 변경 비용이 최소화됩니다.

---

## 10. 현재 레포지토리에 적용할 최종 코드 예시

아래 코드는 현재 `Server-Backend` 프로젝트에 그대로 반영 가능하도록 작성한 최종안입니다. 기본 프로파일 운영 환경에서는 멤버십 연동 구현이 사용되고, `stub` 프로파일에서는 기존처럼 모든 사용자를 구독자로 간주합니다.

### 10.1. 멤버십 연동 구현 추가

```java
// src/main/java/com/okebari/artbite/note/service/support/MembershipSubscriptionService.java
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
```

### 10.2. 스텁 구현은 개발용 프로파일로 한정

```java
// src/main/java/com/okebari/artbite/note/service/support/AlwaysActiveSubscriptionService.java
package com.okebari.artbite.note.service.support;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.okebari.artbite.note.service.SubscriptionService;

@Service
@Profile("stub")
public class AlwaysActiveSubscriptionService implements SubscriptionService {

	@Override
	public boolean isActiveSubscriber(Long userId) {
		return true;
	}
}
```

위 변경을 적용하면 운영 환경에서는 멤버십 상태에 따라 노트 열람이 제한되고, `--spring.profiles.active=stub`로 실행하는 개발/테스트 환경에서는 기존 동작을 유지할 수 있습니다.
