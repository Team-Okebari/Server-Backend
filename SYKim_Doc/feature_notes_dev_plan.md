# 노트 기능(Notes) 설계 및 구현 계획

`feature_notes` 브랜치에서 진행할 노트(Notes) 관련 데이터베이스 스키마와 기본 CRUD 구현의 전체 로드맵입니다. 본 문서 승인 이후 작업을 시작합니다.

## 업데이트 메모 (2025-11-03)
- 서비스/컨트롤러/스케줄러/북마크 구현이 `src/main/java/com/okebari/artbite/note/**` 경로에 생성되었습니다. 문서 내 코드 초안과 실제 구현을 비교 검토할 때 참고하세요.
- 매일 자정 배포는 `NoteStatusScheduler`가 `findCompletedOrderByUpdatedAtAsc()` 결과 중 가장 오래된 한 건만 `PUBLISHED`로 전환하도록 구현되었습니다.
- 구독 검증은 현재 `AlwaysActiveSubscriptionService` 스텁을 통해 모두 허용하고 있으므로, 결제 모듈 연동 시 해당 빈을 교체해야 합니다.
- `CustomUserDetails#getUser().getId()` 를 사용해 주입 사용자 정보를 활용하므로 별도의 편의 메서드 추가가 필요 없습니다.
- 테스트 전략에서 Auth 모듈과의 차이점을 명확히 정리했습니다. Auth 컨트롤러 테스트는 `@SpringBootTest` + `@MockitoBean`을 사용해 실제 스프링 컨텍스트와 시큐리티 필터 체인을 유지한 채 필요한 빈만 Mockito 목으로 대체합니다. 반면 노트 서비스/북마크 테스트는 `@ExtendWith(MockitoExtension.class)` + `@Mock`/`@InjectMocks` 조합으로 순수 단위 테스트를 작성해 스프링 컨텍스트 없이 서비스 로직만 빠르게 검증합니다. 따라서 Auth 테스트는 Docker 기반 Testcontainers(PostgreSQL)를 요구하며 통합 테스트에 가깝고, 노트 테스트는 가벼운 단위 테스트로 Testcontainers(및 Docker) 의존성이 전혀 없습니다. 향후 Notes 도메인에서 실제 DB 연동 시나리오를 검증하고 싶다면 별도의 `@SpringBootTest` 또는 Testcontainers 기반 통합 테스트를 추가로 마련해야 합니다.
- **Auth 테스트와 노트 테스트 방식의 상세 비교**  
  `com/okebari/artbite/auth/controller/AuthControllerTest.java:53`은 `@SpringBootTest` 환경에서 실행되므로 스프링이 애플리케이션 컨텍스트 전체를 기동한 뒤 필요한 빈을 주입합니다. 이때 `@MockitoBean`(Spring Framework 6.1/Spring Boot 3.2에서 `@MockBean`을 대체하는 애노테이션)으로 선언한 클래스는 “실제 스프링 빈” 대신 Mockito 목으로 교체되어 컨텍스트에 올라갑니다. 컨트롤러가 `AuthService`, `AuthenticationManager`, `JwtProvider` 등 여러 빈에 의존하기 때문에, 이런 방식으로 스프링 MVC 필터·시큐리티 구성을 그대로 유지하면서 핵심 협력자만 목으로 바꿔 통합 테스트에 가까운 환경을 구성합니다.  
  반면 노트 테스트(`src/test/java/com/okebari/artbite/note/service/NoteServiceTest.java:12`, `NoteBookmarkServiceTest.java:12`)는 `@ExtendWith(MockitoExtension.class)` + `@Mock`/`@InjectMocks` 조합을 사용합니다. 스프링 컨텍스트를 전혀 띄우지 않는 순수 단위 테스트이므로, 서비스에서 직접 사용하는 리포지토리/매퍼 등만 Mockito 목으로 두고 `@InjectMocks`가 실제 서비스 인스턴스를 만들어 줍니다. 즉 “스프링 빈 교체”가 아니라 “필드 의존성을 Mockito가 직접 주입”하는 차이가 있으며 실행 속도도 훨씬 빠릅니다.  
  요약하면 `@MockitoBean(@MockBean)`은 스프링 컨테이너가 로드한 빈을 목으로 대체해 통합/컨텍스트 기반 테스트를 구성할 때 사용하고, `@Mock + @InjectMocks`는 스프링을 사용하지 않는 순수 단위 테스트에서 객체 간 의존성을 직접 주입할 때 사용합니다. Auth 테스트는 스프링 시큐리티와 MockMvc까지 포함한 실제 환경을 검증하려고 전자를 택했으며, 노트 테스트는 비즈니스 로직을 빠르게 확인하기 위해 후자를 택했습니다. 앞으로 Notes 도메인에서도 실제 Postgres 연동이나 시큐리티 흐름을 검증하려면 Auth 테스트와 같은 패턴으로 별도의 통합 테스트를 추가해야 합니다.

## 1. 목표 및 범위
- **데이터 계층**: Flyway 마이그레이션을 통해 ERD에 정의된 노트 관련 테이블(`notes_head` 외 7개)을 생성하고, 필요한 제약 조건/인덱스를 설정합니다.
- **애플리케이션 계층**: Spring Data JPA 엔티티, 레포지토리, 서비스, 컨트롤러, DTO를 구성해 기본 CRUD API를 제공합니다.
- **테스트**: 서비스/컨트롤러 단위의 기본 동작 검증 테스트를 추가합니다.
- **예외 처리 및 검증**: 입력 값 검증(@Valid)과 존재하지 않는 리소스 요청 시 예외 응답을 정의합니다. `ErrorCode` enum에 NOTE_/CREATOR_ 계열 코드를 추가해 노트/작가 도메인 오류를 구분합니다.
- **설계 원칙 준수**: 모듈 간 결합도를 낮추고 단일 책임을 유지해 SOLID 원칙에 부합하는 구조를 지향합니다.

## 2. 데이터베이스 마이그레이션 계획
- **마이그레이션 파일 구성**
  - `src/main/resources/db/migration`에 `V2__create_notes_tables.sql` 신규 추가.
  - 기존 스키마와 충돌하지 않도록 테이블/컬럼 네이밍은 ERD 문서 기준을 따르되, `snake_case`로 정리.
- **테이블별 구현 사항**
- `notes_head`: PK(`id`), 상태(`status`), 태그(`tag_text` 선택 입력), 생성 시각(`created_at TIMESTAMPTZ`). `created_at` 기본값 `CURRENT_TIMESTAMP`, 상태 전환 기록(`published_at`, `archived_at`)과 외부 출처 링크(`source_url`)를 보관합니다. 작가 프로필은 `creator_id` FK로 `note_creator` 테이블을 참조합니다.
  - `source_url`은 기능 요구사항 `REQ_106`의 관련 링크를 저장해 프론트에서 “더 알아보기” 버튼 등에 활용합니다.
  - `note_cover`, `note_overview`, `note_retrospect`: `note_id`를 PK로 사용하고 `notes_head(id)`와 1:1 매핑, `ON DELETE CASCADE`. `note_cover.main_image_url`은 메인 이미지와 썸네일을 겸용하므로 추가 썸네일 컬럼은 두지 않는다.
  - `note_process`: 복합 PK(`note_id`, `position`), `position`은 1부터, `CHECK(position BETWEEN 1 AND 2)`로 제약. 프런트에서 동일 형식의 “프로세스 1/2” 입력폼을 제공하므로 두 레코드를 하나의 테이블에 저장하고 `position` 값으로 구분합니다.
  - `note_question`: PK(`id`), `note_id` FK. `question_no`는 `SMALLINT`, 노트당 한 행을 유지하기 위해 `UNIQUE(note_id)`.
  - `note_answer`: PK(`id`), `question_id` 및 `user_id` FK, `created_at/updated_at`에 타임스탬프 기본값과 업데이트 트리거(`DEFAULT CURRENT_TIMESTAMP`, `ON UPDATE CURRENT_TIMESTAMP` 또는 트리거 대신 애플리케이션에서 관리).
  - `note_creator`: 작가 정보 테이블. 이름, 소개, 프로필 이미지 URL, SNS 링크를 보관하고 `notes_head.creator_id`로 참조합니다.
  - `note_bookmark`: 사용자 북마크 테이블. `note_id`, `user_id` 복합 유니크로 중복 북마크를 방지합니다.
  - 모든 FK에 `ON DELETE CASCADE` 적용하여 상위 노트 삭제 시 하위 자료 자동 제거.
- **인덱스/유니크 제약**
  - `note_process(note_id, position)` PK.
  - `note_question(note_id)` 유니크.
  - `note_answer(question_id)` 유니크(질문당 하나의 답변).
  - `note_bookmark` 는 `note_id`, `user_id` 복합 유니크 + `user_id`/`note_id` 인덱스 추가.
  - 지난 노트 검색을 위해 `note_cover.title`, `notes_head.tag_text`, `note_creator.name` 에 `lower(...)` 기반 인덱스 생성.
- **롤백 고려**
  - Flyway는 롤백 미지원이므로, 필요 시 별도 다운그레이드 스크립트 문서화.

## 3. 도메인 모델 및 매핑 전략
- **패키지 구조**  
  - `com.okebari.artbite.creator` 모듈에서 작가(Creator) 관련 도메인을 독립적으로 관리한다.
  - `com.okebari.artbite.note` 모듈은 노트 본문과 하위 섹션(커버/개요/프로세스/질문/답변)을 담당한다.
- **엔티티 구성**
- Creator: `note_creator` 테이블에 매핑, `name`(필수 60자), `bio`(선택 100자), `profile_image_url`(선택·작가 프로필 사진 URL), SNS/포트폴리오 URL(선택) 필드를 보유합니다.
  - `jobTitle` 컬럼은 프론트에서 작가 직함(`creatorJobTitle`)으로 노출되고, `bio`는 별도 소개 텍스트로 유지합니다.
  - Note: 상태/태그/외부 링크/작성 시각을 관리하며 Creator와 N:1 관계(FK `creator_id`) 구성.
  - NoteCover/NoteOverview/NoteRetrospect/NoteProcess/NoteQuestion/NoteAnswer: 노트 하위 디테일 섹션을 각각 1:1 또는 1:N으로 표현.
- **공통 규칙**
  - 감사 필드가 필요한 엔티티는 `BaseTimeEntity` 상속.
  - `Note` 엔티티 기준 단방향 연관관계로 하위 구성요소를 집계; 필요 시 `@OneToOne`, `@OneToMany` 적용.
  - 하위 엔티티는 지연로딩 기본, cascade + orphanRemoval 조합으로 생명주기 관리.
  - `NoteStatus`는 `IN_PROGRESS → COMPLETED → PUBLISHED → ARCHIVED` 4단계를 유지하며, `published_at`/`archived_at`으로 전환 시각을 기록합니다.
  - `ARCHIVED` 상태는 메인 노출에서 제외하되 “지난 노트” 목록에서 열람 가능(유료 구독자만 상세 접근).
  - `Note`는 `Creator`를 참조하는 N:1 관계로 구성해 동일 작가가 여러 노트를 작성할 수 있도록 합니다.
- **권장 디렉터리 구조**
  ```
  src/main/java/com/okebari/artbite/
  ├─ creator/
  │  ├─ domain/…          // Creator 엔티티
  │  ├─ dto/…             // CreatorRequest/Response/Summary
  │  ├─ mapper/…          // CreatorMapper
  │  ├─ repository/…      // CreatorRepository
  │  ├─ service/…         // CreatorService
  │  └─ controller/…      // CreatorAdminController (ADMIN 전용 CRUD)
  ├─ note/
  │  ├─ domain/…          // 엔티티
  │  ├─ dto/
  │  │  ├─ bookmark/…   // 내부/외부 북마크 DTO 분리
  │  │  ├─ note/…       // 노트 생성/수정/조회 DTO
  │  │  ├─ process/…    // 제작 과정 DTO
  │  │  ├─ question/…   // 질문 DTO
  │  │  ├─ answer/…     // NoteAnswerRequest, NoteAnswerDto(내부), NoteAnswerResponse(프론트 응답)
  │  │  └─ summary/…    // 요약 응답 DTO
  │  ├─ repository/NoteRepository.java
  │  ├─ mapper/NoteMapper.java       // CreatorMapper를 주입 받아 응답 DTO 조립
  │  ├─ service/…         // NoteService, NoteQueryService, NoteBookmarkService, NoteAnswerService
  │  ├─ controller/…      // NoteAdminController, NoteQueryController, NoteBookmarkController, NoteAnswerController
  │  └─ scheduler/…       // NoteStatusScheduler
  └─ (기존 auth, domain 등 다른 패키지 유지)
  ```
  테스트 코드는 동일한 패키지 구조로 `src/test/java` 하위에 배치합니다.
- **DTO 계층**
- `note/dto` 하위에 도메인별 하위 패키지를 두어 협업 시 책임을 명확히 했습니다. 예: `dto/bookmark`(서비스용 + 프론트용 분리), `dto/note`, `dto/process`, `dto/summary` 등.
- DTO는 Java record로 선언해 불변 객체로 유지하며, 노트에서는 작가 식별자(`creatorId`)와 외부 링크(`NoteExternalLinkDto`)를 분리해 전달합니다.
  - 모든 사용자 입력 DTO는 `jakarta.validation` 애노테이션으로 검증.
  - `NoteQuestionDto`는 질문 본문만 노출하고, 답변 데이터는 `NoteAnswerResponse`로 분리해 USER 입력과 관리자 질문을 명확히 구분합니다.

## 4. 애플리케이션 서비스 설계
- **레포지토리**
  - 각 엔티티별 `JpaRepository` 인터페이스 생성, 최소한 `NoteRepository` 중심으로 CRUD 지원.
  - 복합 키(`note_process`)는 `@Embeddable` 키 클래스로 관리.
  - 작가 관리를 위해 `CreatorRepository`를 별도로 두고, 노트 생성/수정 시 `creator_id`를 지정해 연결합니다.
  - 북마크 토글을 위해 `NoteBookmarkRepository`를 추가하고 `findByNoteIdAndUserId`, `findByUserIdOrderByCreatedAtDesc` 메서드를 제공합니다.
- **서비스 계층**
- `NoteService`: 관리자용 생성/수정/삭제 담당. `IN_PROGRESS` 상태에서만 수정 허용, `PUBLISHED/ARCHIVED`는 읽기 전용.
  - 노트 생성/수정 시 `creatorId`는 필수이며, 누락되면 예외가 발생한다.
  - `NoteQueryService`: 메인 노출용 `PUBLISHED` 목록, 지난 노트(`ARCHIVED`) 목록 및 상세 제공. 구독 검증은 별도 `SubscriptionService` 연동.
  - `NoteAnswerService`: `USER` 롤만 질문에 대한 답변을 작성/수정/삭제할 수 있도록 검증하고, 동일 사용자가 아닌 경우 수정·삭제 시 `AccessDeniedException` 처리. 답변은 `NoteQuestion`에 연결된 상태로 저장해 별도 리포지토리 없이 `NoteQuestionRepository`에서 cascade로 관리합니다. 서비스에서는 내부 DTO(`NoteAnswerDto`)로 처리하고, 컨트롤러에서 `NoteAnswerResponse`로 변환해 프론트에는 answerText만 노출합니다.
  - `NoteStatusScheduler`: 매일 자정 `COMPLETED → PUBLISHED`, 24시간 경과 시 `PUBLISHED → ARCHIVED` 자동 전환.
  - 리포지토리 조회 규칙: `IN_PROGRESS`·`COMPLETED`는 하나로 모아 관리자 목록에 보여주고, `PUBLISHED`는 하루 1건 기준으로 최신 순 정렬, `COMPLETED` 중 가장 오래된 노트를 배포 대상으로 선택합니다.
  - 지난 노트 API는 제목/태그/작가 키워드 검색과 페이지네이션을 지원해 REQ_201/REQ_202를 충족합니다.
  - 작가 관리는 `creator` 모듈로 분리해 재사용 가능하며, 노트 생성 시 `creator_id`를 지정해 연결합니다.
- `NoteBookmarkService`: 북마크 토글(추가/삭제)과 사용자별 북마크 목록 조회 기능을 제공해 REQ_301/REQ_302를 충족합니다. 검색·정렬 로직은 추후 기획 확정 시 추가 개선 예정입니다.
- 북마크 목록 응답은 서비스 전용 DTO(`NoteBookmarkResponse`) → 프론트 전용 DTO(`BookmarkListItemResponse`)로 변환해 UI와의 계약을 명확히 유지합니다.
  - 두 DTO 모두 `creatorName`과 함께 `creatorJobTitle`(Creator.jobTitle)을 노출해 북마크 카드에서 작가 직함을 표시합니다.
- **연관관계 관리**
  - **편의 메서드 정의**: JPA에서 양방향 연관관계를 사용할 때, 한쪽 엔티티에만 값을 넣으면 다른 쪽의 외래 키가 비어 있는 상태가 되어 영속성 컨텍스트가 꼬이거나 `orphanRemoval`이 동작하지 않는 문제가 발생할 수 있습니다. 이런 상황을 방지하려고 두 엔티티의 참조를 동시에 세팅해 주는 전용 메서드를 “연관관계 편의 메서드”라고 합니다.
  - **필요성**: Lombok의 `@Getter/@Setter`는 단순히 필드 값만 읽고 쓰는 수준이라, 예를 들어 `note.setCover(cover)`만 호출하면 `cover` 객체 안의 `note` 필드는 비어 있습니다. 그 결과 JPA는 부모-자식 관계를 정상적으로 인식하지 못해 FK가 null이 되거나 flush 시 예외가 발생합니다.
  - **적용 방식**: `Note.assignCover`, `Note.replaceProcesses` 등 편의 메서드는 부모(`Note`)에 자식을 추가하는 동시에 자식 엔티티의 `note` 필드를 현재 부모로 다시 설정합니다. 이렇게 하면 호출 한 번으로 양쪽이 일관된 상태를 유지하며, FK 누락·orphan 처리 실패 같은 문제를 예방할 수 있습니다.
  - **구현 원칙**: 편의 메서드는 반드시 한 곳에서만 호출하도록 컨벤션을 정하고, 내부에서 `this` 참조를 자식에 전달해 양방향 연결을 완성합니다. 테스트 코드에서도 편의 메서드를 사용해 실제 운영 시나리오와 동일한 객체 그래프를 구성합니다.
- **컨트롤러**
  - `NoteAdminController`(`/api/admin/notes`): 관리자 CRUD + 페이징 리스트 제공.
  - `NoteQueryController`(`/api/notes`): `GET /published`는 공개, `GET /archived`는 `keyword`, `page`, `size` 파라미터로 검색/페이지네이션을 지원하고, `GET /archived/{id}`는 로그인 사용자 전용이며 상세는 유료 구독자만 허용.
  - `CreatorAdminController`(`/api/admin/creators`): ADMIN 전용 작가 등록/조회/수정을 제공하며, 노트 작성 화면은 이 API를 통해 등록된 작가 목록을 로드합니다.
  - `NoteBookmarkController`(`/api/notes`): `POST /{noteId}/bookmark` 토글, `GET /bookmarks`로 사용자 북마크 목록 조회.
- `NoteAnswerController`(`/api/notes/questions/{questionId}/answer`): `POST`(생성), `PUT`(수정), `DELETE`(삭제)을 모두 제공하고 `USER` 롤을 필수로 요구합니다. 생성·수정 시 `NoteAnswerResponse`를 반환해 프론트에서 바로 표시할 수 있게 합니다.
- `NoteQueryController`: `GET /api/notes/published/today-preview`를 통해 로그인 사용자(무료 구독자 포함)에게 미리보기를 제공합니다. 커버 정보와 개요 본문 100자를 잘라 내려주며, 전체 본문은 `GET /api/notes/published/today-detail`에서 유료 구독자만 접근 가능합니다.
- `NoteQueryController`: `GET /api/notes/published/today-preview`로 금일 미리보기(커버 + 개요 100자)를 제공하고, `GET /api/notes/published/today-cover`로 온보딩 다음 메인화면에 노출할 커버만 전달합니다. `GET /api/notes/published/today-detail`은 `TodayPublishedResponse`를 반환해 구독자는 전체 본문(`accessible=true`, `note` 필드), 비구독자는 미리보기(`accessible=false`, `preview` 필드)만 내려줍니다. 지난 노트는 `GET /api/notes/archived/{noteId}`로 유료 구독자에게만 공개됩니다.
  - cover 응답에는 `creatorJobTitle`(Creator.jobTitle)이 추가되어 프론트 히어로 카드에 직함을 노출합니다.
  - 응답 규격은 기존 `CustomApiResponse` 활용.
- 메인 화면은 `NoteCoverResponse`를 그대로 사용하고, 지난 노트 목록은 `ArchivedNoteSummaryResponse(id, tagText, title, mainImageUrl, creatorName, publishedDate)`만 내려 간결한 카드를 구성합니다.
- **사용자 답변 처리**
  - `NoteAnswer`는 사용자 입력 기반이라 선택 입력으로 구성하며, 답변과 작성자 ID 모두 null 허용. 답변이 처음 등록될 때만 `NoteQuestion`과 함께 persist 되고, 수정 시에는 기존 `NoteAnswer`를 재사용해 `answer.update(answerText)` 형태로 변경합니다.
  - `NoteAnswerService`는 질문 식별자와 로그인 사용자 ID를 이용해 권한 검증 후 답변을 생성/수정/삭제하고, `NoteMapper`를 통해 `NoteAnswerDto`로 변환합니다.
  - 컨트롤러에서 `@PreAuthorize("hasRole('USER')")`를 적용해 USER 롤만 접근하게 하고, 응답 결과는 프론트에 바로 전달해 상태를 업데이트합니다.

## 5. 예외 처리 및 검증
- `@Valid` + `BindingResult` 또는 `@ControllerAdvice` 기반 글로벌 예외 처리를 재활용.
- 노트/작가 도메인은 `NoteNotFoundException`, `NoteAccessDeniedException`, `NoteInvalidStatusException`, `CreatorNotFoundException` 등 `BusinessException` 파생 클래스로 관리하고, `ErrorCode`에서 정의한 NOTE_/CREATOR_ 코드를 내려줍니다.
- Enum/컬렉션 유효성 검증 로직은 서비스 계층에서 처리.

## 6. 테스트 전략
- `NoteServiceTest`: `IN_PROGRESS`/`COMPLETED` 전환, `PUBLISHED` 편집 금지, CRUD 성공/실패 케이스 검증.
- `NoteQueryServiceTest`: `PUBLISHED`/`ARCHIVED` 조회 로직과 구독 검증(`SubscriptionService`) 행위 확인.
- `NoteAdminControllerTest` & `NoteQueryControllerTest`: 권한별 접근 제어, Validation 실패, 비구독자 403 응답, 메인/지난 노트 목록 노출 검증.
- `NoteStatusSchedulerTest`: 자정 배치 및 24시간 아카이빙 스케줄 동작 검증.
- `CreatorServiceTest` & (선택) `CreatorAdminControllerTest`: ADMIN 작가 등록/조회/수정/삭제 플로우 검증.
- `NoteBookmarkServiceTest` & `NoteBookmarkControllerTest`: 북마크 토글(추가/삭제)과 리스트 조회 시나리오 검증.
- `NoteAnswerServiceTest` & (선택) `NoteAnswerControllerTest`: USER 권한 검증, 답변 신규 작성·수정·삭제 성공/실패 케이스 검증.
- 필요 시 H2 또는 Testcontainers(PostgreSQL) 환경을 활용해 통합 테스트 신뢰도 확보.

## 7. 진행 순서 및 마일스톤
1. 마이그레이션 스크립트 작성 및 로컬 DB 적용 확인.
2. 엔티티/레포지토리 구현 + 기본 매핑 테스트.
3. 서비스/DTO/컨트롤러 작성 및 CRUD 기능 완성.
4. 테스트 작성 및 통합 검증.
5. 문서/코드 점검 후 리뷰 요청 및 원격 브랜치 푸시.

## 8. 추가 고려 사항
- ERD에서 “정확히 N행 유지” 제약은 애플리케이션 로직으로 보완(마이그레이션에서 CHECK 제약으로 가능한 부분만 처리).
- 이미지 URL 등 문자열 길이는 상한을 설정해 과도한 입력 방지.
- 구독자 전용 열람 정책을 위해 `SubscriptionService` 또는 결제 도메인과의 연동 방법을 사전에 협의.
- 작가 정보 재사용을 위해 `Creator` 관리 화면/API가 별도로 필요하며, 노트 작성 시 기존 작가를 선택하는 플로우를 지원합니다.

위 계획에 대한 의견/수정을 전달받은 뒤 구현을 시작하겠습니다.
