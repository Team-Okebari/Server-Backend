# 노트 / 크리에이터 API 명세

`Server-Backend`의 `/api/notes` 및 `/api/admin/creators` 영역에서 제공하는 REST API를 정리했습니다. 모든 응답은 `CustomApiResponse<T>` 포맷을 따릅니다.

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "timestamp": "2025-11-04T13:50:39.236868"
}
```

실패 시 `success=false`, `data=null`, `error={ code, message }`.

---

## API 요약 테이블

| 구분 | Method | Path | 권한 | 설명 |
| --- | --- | --- | --- | --- |
| Auth | POST | `/api/auth/signup` | 공개 | 회원가입 |
| Auth | POST | `/api/auth/login` | 공개 | 로그인 및 토큰 발급 |
| Auth | POST | `/api/auth/reissue` | 로그인 | Refresh Token 기반 Access Token 재발급 |
| Auth | POST | `/api/auth/logout` | 로그인 | Access/Refresh Token 무효화 |
| Notes | GET | `/api/notes/published/today-cover` | 공개 | 온보딩 이후 메인 커버 조회 |
| Notes | GET | `/api/notes/published/today-detail` | USER/ADMIN | 구독자 여부에 따라 본문/미리보기 제공 |
| Notes | GET | `/api/notes/published/today-preview` | USER/ADMIN | 무료 구독자용 미리보기 |
| Notes | GET | `/api/notes/archived` | USER/ADMIN | 지난 노트 목록/검색 |
| Notes | GET | `/api/notes/archived/{noteId}` | USER/ADMIN(구독) | 지난 노트 상세 |
| Notes | POST | `/api/notes/{noteId}/bookmark` | USER/ADMIN | 북마크 토글 |
| Notes | GET | `/api/notes/bookmarks` | USER/ADMIN | 북마크 목록 |
| Admin Notes | POST | `/api/admin/notes` | ADMIN | 노트 생성 |
| Admin Notes | PUT | `/api/admin/notes/{noteId}` | ADMIN | 노트 수정 |
| Admin Notes | DELETE | `/api/admin/notes/{noteId}` | ADMIN | 노트 삭제 |
| Admin Notes | GET | `/api/admin/notes` | ADMIN | 노트 목록 (페이지네이션) |
| Answers | POST | `/api/notes/questions/{questionId}/answer` | USER | 답변 생성 |
| Answers | PUT | `/api/notes/questions/{questionId}/answer` | USER | 답변 수정 |
| Answers | DELETE | `/api/notes/questions/{questionId}/answer` | USER | 답변 삭제 |

---

## Front-End Quick Reference

| 구분 | Method | Path | 인증/역할 | 요청 파라미터 | 핵심 응답 데이터 |
| --- | --- | --- | --- | --- | --- |
| 커버 | GET | `/api/notes/published/today-cover` | 없음 | 없음 | `NoteCoverResponse { title, teaser, mainImageUrl, creatorName, creatorJobTitle, publishedAt }` |
| 오늘 상세 | GET | `/api/notes/published/today-detail` | 토큰 (USER/ADMIN) | 없음 | `TodayPublishedResponse { accessible, note?, preview? }` |
| 오늘 미리보기 | GET | `/api/notes/published/today-preview` | 토큰 (USER/ADMIN) | 없음 | `NotePreviewResponse { id, cover, overviewPreview, externalLink, creator }` |
| 지난 노트 목록 | GET | `/api/notes/archived` | 토큰 (USER/ADMIN) | `keyword`, `page`, `size` | `Page<ArchivedNoteSummaryResponse { id, tagText, title, mainImageUrl, teaser }>` |
| 지난 노트 상세 | GET | `/api/notes/archived/{noteId}` | 토큰 (USER/ADMIN) + 유료 구독 | `noteId` | `NoteResponse` 전체 구조 |
| 북마크 토글 | POST | `/api/notes/{noteId}/bookmark` | 토큰 (USER/ADMIN) | `noteId` | `{ bookmarked: boolean }` |
| 북마크 목록 | GET | `/api/notes/bookmarks` | 토큰 (USER/ADMIN) | 없음 | `BookmarkListItemResponse[] { title, mainImageUrl, creatorName, creatorJobTitle }` |
| 답변 생성 | POST | `/api/notes/questions/{questionId}/answer` | 토큰 (USER) | `questionId` + `NoteAnswerRequest { answerText }` | `NoteAnswerResponse { answerText }` |
| 답변 수정 | PUT | `/api/notes/questions/{questionId}/answer` | 토큰 (USER) | `questionId` + `NoteAnswerRequest { answerText }` | 없음 (`204`) |
| 답변 삭제 | DELETE | `/api/notes/questions/{questionId}/answer` | 토큰 (USER) | `questionId` | 없음 (`204`) |

> [2025-11-05] 미리보기 응답(`NotePreviewResponse`), `creatorId` 필수 검증, 로그아웃 API 반환 형식을 최신 코드 기준으로 갱신했습니다.

---

## 1. 인증·회원 API (`AuthController`)

### 1.1 회원가입

- **POST** `/api/auth/signup`
- **권한**: 전체 공개
- **요청** `SignupRequestDto`

```json
{
  "email": "user@example.com",
  "password": "비밀번호(8자 이상)",
  "username": "표시 이름"
}
```

- **응답**: 생성된 사용자 ID (`Long`)
- **비고**: 비밀번호는 서버에서 `BCrypt`로 암호화되며 기본 권한은 `USER`로 설정됩니다.
- **에러**
  - `A001`: 이미 존재하는 이메일
  - `C001`: 요청 본문 검증 실패 (필드 누락, 형식 오류)

### 1.2 로그인

- **POST** `/api/auth/login`
- **권한**: 전체 공개
- **요청** `LoginRequestDto`

```json
{
  "email": "user@example.com",
  "password": "plain-password"
}
```

- **응답** `TokenDto`

```json
{
  "success": true,
  "data": "https://social-logout.example",
  "error": null,
  "timestamp": "..."
}
```

- **비고**
  - 성공 시 `refreshToken`이 `HttpOnly`, `Secure` 쿠키(`refreshToken`)로 내려갑니다.
  - Access Token은 `Authorization: Bearer {token}` 헤더로 사용합니다.
- **에러**
  - `A002`: 이메일 또는 비밀번호 불일치
  - `A005`: 사용자 정보 없음
  - `C001`: 요청 본문 검증 실패

### 1.3 Access Token 재발급

- **POST** `/api/auth/reissue`
- **권한**: 로그인 사용자 (Refresh Token 쿠키 필요)
- **요청**: 본문 없음, 쿠키의 `refreshToken` 사용
- **응답** `TokenDto` (새 Access Token) + 교체된 Refresh Token 쿠키
- **에러**
  - `A004`: Refresh Token 누락·형식 오류·무효
  - `A003`: Refresh Token 만료

### 1.4 로그아웃

- **POST** `/api/auth/logout`
- **권한**: 로그인 사용자
- **헤더**: `Authorization: Bearer {accessToken}`
- **응답**

```json
{
  "success": true,
  "data": "https://social-logout.example",
  "error": null,
  "timestamp": "..."
}
```

- **비고**
  - Access Token은 블랙리스트에 등록되며 Refresh Token은 Redis 및 쿠키에서 삭제됩니다.
  - 소셜 연동 계정의 경우 프론트엔드는 응답으로 받은 URL로 리디렉션합니다.
  - 리디렉션이 필요 없는 경우 `data` 값은 `null`입니다.
- **에러**
  - `C002`: 인증 정보 누락
  - `A004`: Access Token 무효
  - `A003`: Access Token 만료

---

## 2. 노트 조회 계열 (`NoteQueryController`)

### 2.1 온보딩 이후 메인 커버

- **GET** `/api/notes/published/today-cover`
- **권한**: 전체 공개
- **응답** `NoteCoverResponse`

```json
{
  "success": true,
  "data": {
    "title": "노트 제목",
    "teaser": "짧은 소개",
    "mainImageUrl": "https://...",
    "creatorName": "작성자",
    "creatorJobTitle": "일러스트레이터",
    "publishedAt": "2025-11-04T00:00:00"
  },
  "error": null,
  "timestamp": "..."
}
```

- `creatorJobTitle`: 작성자의 직함/한줄 소개 (`Creator.jobTitle`).

### 2.2 오늘 노트 상세 (구독자/비구독자 대응)

- **GET** `/api/notes/published/today-detail` (유료 구독자 전용 상세)
- **권한**: USER / ADMIN (로그인 필수, 구독 여부에 따라 본문/미리보기 분기)
- **응답** `TodayPublishedResponse`

```json
{
  "success": true,
  "data": {
    "accessible": true,
    "note": {
      "id": 101,
      "status": "PUBLISHED",
      "tagText": "워터컬러, 드로잉",
      "cover": {
        "title": "금일 노트 타이틀",
        "teaser": "채도가 낮은 봄빛을 담는 과정",
        "mainImageUrl": "https://cdn.example.com/note/today_main.jpg",
        "creatorName": "어트바이트",
        "creatorJobTitle": "일러스트레이터",
        "publishedAt": "2025-11-05T00:00:00"
      },
      "overview": {
        "sectionTitle": "이번 작업을 시작한 이유",
        "bodyText": "새 시즌 컬러 팔레트를 테스트하며 나온 인사이트를 정리했습니다...",
        "imageUrl": "https://cdn.example.com/note/overview.jpg"
      },
      "retrospect": {
        "sectionTitle": "돌아보며",
        "bodyText": "되돌아보니 보색 대비를 더 살리고 싶었습니다..."
      },
      "processes": [
        {
          "position": 1,
          "sectionTitle": "프로세스 1 - 구상",
          "bodyText": "아이패드로 색 조합을 먼저 시뮬레이션하며 대비를 확인했습니다.",
          "imageUrl": "https://cdn.example.com/note/process1.jpg"
        },
        {
          "position": 2,
          "sectionTitle": "프로세스 2 - 채색",
          "bodyText": "KST 기준 새벽 시간대에 채색을 완료했고, 건조 후 디테일을 보정했습니다.",
          "imageUrl": "https://cdn.example.com/note/process2.jpg"
        }
      ],
      "question": { "questionText": "이번 작업에서 가장 어려웠던 점은?" },
      "answer": { "answerText": "작업 중반에 색이 탁해지는 바람에 여러 번 레이어를 걷어냈어요." },
      "creatorId": 12,
      "externalLink": { "sourceUrl": "https://blog.example.com/behind-story" },
      "creator": {
        "id": 12,
        "name": "어트바이트",
        "bio": "수채화 기반 일러스트 작가",
        "jobTitle": "일러스트레이터",
        "profileImageUrl": "https://cdn.example.com/creator/12.jpg",
        "instagramUrl": "https://instagram.com/artbite",
        "youtubeUrl": null,
        "behanceUrl": null,
        "xUrl": "https://x.com/artbite",
        "blogUrl": "https://artbite.example.com",
        "newsUrl": null
      },
      "publishedAt": "2025-11-05T00:00:00",
      "archivedAt": null,
      "createdAt": "2025-11-03T09:12:00",
      "updatedAt": "2025-11-04T22:10:00"
    },
    "preview": null
  },
  "error": null,
  "timestamp": "2025-11-05T02:50:00"
}
```

- `creatorId`는 상세 페이지에서 작가 정보를 로딩하기 위해 필수입니다.
- `accessible=false`이면 `note=null`, `preview`에 미리보기 전용 데이터가 들어갑니다.

```json
{
  "success": true,
  "data": {
    "accessible": false,
    "note": null,
    "preview": {
      "id": 101,
      "cover": {
        "title": "금일 노트 타이틀",
        "teaser": "채도가 낮은 봄빛을 담는 과정",
        "mainImageUrl": "https://cdn.example.com/note/today_main.jpg",
        "creatorName": "어트바이트",
        "creatorJobTitle": "일러스트레이터",
        "publishedAt": "2025-11-05T00:00:00"
      },
      "overviewPreview": "이번 작업에서는 컬러 팔레트를 재구성했습니다...",
      "externalLink": { "sourceUrl": "https://blog.example.com/behind-story" },
      "creator": {
        "id": 12,
        "name": "어트바이트",
        "bio": "수채화 기반 일러스트 작가",
        "jobTitle": "일러스트레이터",
        "profileImageUrl": "https://cdn.example.com/creator/12.jpg"
      }
    }
  },
  "error": null,
  "timestamp": "2025-11-05T02:50:00"
}
```

- 에러 코드 없음 (구독자 여부 분기로 대체하므로 `NOTE_ACCESS_DENIED` 미사용).

### 2.3 오늘 노트 미리보기

- **GET** `/api/notes/published/today-preview`
- **권한**: USER / ADMIN (로그인 필수, 무료 구독자 포함)
- **응답** `NotePreviewResponse` (커버 + 개요 100자)

```json
{
  "success": true,
  "data": {
    "id": 101,
    "cover": {
      "title": "금일 노트 타이틀",
      "teaser": "10분 요약",
      "mainImageUrl": "https://cdn.example.com/note/today_main.jpg",
      "creatorName": "어트바이트",
      "creatorJobTitle": "일러스트레이터",
      "publishedAt": "2025-11-05T00:00:00"
    },
    "overviewPreview": "이번 작업에서는 컬러 팔레트를 재구성했습니다...",
    "externalLink": {
      "sourceUrl": "https://blog.example.com/behind-story"
    },
    "creator": {
      "id": 12,
      "name": "어트바이트",
      "bio": "수채화 기반 일러스트 작가",
      "jobTitle": "일러스트레이터",
      "profileImageUrl": "https://cdn.example.com/creator/12.jpg"
    }
  },
  "error": null,
  "timestamp": "2025-11-05T02:50:00"
}
```

- `overviewPreview`: 개요 본문 앞 100자를 잘라 문자열로 전달.
- `creator`: `CreatorSummaryDto`와 동일한 구조로 이름, 직함, 프로필 이미지, SNS 링크 등을 포함.
- **에러**
  - `N001`: 오늘 게시된 노트가 없을 때

### 2.4 지난 노트 목록 / 검색

- **GET** `/api/notes/archived`
- **권한**: USER / ADMIN
- **Query**: `keyword`(선택), `page`, `size`
- **응답** `Page<ArchivedNoteSummaryResponse>`

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 95,
        "tagText": "워터컬러",
        "title": "지난주: 팔레트 만들기",
        "mainImageUrl": "https://cdn.example.com/note/95_main.jpg",
        "teaser": "채도가 낮은 봄빛을 담는 과정"
      },
      {
        "id": 94,
        "tagText": "스케치",
        "title": "지난주: 콘셉트 스케치",
        "mainImageUrl": "https://cdn.example.com/note/94_main.jpg",
        "teaser": "콘셉트 스케치 과정을 공유합니다"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "offset": 0
    },
    "totalElements": 2,
    "totalPages": 1,
    "last": true,
    "first": true
  },
  "error": null,
  "timestamp": "2025-11-05T02:51:00"
}
```

- `tagText`: 검색 키워드 필터용 문자열 (미입력 시 `null`)
- `teaser`: 커버 요약 문구. 지난 노트 카드에서 서브텍스트로 사용.
- **에러**: 없음 (빈 페이지 시 `content=[]`)

### 2.5 지난 노트 상세

- **GET** `/api/notes/archived/{noteId}`
- **권한**: USER / ADMIN + 유료 구독
- **응답** `NoteResponse`

```json
{
  "success": true,
  "data": {
    "id": 95,
    "status": "ARCHIVED",
    "tagText": "워터컬러",
    "cover": {
      "title": "지난주: 팔레트 만들기",
      "teaser": "색 조합 실험",
      "mainImageUrl": "https://cdn.example.com/note/95_main.jpg",
      "creatorName": "어트바이트",
      "creatorJobTitle": "일러스트레이터",
      "publishedAt": "2025-11-02T00:00:00"
    },
    "overview": {
      "sectionTitle": "Overview",
      "bodyText": "이번 작업은 컬러 팔레트 연구가 핵심입니다...",
      "imageUrl": "https://cdn.example.com/note/95_overview.jpg"
    },
    "retrospect": {
      "sectionTitle": "Retrospect",
      "bodyText": "되돌아보니 보색 대비를 더 살리고 싶었습니다..."
    },
    "processes": [
      {
        "position": 1,
        "sectionTitle": "Process 1",
        "bodyText": "초기 스케치를 진행했습니다...",
        "imageUrl": "https://cdn.example.com/note/95_proc1.jpg"
      },
      {
        "position": 2,
        "sectionTitle": "Process 2",
        "bodyText": "컬러 팔레트를 확정했습니다...",
        "imageUrl": "https://cdn.example.com/note/95_proc2.jpg"
      }
    ],
    "question": { "questionText": "이번 작업에서 가장 어려웠던 점은?" },
    "answer": { "answerText": "색감 균형을 잡는 데 시간이 걸렸습니다." },
    "creatorId": 12,
    "externalLink": { "sourceUrl": "https://blog.example.com/archive/95" },
    "creator": {
      "id": 12,
      "name": "어트바이트",
      "profileImageUrl": "https://cdn.example.com/creator/12.jpg"
    },
    "publishedAt": "2025-11-02T00:00:00",
    "archivedAt": "2025-11-03T00:00:00",
    "createdAt": "2025-10-30T08:12:00",
    "updatedAt": "2025-10-31T10:05:00"
  },
  "error": null,
  "timestamp": "2025-11-05T02:52:00"
}
```

- **에러**
  - `N003`: 구독 중이 아닌 경우
  - `N001`: 존재하지 않는 노트
  - `N002`: 요청한 노트가 ARCHIVED 상태가 아닌 경우

### 2.6 북마크 토글

- **POST** `/api/notes/{noteId}/bookmark`
- **권한**: USER / ADMIN
- **응답**

```json
{
  "success": true,
  "data": { "bookmarked": true },
  "error": null,
  "timestamp": "..."
}
```

- **에러**
  - `N001`: 노트 미존재

### 2.7 북마크 목록

- **GET** `/api/notes/bookmarks`
- **권한**: USER / ADMIN
- **응답** `BookmarkListItemResponse[]`

```json
{
  "success": true,
  "data": [
    {
      "title": "봄날의 수채화 정리",
      "mainImageUrl": "https://cdn.example.com/note/main_20240201.jpg",
      "creatorName": "어트바이트",
      "creatorJobTitle": "마케터"
    },
    {
      "title": "지난주: 팔레트 만들기",
      "mainImageUrl": "https://cdn.example.com/note/95_main.jpg",
      "creatorName": "어트바이트",
      "creatorJobTitle": "그래픽디자이너"
    }
  ],
  "error": null,
  "timestamp": "2025-11-05T02:55:00"
}
```

- `data`는 사용자가 최근에 저장한 순으로 정렬됩니다(`createdAt desc`).
- `title`/`mainImageUrl`/`creatorName`/`creatorJobTitle`만 내려 UI 카드에서 바로 사용할 수 있도록 최소 필드만 포함합니다.
- **에러**
  - `N001`: 노트 미존재 (토글 직후 레이스 컨디션 등)

---

## 3. 노트 관리 (`NoteAdminController`, `NoteAnswerController`)

### 3.1 노트 생성

- **POST** `/api/admin/notes`
- **권한**: ADMIN (헤더 `Authorization: Bearer {accessToken}` 필수)
- **요청 본문 구조 (`NoteCreateRequest`)**

| 필드 | 필수 | 타입 | 설명 |
| --- | --- | --- | --- |
| `status` | O | `IN_PROGRESS` \| `COMPLETED` | 작성 단계. `COMPLETED`만 배포 후보가 됨. |
| `tagText` | X | `string` (≤60) | 태그 요약. 미입력 가능. |
| `cover.title` | O | `string` | 커버 타이틀. |
| `cover.teaser` | O | `string` | 커버 소개 문구. |
| `cover.mainImageUrl` | O | `string` | 커버 대표 이미지. 필수 URL. |
| `overview.sectionTitle` | O | `string` | 개요 섹션 제목. |
| `overview.bodyText` | O | `string` | 개요 본문. |
| `overview.imageUrl` | O | `string` | 개요 영역 이미지. |
| `retrospect.sectionTitle` | O | `string` | 회고 섹션 제목. |
| `retrospect.bodyText` | O | `string` | 회고 본문. |
| `processes` | O | 배열(2) | `NoteProcessDto` 2건이 반드시 존재해야 함. |
| `processes[].position` | O | `number` | 1과 2로 구분. 프론트 상단 프로세스 순서. |
| `processes[].sectionTitle` | O | `string` | 프로세스 제목. |
| `processes[].bodyText` | O | `string` | 프로세스 설명. |
| `processes[].imageUrl` | O | `string` | 프로세스 이미지 URL. |
| `question.questionText` | O | `string` | ADMIN이 등록하는 질문. |
| `creatorId` | O | `number` | 연결할 작가 ID. 애플리케이션 검증상 필수. |
| `externalLink.sourceUrl` | X | `string` (≤255) | 참고용 외부 링크. |

- **샘플 요청**

```json
{
  "status": "IN_PROGRESS",
  "tagText": "워터컬러, 드로잉",
  "cover": {
    "title": "봄날의 수채화 정리",
    "teaser": "채도가 낮은 봄빛을 담는 과정",
    "mainImageUrl": "https://cdn.example.com/note/main_20240201.jpg"
  },
  "overview": {
    "sectionTitle": "이번 작업을 시작한 이유",
    "bodyText": "이번 작업은 새로운 팔레트 실험을 중심으로 진행했습니다...",
    "imageUrl": "https://cdn.example.com/note/overview_20240201.jpg"
  },
  "retrospect": {
    "sectionTitle": "돌아보며",
    "bodyText": "색을 줄였더니 형태가 선명해졌습니다..."
  },
  "processes": [
    {
      "position": 1,
      "sectionTitle": "프로세스 1 - 구상",
      "bodyText": "아이패드로 색 조합을 먼저 시뮬레이션...",
      "imageUrl": "https://cdn.example.com/note/process1.jpg"
    },
    {
      "position": 2,
      "sectionTitle": "프로세스 2 - 채색",
      "bodyText": "KST 기준 새벽 시간대에 채색을 완료...",
      "imageUrl": "https://cdn.example.com/note/process2.jpg"
    }
  ],
  "question": {
    "questionText": "이번 작업에서 가장 어려웠던 점은?"
  },
  "creatorId": 12,
  "externalLink": {
    "sourceUrl": "https://blog.example.com/behind-story"
  }
}
```

- **응답**: `201 Created`, 본문은 `CustomApiResponse<Long>` (생성된 `noteId`)
- **생성 여부 확인 절차**
    1. ADMIN 계정으로 `/api/auth/login` → Access Token 확보.
    2. 위 JSON을 `POST /api/admin/notes`에 전송 (`Content-Type: application/json`, `Authorization` 헤더 필수).
    3. 응답 `noteId`를 이용해 `GET /api/admin/notes/{noteId}`로 내용 확인.
    4. 또는 `GET /api/admin/notes?page=0&size=10`에서 최신 생성 노트 확인.
- **에러**
    - `N003`: ADMIN 권한 아님.
    - `N002`: 상태 전환 규칙 위반, 프로세스 2건 미만 등 검증 실패.
    - `CR001`: 존재하지 않는 `creatorId`.

### 3.2 노트 수정

- **PUT** `/api/admin/notes/{noteId}`
- **권한**: ADMIN
- **요청** `NoteUpdateRequest` (구조는 생성 요청과 동일)
- **샘플 요청**

```json
{
  "status": "COMPLETED",
  "tagText": "워터컬러, 드로잉",
  "cover": {
    "title": "봄날의 수채화 정리 (최종)",
    "teaser": "채도가 낮은 봄빛을 담는 과정",
    "mainImageUrl": "https://cdn.example.com/note/main_20240201_final.jpg"
  },
  "overview": {
    "sectionTitle": "이번 작업을 시작한 이유",
    "bodyText": "피드백을 반영해 색 대비를 조정했습니다...",
    "imageUrl": "https://cdn.example.com/note/overview_20240201.jpg"
  },
  "retrospect": {
    "sectionTitle": "돌아보며",
    "bodyText": "최종적으로는 콘트라스트를 조금 더 살렸습니다..."
  },
  "processes": [
    {
      "position": 1,
      "sectionTitle": "프로세스 1 - 구상",
      "bodyText": "보노보노스러운 톤으로 수정...",
      "imageUrl": "https://cdn.example.com/note/process1.jpg"
    },
    {
      "position": 2,
      "sectionTitle": "프로세스 2 - 채색",
      "bodyText": "채색 단계에서 번짐을 줄이기 위해...",
      "imageUrl": "https://cdn.example.com/note/process2_v2.jpg"
    }
  ],
  "question": {
    "questionText": "완성 후 추가로 개선하고 싶은 점은?"
  },
  "creatorId": 12,
  "externalLink": {
    "sourceUrl": "https://blog.example.com/behind-story"
  }
}
```

- **응답**: `200 OK`, `CustomApiResponse<Void>`
- **에러**
    - `N001`: 존재하지 않는 노트.
    - `N002`: PUBLISHED/ARCHIVED 수정 시도, 상태 전환 규칙 위반.
    - `CR001`: 존재하지 않는 작가 ID.

### 3.3 노트 삭제

- **DELETE** `/api/admin/notes/{noteId}`
- **권한**: ADMIN
- **응답**: `204 No Content`
- **에러**
    - `N001`: 존재하지 않는 노트.

### 3.4 노트 목록 (관리자)

- **GET** `/api/admin/notes`
- **권한**: ADMIN
- **Query**: `page`(기본 0), `size`(기본 20), `sort` (예: `status,desc`)
- **응답** `Page<NoteResponse>`

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 101,
        "status": "IN_PROGRESS",
        "tagText": "워터컬러",
        "cover": { "...": "..." },
        "overview": { "...": "..." },
        "retrospect": { "...": "..." },
        "processes": [ { "...": "..." }, { "...": "..." } ],
        "question": { "questionText": "..." },
        "answer": null,
        "creator": {
          "id": 12,
          "name": "어드민",
          "profileImageUrl": "https://..."
        },
        "publishedAt": "2025-02-01T00:00:00",
        "archivedAt": null,
        "createdAt": "2025-01-31T10:12:00",
        "updatedAt": "2025-01-31T10:12:00"
      }
    ],
    "pageable": { "...": "..." },
    "totalElements": 1,
    "totalPages": 1
  },
  "error": null,
  "timestamp": "..."
}
```

- **비고**: 목록 응답은 관리용 `NoteResponse`이므로 생성/수정 직후 값 검증에 활용.

### 3.5 답변 생성

- **POST** `/api/notes/questions/{questionId}/answer`
- **권한**: USER
- **요청**

```json
{ "answerText": "텍스트 (최대 200자)" }
```

- **응답** `NoteAnswerDto`
- **에러**
    - `N002`: 이미 답변이 존재
    - `N001`: 존재하지 않는 질문
    - `AUTH_USER_NOT_FOUND`: 사용자 ID 미존재

### 3.6 답변 수정

- **PUT** `/api/notes/questions/{questionId}/answer`
- **권한**: USER
- **에러**
    - `N002`: 답변 없음
    - `N003`: 다른 사용자의 답변

### 3.7 답변 삭제

- **DELETE** `/api/notes/questions/{questionId}/answer`
- **권한**: USER
- **에러**
    - `N002`: 답변 없음
    - `N003`: 다른 사용자의 답변

---

## 4. 작가 관리 (`CreatorAdminController`)

> 모든 엔드포인트는 /api/admin/creators prefix이며 ADMIN 권한이 필수입니다.

| Method | Path | 설명 | 에러 |
| --- | --- | --- | --- |
| POST | `/` | 작가 등록 (`CreatorRequest`) → `Long` | - |
| GET | `/` | 작가 목록 (`CreatorSummaryDto[]`) | - |
| GET | `/{id}` | 작가 상세 (`CreatorResponse`) | `CR001` |
| PUT | `/{id}` | 작가 수정 (`CreatorRequest`) | `CR001` |
| DELETE | `/{id}` | 작가 삭제 | `CR001` |

`CreatorRequest` 필드: `name(필수)`, `bio(선택)`, `jobTitle(선택)`, `profileImageUrl`, `instagramUrl`, `youtubeUrl`, `behanceUrl`, `xUrl`, `blogUrl`, `newsUrl` (모두 255자 제한).

### 4.1 작가 등록

- **POST** `/api/admin/creators`
- **권한**: ADMIN (헤더 `Authorization: Bearer {accessToken}`)
- **요청**

```json
{
  "name": "어트바이트",
  "bio": "수채화 기반 일러스트 작가",
  "jobTitle": "일러스트레이터",
  "profileImageUrl": "https://cdn.example.com/creator/12.jpg",
  "instagramUrl": "https://instagram.com/artbite",
  "youtubeUrl": null,
  "behanceUrl": null,
  "xUrl": "https://x.com/artbite",
  "blogUrl": "https://artbite.example.com",
  "newsUrl": null
}
```

- **응답**: `CustomApiResponse<Long>` (생성된 `creatorId`)
- **에러**
    - `C001`: 입력값 검증 실패 (필수값 누락, 길이 초과 등)

> [2025-11-05] `jobTitle` 필드를 작가 소개(`bio`)와 별도의 직위 정보로 사용함을 명시했습니다.

### 4.2 작가 목록

- **GET** `/api/admin/creators`
- **권한**: ADMIN
- **응답** `CustomApiResponse<CreatorSummaryDto[]>`

```json
{
  "success": true,
  "data": [
    {
      "id": 12,
      "name": "어트바이트",
      "bio": "수채화 기반 일러스트 작가",
      "jobTitle": "일러스트레이터",
      "profileImageUrl": "https://cdn.example.com/creator/12.jpg",
      "instagramUrl": "https://instagram.com/artbite",
      "youtubeUrl": null,
      "behanceUrl": null,
      "xUrl": "https://x.com/artbite",
      "blogUrl": "https://artbite.example.com",
      "newsUrl": null
    }
  ],
  "error": null,
  "timestamp": "..."
}
```

### 4.3 작가 상세

- **GET** `/api/admin/creators/{creatorId}`
- **권한**: ADMIN
- **응답** `CustomApiResponse<CreatorResponse>`

```json
{
  "success": true,
  "data": {
    "id": 12,
    "name": "어트바이트",
    "bio": "수채화 기반 일러스트 작가",
    "jobTitle": "일러스트레이터",
    "profileImageUrl": "https://cdn.example.com/creator/12.jpg",
    "instagramUrl": "https://instagram.com/artbite",
    "youtubeUrl": null,
    "behanceUrl": null,
    "xUrl": "https://x.com/artbite",
    "blogUrl": "https://artbite.example.com",
    "newsUrl": null
  },
  "error": null,
  "timestamp": "..."
}
```

- **에러**
    - `CR001`: 존재하지 않는 작가 ID

### 4.4 작가 수정

- **PUT** `/api/admin/creators/{creatorId}`
- **권한**: ADMIN
- **요청**: `CreatorRequest` (구조는 등록과 동일)
- **응답**: `CustomApiResponse<Void>` (본문 `null`)
- **에러**
    - `CR001`: 존재하지 않는 작가 ID
    - `C001`: 입력값 검증 실패

### 4.5 작가 삭제

- **DELETE** `/api/admin/creators/{creatorId}`
- **권한**: ADMIN
- **응답**: `CustomApiResponse<Void>`
- **에러**
    - `CR001`: 존재하지 않는 작가 ID

---

## 5. Error Code 요약

| Code | HTTP | 메시지 | 주요 상황 |
| --- | --- | --- | --- |
| C001 | 400 | 잘못된 요청입니다. | DTO 검증 실패, 파라미터 오류 |
| C002 | 401 | 인증되지 않은 사용자입니다. | 토큰 또는 인증 정보 누락 |
| C003 | 403 | 접근 권한이 없습니다. | 인가 실패 (ADMIN 전용 등) |
| A001 | 409 | 이미 가입된 이메일입니다. | 회원가입 중 중복 이메일 |
| A002 | 401 | 이메일 또는 비밀번호가 일치하지 않습니다. | 로그인 실패 |
| A003 | 401 | 토큰이 만료되었습니다. | Access/Refresh Token 만료 |
| A004 | 401 | 유효하지 않은 토큰입니다. | 조작·블랙리스트 토큰 |
| A005 | 404 | 사용자를 찾을 수 없습니다. | 로그인/토큰 재발급 시 사용자 미존재 |
| N001 | 404 | 노트를 찾을 수 없습니다. | 존재하지 않는 노트 ID |
| N002 | 400 | 허용되지 않은 노트 상태입니다. | 상태 전환 규칙 위반, 비공개 노트 조회 |
| N003 | 403 | 노트에 접근할 권한이 없습니다. | 비구독자 접근 등 |
| CR001 | 404 | 작가 정보를 찾을 수 없습니다. | 잘못된 creatorId |

공통 오류와 인증 오류는 `GlobalExceptionHandler`에서 `CustomApiResponse` 포맷으로 내려보냅니다.

---

## 6. 응답 DTO 참고

- `NoteResponse`: 커버, 개요, 회고, 프로세스(2건), 질문, 답변, 작가 정보, 외부 링크, 상태/시각 필드 포함.
- `NotePreviewResponse`: 커버, 개요 프리뷰(본문 100자 문자열), 외부 링크, 작가 요약.
- `TodayPublishedResponse`: `accessible`, `note`, `preview` 세 필드로 구성.
- `ArchivedNoteSummaryResponse`: `id`, `tagText`, `title`, `mainImageUrl`, `teaser`만 포함해 카드 렌더링에 바로 활용.
- `BookmarkListItemResponse`: `title`, `mainImageUrl`, `creatorName`, `creatorJobTitle`.
- `CreatorSummaryDto`: 작가 기본 소개·직함 + SNS 링크 + 프로필 이미지.

---

## 7. 테스트 참고

단위 테스트에서 각 API의 예외가 검증되어 있습니다.

- `NoteServiceTest`: 권한, 상태 전환 검증.
- `NoteQueryServiceTest`: 프리뷰/오늘 노트 응답 분기 검증.
- `NoteAnswerServiceTest`: 답변 상태/권한 검증.
- `CreatorServiceTest`: 작가 미존재 예외 검증.

---

## 8. TODO

1. 프론트 `notesApi.ts`, `creatorApi.ts`를 `CustomApiResponse` 구조에 맞게 수정.
2. 멤버십(구독) 관련 백엔드 API가 추가되면 햄버거 메뉴 “멤버십” 항목에 연동.
3. `SubscriptionService` 스텁을 실제 구독 조회 로직으로 교체.
4. 필요 시 에러 코드 추가 (예: 북마크 중복, 질문 미존재 등). 이게 현재 내가 가지고 있는 파일이야. 이게 최신이야. 적용해.
