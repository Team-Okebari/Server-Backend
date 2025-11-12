[#3] fix: Note 조회 서비스에 멤버십 구독 검증 적용

## 관련 이슈
- 메인 이슈: #3
- 서브 이슈: 없음

## 변경 사항
> (무엇을) 어떻게 바꿨는지 구체적으로 작성
- `MembershipSubscriptionService`를 새로 도입해 `MembershipRepository`와 `UserRepository`를 사용한 ACTIVE 멤버십 검증 로직을 구현했습니다.
- 기존 스텁 `AlwaysActiveSubscriptionService`에 `@Profile("stub")`을 부여해 개발/테스트용과 운영용 구현을 프로파일로 분리했습니다.
- 노트 접근 흐름은 변경 없이 `SubscriptionService` 인터페이스를 통해 새 구현을 주입받도록 유지했습니다.

## 변경 이유
> 왜 이 변경이 필요한지 설명
- 운영 환경에서 멤버십 ACTIVE 상태가 아닌 사용자가 프리미엄 노트에 접근하지 못하도록 하기 위함입니다.
- 스텁을 프로파일로 분리함으로써 로컬 개발에서의 빠른 테스트와 운영 환경의 실제 검증을 모두 지원합니다.

## 테스트
> 어떤 방법으로 검증했는지 작성
- `./gradlew checkstyleMain test` 실행 (기존 Checkstyle 경고 3건은 그대로 존재함을 확인).

## 참고 자료 (선택)
- [설계 문서](SYKim_Doc/service_integration_reasoning.md)
- [관련 기술 자료](SYKim_Doc/membership_subscription_proposal.md)

## 리뷰 요구사항 (선택)
> 리뷰어가 특별히 봐주었으면 하는 부분이 있다면 작성
- 프로파일 분리 방식이 운영/개발 환경 요구사항과 맞는지 확인 부탁드립니다.

## PR 체크리스트
- [x] 빌드 및 테스트 통과
- [ ] 코드 컨벤션 준수
- [x] 예외 케이스 처리 완료
- [ ] Swagger / API 문서 반영 완료
