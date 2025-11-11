# 노트 기능 구현 코드 초안

`feature_notes` 브랜치에서 실제로 반영할 코드 템플릿입니다. 승인 후 각 파일을 생성/수정합니다. 모든 예시는 현재 프로젝트 컨벤션(BaseTimeEntity, CustomApiResponse, SecurityConfig 등)과 SOLID 원칙을 고려해 구성했습니다.

## 0-1. 구현 반영 현황 (2025-11-03)

- `src/main/java/com/okebari/artbite/note/service`에 `NoteService`, `NoteQueryService`, `NoteBookmarkService`, `NoteAnswerService`를 생성하고 각 메서드에 역할 주석을 추가했습니다.
- `src/main/java/com/okebari/artbite/note/controller`에 관리자/조회/북마크/답변 컨트롤러를 나누어 배치하고, `CustomUserDetails` 기반 권한 검증과 상세한 설명 주석을 작성했습니다.
- 노트 답변 API는 REST 원칙에 맞춰 생성(POST)·수정(PUT)·삭제(DELETE)을 분리했으며, 각 액션별 예외 메시지를 명확히 정의했습니다.
- 컨트롤러에서는 `CustomUserDetails#getUser().getId()` 호출로 사용자 식별자를 얻도록 정리했습니다. (`getUserId()` 추가 없이 기존 도메인을 재사용)
- `src/main/java/com/okebari/artbite/note/scheduler/NoteStatusScheduler.java`를 추가해 자정에 가장 오래된 `COMPLETED` 노트를 한 건만 `PUBLISHED`로 전환하고 24시간 후 `ARCHIVED`로 자동 변경되도록 구현했습니다.
- `src/main/java/com/okebari/artbite/note/service/support/AlwaysActiveSubscriptionService.java`를 임시 스텁으로 두어 구독 기능이 준비되기 전까지 모든 사용자를 활성 구독자로 간주하도록 구성했습니다. 향후 실 서비스와 연동 시 교체가 필요합니다.
- `src/main/java/com/okebari/artbite/note/repository/NoteQuestionRepository.java`를 추가해 질문-답변 묶음을 한 리포지토리에서 관리하고, `NoteAnswer`는 `NoteQuestion`에 연관관계 편의 메서드로 연결해 영속성을 유지합니다.
- `src/main/resources/db/migration/V2__create_notes_tables.sql`을 추가해 노트/작가/북마크 테이블 및 검색 인덱스를 생성했습니다.
- 스케줄러 실행을 위해 `ArtbiteApplication`에 `@EnableScheduling`을 선언했습니다.
- `ErrorCode` enum에 NOTE_/CREATOR_ 계열 코드를 추가하고, 노트/작가 서비스는 `NoteAccessDeniedException`, `NoteInvalidStatusException`, `CreatorNotFoundException` 등 `BusinessException` 파생 클래스를 사용하도록 정리했습니다.
- 테스트 구성: Auth 모듈과 대비되는 테스트 전략을 명시했습니다. Auth 컨트롤러 테스트는 `@SpringBootTest` + `@MockitoBean`으로 실제 스프링 컨텍스트와 시큐리티 필터 체인을 유지하면서 Testcontainers(PostgreSQL)가 필요한 통합 테스트에 가깝습니다. 노트 서비스/북마크 테스트는 `@ExtendWith(MockitoExtension.class)` + `@Mock`/`@InjectMocks` 조합으로 순수 단위 테스트만 수행하며 Docker나 Testcontainers 의존성이 없습니다. 향후 Notes 도메인에서도 DB 연동 시나리오를 검증하려면 별도의 통합 테스트 케이스를 추가 계획해야 합니다.
- **테스트 방식 상세 비교**  
  `com/okebari/artbite/auth/controller/AuthControllerTest.java:53` 은 `@SpringBootTest` 환경에서 실행되며, 스프링이 애플리케이션 컨텍스트 전체를 기동한 뒤 필요한 빈을 모두 주입합니다. 이 과정에서 `@MockitoBean`(Spring Framework 6.1/Boot 3.2 이후 `@MockBean`을 대체)을 사용하면 실제 스프링 빈 대신 Mockito 목으로 컨텍스트를 구성할 수 있습니다. Auth 컨트롤러는 `AuthService`, `AuthenticationManager`, `JwtProvider` 등 많은 협력자를 필요로 하므로, 이런 방식으로 스프링 MVC 필터와 시큐리티 구성을 그대로 유지하면서 핵심 의존성만 목으로 치환해 통합 테스트에 가까운 시나리오를 다룹니다.  
  반면 노트 테스트(`src/test/java/com/okebari/artbite/note/service/NoteServiceTest.java:12`, `NoteBookmarkServiceTest.java:12`)는 `@ExtendWith(MockitoExtension.class)` + `@Mock`/`@InjectMocks` 조합을 이용합니다. 스프링 컨텍스트를 완전히 배제하고 Mockito가 직접 의존성을 주입하는 순수 단위 테스트이므로, 서비스에서 사용하는 리포지토리/매퍼 등만 목 객체로 두고 로직 검증에 집중합니다. 즉, “스프링 빈을 교체”하는 대신 “필드 주입을 Mockito가 직접 담당”하며 실행 속도가 빠르고 Docker/Testcontainers 의존성도 없습니다.  
  정리하면 `@MockitoBean(@MockBean)`은 스프링 컨테이너에 등록된 빈을 목으로 대체해 통합 테스트 성격의 검증을 할 때, `@Mock + @InjectMocks`는 스프링을 띄우지 않고 객체 간 의존성을 직접 주입해 단위 테스트를 할 때 사용합니다. Auth 테스트는 시큐리티·MockMvc까지 포함한 실제 환경을 검증하기 위해 전자를 채택했고, 노트 테스트는 도메인 로직만 빠르게 확인하려고 후자를 채택했습니다. 향후 노트 도메인도 실 DB(PostgreSQL)와의 연동이나 보안 흐름까지 확인하려면 Auth와 유사한 패턴의 통합 테스트를 별도로 추가해야 합니다.
- **추가된 테스트 코드**  
  - `src/test/java/com/okebari/artbite/note/service/NoteServiceTest.java`: 노트 생성/수정 규칙을 Mockito 기반 단위 테스트로 검증.
  - `src/test/java/com/okebari/artbite/note/service/NoteBookmarkServiceTest.java`: 북마크 토글·조회 로직을 검증하고 매퍼 결과를 확인.
  - `src/test/java/com/okebari/artbite/note/service/NoteAnswerServiceTest.java`: `USER` 롤 답변 작성/수정 로직과 권한/예외 처리를 검증.
- DTO 구조를 도메인/계층별 하위 패키지(bookmark, note, process, question, summary 등)로 분리하고, 북마크 응답은 서비스용(`NoteBookmarkResponse`)과 프론트 전용(`BookmarkListItemResponse`)으로 이원화했습니다.

## 0. 디렉터리 구성 제안

실제 구현 시 패키지/폴더 구조는 아래와 같이 나누어 정리합니다.

```
src/main/java/com/okebari/artbite/
├─ note/
│  ├─ domain/…
│  ├─ dto/
│  │  ├─ bookmark/…       // NoteBookmarkResponse (내부용), BookmarkListItemResponse (외부 응답)
│  │  ├─ note/…           // NoteCreateRequest, NoteResponse, NoteUpdateRequest, Link/Cover 등
│  │  ├─ process/…        // NoteProcessDto
│  │  ├─ question/…       // NoteQuestionDto
│  │  ├─ answer/…         // NoteAnswerRequest(입력), NoteAnswerDto(내부 용도), NoteAnswerResponse(프론트 응답)
│  │  └─ summary/…        // ArchivedNoteSummaryResponse
│  ├─ repository/…
│  ├─ mapper/…
│  ├─ service/…
│  ├─ controller/…
│  └─ scheduler/…
├─ domain/…                 // 공용 도메인 (user, common 등) 유지
├─ auth/…
└─ …

테스트 패키지는 `src/test/java/com/okebari/artbite/note/...` 하위에 동일한 구조로 구성하여 서비스/매퍼 단위 검증을 진행합니다.
```

테스트 코드는 `src/test/java/com/okebari/artbite/note/...` 하위에 동일한 패키지 구조로 배치합니다.

## 1. Flyway 마이그레이션 (V2)

`src/main/resources/db/migration/V2__create_notes_tables.sql`

```sql
-- 노트 도메인 테이블 생성
CREATE TABLE note_creator (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(60) NOT NULL,
    bio VARCHAR(100),
    profile_image_url VARCHAR(255),
    instagram_url VARCHAR(255),
    youtube_url VARCHAR(255),
    behance_url VARCHAR(255),
    x_url VARCHAR(255),
    blog_url VARCHAR(255),
    news_url VARCHAR(255)
);

CREATE TABLE notes_head (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    tag_text VARCHAR(60),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP WITH TIME ZONE,
    archived_at TIMESTAMP WITH TIME ZONE,
    source_url VARCHAR(255),
    creator_id BIGINT,
    CONSTRAINT fk_note_creator FOREIGN KEY (creator_id)
        REFERENCES note_creator (id)
);

CREATE TABLE note_cover (
    note_id BIGINT PRIMARY KEY,
    title VARCHAR(30) NOT NULL,
    teaser VARCHAR(100) NOT NULL,
    main_image_url VARCHAR(255) NOT NULL, -- 메인/썸네일 이미지를 겸용으로 저장
    CONSTRAINT fk_note_cover_note FOREIGN KEY (note_id)
        REFERENCES notes_head (id) ON DELETE CASCADE
);

CREATE TABLE note_overview (
    note_id BIGINT PRIMARY KEY,
    section_title VARCHAR(30) NOT NULL,
    body_text VARCHAR(200) NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    CONSTRAINT fk_note_overview_note FOREIGN KEY (note_id)
        REFERENCES notes_head (id) ON DELETE CASCADE
);

CREATE TABLE note_retrospect (
    note_id BIGINT PRIMARY KEY,
    section_title VARCHAR(30) NOT NULL,
    body_text VARCHAR(200) NOT NULL,
    CONSTRAINT fk_note_retrospect_note FOREIGN KEY (note_id)
        REFERENCES notes_head (id) ON DELETE CASCADE
);

CREATE TABLE note_process (
    note_id BIGINT NOT NULL,
    position SMALLINT NOT NULL,
    section_title VARCHAR(30) NOT NULL,
    body_text VARCHAR(500) NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    PRIMARY KEY (note_id, position),
    CONSTRAINT fk_note_process_note FOREIGN KEY (note_id)
        REFERENCES notes_head (id) ON DELETE CASCADE,
    CONSTRAINT ck_note_process_position CHECK (position BETWEEN 1 AND 2)
);

CREATE TABLE note_question (
    id BIGSERIAL PRIMARY KEY,
    note_id BIGINT NOT NULL UNIQUE,
    question_no SMALLINT NOT NULL,
    question_txt VARCHAR(100) NOT NULL,
    CONSTRAINT fk_note_question_note FOREIGN KEY (note_id)
        REFERENCES notes_head (id) ON DELETE CASCADE
);

CREATE TABLE note_answer (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT,
    answer_txt VARCHAR(200),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_note_answer_question FOREIGN KEY (question_id)
        REFERENCES note_question (id) ON DELETE CASCADE,
    CONSTRAINT fk_note_answer_user FOREIGN KEY (user_id)
        REFERENCES users (id)
);

-- 노트 북마크 테이블
CREATE TABLE note_bookmark (
    id BIGSERIAL PRIMARY KEY,
    note_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_note_bookmark_note FOREIGN KEY (note_id)
        REFERENCES notes_head (id) ON DELETE CASCADE,
    CONSTRAINT fk_note_bookmark_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_note_bookmark UNIQUE (note_id, user_id)
);

-- 조회 성능 향상을 위한 인덱스
CREATE INDEX idx_note_bookmark_user ON note_bookmark (user_id);
CREATE INDEX idx_note_bookmark_note ON note_bookmark (note_id);

-- 지난 노트 검색(제목/태그/작가) 최적화를 위한 인덱스
CREATE INDEX idx_note_cover_title_lower ON note_cover (lower(title));
CREATE INDEX idx_notes_head_tag_lower ON notes_head (lower(tag_text));
CREATE INDEX idx_note_creator_name_lower ON note_creator (lower(name));
```

> **타임존 설정**  
> 모든 타임스탬프 컬럼은 `TIMESTAMP WITH TIME ZONE`으로 정의하고 기본값은 `CURRENT_TIMESTAMP`를 사용합니다. 애플리케이션과 데이터베이스 세션 타임존을 `Asia/Seoul`로 고정해 일관된 한국 시간으로 기록합니다.

> **수동 롤백 문서화 예시**  
> (운영환경에서 V2 제거가 필요할 때 DB 관리자가 수동 실행)
> ```sql
> DROP TABLE IF EXISTS note_answer;
> DROP TABLE IF EXISTS note_question;
> DROP TABLE IF EXISTS note_process;
> DROP TABLE IF EXISTS note_retrospect;
> DROP TABLE IF EXISTS note_overview;
> DROP TABLE IF EXISTS note_cover;
> DROP TABLE IF EXISTS notes_head;
> DROP TABLE IF EXISTS note_creator;
> ```

## 2. 도메인 & 엔티티 설계

패키지: `com.okebari.artbite.note.domain`

### 2.1 공통 Enum

`NoteStatus`는 작성·배포 라이프사이클을 4단계로 관리합니다.

```java
package com.okebari.artbite.note.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NoteStatus {
	IN_PROGRESS("임시 저장"),
	COMPLETED("작성 완료"),
	PUBLISHED("게시"),
	ARCHIVED("아카이빙");

	private final String description;
}
```

> **상태 전이 규칙**  
> - 기본 흐름: `IN_PROGRESS → COMPLETED → PUBLISHED → ARCHIVED`.  
> - **작성/수정**: `IN_PROGRESS` 상태에서만 컨텐츠 수정과 자동 저장을 허용합니다.  
> - **완료**: 관리자가 작성 완료 시 `COMPLETED`로 전환하며, 수정이 필요하면 다시 `IN_PROGRESS`로 되돌린 뒤 편집합니다.  
> - **배포**: 매일 자정 배치 작업이 `COMPLETED` 상태 노트를 `PUBLISHED`로 자동 전환합니다. `PUBLISHED` 노트만 메인 화면에 노출됩니다.  
> - **아카이빙**: `PUBLISHED` 후 24시간이 경과하면 배치 작업이 `ARCHIVED`로 자동 전환하며, 메인 화면에서는 숨기되 “지난 노트” 전용 목록으로는 열람 가능합니다.  
> - **열람 정책**: `ARCHIVED` 목록은 `USER` 권한으로 리스트 조회가 가능하고, 유료 구독자는 상세 페이지 열람 권한을 추가로 부여받습니다(세부 권한 체크는 구독 서비스와 연동).  
> - 서비스 계층에서 `PUBLISHED`, `ARCHIVED` 상태는 수동 변경/수정이 불가하도록 검증합니다.

> **태그 입력 정책**  
> `tag_text`는 선택 필드이므로 입력하지 않은 경우 `NULL`로 저장하며, 제공 시 최대 60자까지 허용합니다.

### 2.2 Note 루트 엔티티

```java
package com.okebari.artbite.note.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.okebari.artbite.domain.common.BaseTimeEntity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notes_head")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Note extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private NoteStatus status;

	@Column(name = "tag_text", length = 60)
	private String tagText;

	@Column(name = "published_at")
	private LocalDateTime publishedAt;

	@Column(name = "archived_at")
	private LocalDateTime archivedAt;

	@Column(name = "source_url", length = 255)
	private String sourceUrl;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "creator_id")
	private Creator creator;

	@OneToOne(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private NoteCover cover;

	@OneToOne(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private NoteOverview overview;

	@OneToOne(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private NoteRetrospect retrospect;

	@OneToMany(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<NoteProcess> processes = new ArrayList<>();

	@OneToOne(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private NoteQuestion question;

	@Builder
	private Note(NoteStatus status, String tagText, String sourceUrl) {
		this.status = status;
		this.tagText = tagText;
		this.sourceUrl = sourceUrl;
	}

	public void updateMeta(NoteStatus status, String tagText) {
		this.tagText = tagText;
		if (status == NoteStatus.IN_PROGRESS) {
			revertToInProgress();
		} else if (status == NoteStatus.COMPLETED) {
			this.status = NoteStatus.COMPLETED;
		}
	}

	public void updateExternalLinks(String sourceUrl) {
		this.sourceUrl = sourceUrl;
	}

	public void assignCreator(Creator creator) {
		this.creator = creator;
	}

/*
 * 연관관계 편의 메서드
 * Lombok의 @Getter/@Setter는 단순 필드 접근만 처리하므로,
 * 부모-자식 엔티티 모두에 동일한 참조를 세팅해 주는 로직은 직접 관리해야 합니다.
 * 아래 메서드는 Note ↔ 하위 엔티티 사이의 일관성을 보장하기 위해 양방향 링크를 동시에 구성합니다.
 */
	public void assignCover(NoteCover cover) {
		this.cover = cover;
		if (cover != null) {
			cover.bindNote(this);
		}
	}

	public void assignOverview(NoteOverview overview) {
		this.overview = overview;
		if (overview != null) {
			overview.bindNote(this);
		}
	}

	public void assignRetrospect(NoteRetrospect retrospect) {
		this.retrospect = retrospect;
		if (retrospect != null) {
			retrospect.bindNote(this);
		}
	}

	public void replaceProcesses(List<NoteProcess> processes) {
		this.processes.clear();
		if (processes != null) {
			processes.forEach(process -> this.addProcess(process));
		}
	}

	public void addProcess(NoteProcess process) {
		if (process == null) {
			return;
		}
		this.processes.add(process);
		process.bindNote(this);
	}

	public void assignQuestion(NoteQuestion question) {
		this.question = question;
		if (question != null) {
			question.bindNote(this);
		}
	}

	public void markPublished(LocalDateTime publishedAt) {
		this.status = NoteStatus.PUBLISHED;
		this.publishedAt = publishedAt;
	}

	public void markArchived(LocalDateTime archivedAt) {
		this.status = NoteStatus.ARCHIVED;
		this.archivedAt = archivedAt;
	}

	public void revertToInProgress() {
		this.status = NoteStatus.IN_PROGRESS;
		if (this.publishedAt != null) {
			this.publishedAt = null;
		}
		if (this.archivedAt != null) {
			this.archivedAt = null;
		}
	}
}
```

### 2.2.1 Creator (작가 도메인)

```java
@Entity
@Table(name = "note_creator")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Creator {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 60)
	private String name;

@Column(length = 100)
private String bio;

@Column(name = "job_title", length = 60)
private String jobTitle;

@Column(name = "profile_image_url", length = 255)
private String profileImageUrl;

	@Column(name = "instagram_url", length = 255)
	private String instagramUrl;

	@Column(name = "youtube_url", length = 255)
	private String youtubeUrl;

	@Column(name = "behance_url", length = 255)
	private String behanceUrl;

	@Column(name = "x_url", length = 255)
	private String xUrl;

	@Column(name = "blog_url", length = 255)
	private String blogUrl;

	@Column(name = "news_url", length = 255)
	private String newsUrl;

@Builder
private Creator(String name, String bio, String jobTitle, String instagramUrl, String youtubeUrl,
	String behanceUrl, String xUrl, String blogUrl, String newsUrl, String profileImageUrl) {
	this.name = name;
	this.bio = bio;
	this.jobTitle = jobTitle;
	this.profileImageUrl = profileImageUrl;
	this.instagramUrl = instagramUrl;
	this.youtubeUrl = youtubeUrl;
	this.behanceUrl = behanceUrl;
	this.xUrl = xUrl;
	this.blogUrl = blogUrl;
	this.newsUrl = newsUrl;
}
}
```

- `profileImageUrl` 필드는 작가 본인의 프로필 사진 URL을 저장합니다. 프론트는 이 값을 카드/상세 화면에서 썸네일로 활용하며, 미입력 시 `null`을 내려보내도록 합니다.
- 관리자 화면에서 bio(소개)와 jobTitle(직함), 프로필 이미지는 함께 수정되며 길이 제한은 각각 100자, 60자, 255자입니다.

### 2.3 1:1 구성요소 엔티티

Cover/Overview/Retrospect는 PK 공유(@MapsId).

```java
package com.okebari.artbite.note.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "note_cover")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoteCover {

	@Id
	private Long id;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "note_id")
	private Note note;

	@Column(nullable = false, length = 30)
	private String title;

	@Column(nullable = false, length = 100)
	private String teaser;

	@Column(name = "main_image_url", nullable = false, length = 255)
	private String mainImageUrl;

	@Builder
	private NoteCover(String title, String teaser, String mainImageUrl) {
		this.title = title;
		this.teaser = teaser;
		this.mainImageUrl = mainImageUrl;
	}

	void bindNote(Note note) {
		this.note = note;
		this.id = note.getId();
	}

	public void update(String title, String teaser, String mainImageUrl) {
		this.title = title;
		this.teaser = teaser;
		this.mainImageUrl = mainImageUrl;
	}
}
```

`NoteOverview`, `NoteRetrospect`는 동일한 패턴으로 작성합니다.

```java
@Entity
@Table(name = "note_overview")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoteOverview {
	@Id
	private Long id;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "note_id")
	private Note note;

	@Column(nullable = false, length = 30)
	private String sectionTitle;

	@Column(nullable = false, length = 200)
	private String bodyText;

	@Column(nullable = false, length = 255)
	private String imageUrl;

	@Builder
	private NoteOverview(String sectionTitle, String bodyText, String imageUrl) {
		this.sectionTitle = sectionTitle;
		this.bodyText = bodyText;
		this.imageUrl = imageUrl;
	}

	void bindNote(Note note) {
		this.note = note;
		this.id = note.getId();
	}

	public void update(String sectionTitle, String bodyText, String imageUrl) {
		this.sectionTitle = sectionTitle;
		this.bodyText = bodyText;
		this.imageUrl = imageUrl;
	}
}

@Entity
@Table(name = "note_retrospect")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoteRetrospect {
	@Id
	private Long id;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "note_id")
	private Note note;

	@Column(nullable = false, length = 30)
	private String sectionTitle;

	@Column(nullable = false, length = 200)
	private String bodyText;

	@Builder
	private NoteRetrospect(String sectionTitle, String bodyText) {
		this.sectionTitle = sectionTitle;
		this.bodyText = bodyText;
	}

	void bindNote(Note note) {
		this.note = note;
		this.id = note.getId();
	}

	public void update(String sectionTitle, String bodyText) {
		this.sectionTitle = sectionTitle;
		this.bodyText = bodyText;
	}
}
```

### 2.4 NoteProcess (2개 프로세스 관리)

```java
package com.okebari.artbite.note.domain;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class NoteProcessId implements Serializable {
	private Long noteId;
	private short position;
}

@Entity
@Table(name = "note_process")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoteProcess {

	@EmbeddedId
	private NoteProcessId id;

	@MapsId("noteId")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "note_id")
	private Note note;

	@Column(name = "section_title", nullable = false, length = 30)
	private String sectionTitle;

	@Column(name = "body_text", nullable = false, length = 500)
	private String bodyText;

	@Column(name = "image_url", nullable = false, length = 255)
	private String imageUrl;

	@Builder
	private NoteProcess(short position, String sectionTitle, String bodyText, String imageUrl) {
		this.id = new NoteProcessId(null, position);
		this.sectionTitle = sectionTitle;
		this.bodyText = bodyText;
		this.imageUrl = imageUrl;
	}

	void bindNote(Note note) {
		this.note = note;
		this.id = new NoteProcessId(note.getId(), this.id.getPosition());
	}

	public void update(String sectionTitle, String bodyText, String imageUrl) {
		this.sectionTitle = sectionTitle;
		this.bodyText = bodyText;
		this.imageUrl = imageUrl;
	}
}
```

### 2.5 Question & Answer

```java
package com.okebari.artbite.note.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "note_question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoteQuestion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "note_id", nullable = false, unique = true)
	private Note note;

	@Column(name = "question_txt", nullable = false, length = 100)
	private String questionText;

	@OneToOne(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private NoteAnswer answer;

	@Builder
	private NoteQuestion(String questionText) {
		this.questionText = questionText;
	}

	void bindNote(Note note) {
		this.note = note;
	}

	public void update(String questionText) {
		this.questionText = questionText;
	}

	public void assignAnswer(NoteAnswer answer) {
		if (this.answer != null) {
			this.answer.clearQuestion();
		}
		this.answer = answer;
		if (answer != null) {
			answer.bindQuestion(this);
		}
	}
}
```

```java
package com.okebari.artbite.note.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.okebari.artbite.domain.common.BaseTimeEntity;
import com.okebari.artbite.domain.user.User;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "note_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoteAnswer extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "question_id", nullable = false, unique = true)
	private NoteQuestion question;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User respondent;

	@Column(name = "answer_txt", length = 200)
	private String answerText;

	@Builder
	private NoteAnswer(User respondent, String answerText) {
		this.respondent = respondent;
		this.answerText = answerText;
	}

	void bindQuestion(NoteQuestion question) {
		this.question = question;
	}

	void clearQuestion() {
		this.question = null;
	}

	public void update(String answerText) {
		this.answerText = answerText;
	}
}
```

### 2.6 리포지토리

```java
package com.okebari.artbite.note.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.okebari.artbite.note.exception.NoteAccessDeniedException;
import com.okebari.artbite.note.exception.NoteInvalidStatusException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {
    // 관리자 화면에 노출할 초안/작성 완료 노트 목록 (내림차순)
    @Query("select n from Note n where n.status in ('IN_PROGRESS','COMPLETED') order by n.createdAt desc")
    List<Note> findAllDrafts();

    // 메인 노출용 게시된 노트 목록 (하루 1건 기준, 최신순)
    @Query("select n from Note n where n.status = 'PUBLISHED' order by n.publishedAt desc")
    List<Note> findAllPublished();

    // 지난 노트 목록(아카이브) 조회
    @Query("select n from Note n where n.status = 'ARCHIVED' order by n.archivedAt desc")
    Page<Note> findAllArchived(Pageable pageable);

    @Query("""
        select n from Note n
        left join n.cover c
        left join n.creator cr
        where n.status = 'ARCHIVED'
          and (
            lower(c.title) like lower(concat('%', :keyword, '%'))
            or lower(n.tagText) like lower(concat('%', :keyword, '%'))
            or lower(cr.name) like lower(concat('%', :keyword, '%'))
          )
        order by n.archivedAt desc
        """)
    Page<Note> searchArchived(String keyword, Pageable pageable);

    // 배포 후보: COMPLETED 상태 중 가장 오래된 노트를 우선 선택
    @Query("select n from Note n where n.status = 'COMPLETED' order by n.updatedAt asc")
    List<Note> findAllCompleted();

    // 게시 후 24시간이 지난 노트(= 아카이브 대상)를 조회
    @Query("select n from Note n where n.status = 'PUBLISHED' and n.publishedAt <= :before")
    List<Note> findPublishedBefore(LocalDateTime before);
}

public interface NoteQuestionRepository extends JpaRepository<NoteQuestion, Long> {
}

public interface CreatorRepository extends JpaRepository<Creator, Long> {
}

public interface NoteBookmarkRepository extends JpaRepository<NoteBookmark, Long> {
	Optional<NoteBookmark> findByNoteIdAndUserId(Long noteId, Long userId);
	List<NoteBookmark> findByUserIdOrderByCreatedAtDesc(Long userId);
	@Query("""
		select nb from NoteBookmark nb
		join nb.note n
		left join n.cover c
		left join n.creator cr
		where nb.user.id = :userId
		and (
			lower(c.title) like lower(concat('%', :keyword, '%'))
			or lower(n.tagText) like lower(concat('%', :keyword, '%'))
			or lower(cr.name) like lower(concat('%', :keyword, '%'))
		)
		order by nb.createdAt desc
		""")
	List<NoteBookmark> searchByUserIdAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);
}
```

> **NoteProcessRepository 생략 이유**
> 노트 저장 시 `Note` 엔티티에 `NoteProcess` 리스트를 구성해 `cascade = CascadeType.ALL`과 `orphanRemoval = true`로 관리합니다. 따라서 `NoteRepository`만으로 프로세스 행까지 함께 persist/merge/delete가 가능하므로 별도의 `NoteProcessRepository`가 필수는 아닙니다. 추후 프로세스를 독립적으로 조회·수정해야 하는 요구가 생기면 `JpaRepository<NoteProcess, NoteProcessId>`를 분리해 추가하면 됩니다.

> **NoteAnswerRepository를 별도로 만들지 않은 이유**
> `NoteAnswer`는 `NoteQuestion`에 단방향으로 소유되고 `NoteQuestionRepository`에서 함께 영속화됩니다. 사용자가 답변을 새로 작성하거나 수정할 때도 질문 엔티티를 저장하면 `cascade = CascadeType.ALL` 규칙에 따라 답변까지 함께 저장되므로, 추가 리포지토리가 없어도 일관된 상태를 유지할 수 있습니다.

## 3. DTO & 매퍼 설계

패키지: `com.okebari.artbite.note.dto` (controller 전용) / `com.okebari.artbite.note.mapper`

```java
// Java 16+ record로 DTO를 정의하면 불변성(모든 필드 final)과 자동 생성된 생성자/equals/hashCode/toString을 얻을 수 있어
// 계층 간 데이터 전달 객체를 간결하게 표현할 수 있다. 직렬화/검증용으로만 사용되고, 필드 수가 많아도 선언이 짧아진다.
public record NoteProcessDto(
	short position,
	@NotBlank String sectionTitle,
	@NotBlank String bodyText,
	@NotBlank String imageUrl
) {}

public record NoteCoverDto(
	@NotBlank String title,
	@NotBlank String teaser,
	@NotBlank String mainImageUrl,
	String creatorName,
	String creatorJobTitle
) {}

// 프론트 응답용 커버 DTO: 작성자 이름/직함과 게시 시각까지 포함한다.
public record NoteCoverResponse(
	String title,
	String teaser,
	String mainImageUrl,
	String creatorName,
	String creatorJobTitle,
	LocalDate publishedDate
) {}

public record NoteOverviewDto(
	@NotBlank String sectionTitle,
	@NotBlank String bodyText,
	@NotBlank String imageUrl
) {}

public record NoteRetrospectDto(
	@NotBlank String sectionTitle,
	@NotBlank String bodyText
) {}

public record NoteExternalLinkDto(
	@Size(max = 255) String sourceUrl
) {}

public record CreatorSummaryDto(
	Long id,
	String name,
	String bio,
	String jobTitle,
	String profileImageUrl,
	String instagramUrl,
	String youtubeUrl,
	String behanceUrl,
	String xUrl,
	String blogUrl,
	String newsUrl
) {}

// Service 계층 내부에서 사용하는 북마크 스냅샷 DTO (ID, 생성 시각 포함)
public record NoteBookmarkResponse(
	Long bookmarkId,
	Long noteId,
	String title,
	String mainImageUrl,
	String tagText,
	String creatorName,
	LocalDateTime bookmarkedAt
) {}

// 프론트 전용 응답 DTO: 카드에 필요한 필드만 전달
public record BookmarkListItemResponse(
	Long noteId,
	String title,
	String mainImageUrl,
	String creatorName,
	String tagText
) {}

// USER 답변 입력 요청 DTO: 프론트에서 텍스트만 받아 서버에 전달한다.
public record NoteAnswerRequest(
	@NotBlank String answerText
) {}

// USER 답변 조회 DTO: questionId/작성자 ID를 함께 내려보내 소유권 검증에 활용한다.
public record NoteAnswerDto(
	Long answerId,
	Long questionId,
	Long respondentId,
	String answerText
) {}

// 프론트 전용 응답 DTO: answerText 한 필드만 노출한다.
public record NoteAnswerResponse(String answerText) {}

- 서비스 계층에서는 `NoteBookmarkResponse`로 내부 정보를 유지하고, 컨트롤러에서 `BookmarkListItemResponse`
  로 변환해 외부 계약을 단순화한다.

public record NoteQuestionDto(
	@NotBlank String questionText
) {}

// creatorId는 반드시 지정되어야 한다.
public record NoteCreateRequest(
	@NotNull NoteStatus status,
	@Size(max = 60) String tagText,
	@Valid NoteCoverDto cover,
	@Valid NoteOverviewDto overview,
	@Valid NoteRetrospectDto retrospect,
	@Size(min = 2, max = 2) @Valid List<NoteProcessDto> processes,
	@Valid NoteQuestionDto question,
	@NotNull Long creatorId,
	@Valid NoteExternalLinkDto externalLink
) {}

// 응답 DTO 역시 record를 사용해 불변 응답 구조를 유지한다.
public record NoteResponse(
	Long id,
	NoteStatus status,
	String tagText,
	NoteCoverResponse cover,
	NoteOverviewDto overview,
	NoteRetrospectDto retrospect,
	List<NoteProcessDto> processes,
	NoteQuestionDto question,
	NoteAnswerResponse answer,
	Long creatorId,
	String creatorJobTitle,
	NoteExternalLinkDto externalLink,
	CreatorSummaryDto creator,
	LocalDate publishedAt,
	LocalDateTime archivedAt,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {}

// 무료 사용자에게 내려주는 노트 미리보기 응답.
public record NotePreviewResponse(
	Long id,
	NoteCoverResponse cover,
	NoteOverviewDto overview
) {}

public record TodayPublishedResponse(
	boolean accessible,
	NoteResponse note,
	NotePreviewResponse preview
) {}

public record ArchivedNoteViewResponse(
	boolean accessible,
	NoteResponse note,
	NotePreviewResponse preview
) {}

public record ArchivedNoteSummaryResponse(
	Long id,
	String tagText,
	String title,
	String mainImageUrl,
	String creatorName,
	LocalDate publishedDate
) {}
```

매퍼 구성 (단일 책임 원칙 준수):

```java
import java.util.List;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NoteMapper {

	private final CreatorMapper creatorMapper;

	// 요청 DTO를 기반으로 Note 엔티티 그래프(커버/프로세스/질문)를 구성합니다.
	public Note toEntity(NoteCreateRequest request) {
		NoteExternalLinkDto externalLink = request.externalLink();

		Note note = Note.builder()
			.status(request.status())
			.tagText(request.tagText())
			// REQ_106: 노트가 참고하는 외부 링크를 함께 보관한다.
			.sourceUrl(externalLink != null ? externalLink.sourceUrl() : null)
			.build();

		note.assignCover(toCover(request.cover()));
		note.assignOverview(toOverview(request.overview()));
		note.assignRetrospect(toRetrospect(request.retrospect()));

		List<NoteProcess> processes = request.processes().stream()
			.map(this::toProcess)
			.toList();
		note.replaceProcesses(processes);

		NoteQuestion question = toQuestion(request.question());
		if (question != null) {
			note.assignQuestion(question);
		}
		return note;
	}

	// 전체 Note 엔티티를 API 응답 DTO로 변환할 때 사용하는 메서드
	public NoteResponse toResponse(Note note) {
		Creator creator = note.getCreator();
		CreatorSummaryDto creatorSummary = creator != null ? creatorMapper.toSummary(creator) : null;
		Long creatorId = creator != null ? creator.getId() : null;
		NoteExternalLinkDto externalLink = note.getSourceUrl() != null
			? new NoteExternalLinkDto(note.getSourceUrl())
			: null;

		return new NoteResponse(
			note.getId(),
			note.getStatus(),
			note.getTagText(),
			toCoverDto(note.getCover()),
			toOverviewDto(note.getOverview()),
			toRetrospectDto(note.getRetrospect()),
			note.getProcesses() != null ? note.getProcesses().stream().map(this::toProcessDto).toList() : List.of(),
			toQuestionDto(note.getQuestion()),
			toAnswerResponse(note.getQuestion() != null ? note.getQuestion().getAnswer() : null),
			creatorId,
			externalLink,
			creatorSummary,
			note.getPublishedAt(),
			note.getArchivedAt(),
			note.getCreatedAt(),
			note.getUpdatedAt()
		);
	}

	// 아카이브 노트를 지난 노트 목록용 요약 DTO로 변환한다.
	public ArchivedNoteSummaryResponse toArchivedSummary(Note note) {
		NoteCover cover = note.getCover();
		return new ArchivedNoteSummaryResponse(
			note.getId(),
			note.getTagText(),
			cover != null ? cover.getTitle() : null,
			cover != null ? cover.getMainImageUrl() : null,
			note.getCreator() != null ? note.getCreator().getName() : null,
			note.getPublishedAt() != null ? note.getPublishedAt().toLocalDate() : null
		);
	}

	private NoteCoverDto toCoverDto(NoteCover cover) {
		if (cover == null) {
			return null;
		}
		Note note = cover.getNote();
		return new NoteCoverDto(
			cover.getTitle(),
			cover.getTeaser(),
			cover.getMainImageUrl(),
			note != null && note.getCreator() != null ? note.getCreator().getName() : null,
			note != null && note.getCreator() != null ? note.getCreator().getJobTitle() : null
		);
	}

	public NoteCover toCover(NoteCoverDto dto) {
		if (dto == null) {
			return null;
		}
		return NoteCover.builder()
			.title(dto.title())
			.teaser(dto.teaser())
			.mainImageUrl(dto.mainImageUrl())
			.build();
	}

	private NoteOverviewDto toOverviewDto(NoteOverview overview) {
		if (overview == null) {
			return null;
		}
		return new NoteOverviewDto(overview.getSectionTitle(), overview.getBodyText(), overview.getImageUrl());
	}

	public NoteOverview toOverview(NoteOverviewDto dto) {
		if (dto == null) {
			return null;
		}
		return NoteOverview.builder()
			.sectionTitle(dto.sectionTitle())
			.bodyText(dto.bodyText())
			.imageUrl(dto.imageUrl())
			.build();
	}

	private NoteRetrospectDto toRetrospectDto(NoteRetrospect retrospect) {
		if (retrospect == null) {
			return null;
		}
		return new NoteRetrospectDto(retrospect.getSectionTitle(), retrospect.getBodyText());
	}

	public NoteRetrospect toRetrospect(NoteRetrospectDto dto) {
		if (dto == null) {
			return null;
		}
		return NoteRetrospect.builder()
			.sectionTitle(dto.sectionTitle())
			.bodyText(dto.bodyText())
			.build();
	}

	private NoteProcessDto toProcessDto(NoteProcess process) {
		return new NoteProcessDto(process.getId().getPosition(), process.getSectionTitle(),
			process.getBodyText(), process.getImageUrl());
	}

	private NoteProcess toProcess(NoteProcessDto dto) {
		return NoteProcess.builder()
			.position(dto.position())
			.sectionTitle(dto.sectionTitle())
			.bodyText(dto.bodyText())
			.imageUrl(dto.imageUrl())
			.build();
	}

	public List<NoteProcess> toProcesses(List<NoteProcessDto> dtos) {
		if (dtos == null) {
			return List.of();
		}
		return dtos.stream().map(this::toProcess).toList();
	}

	private NoteQuestionDto toQuestionDto(NoteQuestion question) {
		if (question == null) {
			return null;
		}
		return new NoteQuestionDto(question.getQuestionText());
	}

	public NoteQuestion toQuestion(NoteQuestionDto dto) {
		if (dto == null) {
			return null;
		}
		return NoteQuestion.builder()
			.questionText(dto.questionText())
			.build();
	}

			return new CreatorSummaryDto(
			creator.getId(),
			creator.getName(),
			creator.getBio(),
			creator.getInstagramUrl(),
			creator.getYoutubeUrl(),
			creator.getBehanceUrl(),
			creator.getXUrl(),
			creator.getBlogUrl(),
			creator.getNewsUrl()
		);
	}

public NoteBookmarkResponse toBookmarkResponse(NoteBookmark bookmark) {
	Note note = bookmark.getNote();
	NoteCover cover = note.getCover();
	return new NoteBookmarkResponse(
		bookmark.getId(),
		note.getId(),
		cover != null ? cover.getTitle() : null,
		cover != null ? cover.getMainImageUrl() : null,
		creator != null ? creator.getName() : null,
		creator != null ? creator.getBio() : null,
		bookmark.getCreatedAt()
	);
}
}
```

`toResponse` 구현 시 엔티티 연관관계를 DTO로 변환하고, Optional 필드는 null 허용합니다.

## 4. 서비스 계층
- **NoteService**
  - 생성은 `IN_PROGRESS/COMPLETED`에 한정하고, 수정은 `IN_PROGRESS` 상태에서만 허용.
  - `COMPLETED` 상태에서 그대로 수정하려 하면 예외를 던져 다시 `IN_PROGRESS`로 전환하도록 강제.
  - `PUBLISHED`, `ARCHIVED` 상태는 읽기 전용이며 수동 변경을 허용하지 않음.
- **NoteQueryService**
  - 메인 노출용 `PUBLISHED` 데이터를 제공하며, `GET /api/notes/published` 호출 시 `TodayPublishedResponse`를 통해 구독자(`accessible=true`)와 비구독자(`accessible=false`)를 구분한다.
  - 구독 여부는 `SubscriptionService`에 위임하여 SOLID 원칙을 유지.
- **NoteAnswerService**
  - `USER` 롤만 질문에 답변을 생성/수정할 수 있도록 검증하고, 기존 응답 소유자 이외의 사용자가 수정하려 할 때 `NoteAccessDeniedException`을 던집니다.
  - 답변은 `NoteQuestion`에 연관된 상태로 저장되므로 `NoteQuestionRepository`를 통해 cascade 저장하며, 매퍼에서 `NoteAnswerDto`로 변환해 컨트롤러에 반환합니다.
- **NoteStatusScheduler**
  - 자정 배치로 `COMPLETED → PUBLISHED`, 게시 후 24시간 경과 시 `PUBLISHED → ARCHIVED` 자동 전환.
  - 향후 상태 유지 시간을 조절할 수 있도록 cron 표현식과 기준 시간을 설정 값으로 추출 예정.

패키지: `com.okebari.artbite.note.service`

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteService {

	private final NoteRepository noteRepository;
	private final NoteMapper noteMapper;
	private final UserRepository userRepository;
	private final CreatorRepository noteCreatorRepository;

	// ADMIN 사용자가 신규 노트를 생성하는 비즈니스 로직
	@Transactional
	public Long create(NoteCreateRequest request, Long adminUserId) {
	User admin = userRepository.findById(adminUserId)
		.orElseThrow(() -> new UserNotFoundException()); // 작성자 검증
	if (admin.getRole() != UserRole.ADMIN) {
		throw new NoteAccessDeniedException("ADMIN 권한만 노트를 등록할 수 있습니다.");
	}
	// 생성 시점에 허용되는 상태만 체크
	if (request.status() != NoteStatus.IN_PROGRESS && request.status() != NoteStatus.COMPLETED) {
		throw new NoteInvalidStatusException("신규 노트는 IN_PROGRESS 또는 COMPLETED 상태로만 생성할 수 있습니다.");
	}
		Creator creator = resolveCreator(request.creatorId());
		Note note = noteMapper.toEntity(request); // DTO -> 엔티티 변환
		note.assignCreator(creator);
		Note saved = noteRepository.save(note); // 저장 후 ID 반환
		return saved.getId();
	}

	// 단건 조회 후 응답 DTO 변환
	public NoteResponse get(Long noteId) {
		Note note = noteRepository.findById(noteId)
			.orElseThrow(() -> new NoteNotFoundException(noteId));
		return noteMapper.toResponse(note);
	}

	// 관리자 수정 로직: 상태 제약 검증 후 매퍼를 통해 값 갱신
	@Transactional
	public void update(Long noteId, NoteUpdateRequest request) {
	Note note = noteRepository.findById(noteId)
		.orElseThrow(() -> new NoteNotFoundException(noteId));
	if (note.getStatus() == NoteStatus.PUBLISHED || note.getStatus() == NoteStatus.ARCHIVED) {
		throw new NoteInvalidStatusException("PUBLISHED 또는 ARCHIVED 노트는 수정할 수 없습니다.");
	}
	if (request.status() == NoteStatus.PUBLISHED || request.status() == NoteStatus.ARCHIVED) {
		throw new NoteInvalidStatusException("PUBLISHED/ARCHIVED 전환은 배치 작업에서만 처리됩니다.");
	}
	if (note.getStatus() == NoteStatus.COMPLETED && request.status() == NoteStatus.COMPLETED) {
		throw new NoteInvalidStatusException("COMPLETED 상태에서 수정하려면 먼저 IN_PROGRESS로 되돌려야 합니다.");
	}
		note.updateMeta(request.status(), request.tagText());
		note.assignCover(noteMapper.toCover(request.cover()));
		note.assignOverview(noteMapper.toOverview(request.overview()));
		note.assignRetrospect(noteMapper.toRetrospect(request.retrospect()));
		note.replaceProcesses(noteMapper.toProcesses(request.processes()));
		NoteQuestion updatedQuestion = noteMapper.toQuestion(request.question());
		if (updatedQuestion != null) {
			note.assignQuestion(updatedQuestion);
		} else {
			note.assignQuestion(null);
		}
		// 외부 링크와 작가 연결도 각각 분리해서 갱신한다.
		note.updateExternalLinks(request.externalLink() != null ? request.externalLink().sourceUrl() : null);
		note.assignCreator(resolveCreator(request.creatorId()));
	}

	// 노트 삭제 (존재 여부 확인 후 삭제)
	@Transactional
	public void delete(Long noteId) {
		if (!noteRepository.existsById(noteId)) {
			throw new NoteNotFoundException(noteId);
		}
		noteRepository.deleteById(noteId);
	}

	// 관리자 목록 조회 (페이징)
	public Page<NoteResponse> list(Pageable pageable) {
		return noteRepository.findAll(pageable).map(entity -> noteMapper.toResponse(entity));
	}

private Creator resolveCreator(Long creatorId) {
	if (creatorId == null) {
		throw new CreatorNotFoundException("creatorId는 필수입니다.");
	}
	return noteCreatorRepository.findById(creatorId)
		.orElseThrow(() -> new CreatorNotFoundException(creatorId));
}
}
```

```java
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteQueryService {

	private final NoteRepository noteRepository;
	private final NoteMapper noteMapper;
	private final SubscriptionService subscriptionService;

	// 무료 미리보기: 개요 본문은 100자까지만 전달
	public NotePreviewResponse getPreview(Long noteId) {
		Note note = noteRepository.findById(noteId)
			.orElseThrow(() -> new NoteNotFoundException(noteId));
		if (note.getStatus() == NoteStatus.IN_PROGRESS || note.getStatus() == NoteStatus.COMPLETED) {
			throw new NoteInvalidStatusException("아직 공개되지 않은 노트입니다.");
		}
		return noteMapper.toPreview(note, 100);
	}

	// 메인 화면: 금일 게시 노트의 커버만 반환
	public NoteCoverResponse getTodayCover() {
		return noteMapper.toCoverResponse(findTodayPublishedNote());
	}

// 금일 게시 노트 상세(유료 구독자 전용)
public TodayPublishedResponse getTodayPublishedDetail(Long userId) {
	Note note = findTodayPublishedNote();
	boolean accessible = subscriptionService.isActiveSubscriber(userId);
	if (!accessible) {
		return new TodayPublishedResponse(false, null, noteMapper.toPreview(note, 100));
	}
	return new TodayPublishedResponse(true, noteMapper.toResponse(note), null);
}

// 지난 노트 목록(ARCHIVED) 조회 + 검색/페이지네이션
public Page<ArchivedNoteSummaryResponse> getArchivedNoteList(String keyword, Pageable pageable) {
	Page<Note> page = (keyword == null || keyword.isBlank())
			? noteRepository.findAllArchived(pageable)
			: noteRepository.searchArchived(keyword, pageable);
		return page.map(note -> noteMapper.toArchivedSummary(note));
	}

// 아카이브 상세/프리뷰: 구독 여부에 따라 분기
public ArchivedNoteViewResponse getArchivedNoteView(Long noteId, Long userId) {
	Note note = noteRepository.findById(noteId)
		.orElseThrow(() -> new NoteNotFoundException(noteId));
	if (note.getStatus() != NoteStatus.ARCHIVED) {
		throw new NoteInvalidStatusException("해당 노트는 아카이브 상태가 아닙니다.");
	}
	boolean subscribed = subscriptionService.isActiveSubscriber(userId);
	if (subscribed) {
		return new ArchivedNoteViewResponse(true, noteMapper.toResponse(note), null);
	}
	return new ArchivedNoteViewResponse(false, null, noteMapper.toPreview(note, 100));
}

	private Note findTodayPublishedNote() {
		LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
		LocalDateTime start = today.atStartOfDay();
		LocalDateTime end = start.plusDays(1);
		return noteRepository.findFirstByStatusAndPublishedAtBetween(NoteStatus.PUBLISHED, start, end)
			.orElseThrow(() -> new NoteNotFoundException("오늘 게시된 노트가 없습니다."));
	}
}
```

> **구독 서비스 연동**  
> `SubscriptionService`는 기존 결제/구독 도메인과 통합해 사용자의 유료 구독 여부를 확인하는 컴포넌트로 가정합니다. 구현 시 실제 서비스 또는 외부 API 클라이언트를 주입해 사용합니다.

```java
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NoteStatusScheduler {

	private final NoteRepository noteRepository;
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	// 매일 자정: COMPLETED → PUBLISHED (가장 오래된 한 건만 처리)
	@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
	public void publishCompletedNotes() {
		LocalDateTime now = LocalDateTime.now(KST);
		List<Note> candidates = noteRepository.findCompletedOrderByUpdatedAtAsc();
		if (candidates.isEmpty()) {
			return;
		}
		Note noteToPublish = candidates.get(0);
		noteToPublish.markPublished(now);
	}

	// 자정 배포 후 24시간 경과: PUBLISHED → ARCHIVED
	@Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul") // 시간당 체크 예시
	public void archiveExpiredNotes() {
		LocalDateTime now = LocalDateTime.now(KST);
		LocalDateTime threshold = now.minusHours(24);
		noteRepository.findPublishedBefore(threshold)
			.forEach(note -> note.markArchived(now));
	}
}
```

```java
/**
 * USER 권한이 질문에 대한 답변을 작성/수정/삭제하는 서비스.
 * - 생성(create): 답변이 없는 상태에서만 허용, 이미 존재하면 예외.
 * - 수정(update): 작성자 본인만 가능, 없는 경우 예외.
 * - 삭제(delete): 작성자 본인만 가능, orphanRemoval 통해 엔티티 제거.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class NoteAnswerService {

	private final NoteQuestionRepository questionRepository;
	private final UserRepository userRepository;
	private final NoteMapper noteMapper;

	public NoteAnswerDto createAnswer(Long questionId, Long userId, String answerText) {
		User user = loadUser(userId);
		NoteQuestion question = loadQuestion(questionId);
		if (question.getAnswer() != null) {
			throw new IllegalStateException("이미 등록된 답변이 있습니다. 수정 API를 사용하세요.");
		}
		NoteAnswer answer = NoteAnswer.builder()
			.respondent(user)
			.answerText(answerText)
			.build();
		question.assignAnswer(answer);
		return noteMapper.toAnswerDto(questionRepository.save(question).getAnswer());
	}

	public NoteAnswerDto updateAnswer(Long questionId, Long userId, String answerText) {
		loadUser(userId);
		NoteQuestion question = loadQuestion(questionId);
		NoteAnswer answer = question.getAnswer();
		if (answer == null) {
			throw new IllegalStateException("등록된 답변이 없습니다. 먼저 생성하세요.");
		}
	if (answer.getRespondent() != null && !answer.getRespondent().getId().equals(userId)) {
		throw new NoteAccessDeniedException("다른 사용자의 답변은 수정할 수 없습니다.");
	}
		answer.update(answerText);
		return noteMapper.toAnswerDto(questionRepository.save(question).getAnswer());
	}

	public void deleteAnswer(Long questionId, Long userId) {
		loadUser(userId);
		NoteQuestion question = loadQuestion(questionId);
		NoteAnswer answer = question.getAnswer();
		if (answer == null) {
			throw new IllegalStateException("삭제할 답변이 존재하지 않습니다.");
		}
	if (answer.getRespondent() != null && !answer.getRespondent().getId().equals(userId)) {
		throw new NoteAccessDeniedException("다른 사용자의 답변은 삭제할 수 없습니다.");
		}
		question.assignAnswer(null);
		questionRepository.save(question);
	}

	private User loadUser(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(UserNotFoundException::new);
	if (user.getRole() != UserRole.USER) {
		throw new NoteAccessDeniedException("USER 권한만 답변을 작성할 수 있습니다.");
	}
		return user;
	}

	private NoteQuestion loadQuestion(Long questionId) {
		return questionRepository.findById(questionId)
			.orElseThrow(() -> new NoteNotFoundException("해당 질문을 찾을 수 없습니다."));
	}
}
```

> **SOLID 적용 포인트**  
> - `NoteMapper`가 엔티티 ↔ DTO 책임을 전담 (Single Responsibility).  
> - `NoteService`는 유스케이스 조립과 트랜잭션만 담당.  
> - Security/권한 검증은 서비스 진입 시 전담(Interface Segregation에 따라 Admin 전용).  
> - 스케줄러는 상태 전이만 담당하여 서비스와 명확히 분리, 향후 `NoteQueryService` 등으로 읽기/쓰기 분리도 가능.

## 4.5 Creator 관리 컨트롤러

패키지: `com.okebari.artbite.creator.controller`

```java
@RestController
@RequestMapping("/api/admin/creators")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CreatorAdminController {

	private final CreatorService creatorService;

	@PostMapping
	public CustomApiResponse<Long> create(@Valid @RequestBody CreatorRequest request) {
		return CustomApiResponse.success(creatorService.create(request));
	}

	@GetMapping
	public CustomApiResponse<List<CreatorSummaryDto>> list() {
		return CustomApiResponse.success(creatorService.list());
	}

	@GetMapping("/{creatorId}")
	public CustomApiResponse<CreatorResponse> get(@PathVariable Long creatorId) {
		return CustomApiResponse.success(creatorService.get(creatorId));
	}

	@PutMapping("/{creatorId}")
	public CustomApiResponse<Void> update(@PathVariable Long creatorId, @Valid @RequestBody CreatorRequest request) {
		creatorService.update(creatorId, request);
		return CustomApiResponse.success(null);
	}

	@DeleteMapping("/{creatorId}")
	public CustomApiResponse<Void> delete(@PathVariable Long creatorId) {
		creatorService.delete(creatorId);
		return CustomApiResponse.success(null);
	}
}
```

## 5. 컨트롤러

패키지: `com.okebari.artbite.note.controller`

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/admin/notes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class NoteAdminController {

	private final NoteService noteService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	// 관리자: 신규 노트를 생성한다.
	public CustomApiResponse<Long> create(
		@AuthenticationPrincipal CustomUserDetails admin,
		@Valid @RequestBody NoteCreateRequest request) {
		Long noteId = noteService.create(request, admin.getUser().getId());
		return CustomApiResponse.success(noteId);
	}

	@GetMapping("/{noteId}")
	// 관리자: 특정 노트의 상세 정보를 조회한다.
	public CustomApiResponse<NoteResponse> get(@PathVariable Long noteId) {
		return CustomApiResponse.success(noteService.get(noteId));
	}

	@PutMapping("/{noteId}")
	// 관리자: 노트 내용을 수정한다.
	public CustomApiResponse<Void> update(
		@PathVariable Long noteId,
		@Valid @RequestBody NoteUpdateRequest request) {
		noteService.update(noteId, request);
		return CustomApiResponse.success();
	}

	@DeleteMapping("/{noteId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	// 관리자: 노트를 삭제한다.
	public void delete(@PathVariable Long noteId) {
		noteService.delete(noteId);
	}

	@GetMapping
	// 관리자: 페이징된 노트 목록을 조회한다.
	public CustomApiResponse<Page<NoteResponse>> list(Pageable pageable) {
		return CustomApiResponse.success(noteService.list(pageable));
	}
}
```

```java
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteQueryController {

	private final NoteQueryService noteQueryService;

	@GetMapping("/published/today-preview")
	// 로그인 사용자(무료 포함)에게 금일 노트 미리보기(개요 100자 제한)를 제공한다.
	public CustomApiResponse<NotePreviewResponse> getTodayPreview() {
		return CustomApiResponse.success(noteQueryService.getTodayPreview());
	}

	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@GetMapping("/published/today-detail")
	// 유료 구독자에게 오늘 게시된 노트 전체 내용을 제공한다.
	public CustomApiResponse<NoteResponse> getTodayPublished(
		@AuthenticationPrincipal CustomUserDetails userDetails) {
		return CustomApiResponse.success(noteQueryService.getTodayPublishedDetail(userDetails.getUser().getId()));
	}

	@GetMapping("/published/today-cover")
	// 온보딩 이후 메인 화면에 노출할 금일 커버만 전달한다.
	public CustomApiResponse<NoteCoverResponse> getTodayCover() {
		return CustomApiResponse.success(noteQueryService.getTodayCover());
	}

	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@GetMapping("/archived")
	// 로그인 사용자: 지난 노트(아카이브) 목록을 조회한다. (검색/페이지네이션 지원)
	public CustomApiResponse<Page<ArchivedNoteSummaryResponse>> getArchivedNotes(
		@RequestParam(value = "keyword", required = false) String keyword,
		Pageable pageable) {
		return CustomApiResponse.success(noteQueryService.getArchivedNoteList(keyword, pageable));
	}

	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@GetMapping("/archived/{noteId}")
	// 구독 상태에 따라 프리뷰/상세를 분기해 돌려준다.
	public CustomApiResponse<ArchivedNoteViewResponse> getArchivedDetail(
		@PathVariable Long noteId,
		@AuthenticationPrincipal CustomUserDetails userDetails) {
		Long userId = userDetails.getUser().getId();
		return CustomApiResponse.success(noteQueryService.getArchivedNoteView(noteId, userId));
	}
}
```

```java
/**
 * USER 답변 작성/수정 컨트롤러.
 * 프론트에서는 질문 ID와 사용자 세션을 바탕으로 POST 요청을 전송한다.
 */
@RestController
@RequestMapping("/api/notes/questions")
@RequiredArgsConstructor
public class NoteAnswerController {

	private final NoteAnswerService noteAnswerService;

	@PreAuthorize("hasRole('USER')")
	@PostMapping("/{questionId}/answer")
	public CustomApiResponse<NoteAnswerResponse> create(
		@PathVariable Long questionId,
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@Valid @RequestBody NoteAnswerRequest request
	) {
		NoteAnswerDto dto = noteAnswerService.createAnswer(
			questionId,
			userDetails.getUser().getId(),
			request.answerText()
		);
		return CustomApiResponse.success(new NoteAnswerResponse(dto.answerText()));
	}

	@PreAuthorize("hasRole('USER')")
	@PutMapping("/{questionId}/answer")
	public CustomApiResponse<NoteAnswerResponse> update(
		@PathVariable Long questionId,
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@Valid @RequestBody NoteAnswerRequest request
	) {
		NoteAnswerDto dto = noteAnswerService.updateAnswer(
			questionId,
			userDetails.getUser().getId(),
			request.answerText()
		);
		return CustomApiResponse.success(new NoteAnswerResponse(dto.answerText()));
	}

	@PreAuthorize("hasRole('USER')")
@DeleteMapping("/{questionId}/answer")
public CustomApiResponse<Void> delete(
	@PathVariable Long questionId,
	@AuthenticationPrincipal CustomUserDetails userDetails
) {
	noteAnswerService.deleteAnswer(questionId, userDetails.getUser().getId());
	return CustomApiResponse.success(null);
}
}
```

## 6. Security 설정

`SecurityConfig` 수정: 관리용(`api/admin/notes/**`)과 열람용(`api/notes/**`) 경로를 분리합니다.

```java
import org.springframework.http.HttpMethod;

.authorizeHttpRequests(authorize -> authorize
	.requestMatchers(whitelistedPaths).permitAll()
	.requestMatchers(HttpMethod.GET, "/api/notes/published/**").permitAll()
	.requestMatchers(HttpMethod.POST, "/api/notes/questions/**").hasRole("USER")
	.requestMatchers("/api/notes/archived/**").hasAnyRole("USER","ADMIN")
	.requestMatchers("/api/admin/notes/**").hasRole("ADMIN")
	.anyRequest().authenticated()
)
```

각 컨트롤러의 `@PreAuthorize` 설정과 중복되지만, 방어차원에서 두 곳 모두 적용해 이중 체크(Defense in Depth).

## 7. 예외 및 검증

- `NoteNotFoundException extends BusinessException` 추가 (기존 예외 구조 재사용).
- `NoteAccessDeniedException`을 통해 도메인별 권한 오류 코드를 내려준다.
- DTO 유효성 위반 시 `MethodArgumentNotValidException` → `GlobalExceptionHandler`가 처리.

## 8. 테스트 전략 구체화

1. **서비스 단위 테스트 (`NoteServiceTest`)**
   - `@DataJpaTest` + `@Import(NoteService.class, NoteMapper.class)`로 구성.
   - 생성/조회/수정/삭제 성공 케이스.
   - ADMIN 이외 권한 접근 시 `NoteAccessDeniedException` 발생 검증.
   - `PUBLISHED/ARCHIVED` 상태 편집 불가, `COMPLETED` 상태에서 상태 변경 요구 사항 등 도메인 규칙 검증.

2. **통합 테스트 (`NoteAdminControllerTest`)**
   - `@WebMvcTest(NoteAdminController.class)` + Mocked `NoteService`, 또는 `@SpringBootTest` + `MockMvc`.
   - JWT 필터를 우회하기 위해 `@WithMockUser(roles = "ADMIN")` 사용.
   - Validation 실패, 미존재 ID 요청, 삭제 성공 응답 코드 확인.
   - 상태 전이 제한(예: `PUBLISHED`로 업데이트 시 HTTP 400) 응답 검증.

3. **열람 API 테스트 (`NoteQueryControllerTest`)**
   - `@WebMvcTest(NoteQueryController.class)` 또는 통합 테스트에서 `NoteQueryService` mocking.
   - `/api/notes/published`는 인증 없이 접근 가능한지, `/api/notes/archived`는 `USER` 이상만 허용되는지 검증.
   - 유료 구독자 여부에 따라 `/archived/{id}` 응답이 달라지는 시나리오(403 vs 200) 테스트.
   - 메인 목록에서 `ARCHIVED` 자료가 포함되지 않는지, 아카이브 목록에서는 포함되는지 확인.

4. **스케줄러 테스트 (`NoteStatusSchedulerTest`)**
   - `@SpringBootTest` + `@ActiveProfiles("test")` 환경에서 스케줄러 빈을 주입해 수동 호출.
   - `publishCompletedNotes()`가 `COMPLETED → PUBLISHED` 전환을 수행하는지, 전환 후 수정 제한이 적용되는지 확인.
   - `archiveExpiredNotes()`가 게시 시간 24시간 경과 노트를 `ARCHIVED`로 이동시키는지 검증.

5. **Flyway 마이그레이션 검증**
   - 로컬에서 `./gradlew flywayMigrate -Dflyway.configFiles=...` 실행 후 스키마 확인.
   - H2 테스트 DB 사용 시 `schema.sql`과 타입 호환성 점검(PostgreSQL ENUM → VARCHAR로 대응).

## 9. 추후 확장 포인트

- 다중 프로세스 확장 시 `CHECK` 제약 조정 및 UI 협의.
- `Note` 작성자 추적 필요 시 `notes_head`에 `created_by` FK 추가 고려.
- 배포 이력 분석을 위해 별도 히스토리 테이블과 통계 API 도입 검토.
- 답변에 첨부파일이 필요해지면 `note_answer_attachment` 보조 테이블 추가.
- 북마크/댓글/추천 등 추가 인터랙션 도메인 확장 시 현재 설계(creator 별도 분리 + bookmark) 패턴을 재사용.

---

위 코드 초안에 문제나 수정 필요 사항을 알려주시면 반영 후 실제 구현에 착수하겠습니다.

## 10. 북마크 기능 설계 (Draft)

아래 코드는 북마크 요구사항(REQ_301/REQ_302)을 만족시키기 위한 초안입니다. 모든 코드에는 역할 설명 주석을 포함했습니다. 컨펌 후 실제 `.java` 파일을 생성하세요.

### 10.1 마이그레이션

```sql
-- 사용자-노트 북마크 테이블: 한 사용자 당 노트 한 건만 북마크 가능
CREATE TABLE note_bookmark (
    id BIGSERIAL PRIMARY KEY,
    note_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_note_bookmark_note FOREIGN KEY (note_id)
        REFERENCES notes_head (id) ON DELETE CASCADE,
    CONSTRAINT fk_note_bookmark_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_note_bookmark UNIQUE (note_id, user_id)
);
```

### 10.2 도메인 엔티티

```java
/**
 * 노트 북마크 엔티티.
 * note↔user 관계를 저장하고, 생성 시각을 기록한다.
 */
@Entity
@Table(name = "note_bookmark")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoteBookmark {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "note_id", nullable = false)
	private Note note;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Builder
	private NoteBookmark(Note note, User user) {
		this.note = note;
		this.user = user;
	}
}
```

### 10.3 리포지토리

```java
/**
 * 북마크 CRUD 및 중복 체크 조합 쿼리 제공.
 */
public interface NoteBookmarkRepository extends JpaRepository<NoteBookmark, Long> {
	Optional<NoteBookmark> findByNoteIdAndUserId(Long noteId, Long userId);
	List<NoteBookmark> findByUserIdOrderByCreatedAtDesc(Long userId);
}
```

### 10.4 DTO 추가

```java
/**
 * 북마크 응답 DTO – 북마크 ID, 노트 ID, 제목, 대표 이미지, 작가 이름/직함, 생성 시각 포함.
 * 썸네일은 mainImageUrl을 그대로 전달해 프론트에서 리사이즈한다(추가 저장 불필요).
 */
public record NoteBookmarkResponse(
	Long bookmarkId,
	Long noteId,
	String title,
	String mainImageUrl,
	String tagText,
	String creatorName,
	LocalDateTime bookmarkedAt
) {}
```

### 10.5 NoteMapper 확장

```java
/**
 * 북마크 엔티티 → DTO 변환. 프론트가 필요한 최소 필드(mainImageUrl/제목/작가명/직함)를 개별 필드로 반환한다.
 */
public NoteBookmarkResponse toBookmarkResponse(NoteBookmark bookmark) {
	Note note = bookmark.getNote();
	return new NoteBookmarkResponse(
		bookmark.getId(),
		note.getId(),
		note.getCover() != null ? note.getCover().getTitle() : null,
		note.getCover() != null ? note.getCover().getMainImageUrl() : null,
		note.getTagText(),
		note.getCreator() != null ? note.getCreator().getName() : null,
		bookmark.getCreatedAt()
	);
}
```
- 지난 노트 카드는 `title`/`mainImageUrl`/`teaser`만으로 구성하고, 태그 검색을 위해 `tagText`를 추가로 내려준다.

### 10.6 서비스 계층

```java
/**
 * 북마크 토글/조회 비즈니스 로직.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class NoteBookmarkService {

	private final NoteBookmarkRepository bookmarkRepository;
	private final NoteRepository noteRepository;
	private final UserRepository userRepository;
	private final NoteMapper noteMapper;

	public boolean toggle(Long noteId, Long userId) {
		NoteBookmark existing = bookmarkRepository.findByNoteIdAndUserId(noteId, userId).orElse(null);
		if (existing != null) {
			bookmarkRepository.delete(existing); // 이미 북마크 상태라면 제거
			return false;
		}
		Note note = noteRepository.findById(noteId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 노트입니다."));
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
		bookmarkRepository.save(NoteBookmark.builder().note(note).user(user).build());
		return true;
	}

	@Transactional(readOnly = true)
	public List<NoteBookmarkResponse> list(Long userId, String keyword) {
		List<NoteBookmark> bookmarks = (keyword == null || keyword.isBlank())
			? bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId)
			: bookmarkRepository.searchByUserIdAndKeyword(userId, keyword.trim());
		return bookmarks.stream()
			.map(noteMapper::toBookmarkResponse)
			.toList();
	}
}
```

- 컨트롤러에서는 위 서비스 DTO 목록을 `BookmarkListItemResponse`로 변환해 프론트에 전달한다.

### 10.7 컨트롤러 초안

```java
/**
 * 북마크 토글 및 조회 API.
 */
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteBookmarkController {

	private final NoteBookmarkService noteBookmarkService;

	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@PostMapping("/{noteId}/bookmark")
	public CustomApiResponse<Map<String, Boolean>> toggle(@PathVariable Long noteId,
		@AuthenticationPrincipal CustomUserDetails user) {
		boolean bookmarked = noteBookmarkService.toggle(noteId, user.getUser().getId());
		return CustomApiResponse.success(Map.of("bookmarked", bookmarked));
	}

	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@GetMapping("/bookmarks")
	public CustomApiResponse<List<BookmarkListItemResponse>> bookmarks(
		@AuthenticationPrincipal CustomUserDetails user,
		@RequestParam(required = false) String keyword) {
		List<NoteBookmarkResponse> snapshots = noteBookmarkService.list(user.getUser().getId(), keyword);
		List<BookmarkListItemResponse> payload = snapshots.stream()
			.map(dto -> new BookmarkListItemResponse(dto.noteId(), dto.title(), dto.mainImageUrl(), dto.creatorName(), dto.tagText()))
			.toList();
		return CustomApiResponse.success(payload);
	}
}
```

### 10.8 프론트엔드 연동 참고
- `POST /api/notes/{noteId}/bookmark` 호출로 토글, 응답 `{ "bookmarked": true/false }`.
- `GET /api/notes/bookmarks` 결과를 기반으로 북마크 목록 화면 구현.
- 북마크 카드는 `BookmarkListItemResponse`에 담긴 `noteId`/제목/대표 이미지/작가 이름을 사용해 상세 이동 및 UI를 구성하고,
  썸네일은 전달된 mainImageUrl을 프론트에서 리사이즈해 구성한다.
- `keyword` 파라미터를 사용하면 제목/작가명/태그 텍스트 기반으로 서버 측 검색이 이뤄지며, `tagText` 필드는 검색 하이라이트/필터링에 활용할 수 있다.
```java
@Service
@RequiredArgsConstructor
@Transactional
public class NoteBookmarkService {

	private final NoteBookmarkRepository bookmarkRepository;
	private final NoteRepository noteRepository;
	private final UserRepository userRepository;
	private final NoteMapper noteMapper;

	public boolean toggle(Long noteId, Long userId) {
		NoteBookmark bookmark = bookmarkRepository.findByNoteIdAndUserId(noteId, userId).orElse(null);
		if (bookmark != null) {
			bookmarkRepository.delete(bookmark);
			return false;
		}
		Note note = noteRepository.findById(noteId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 노트입니다."));
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
		bookmarkRepository.save(NoteBookmark.builder()
			.note(note)
			.user(user)
			.build());
		return true;
	}

	@Transactional(readOnly = true)
	public List<NoteBookmarkResponse> list(Long userId) {
		return bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
			.map(noteMapper::toBookmarkResponse)
			.toList();
	}
}
```

```java
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteBookmarkController {

	private final NoteBookmarkService noteBookmarkService;

	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@PostMapping("/{noteId}/bookmark")
	public CustomApiResponse<Map<String, Boolean>> toggle(@PathVariable Long noteId,
		@AuthenticationPrincipal CustomUserDetails userDetails) {
		boolean bookmarked = noteBookmarkService.toggle(noteId, userDetails.getUser().getId());
		return CustomApiResponse.success(Map.of("bookmarked", bookmarked));
	}

@PreAuthorize("hasAnyRole('USER','ADMIN')")
@GetMapping("/bookmarks")
public CustomApiResponse<List<BookmarkListItemResponse>> bookmarks(
    @AuthenticationPrincipal CustomUserDetails userDetails) {
    List<NoteBookmarkResponse> snapshots = noteBookmarkService.list(userDetails.getUser().getId());
    List<BookmarkListItemResponse> payload = snapshots.stream()
        .map(dto -> new BookmarkListItemResponse(dto.noteId(), dto.title(), dto.mainImageUrl(), dto.creatorName()))
        .toList();
    return CustomApiResponse.success(payload);
}
}
```

### 10.8 단위 테스트 코드

```java
@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

	@Mock
	private NoteRepository noteRepository;

	@Mock
	private CreatorRepository noteCreatorRepository;

	@Mock
	private NoteMapper noteMapper;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private NoteService noteService;

	@Test
	void createThrowsWhenCallerIsNotAdmin() {
		User normalUser = buildUser(UserRole.USER, 10L);
		when(userRepository.findById(10L)).thenReturn(Optional.of(normalUser));

		NoteCreateRequest request = createNoteRequest(NoteStatus.IN_PROGRESS, null);

	assertThatThrownBy(() -> noteService.create(request, 10L))
		.isInstanceOf(NoteAccessDeniedException.class);
		verify(noteRepository, never()).save(any());
	}

	@Test
	void createThrowsWhenStatusIsNotAllowed() {
		User admin = buildUser(UserRole.ADMIN, 1L);
		when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

		NoteCreateRequest request = createNoteRequest(NoteStatus.PUBLISHED, null);

	assertThatThrownBy(() -> noteService.create(request, 1L))
		.isInstanceOf(NoteInvalidStatusException.class)
			.hasMessageContaining("IN_PROGRESS 또는 COMPLETED");
		verify(noteMapper, never()).toEntity(any());
	}

	@Test
	void createSavesNoteAndReturnsId() {
		User admin = buildUser(UserRole.ADMIN, 1L);
		when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

		Long creatorId = 99L;
		NoteCreateRequest request = createNoteRequest(NoteStatus.IN_PROGRESS, creatorId);

	Creator creator = Creator.builder().name("Creator").bio("Bio").jobTitle("Job").build();
		ReflectionTestUtils.setField(creator, "id", creatorId);
		when(noteCreatorRepository.findById(creatorId)).thenReturn(Optional.of(creator));

		Note mapped = Note.builder()
			.status(NoteStatus.IN_PROGRESS)
			.tagText("tag")
			.sourceUrl("https://source")
			.build();
		when(noteMapper.toEntity(request)).thenReturn(mapped);
		ReflectionTestUtils.setField(mapped, "id", 123L);
		when(noteRepository.save(mapped)).thenReturn(mapped);

		Long noteId = noteService.create(request, 1L);

		assertThat(noteId).isEqualTo(123L);
		assertThat(mapped.getCreator()).isEqualTo(creator);
		verify(noteRepository).save(mapped);
	}

	@Test
	void updateThrowsWhenNoteIsPublished() {
		Note note = Note.builder()
			.status(NoteStatus.PUBLISHED)
			.tagText("tag")
			.sourceUrl(null)
			.build();
		when(noteRepository.findById(5L)).thenReturn(Optional.of(note));

		NoteUpdateRequest request = createNoteUpdateRequest(NoteStatus.IN_PROGRESS, null);

	assertThatThrownBy(() -> noteService.update(5L, request))
		.isInstanceOf(NoteInvalidStatusException.class)
			.hasMessageContaining("PUBLISHED 또는 ARCHIVED");
	}

	private NoteCreateRequest createNoteRequest(NoteStatus status, Long creatorId) {
		return new NoteCreateRequest(
			status,
			"tag",
			new NoteCoverDto("title", "teaser", "https://img.main", "홍길동", "일러스트레이터"),
			new NoteOverviewDto("overview", "overview body", "https://img.overview"),
			new NoteRetrospectDto("retro", "retro body"),
			List.of(
				new NoteProcessDto((short)1, "process 1", "body 1", "https://img.process1"),
				new NoteProcessDto((short)2, "process 2", "body 2", "https://img.process2")
			),
			new NoteQuestionDto((short)1, "question?", "answer", null),
			creatorId,
			new NoteExternalLinkDto("https://source")
		);
	}

	private NoteUpdateRequest createNoteUpdateRequest(NoteStatus status, Long creatorId) {
		return new NoteUpdateRequest(
			status,
			"tag",
			new NoteCoverDto("title", "teaser", "https://img.main", "홍길동", "일러스트레이터"),
			new NoteOverviewDto("overview", "overview body", "https://img.overview"),
			new NoteRetrospectDto("retro", "retro body"),
			List.of(
				new NoteProcessDto((short)1, "process 1", "body 1", "https://img.process1"),
				new NoteProcessDto((short)2, "process 2", "body 2", "https://img.process2")
			),
			new NoteQuestionDto((short)1, "question?", "answer", null),
			creatorId,
			new NoteExternalLinkDto("https://source")
		);
	}

	private User buildUser(UserRole role, Long id) {
		User user = User.builder()
			.email(role.name().toLowerCase() + "@test.com")
			.password("password")
			.username(role.name().toLowerCase())
			.role(role)
			.enabled(true)
			.accountNonExpired(true)
			.accountNonLocked(true)
			.credentialsNonExpired(true)
			.tokenVersion(0)
			.build();
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}
}
```

```java
@ExtendWith(MockitoExtension.class)
class NoteBookmarkServiceTest {

	@Mock
	private NoteBookmarkRepository bookmarkRepository;

	@Mock
	private NoteRepository noteRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private NoteMapper noteMapper;

	@InjectMocks
	private NoteBookmarkService noteBookmarkService;

	@Test
	void toggleDeletesBookmarkWhenAlreadyExists() {
		User user = buildUser(1L, UserRole.USER);
		Note note = Note.builder()
			.status(NoteStatus.IN_PROGRESS)
			.tagText("tag")
			.sourceUrl(null)
			.build();
		NoteBookmark bookmark = NoteBookmark.builder()
			.note(note)
			.user(user)
			.build();

		when(bookmarkRepository.findByNoteIdAndUserId(5L, 1L)).thenReturn(Optional.of(bookmark));

		boolean bookmarked = noteBookmarkService.toggle(5L, 1L);

		assertThat(bookmarked).isFalse();
		verify(bookmarkRepository).delete(bookmark);
		verify(bookmarkRepository, never()).save(any());
	}

	@Test
	void toggleCreatesBookmarkWhenAbsent() {
		User user = buildUser(2L, UserRole.USER);
		Note note = Note.builder()
			.status(NoteStatus.IN_PROGRESS)
			.tagText("tag")
			.sourceUrl(null)
			.build();

		when(bookmarkRepository.findByNoteIdAndUserId(7L, 2L)).thenReturn(Optional.empty());
		when(noteRepository.findById(7L)).thenReturn(Optional.of(note));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user));

		boolean bookmarked = noteBookmarkService.toggle(7L, 2L);

		assertThat(bookmarked).isTrue();
		ArgumentCaptor<NoteBookmark> captor = ArgumentCaptor.forClass(NoteBookmark.class);
		verify(bookmarkRepository).save(captor.capture());
		NoteBookmark saved = captor.getValue();
		assertThat(saved.getNote()).isEqualTo(note);
		assertThat(saved.getUser()).isEqualTo(user);
	}

	@Test
	void listReturnsResponsesFromMapper() {
		User user = buildUser(3L, UserRole.USER);
		Note note = Note.builder()
			.status(NoteStatus.IN_PROGRESS)
			.tagText("tag")
			.sourceUrl(null)
			.build();
		NoteBookmark bookmark = NoteBookmark.builder()
			.note(note)
			.user(user)
			.build();

	when(bookmarkRepository.findByUserIdOrderByCreatedAtDesc(3L)).thenReturn(List.of(bookmark));
	NoteBookmarkResponse response = new NoteBookmarkResponse(10L, 20L, "title", "image", "creator", "jobTitle", bookmark.getCreatedAt());
	when(noteMapper.toBookmarkResponse(bookmark)).thenReturn(response);

		assertThat(noteBookmarkService.list(3L)).containsExactly(response);
	}

	private User buildUser(Long id, UserRole role) {
		User user = User.builder()
			.email(id + "@test.com")
			.password("password")
			.username("user" + id)
			.role(role)
			.enabled(true)
			.accountNonExpired(true)
			.accountNonLocked(true)
			.credentialsNonExpired(true)
			.tokenVersion(0)
			.build();
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}
}
```

```java
/**
 * NoteAnswerService 단위 테스트 – USER 권한 검증 및 답변 생성/수정/삭제 흐름을 점검한다.
 */
@ExtendWith(MockitoExtension.class)
class NoteAnswerServiceTest {

	@Mock
	private NoteQuestionRepository noteQuestionRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private NoteMapper noteMapper;

	@InjectMocks
	private NoteAnswerService noteAnswerService;

	@Test
	void createAnswerPersistsNewEntity() {
		User user = buildUser(10L, UserRole.USER);
		NoteQuestion question = buildQuestion(5L, null);

		when(userRepository.findById(10L)).thenReturn(Optional.of(user));
		when(noteQuestionRepository.findById(5L)).thenReturn(Optional.of(question));
		when(noteQuestionRepository.save(question)).thenReturn(question);
		when(noteMapper.toAnswerDto(any())).thenReturn(new NoteAnswerDto(1L, 5L, 10L, "answer"));

		NoteAnswerDto dto = noteAnswerService.createAnswer(5L, 10L, "answer");

		assertThat(dto.answerText()).isEqualTo("answer");
		verify(noteQuestionRepository).save(question);
	}

	@Test
	void createAnswerFailsWhenAlreadyExists() {
		User user = buildUser(10L, UserRole.USER);
		NoteAnswer answer = NoteAnswer.builder()
			.respondent(user)
			.answerText("old")
			.build();
		NoteQuestion question = buildQuestion(5L, answer);

		when(userRepository.findById(10L)).thenReturn(Optional.of(user));
		when(noteQuestionRepository.findById(5L)).thenReturn(Optional.of(question));

		assertThatThrownBy(() -> noteAnswerService.createAnswer(5L, 10L, "new"))
			.isInstanceOf(IllegalStateException.class);
		verify(noteQuestionRepository, never()).save(any());
	}

	@Test
	void updateAnswerModifiesExistingEntity() {
		User user = buildUser(10L, UserRole.USER);
		NoteAnswer answer = NoteAnswer.builder()
			.respondent(user)
			.answerText("old")
			.build();
		NoteQuestion question = buildQuestion(7L, answer);

		when(userRepository.findById(10L)).thenReturn(Optional.of(user));
		when(noteQuestionRepository.findById(7L)).thenReturn(Optional.of(question));
		when(noteQuestionRepository.save(question)).thenReturn(question);
		when(noteMapper.toAnswerDto(question.getAnswer()))
			.thenReturn(new NoteAnswerDto(2L, 7L, 10L, "updated"));

		NoteAnswerDto dto = noteAnswerService.updateAnswer(7L, 10L, "updated");

		assertThat(dto.answerText()).isEqualTo("updated");
		verify(noteQuestionRepository).save(question);
	}

	@Test
	void deleteAnswerRemovesAssociation() {
		User user = buildUser(10L, UserRole.USER);
		NoteAnswer answer = NoteAnswer.builder()
			.respondent(user)
			.answerText("data")
			.build();
		NoteQuestion question = buildQuestion(8L, answer);

		when(userRepository.findById(10L)).thenReturn(Optional.of(user));
		when(noteQuestionRepository.findById(8L)).thenReturn(Optional.of(question));

		noteAnswerService.deleteAnswer(8L, 10L);

		verify(noteQuestionRepository).save(question);
		assertThat(question.getAnswer()).isNull();
	}

	@Test
	void createAnswerThrowsWhenUserNotFound() {
		when(userRepository.findById(1L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> noteAnswerService.createAnswer(1L, 1L, "answer"))
			.isInstanceOf(UserNotFoundException.class);
	}

	@Test
	void createAnswerThrowsWhenQuestionNotFound() {
		User user = buildUser(1L, UserRole.USER);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(noteQuestionRepository.findById(1L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> noteAnswerService.createAnswer(1L, 1L, "answer"))
			.isInstanceOf(NoteNotFoundException.class);
	}

	private User buildUser(Long id, UserRole role) {
		User user = User.builder()
			.email(id + "@test.com")
			.password("pw")
			.username("user")
			.role(role)
			.enabled(true)
			.accountNonExpired(true)
			.accountNonLocked(true)
			.credentialsNonExpired(true)
			.tokenVersion(0)
			.build();
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private NoteQuestion buildQuestion(Long id, NoteAnswer answer) {
		NoteQuestion question = NoteQuestion.builder()
			.questionText("Q?")
			.build();
		setId(question, id);
		if (answer != null) {
			question.assignAnswer(answer);
		}
		return question;
	}

	private void setId(NoteQuestion question, Long id) {
		try {
			var field = NoteQuestion.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(question, id);
		} catch (ReflectiveOperationException ignored) {
		}
	}
}
```

### 10.9 통합 테스트 (PostgreSQL + Redis 컨테이너)

```java
package com.okebari.artbite.note.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 노트 도메인 통합 테스트 전용 베이스 클래스.
 * Postgres와 Redis 컨테이너를 동시에 기동하고,
 * 스프링 데이터소스/Redis 설정을 동적으로 덮어쓴다.
 */
@Testcontainers
public abstract class NoteContainerBaseTest {

	@Container
	protected static final PostgreSQLContainer<?> POSTGRES =
		new PostgreSQLContainer<>("postgres:14")
			.withDatabaseName("artbite-note-test")
			.withUsername("note_test_user")
			.withPassword("note_test_pass");

	@Container
	protected static final GenericContainer<?> REDIS =
		new GenericContainer<>("redis:7-alpine")
			.withExposedPorts(6379);

	@DynamicPropertySource
	static void overrideProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

		registry.add("spring.data.redis.host", () -> REDIS.getHost());
		registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
	}
}
```

```java
package com.okebari.artbite.note.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.domain.user.UserRepository;
import com.okebari.artbite.domain.user.UserRole;
import com.okebari.artbite.note.domain.Note;
import com.okebari.artbite.note.domain.NoteStatus;
import com.okebari.artbite.note.repository.NoteRepository;
import com.okebari.artbite.note.scheduler.NoteStatusScheduler;

/**
 * 실제 Docker 컨테이너(PostgreSQL + Redis)를 활용한 통합 테스트 예시.
 * - COMPLETED 노트가 스케줄러에 의해 PUBLISHED로 전환되는지 확인
 * - Redis에 캐시(예: 노트 썸네일 정보)를 저장/조회할 수 있는지 확인
 */
@SpringBootTest
@Transactional
class NoteRedisIntegrationTest extends NoteContainerBaseTest {

	@Autowired
	private NoteRepository noteRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private NoteStatusScheduler noteStatusScheduler;

	@Autowired
	private StringRedisTemplate redisTemplate;

	@Test
	void completedNoteIsPublishedAndCached() {
		// given: Postgres에 ADMIN + COMPLETED 노트를 저장
		User admin = userRepository.save(
			User.builder()
				.email("admin@test.com")
				.password("pw")
				.username("admin")
				.role(UserRole.ADMIN)
				.enabled(true)
				.accountNonExpired(true)
				.accountNonLocked(true)
				.credentialsNonExpired(true)
				.tokenVersion(0)
				.build()
		);

		Note note = noteRepository.save(
			Note.builder()
				.status(NoteStatus.COMPLETED)
				.tagText("daily-tag")
				.sourceUrl("https://source")
				.build()
		);

		// when: 자정 배포 스케줄러를 직접 호출
		noteStatusScheduler.publishCompletedNotes();

		// then: DB 상태 검증
		Note published = noteRepository.findById(note.getId()).orElseThrow();
		assertThat(published.getStatus()).isEqualTo(NoteStatus.PUBLISHED);
		assertThat(published.getPublishedAt()).isBeforeOrEqualTo(LocalDateTime.now());

		// and: Redis에 노트 썸네일/메타데이터 캐시 저장 → 즉시 조회 확인
		String cacheKey = "notes:published:" + published.getId();
		redisTemplate.opsForHash().put(cacheKey, "title", "오늘의 작업노트");
		redisTemplate.opsForHash().put(cacheKey, "tag", published.getTagText());

		assertThat(redisTemplate.opsForHash().entries(cacheKey))
			.containsEntry("title", "오늘의 작업노트")
			.containsEntry("tag", "daily-tag");
	}
}
```
