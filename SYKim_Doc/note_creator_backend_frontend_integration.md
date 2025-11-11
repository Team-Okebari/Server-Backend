# 노트/크리에이터 백엔드 동작 & 프론트 연동 보고서

본 문서는 `Server-Backend`의 `note` / `creator` 패키지가 제공하는 API와 예외 정책, 그리고 `Frontend-1` 프로젝트에서 해당 API를 소비하는 흐름을 정리한 보고서입니다. 프론트엔드 개발자가 실제 구현을 진행할 때 참고할 수 있도록 엔드포인트, 응답 구조, 권한 제어, 에러 코드, 화면 매핑을 상세하게 설명합니다.

---

## 1. 공통 응답 & 예외 정책

### 1.1 CustomApiResponse 포맷

백엔드의 모든 REST 응답은 `CustomApiResponse<T>` 래퍼를 사용합니다.

```json
{
  "success": true,
  "data": { ... },          // 성공 시 페이로드
  "error": null,
  "timestamp": "2025-11-04T02:52:56.900774"
}
```

실패 시 `success`는 `false`, `data`는 `null`, `error`는 다음 구조입니다.

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "N003",
    "message": "노트에 접근할 권한이 없습니다."
  },
  "timestamp": "2025-11-04T02:52:56.746799"
}
```

프론트엔드에서는 `const payload = response.data.data;` 와 같이 두 단계를 거쳐 실제 데이터를 추출해야 합니다.

### 1.2 ErrorCode 정의

`com.okebari.artbite.common.exception.ErrorCode`에 도메인별 코드가 정의돼 있습니다.

| 분류 | 코드 | HTTP | 기본 메시지 | 사용 상황 |
|------|------|------|-------------|-----------|
| Common | C001~C005 | 400/401/403/404/500 | 공통 오류 | 유효성 실패, 인증 실패, 서버 오류 등 |
| Auth | A001~A005 | 401/404/409 | 인증/가입 관련 | 로그인 실패, 토큰 만료 등 |
| Note | N001 | 404 | 노트를 찾을 수 없습니다. | 존재하지 않는 노트 ID |
| Note | N002 | 400 | 허용되지 않은 노트 상태입니다. | 잘못된 상태 전환, 잘못된 답변 상태 등 |
| Note | N003 | 403 | 노트에 접근할 권한이 없습니다. | 권한 없이 접근 시 |
| Creator | CR001 | 404 | 작가 정보를 찾을 수 없습니다. | 존재하지 않는 크리에이터 |

### 1.3 도메인 예외 클래스

- `NoteNotFoundException` → `N001`
- `NoteInvalidStatusException` → `N002`
- `NoteAccessDeniedException` → `N003`
- `CreatorNotFoundException` → `CR001`

`BusinessException`을 확장하므로 `GlobalExceptionHandler`가 자동으로 일관된 포맷으로 응답합니다.

---

## 2. 노트 도메인 API

### 2.1 오늘 노트(메인 화면)

| 목적 | 엔드포인트 | 권한 | 설명 |
|------|------------|------|------|
| 오늘 노트 커버 | `GET /api/notes/published/today-cover` | 전체 공개 | 금일 게시된 `PUBLISHED` 노트의 커버 정보 |
| 오늘 노트 상세 | `GET /api/notes/published` | USER / ADMIN | 구독자는 전체 노트, 비구독자는 미리보기 |

**응답 구조**  
`NoteCoverResponse`:
```json
{
  "title": "커버 제목",
  "teaser": "짧은 소개",
  "mainImageUrl": "https://...",
  "creatorName": "작성자",
  "creatorJobTitle": "일러스트레이터",
  "publishedDate": "2025-11-04"
}
```
- `creatorJobTitle`는 작가의 직함/한줄 소개(`Creator.jobTitle`)로 프론트 히어로 카드에서 이름과 함께 노출합니다.

`GET /api/notes/published` 는 `TodayPublishedResponse` 를 반환합니다.

```json
{
  "accessible": true,
  "note": { ... NoteResponse ... },
  "preview": null
}
```

- `accessible`이 `true` → 구독자. `note` 필드에 `NoteResponse` 전체가 들어가며 `preview`는 `null`.
- `accessible`이 `false` → 비구독자. `note`는 `null`, `preview` 필드에 `NotePreviewResponse`(커버 + 개요 100자) 포함.

`NoteResponse` 참고 구조:
```json
{
  "id": 1,
  "status": "PUBLISHED",
  "tagText": "태그",
  "cover": { ... },
  "overview": { "sectionTitle": "...", "bodyText": "...", "imageUrl": "..." },
  "retrospect": { ... },
  "processes": [
    { "position": 1, "sectionTitle": "...", "bodyText": "...", "imageUrl": "..." },
    { "position": 2, ... }
  ],
  "question": { "questionText": "..." },
  "answer": { "answerText": "..." },
  "creatorId": 3,
  "externalLink": { "sourceUrl": "https://..." },
  "creator": {
    "id": 3,
    "name": "작성자",
    "bio": "소개",
    "jobTitle": "일러스트레이터",
    "profileImageUrl": "...",
    "instagramUrl": "...",
    "...": "..."
  },
  "publishedAt": "...",
  "archivedAt": null,
  "createdAt": "...",
  "updatedAt": "..."
}
```

**프론트 연동 (Frontend-1)**  
- 위치: `src/pages/main/MainPage.tsx`
- 현재는 테스트 이미지/텍스트를 사용함 → `useEffect`에서 `GET /api/notes/published/today-cover` 호출 후 상태에 반영.
- “오늘의 작업노트 보러가기” 버튼 클릭 시 `GET /api/notes/published` 호출 → 응답의 `accessible` 값에 따라 분기합니다.
  - `true`면 `note` 필드를 상세 페이지(`src/pages/detail`)에서 렌더링.
  - `false`면 `preview`만 내려오므로 구독 유도 화면을 유지하거나 프리뷰 전용 페이지로 이동.

### 2.2 미리보기 (무료 사용자를 위한 미리보기)

- 엔드포인트: `GET /api/notes/{noteId}/preview`
- 응답: `NotePreviewResponse { id, cover(title/mainImage/creator/publishedDate, teaser=null), overview { sectionTitle, bodyText<=100자, imageUrl } }`
- 용도: 상세 페이지 진입 시 즉시 보여줄 원본 데이터 (전체 작성 완료 전에도 노출 가능)

### 2.3 지난 노트 (아카이브)

| 목적 | 엔드포인트 | 권한 | 설명 |
|------|------------|------|------|
| 목록/검색 | `GET /api/notes/archived?keyword=&page=&size=` | USER / ADMIN | `archivedAt desc` 페이징, 키워드 검색 |
| 상세 | `GET /api/notes/archived/{noteId}` | USER / ADMIN | 구독자만 열람 가능 |

`ArchivedNoteSummaryResponse` (수정 후):
```json
{
  "id": 12,
  "tagText": "태그",
  "title": "노트 제목",
  "mainImageUrl": "https://...",
  "creatorName": "작가명",
  "publishedDate": "2024-06-01"
}
```

프론트 측에서는 `data.data.content` 배열을 카드로 렌더링하고, `data.data.totalPages`, `pageable` 정보를 이용해 페이지네이션을 구성합니다. 검색창은 `keyword` 파라미터를 그대로 전달하면 됩니다.

### 2.4 북마크

| 목적 | 엔드포인트 | 권한 | 설명 |
|------|------------|------|------|
| 북마크 토글 | `POST /api/notes/{noteId}/bookmark` | USER / ADMIN | 존재 여부에 따라 추가/삭제, `{ "bookmarked": true/false }` |
| 북마크 목록/검색 | `GET /api/notes/bookmarks?keyword=` | USER / ADMIN | `BookmarkListItemResponse { noteId, title, mainImageUrl, creatorName, tagText }` |

프론트에서는 `CustomApiResponse`를 파싱 후 토글 결과(`bookmarked`)를 상태에 반영합니다.
목록 API는 `keyword`(작가명/태그/제목 검색) 파라미터를 지원하며, 응답 `tagText`는 검색 하이라이트용으로만 사용하고 UI에는 제목·작가명·메인 이미지만 노출합니다.

### 2.5 노트 작성/수정/삭제 (Admin)

| 엔드포인트 | 설명 | 예외 |
|------------|------|------|
| `POST /api/admin/notes` | `NoteCreateRequest` → 노트 생성 (creatorId 필수) | `N003` (관리자만), `N002` (잘못된 초기 상태) |
| `PUT /api/admin/notes/{id}` | `NoteUpdateRequest` → 노트 업데이트 (creatorId 필수) | `N001` (존재하지 않음), `N002` (수정 불가 상태) |
| `DELETE /api/admin/notes/{id}` | 노트 삭제 | `N001` (존재하지 않음) |

`NoteService`는 상태 검증을 전부 커스텀 예외로 던집니다. 예를 들면 `PUBLISHED` 상태에서 수정하려고 하면 `N002`를 반환합니다.

### 2.6 질문/답변 (USER)

| 엔드포인트 | 설명 |
|------------|------|
| `POST /api/notes/questions/{id}/answer` | 최초 답변 작성 (`NOTE_INVALID_STATUS` 이미 존재 시) |
| `PUT /api/notes/questions/{id}/answer` | 답변 수정 (`NOTE_ACCESS_DENIED` 타 사용자 접근) |
| `DELETE /api/notes/questions/{id}/answer` | 답변 삭제 (`NOTE_INVALID_STATUS` 답변 없음) |

- 사용자 권한은 USER만 허용 (`NoteAccessDeniedException`).
- 질문 ID가 존재하지 않으면 `NoteNotFoundException` → `N001`.

---

## 3. 크리에이터 도메인 API

| 목적 | 엔드포인트 | 권한 | 설명 |
|------|------------|------|------|
| 작가 등록 | `POST /api/admin/creators` | ADMIN | `CreatorRequest` |
| 작가 목록 | `GET /api/admin/creators` | ADMIN | 관리용 전체 목록 |
| 작가 상세 | `GET /api/admin/creators/{id}` | ADMIN | 노트 작성 시 필요 |
| 작가 수정 | `PUT /api/admin/creators/{id}` | ADMIN | |
| 작가 삭제 | `DELETE /api/admin/creators/{id}` | ADMIN | |

`CreatorService`는 존재하지 않는 작가 ID에 대해 `CreatorNotFoundException` (`CR001`)을 던집니다.

프론트 `Frontend-1/src/pages/creator`에서 이 API들을 호출해 관리 UI를 구성하도록 되어 있습니다.

---

## 4. Frontend-1 연동 지침

### 4.1 API 클라이언트 (axios)

- 위치: `src/api/client.ts`
- 모든 요청은 `/api` prefix로 백엔드에 전달한다고 가정.
- 응답을 받을 때 `const { data } = await apiClient.get<CustomApiResponse<T>>(url); return data.data;` 패턴으로 구현해야 합니다.

### 4.2 페이지 매핑

| 화면 | 파일 | 사용하는 API | 주요 데이터 |
|------|------|--------------|-------------|
| 메인 | `pages/main/MainPage.tsx` | `GET /api/notes/published/today-cover` → 커버, `GET /api/notes/published/today-detail` (구독자) | `NoteCoverResponse`, `NoteResponse` |
| 오늘 노트 상세 | `pages/detail/NoteDetailPage.tsx` (가정) | `GET /api/notes/published`, `GET /api/notes/{id}/preview` | 전체 본문, 답변 |
| 지난 노트 목록 | `pages/note/ArchivedListPage.tsx` (가정) | `GET /api/notes/archived` | `ArchivedNoteSummaryResponse` 목록 |
| 지난 노트 상세 | `pages/note/ArchivedDetailPage.tsx` (가정) | `GET /api/notes/archived/{id}` | `NoteResponse` |
| 북마크 목록 | `pages/note/BookmarkPage.tsx` | `GET /api/notes/bookmarks`, `POST /api/notes/{id}/bookmark` | 북마크 카드 정보 |
| 크리에이터 관리 | `pages/creator/*` | `GET/POST/PUT/DELETE /api/admin/creators` | 작가 요약 / 상세 |

### 4.3 에러 처리

프론트에서는 `error.code`를 기준으로 UX를 분기하세요.

- `N003`: 구독자 전용 콘텐츠 → 구독 안내 모달/리다이렉트.
- `N002`: 잘못된 상태 → 토스트 혹은 폼 에러 메시지.
- `N001`, `CR001`: 404 페이지나 “존재하지 않는 데이터” 안내.

`CustomApiResponse.success`가 `false`일 때에는 `error.message`를 우선적으로 보여주도록 구현합니다.

---

## 5. TODO 및 향후 계획

1. **프론트 API 래퍼 정비**  
   - 현재 `Frontend-1/src/api/noteApi.ts`는 mock 형태이므로, `CustomApiResponse` 구조에 맞춰 다시 작성해야 합니다.
2. **구독 서비스 연동**  
   - 현재 `SubscriptionService`는 `AlwaysActiveSubscriptionService` 스텁이므로, 실제 구독 여부를 판단하는 구현으로 교체 예정.
3. **멤버십 메뉴**  
   - 햄버거 메뉴의 “멤버십” 항목은 아직 백엔드 API가 없으므로, 추후 팀장님이 제공하는 구독 관리 API 경로에 맞춰 연결하면 됩니다.
4. **프론트 디자인 반영**  
   - 최신 디자인 시스템을 적용하여 UI/UX 개선 예정. 현재 문서는 데이터 연동 중심이므로, 스타일링은 별도 디자인 가이드를 참고합니다.

---

## 6. 참고 소스 경로 요약

| 구분 | 경로 |
|------|------|
| Custom 응답 DTO | `src/main/java/com/okebari/artbite/common/dto/CustomApiResponse.java` |
| ErrorCode Enum | `src/main/java/com/okebari/artbite/common/exception/ErrorCode.java` |
| 노트 서비스 | `src/main/java/com/okebari/artbite/note/service/NoteService.java`, `NoteQueryService.java`, `NoteAnswerService.java` |
| 노트 컨트롤러 | `src/main/java/com/okebari/artbite/note/controller/NoteQueryController.java`, `NoteAdminController.java`, `NoteAnswerController.java`, `NoteBookmarkController.java` |
| 크리에이터 서비스/컨트롤러 | `src/main/java/com/okebari/artbite/creator/service/CreatorService.java`, `controller/CreatorAdminController.java` |
| 프론트 메인 페이지 | `Frontend-1/src/pages/main/MainPage.tsx` |
| 프론트 API 래퍼 | `Frontend-1/src/api/noteApi.ts`, `creatorApi.ts` |

---

본 문서는 백엔드와 프론트 간 데이터 계약을 명확히 하기 위한 기준점입니다. 변경 사항이 발생하면 동일 문서를 갱신하여 공유해 주세요.
