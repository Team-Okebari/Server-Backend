# GitHub Issue 초안: Note / Creator 기능 정리 (2025-11-05)

## 기능
- **노트 작성/관리 (ADMIN 전용)**  
  - `NoteAdminController`를 통해 노트 생성·수정·삭제·목록 조회를 지원합니다.  
  - 커버·개요·프로세스(2단계)·회고·질문 구조를 한 번에 등록하며, 모든 이미지 URL은 필수 값으로 검증됩니다.  
  - 노트 상태는 `IN_PROGRESS → COMPLETED → PUBLISHED → ARCHIVED` 흐름을 따르며, 완료된 노트만 배포 후보로 취급합니다.
- **노트 조회 (USER/ADMIN)**  
  - `/api/notes/published/today-cover`, `/today-detail`, `/today-preview`로 금일 노트를 조회합니다.  
  - 무료 사용자용 미리보기(`NotePreviewResponse`)는 커버와 개요 100자 요약, 작가 요약 정보를 제공합니다.  
  - 지난 노트 목록/상세, 북마크 토글/조회 API를 통해 PUBLISHED→ARCHIVED 이후 콘텐츠 열람과 즐겨찾기를 처리합니다.
- **작가(Creator) 관리 (ADMIN 전용)**  
  - `/api/admin/creators`에서 작가 등록/수정/삭제/조회 기능을 제공합니다.  
  - `jobTitle`과 `bio`를 분리해 직위/직함과 상세 소개를 별도로 저장하며, 프로필 이미지 및 SNS 링크를 함께 관리합니다.

## 로직
- **상태 전환 규칙**  
  - 새 노트는 `IN_PROGRESS` 또는 `COMPLETED` 상태로만 생성할 수 있습니다.  
  - 게시(`PUBLISHED`)·보관(`ARCHIVED`) 상태는 스케줄러/배치 전용이며, 수동 수정 시 해당 상태로 전환을 금지합니다.  
  - `COMPLETED` 상태의 노트를 다시 수정하려면 반드시 `IN_PROGRESS`로 되돌려야 합니다.
- **시간 처리 및 배포 흐름**  
  - 금일 노트 조회는 `Asia/Seoul` 타임존 기준 자정~자정 구간(`findTodayPublishedNote`)으로 결정합니다.  
  - 자정 배포 후 24시간 경과 시 자동 보관되어 메인 화면에서 제외되고, 아카이브 목록을 통해서만 조회 가능합니다.
- **권한/검증 로직**  
  - ADMIN 계정만 노트·작가 CRUD를 수행할 수 있으며, 검증 실패 시 `NoteAccessDeniedException`을 반환합니다.  
  - 노트 생성/수정 시 `creatorId`는 필수이며, 존재하지 않는 작가 ID에 대해서는 `CreatorNotFoundException`을 발생시킵니다.  
  - 미리보기 응답은 구독 여부에 따라 본문(`note`) 또는 미리보기(`preview`) 중 하나만 전달하고, 구독권이 없으면 상세 본문 접근을 차단합니다.

## 구현 이슈
- **스키마 분리 및 문서 정합성**  
  - 기존 문서에서 `bio`와 `job_title`을 혼용해 안내하던 부분을 분리하고, `profile_image_url` 컬럼 정의가 누락된 문제를 수정했습니다.  
  - `creatorId`를 “선택 사항”으로 안내하던 문서를 실제 검증(필수) 로직에 맞춰 정정했습니다.
- **응답 DTO 불일치**  
  - `NotePreviewResponse.overviewPreview`가 문자열로 내려가는데도 기존 명세에서 객체로 기술되던 오류를 발견해 수정했습니다.  
  - 로그아웃 API가 리디렉션 URL(또는 `null`)을 반환함에도 불구하고 `accessToken`을 내려준다고 문서화된 부분을 현행 코드와 맞추었습니다.
- **환경/설정 변경**  
  - Dockerfile의 런타임 이미지를 `openjdk:21-slim`에서 `eclipse-temurin:21-jre`로 교체했으며, 신규 베이스 이미지와의 호환성(인증, 타임존, 폰트 등)을 재확인해야 합니다.  
  - `build.gradle`에 `spring-boot-starter-actuator`를 추가했고, `application-dev.yml`에서 `/api/notes/published/today-cover`와 `/actuator/health`를 화이트리스트에 편입했습니다.  
  - 스케줄러 기반 배포를 위해 `ArtbiteApplication`에 `@EnableScheduling`을 적용했으므로, 운영 환경에서 예상치 못한 크론 실행이 없는지 점검이 필요합니다.
- **데이터 마이그레이션 후속 처리**  
  - `V3__add_job_title_to_note_creator.sql`에서 기존 `bio` 값을 `job_title`로 복사했기 때문에, 실제 직함 데이터와 소개 문구를 분리하려면 후속 데이터 정리가 필요합니다.  
  - 새 필드 도입 이후 Admin UI/CSV 등에서 `jobTitle` 입력을 강제하지 않으면 기존 값이 그대로 남을 수 있으므로, 배포 전에 초기 데이터 클렌징 및 입력 가이드 마련이 필요합니다.
- **구독 서비스 스텁 교체 필요**  
  - 현재 `SubscriptionService`는 `AlwaysActiveSubscriptionService`로 대체되어 있어 모든 사용자를 구독 중으로 처리합니다.  
  - 실제 결제/멤버십 시스템과 연동하기 전까지는 권한 검증이 무력화되므로, 배포 전에 실 구독 조회 로직으로 교체하거나 Feature Flag로 차단해야 합니다.
- **스케줄러/배치 안정성**  
  - `NoteStatusScheduler`가 PUBLISHED/ARCHIVED 전환을 담당하지만, 로컬에서는 테스트 더블을 사용하고 있어 운영 크론 스케줄 식(`0 0 * * *` 등)을 명확히 정해야 합니다.  
  - 배포 직후에는 스케줄러 실행 여부를 모니터링하고, 필요 시 롤백을 대비한 수동 상태 전환 절차도 마련해야 합니다.
- **타임존 및 배포 스케줄링**  
  - 배포/보관 로직이 KST 자정 기준으로 동작하므로, 스케줄러 도입 시 서버 타임존 설정과 크론 표현식을 반드시 KST에 맞춰야 합니다.  
  - 자정 자동 배포 후 24시간 내 재수정 요청이 발생할 수 있으므로, 상태 전환 로직(TodayPublished/Archived 전환)에 대한 테스트 케이스 보강 필요성이 있습니다.
