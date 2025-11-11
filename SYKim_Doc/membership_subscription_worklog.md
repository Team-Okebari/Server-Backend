## 작업 내용
> 멤버십 구독 서비스를 적용하여 구독여부에 따른 노트 조회서비스 수정
- `MembershipSubscriptionService`를 추가해 `MembershipRepository`와 `UserRepository` 기반으로 ACTIVE 멤버십 여부를 판별하도록 구성했습니다.
- `AlwaysActiveSubscriptionService`에 `@Profile("stub")`을 부여해 개발용 스텁과 운영용 구현을 프로파일로 분리했습니다.
- `./gradlew checkstyleMain test`를 실행해 기능 추가 후 회귀를 확인했으며, 기존에 알려진 Checkstyle 경고 3건이 그대로 남아 있음을 확인했습니다.

## 작업 목적
> 멤버십 구독 서비스 적용전, 테스트관점에서 모든 USER에게 노트 조회 서비스 제공  
> 멤버십 구독 서비스 기능 구현 완료 후 노트 서비스에 적용
- 스텁 기반 환경에서는 빠른 테스트와 개발을 유지하고, 운영 환경에서는 실제 멤버십 상태를 반영해 접근 제어가 동작하도록 설계했습니다.
- KST 기준 만료 검증을 통해 자정 게시·24시간 후 아카이브 규칙과 일관성 있게 노트 접근을 제어합니다.

## 참고 자료 (선택)
- SYKim_Doc/service_integration_reasoning.md
- SYKim_Doc/membership_subscription_proposal.md

## 관련 이슈
- 메인 이슈: #3 (노트 서비스 멤버십 연동)
- 관련 이슈: 없음
