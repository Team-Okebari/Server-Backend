# Artbite API 상세 가이드

이 문서는 Artbite 백엔드 API의 모든 엔드포인트에 대한 상세 명세와 사용법을 제공합니다.

## 1. API 기본 정보

### 1.1. 기본 URL

로컬 환경에서 애플리케이션이 실행 중인 경우, API의 기본 URL은 다음과 같습니다:

`http://localhost:8080`

### 1.2. 공통 응답 구조

모든 API 응답은 `CustomApiResponse<T>` 래퍼 클래스를 따릅니다.

* **성공 응답:**
  ```json
  {
    "success": true,
    "data": { ... }, // 실제 응답 데이터. 없을 경우 null.
    "timestamp": "2025-11-24T12:00:00.123456"
  }
  ```
* **실패 응답:**
  ```json
  {
    "success": false,
    "error": {
      "code": "A001", // ErrorCode
      "message": "오류 메시지"
    },
    "timestamp": "2025-11-24T12:00:00.123456"
  }
  ```

---

## 2. 인증 (Authentication) API (`/api/auth`)

### 2.1. `POST /api/auth/signup`

- **설명:** 새로운 사용자를 등록합니다.
- **권한:** `permitAll`
- **Request Body (`SignupRequestDto`):**
    - **필드 설명:**
        - `email` (String): 사용자의 이메일 주소. (유효성: `@Email`, `@NotBlank`)
        - `password` (String): 사용자의 비밀번호. (유효성: `@NotBlank`, 8자 이상)
        - `username` (String): 사용자의 이름. (유효성: `@NotBlank`)
    - **Example:**
      ```json
      {
        "email": "user@example.com",
        "password": "password1234",
        "username": "testuser"
      }
      ```
- **Success Response (201 Created):**
    - **Body (`CustomApiResponse<Long>`):** 새로 생성된 사용자의 `id`를 반환합니다.
      ```json
      {
        "success": true,
        "data": 1, // 새로 생성된 사용자 ID
        "timestamp": "..."
      }
      ```
- **Error Responses:**
    - `409 Conflict` (`A001` - `AUTH_EMAIL_ALREADY_EXISTS`): 이메일 중복.
    - `400 Bad Request` (`C001` - `COMMON_BAD_REQUEST`): 요청 바디 유효성 검증 실패.

### 2.2. `POST /api/auth/login`

- **설명:** 이메일과 비밀번호로 로그인하여 `accessToken`을 발급받고, `refreshToken`은 HTTP-only 쿠키로 설정됩니다.
- **권한:** `permitAll`
- **Request Body (`LoginRequestDto`):**
    - **필드 설명:**
        - `email` (String): 사용자의 이메일 주소. (유효성: `@Email`, `@NotBlank`)
        - `password` (String): 사용자의 비밀번호. (유효성: `@NotBlank`)
    - **Example:**
      ```json
      {
        "email": "user@example.com",
        "password": "password1234"
      }
      ```
- **Success Response (200 OK):**
    - **Body (`CustomApiResponse<TokenDto>`):** `accessToken`을 포함한 DTO를 반환합니다.
      ```json
      {
        "success": true,
        "data": {
          "accessToken": "eyJhbGciOiJIUzM4NCJ9..." // String
        },
        "timestamp": "..."
      }
      ```
    - **Headers:** `Set-Cookie: refreshToken=...; HttpOnly; Secure; ...`
- **Error Responses:**
    - `401 Unauthorized` (`A002` - `AUTH_INVALID_CREDENTIALS`): 이메일 또는 비밀번호 불일치.
    - `400 Bad Request` (`C001` - `COMMON_BAD_REQUEST`): 요청 바디 유효성 검증 실패.

### 2.3. OAuth2 소셜 로그인 (OAuth2 Social Login)

- **설명:** 소셜 계정(provider: `google`, `kakao`, `naver`)으로 로그인 흐름을 시작합니다.
- **시작 엔드포인트:**
    - 카카오: `GET http://localhost:8080/oauth2/authorization/kakao`
    - 구글: `GET http://localhost:8080/oauth2/authorization/google`
    - 네이버: `GET http://localhost:8080/oauth2/authorization/naver`
- **권한:** `permitAll()`
- **인증 흐름:**
    1. 사용자가 위 시작 엔드포인트 중 하나로 접근합니다.
    2. 백엔드는 해당 소셜 공급자의 인증 페이지로 사용자를 리다이렉트합니다.
    3. 사용자가 소셜 공급자에서 인증을 완료하면, 소셜 공급자는 백엔드의 콜백 URL(`http://localhost:8080/login/oauth2/code/{provider}`)로 리다이렉트합니다.
    4. 백엔드는 콜백을 처리하여 사용자 정보를 가져오고, `User` 및 `UserSocialLogin` 엔티티를 생성/업데이트하며, Access Token과 Refresh Token을 발급합니다.
    5. 백엔드는 최종적으로 프론트엔드의 `successRedirectUri`로 리다이렉트합니다. (`http://localhost:3000/oauth2/redirect?accessToken=...`)
- **Request Body:** 없음 (브라우저 리다이렉션을 통해 시작됨)
- **Success Response (302 Redirect):**
    - **Redirect URL 예시:**
      ```
      http://localhost:3000/oauth2/redirect?accessToken=eyJhbGciOiJIUzI1NiI...
      ```
    - **Headers:**
      ```
      Set-Cookie: refreshToken=a3f6747d-5d9f-4099-...; Max-Age=1209600; Path=/; Secure; HttpOnly
      ```
    - **토큰 처리:**
        - **Access Token**: 프론트엔드는 리다이렉트된 URL의 쿼리 파라미터에서 `accessToken`을 추출하여 로컬 스토리지 또는 애플리케이션 상태에 저장해야 합니다.
        - **Refresh Token**: `Set-Cookie` 헤더를 통해 HTTP-only 쿠키로 자동 설정됩니다.
- **Error Responses:** 소셜 로그인 과정에서 발생하는 오류는 백엔드 콜백 시 프론트엔드 `failRedirectUri`로 리다이렉트되어 처리됩니다. (예: `A002` -
  `AUTH_INVALID_CREDENTIALS` 등)

### 2.4. `POST /api/auth/reissue`

- **설명:** `accessToken`이 만료되었을 때, `refreshToken`을 사용해 새로운 `accessToken`과 `refreshToken`을 발급받습니다. **Refresh Token
  Rotation**이 적용되어, 사용된 `refreshToken`은 무효화되고 새로운 `refreshToken`이 발급됩니다.
- **권한:** `isAuthenticated()`
- **Request Body:** 없음
- **Success Response (200 OK):**
    - **Body (`CustomApiResponse<TokenDto>`):**
      ```json
      {
        "success": true,
        "data": { "accessToken": "new_eyJhbGciOiJIUzM4NCJ9..." }, // String
        "timestamp": "..."
      }
      ```
    - **Headers:** `Set-Cookie: refreshToken=new_...; HttpOnly; Secure; ...`
- **Error Responses:** `A004` (`refreshToken`이 유효하지 않음)

### 2.5. `POST /api/auth/logout`

- **설명:** 현재 세션을 종료합니다. `accessToken`은 Redis 블랙리스트에 등록되고, `refreshToken`은 삭제됩니다.
- **권한:** `isAuthenticated()`
- **Request Body:** 없음
- **Success Response (200 OK):**
    - **Body (`CustomApiResponse<String>`):** 카카오 로그아웃의 경우 리다이렉션 URL(`String`)을, 일반 로그아웃의 경우 `null`을 반환합니다.
      ```json
      { "success": true, "data": null, "timestamp": "..." }
      ```
    - **Headers:** `refreshToken` 쿠키를 삭제하는 `Set-Cookie` 헤더 (`Max-Age=0`).
- **Error Responses:** 없음

### 2.6. 토큰 무효화 (Token Invalidation)

보안 강화를 위해, 사용자의 비밀번호 변경과 같은 중요한 보안 이벤트 발생 시 해당 사용자의 모든 기존 Refresh Token이 자동으로 무효화됩니다. 이 경우, 기존 Refresh Token을 사용한 재발급
요청은 실패하게 됩니다. (`User` 엔티티의 `tokenVersion` 필드 참조)

---

## 3. 멤버십 API (`/api/memberships`)

### 3.1. `GET /api/memberships/status`

- **설명:** 현재 로그인한 사용자의 멤버십 상태를 조회합니다.
- **권한:** `isAuthenticated()`
- **Request Body:** 없음
- **Success Response (200 OK):**
    - **Body (`CustomApiResponse<MembershipStatusResponseDto>`):**
      ```json
      {
        "success": true,
        "data": {
          "status": "ACTIVE",              // Enum (ACTIVE, CANCELED, EXPIRED, BANNED)
          "planType": "DEFAULT_MEMBER_PLAN",// Enum
          "startDate": "2025-10-26T10:00:00",// LocalDateTime
          "endDate": "2025-11-26T10:00:00",  // LocalDateTime
          "consecutiveMonths": 1,           // int
          "autoRenew": true                 // boolean
        },
        "timestamp": "..."
      }
      ```
- **Error Responses:**
    - `M001`: 멤버십 정보를 찾을 수 없음.
    - `A005`: 사용자 정보를 찾을 수 없음.

### 3.2. `POST /api/memberships/cancel`

- **설명:** '스마트 취소'를 실행합니다. 환불 조건(7일 이내, 콘텐츠 미사용) 충족 시 환불 후 즉시 만료, 아닐 경우 자동 갱신만 중단됩니다.
- **권한:** `hasRole('USER')`
- **Request Body:** 없음
- **Success Response (200 OK):**
    - **Body (`CustomApiResponse<Void>`):**
      ```json
      { "success": true, "data": null, "timestamp": "..." }
      ```
- **Error Responses:**
    - `M001`: 활성 멤버십이 없음.
    - `P013`: 유료 콘텐츠를 이미 이용하여 환불 불가.

### 3.3. `POST /api/memberships/reactivate-canceled`

- **설명:** 취소했던 멤버십의 자동 갱신을 다시 활성화합니다.
- **권한:** `hasRole('USER')`
- **Request Body:** 없음
- **Success Response (200 OK):**
    - **Body (`CustomApiResponse<MembershipStatusResponseDto>`):**
      ```json
      {
        "success": true,
        "data": {
          "status": "ACTIVE", // ACTIVE, CANCELED, EXPIRED, BANNED
          "planType": "DEFAULT_MEMBER_PLAN",
          "startDate": "2025-10-26T10:00:00",
          "endDate": "2025-11-26T10:00:00",
          "consecutiveMonths": 1,
          "autoRenew": true
        },
        "timestamp": "..."
      }
      ```
- **Error Responses:**
    - `M001`: 취소된 멤버십을 찾을 수 없음.

### 3.4. `GET /api/membership-inducement-image`

- **설명:** 멤버십 가입 유도 이미지의 URL을 조회합니다.
- **권한:** `permitAll()`
- **Request Body:** 없음
- **Success Response (200 OK):**
    - **Body (`CustomApiResponse<MembershipInducementImageResponseDto>`):**
      ```json
      {
        "success": true,
        "data": {
          "imageUrl": "https://.../resource_membership.png" // String
        },
        "timestamp": "..."
      }
      ```
- **Error Responses:**
    - `C004`: 유도 이미지 정보를 찾을 수 없음.

---

## 4. 노트 API (`/api/notes`)

### 4.1. 노트 조회 (Query)

- **`GET /api/notes/published/today-cover`**
    - **설명:** 공개용 '오늘의 노트' 커버 정보를 조회합니다.
    - **권한:** `permitAll`
    - **Request Body:** 없음
    - **Success Response (200 OK):**
        - **Body (`CustomApiResponse<NoteCoverResponse>`):**
          ```json
          {
            "success": true,
            "data": {
              "title": "오늘의 노트 제목", // String
              "teaser": "짧은 요약",    // String
              "mainImageUrl": "https://...", // String
              "creatorName": "작가 이름",   // String
              "creatorJobTitle": "작가 직무", // String
              "publishedDate": "2025-11-24", // LocalDate
              "category": { // Object
                "type": "MURAL", // Enum (MURAL, EMOTICON, GRAPHIC, etc.)
                "displayName": "벽화" // String
              }
            },
            "timestamp": "..."
          }
          ```
    - **Error Responses:**
        - `N001`: 오늘 게시된 노트를 찾을 수 없음.

- **`GET /api/notes/published/today-preview`**
    - **설명:** 로그인한 사용자를 위한 '오늘의 노트' 미리보기 정보를 조회합니다.
    - **권한:** `isAuthenticated()`
    - **Request Body:** 없음
    - **Success Response (200 OK):**
        - **Body (`CustomApiResponse<NotePreviewResponse>`):**
          ```json
          {
            "success": true,
            "data": {
              "id": 1, // Long
              "cover": { // NoteCoverResponse (상세 구조는 GET /api/notes/published/today-cover 참조)
                "title": "미리보기 노트 제목",
                "mainImageUrl": "https://..."
              },
              "overview": { // NoteOverviewDto
                "sectionTitle": "개요", // String
                "bodyText": "미리보기 내용 (100자까지)...", // String
                "imageUrl": "https://..." // String
              },
              "isBookmarked": false // Boolean
            },
            "timestamp": "..."
          }
          ```
    - **Error Responses:**
        - `N001`: 오늘 게시된 노트를 찾을 수 없음.

- **`GET /api/notes/published/today-detail`**
    - **설명:** 멤버십 구독자에게 '오늘의 노트' 전체 상세 정보를, 비구독자에게는 미리보기를 반환합니다.
    - **권한:** `isAuthenticated()`
    - **Request Body:** 없음
    - **Success Response (200 OK):**
        - **Body (`CustomApiResponse<TodayPublishedResponse>`):**
          ```json
          {
            "success": true,
            "data": {
              "accessible": true, // boolean (구독 여부)
              "note": { // NoteResponse (구독자에게만 제공, 상세 구조는 7.2 노트 관리 GET /{noteId} 참조)
                "id": 1, "status": "PUBLISHED", "tagText": "디자인",
                "cover": { ... }, "overview": { ... }, "retrospect": { ... },
                "processes": [ ... ], "question": { ... }, "answer": { ... },
                "creatorId": 1, "creator": { ... },
                "externalLink": { "sourceUrl": "https://..." },
                "isBookmarked": true
              },
              "preview": null // 비구독자에게는 NotePreviewResponse 제공
            },
            "timestamp": "..."
          }
          ```
    - **Error Responses:**
        - `N001`: 오늘 게시된 노트를 찾을 수 없음.

- **`GET /api/notes/archived`**
    - **설명:** 지난 노트 목록을 검색 조건과 함께 페이징하여 조회합니다.
    - **권한:** `isAuthenticated()`
    - **Request Parameters:**
        - `keyword` (String, optional): 검색어.
        - `page` (int, default 0): 페이지 번호.
        - `size` (int, default 10): 페이지당 항목 수.
        - `sort` (String, default `publishedAt,desc`): 정렬 기준 필드 및 방향.
    - **Success Response (200 OK):**
        - **Body (`CustomApiResponse<Page<ArchivedNoteSummaryResponse>>`):**
          ```json
          {
            "success": true,
            "data": {
              "content": [ // List<ArchivedNoteSummaryResponse>
                {
                  "id": 10, // Long
                  "tagText": "디자인", // String
                  "title": "지난 노트 1 제목", // String
                  "mainImageUrl": "https://...", // String
                  "creatorName": "작가 A", // String
                  "publishedDate": "2025-11-23" // LocalDate
                }
              ],
              "pageable": { /* 페이지네이션 메타데이터 */ },
              "last": false, "totalPages": 5, "totalElements": 50, "size": 10,
              "number": 0, "first": true, "numberOfElements": 10, "empty": false
            },
            "timestamp": "..."
          }
          ```
    - **Error Responses:** 없음.

- **`GET /api/notes/archived/{noteId}`**
    - **설명:** 특정 지난 노트의 상세 정보를 조회합니다. (비구독자는 미리보기만 반환)
    - **권한:** `isAuthenticated()`
    - **Path Variable:** `noteId` (Long).
    - **Request Body:** 없음
    - **Success Response (200 OK):**
        - **Body (`CustomApiResponse<ArchivedNoteViewResponse>`):**
          ```json
          {
            "success": true,
            "data": {
              "accessible": true, // boolean (구독 여부)
              "note": { // NoteResponse (구독자에게만 제공)
                "id": 1, "status": "PUBLISHED", "tagText": "디자인",
                "cover": { /* NoteCoverResponse 상세 */ },
                "overview": { /* NoteOverviewDto 상세 */ },
                "retrospect": { /* NoteRetrospectDto 상세 */ },
                "processes": [ /* List<NoteProcessDto> 상세 */ ],
                "question": { /* NoteQuestionDto 상세 */ },
                "answer": { /* NoteAnswerResponse 상세 */ },
                "creatorId": 1, "creator": { /* CreatorSummaryDto 상세 */ },
                "externalLink": { "sourceUrl": "https://..." },
                "publishedAt": "2025-11-23", "archivedAt": null, "createdAt": "...", "updatedAt": "...",
                "isBookmarked": true
              },
              "preview": null // 비구독자에게는 NotePreviewResponse 제공
            },
            "timestamp": "..."
          }
          ```
    - **Error Responses:**
        - `N001`: 노트를 찾을 수 없음.
        - `N002`: 노트가 열람 가능한 상태가 아님.

### 4.2. 노트 북마크 (Bookmark) API

- **`POST /api/notes/{noteId}/bookmark`**
    - **설명:** 노트를 북마크하거나 북마크를 해제합니다 (토글).
    - **권한:** `isAuthenticated()`
    - **Path Variable:** `noteId` (Long).
    - **Request Body:** 없음
    - **Success Response (200 OK):**
        - **Body (`CustomApiResponse<Map<String, Boolean>>`):**
          ```json
          {
            "success": true,
            "data": { "bookmarked": true }, // true면 북마크됨, false면 북마크 해제됨
            "timestamp": "..."
          }
          ```
    - **Error Responses:**
        - `N001`: 노트를 찾을 수 없음.
        - `A005`: 사용자 정보를 찾을 수 없음.

- **`GET /api/notes/bookmarks`**
    - **설명:** 현재 사용자가 북마크한 모든 노트 목록을 조회합니다. `keyword` 파라미터를 통해 검색할 수 있습니다.
    - **권한:** `isAuthenticated()`
    - **Request Parameters:** `keyword` (String, optional).
    - **Request Body:** 없음
    - **Success Response (200 OK):**
        - **Body (`CustomApiResponse<List<BookmarkListItemResponse>>`):**
          ```json
          {
            "success": true,
            "data": [
              {
                "noteId": 10,          // Long
                "title": "북마크된 노트 제목", // String
                "mainImageUrl": "https://...", // String
                "creatorName": "작가 이름",   // String
                "tagText": "태그"             // String
              }
            ],
            "timestamp": "..."
          }
          ```
    - **Error Responses:**
        - `A005`: 사용자 정보를 찾을 수 없음.

### 4.3. 질문 및 답변 (Q&A) API (`/api/notes/questions`)

- **`POST /api/notes/questions/{questionId}/answer`**
    - **설명:** 특정 질문에 대한 답변을 작성합니다.
    - **권한:** `hasRole('USER')`
    - **Path Variable:** `questionId` (Long).
    - **Request Body (`NoteAnswerRequest`):**
      ```json
      {
        "answerText": "이 질문에 대한 저의 답변입니다." // String, @NotBlank
      }
      ```
    - **Success Response (200 OK):**
        - **Body (`CustomApiResponse<NoteAnswerResponse>`):**
          ```json
          {
            "success": true,
            "data": { "answerText": "이 질문에 대한 저의 답변입니다." }, // String
            "timestamp": "..."
          }
          ```
    - **Error Responses:**
        - `N002`: 이미 등록된 답변이 있는 경우 (수정 API 사용 안내).
        - `N001`: 질문을 찾을 수 없음.
        - `A005`: 사용자 정보를 찾을 수 없음.

- **`PUT /api/notes/questions/{questionId}/answer`**
    - **설명:** 자신의 답변을 수정합니다.
    - **권한:** `hasRole('USER')`
    - **Path Variable:** `questionId` (Long).
    - **Request Body (`NoteAnswerRequest`):** (POST와 동일)
    - **Success Response (200 OK):**
        - **Body (`CustomApiResponse<NoteAnswerResponse>`):** (POST와 동일)
    - **Error Responses:**
        - `N002`: 등록된 답변이 없는 경우.

- **`DELETE /api/notes/questions/{questionId}/answer`**
    - **설명:** 자신의 답변을 삭제합니다.
    - **권한:** `hasRole('USER')`
    - **Path Variable:** `questionId` (Long).
    - **Request Body:** 없음
    - **Success Response (200 OK):**
        - **Body (`CustomApiResponse<Void>`):**
          ```json
          { "success": true, "data": null, "timestamp": "..." }
          ```
    - **Error Responses:**
        - `N002`: 삭제할 답변이 없는 경우.

- **`GET /api/notes/questions/{questionId}/answer`**
    - **설명:** 자신의 답변을 조회합니다.
    - **권한:** `hasRole('USER')`
    - **Path Variable:** `questionId` (Long).
    - **Request Body:** 없음
    - **Success Response (200 OK):**
        - **Body (`CustomApiResponse<NoteAnswerResponse>`):** (POST와 동일)
    - **Success Response (204 No Content):** 해당 질문에 대한 답변이 없는 경우.
    - **Error Responses:**
        - `N001`: 질문을 찾을 수 없음.

### 4.4. 리마인더 (Reminder) API

- **`GET /api/notes/reminder/today`**
    - **설명:** 오늘 사용자에게 표시될 노트 리마인더 정보를 조회합니다.
    - **권한:** `isAuthenticated()`
    - **Request Body:** 없음
    - **Success Response (200 OK):**
        - **Body (`CustomApiResponse<NoteReminderResponse>`):**
          ```json
          {
            "success": true,
            "data": {
              "surfaceHint": "BANNER", // Enum (DEFERRED, BANNER, MODAL, NONE)
              "noteId": 1,             // Long
              "title": "리마인더 노트 제목", // String
              "mainImageUrl": "https://...", // String
              "sourceType": "BOOKMARK",// Enum (BOOKMARK, ANSWER)
              "reminderDate": "2025-11-24", // LocalDate
              "dismissed": false       // boolean
            },
            "timestamp": "..."
          }
          ```
    - **Success Response (204 No Content):** 오늘 표시될 리마인더가 없는 경우.
    - **Error Responses:** 없음 (서비스에서 204 처리).

- **`POST /api/notes/reminder/dismiss`**
    - **설명:** '오늘은 그만 보기'를 선택하여 당일 리마인더를 숨깁니다.
    - **권한:** `isAuthenticated()`
    - **Request Body:** 없음
    - **Success Response (204 No Content):** 응답 바디 없음.
    - **Error Responses:**
        - `NR001`: 해제할 리마인더를 찾을 수 없음.

---

## 5. 결제 API (Toss Payments) (`/api/payments/toss`)

### 5.1. `POST /`

- **설명:** 클라이언트에서 결제창을 띄우기 전에 백엔드에 결제 정보를 요청합니다. 이 요청을 통해 `orderId`가 생성되고, 토스페이먼츠 결제창 호출에 필요한 정보가 반환됩니다.
- **권한:** `isAuthenticated()`
- **Request Body (`PaymentDto`):**
    - **필드 설명:**
        - `payType` (String): 결제 수단. (Enum: `CARD`, `VIRTUAL_ACCOUNT`, `TRANSFER`, `MOBILE_PHONE`).
        - `amount` (Long): 결제 금액.
        - `orderName` (String): 주문명. (선택 사항, 없으면 기본값 사용)
        - `membershipPlanType` (String): 멤버십 플랜 유형. (Enum: `DEFAULT_MEMBER_PLAN`).
        - `yourSuccessUrl` (String): 프론트엔드의 최종 성공 리다이렉트 URL. (선택 사항)
        - `yourFailUrl` (String): 프론트엔드의 최종 실패 리다이렉트 URL. (선택 사항)
    - **Example:**
      ```json
      {
        "payType": "CARD",
        "amount": 4900,
        "orderName": "Sparki 월간 구독",
        "membershipPlanType": "DEFAULT_MEMBER_PLAN",
        "yourSuccessUrl": "http://frontend.com/payment/success",
        "yourFailUrl": "http://frontend.com/payment/fail"
      }
      ```
- **Success Response (200 OK):**
    - **Body (`CustomApiResponse<PaymentResDto>`):**
      ```json
      {
        "success": true,
        "data": {
          "payType": "카드", // String, 결제 수단 설명 (백엔드에서 변환)
          "amount": 4900,   // Long
          "orderName": "Sparki 월간 구독", // String
          "orderId": "a1b2c3d4-e5f6-...", // String, 백엔드에서 생성된 고유 주문 ID
          "customerEmail": "user@example.com", // String
          "customerName": "사용자이름",          // String
          "successUrl": "http://localhost:8080/api/payments/toss/success?...", // String, Toss Payments에 전달할 백엔드 콜백 URL
          "failUrl": "http://localhost:8080/api/payments/toss/fail?...",     // String, Toss Payments에 전달할 백엔드 콜백 URL
          "frontendSuccessUrl": "http://frontend.com/payment/success", // String, 프론트엔드에서 요청한 최종 성공 URL
          "frontendFailUrl": "http://frontend.com/payment/fail",     // String, 프론트엔드에서 요청한 최종 실패 URL
          "createdAt": "2023-10-26T10:00:00.123456789" // String (ISO 8601), 결제 정보 생성 시각
        },
        "timestamp": "..."
      }
      ```
- **Error Responses:**
    - `P002`: 결제 금액 불일치.
    - `M002`: 이미 활성 멤버십 보유.
    - `M006`: 취소된 멤버십으로 신규 결제 불가.
    - `M003`: 정지된 멤버십.

### 5.2. `GET /api/payments/toss/success`

- **설명:** 토스페이먼츠 결제창에서 결제가 성공적으로 완료되면, 이 백엔드 엔드포인트로 리다이렉트됩니다. 백엔드는 결제를 최종 승인하고 멤버십을 활성화합니다.
- **권한:** `permitAll()`
- **Request Body:** 없음
- **Request Parameters:**
    - `paymentKey` (String): 토스페이먼츠에서 발급한 결제 키.
    - `orderId` (String): 백엔드에서 생성한 주문 ID.
    - `amount` (Long): 결제 금액.
    - `frontendSuccessUrl` (String): 프론트엔드의 최종 성공 리다이렉트 URL.
    - `frontendFailUrl` (String): 프론트엔드의 최종 실패 리다이렉트 URL.
- **Success Response (302 Redirect):** 제공된 `frontendSuccessUrl`로 리다이렉트됩니다.
- **Error Responses (리다이렉트 시):**
    - `P007`: 결제 승인 실패.
    - `M005`: 멤버십 활성화 실패.

### 5.3. `GET /api/payments/toss/fail`

- **설명:** 토스페이먼츠 결제창에서 결제가 실패하면, 이 백엔드 엔드포인트로 리다이렉트됩니다. 백엔드는 실패 정보를 기록합니다.
- **권한:** `permitAll()`
- **Request Body:** 없음
- **Request Parameters:**
    - `code` (String): 토스페이먼츠 오류 코드.
    - `message` (String): 토스페이먼츠 오류 메시지.
    - `orderId` (String): 백엔드에서 생성한 주문 ID.
    - `frontendSuccessUrl` (String): 프론트엔드의 최종 성공 리다이렉트 URL.
    - `frontendFailUrl` (String): 프론트엔드의 최종 실패 리다이렉트 URL.
- **Success Response (302 Redirect):** 제공된 `frontendFailUrl`로 리다이렉트되며, 쿼리 파라미터에 오류 정보가 포함됩니다.

### 5.4. `GET /api/payments/toss/history`

- **설명:** 현재 로그인한 사용자의 성공적인 결제 내역을 페이지네이션하여 조회합니다.
- **권한:** `isAuthenticated()`
- **Request Body:** 없음
- **Request Parameters:** `page`, `size`, `sort`.
- **Success Response (200 OK):**
    - **Body (`CustomApiResponse<SliceResponseDto<PaymentHistoryDto>>`):**
      ```json
      {
        "success": true,
        "data": {
          "data": [ // List<PaymentHistoryDto>
            {
              "paymentHistoryId": 1,      // Long
              "paymentKey": "paymentKey_123", // String
              "amount": 3900,             // Long
              "orderName": "Sparki 월간 구독", // String
              "createdAt": "2023-10-26T10:00:00", // LocalDateTime
              "status": "SUCCESS"         // Enum (SUCCESS, FAILED, CANCELED, etc.)
            }
          ],
          "sliceInfo": { // SliceInfo (페이징 메타데이터)
            "number": 0, // int, 현재 페이지 번호
            "size": 10,  // int, 페이지 크기
            "numberOfElements": 1, // int, 현재 페이지의 요소 수
            "hasNext": false       // boolean, 다음 페이지 존재 여부
          }
        },
        "timestamp": "..."
      }
      ```
- **Error Responses:**
    - `A005`: 사용자 정보를 찾을 수 없음.

---

## 6. 설정 API (`/api/config`)

### 6.1. `GET /api/config/payment`

- **설명:** 프론트엔드에서 사용할 토스페이먼츠 클라이언트 키, 주문명, 멤버십 금액 등 결제 관련 설정 정보를 조회합니다.
- **권한:** `permitAll()`
- **Request Body:** 없음
- **Success Response (200 OK):**
    - **Body (`CustomApiResponse<PaymentConfigResponseDto>`):**
      ```json
      {
        "success": true,
        "data": {
          "clientKey": "토스페이먼츠 클라이언트 API 키", // String
          "orderName": "Sparki 월간 구독",          // String
          "amount": 3900                       // Long
        },
        "timestamp": "..."
      }
      ```
- **Error Responses:** 없음 (백엔드 설정에 따라 반환)

---

## 7. 관리자 API (`/api/admin`)

**참고:** 모든 관리자 API는 `ADMIN` 역할을 가진 사용자만 접근 가능합니다.

### 7.1. 작가 관리 (`/api/admin/creators`)

- **`POST /api/admin/creators`**
    - **설명:** 새 작가를 등록합니다.
    - **권한:** `hasRole('ADMIN')`
    - **Request Body (`CreatorRequest`):**
        - **필드 설명:**
            - `name` (String): 작가 이름. (유효성: `@NotBlank`, 최대 60자)
            - `bio` (String): 작가 소개. (최대 100자)
            - `jobTitle` (String): 작가 직무. (최대 60자)
            - `profileImageUrl` (String): 프로필 이미지 URL. (최대 500자)
            - `instagramUrl` (String): 인스타그램 URL. (최대 500자)
            - `youtubeUrl` (String): 유튜브 URL. (최대 500자)
            - `behanceUrl` (String): 비핸스 URL. (최대 500자)
            - `xUrl` (String): X(트위터) URL. (최대 500자)
            - `blogUrl` (String): 블로그 URL. (최대 500자)
            - `newsUrl` (String): 뉴스/웹사이트 URL. (최대 500자)
        - **Example:**
          ```json
          {
            "name": "New Creator",
            "bio": "창의적인 디자인을 추구하는 작가입니다.",
            "jobTitle": "일러스트레이터",
            "profileImageUrl": "https://example.com/profile/creator1.png",
            "instagramUrl": "https://instagram.com/creator1",
            "youtubeUrl": null,
            "behanceUrl": "https://behance.net/creator1",
            "xUrl": null,
            "blogUrl": "https://creator1.blog.com",
            "newsUrl": null
          }
          ```
    - **Success Response (200 OK):**
        - **Body (`CustomApiResponse<Long>`):** 새로 생성된 작가의 `id`를 반환합니다.
          ```json
          { "success": true, "data": 123, "timestamp": "..." }
          ```
    - **Error Responses:**
        - `C001`: 요청 바디 유효성 검증 실패.

- **`GET /api/admin/creators`**
    - **설명:** 모든 작가 목록을 요약 정보와 함께 조회합니다.
    - **권한:** `hasRole('ADMIN')`
    - **Request Body:** 없음
    - **Success Response (200 OK):**
        - **Body (`CustomApiResponse<List<CreatorSummaryDto>>`):**
          ```json
          {
            "success": true,
            "data": [
              {
                "id": 1,                  // Long
                "name": "작가 1",            // String
                "bio": "디자이너입니다.",      // String
                "jobTitle": "그래픽 디자이너", // String
                "profileImageUrl": "https://...",// String
                "instagramUrl": "https://...", // String
                "youtubeUrl": null,           // String
                "behanceUrl": "https://...",  // String
                "xUrl": null,                 // String
                "blogUrl": "https://...",     // String
                "newsUrl": null               // String
              }
            ],
            "timestamp": "..."
          }
          ```
    - **Error Responses:** 없음.

- **`GET /api/admin/creators/{creatorId}`**
    - **설명:** 특정 작가의 상세 정보를 조회합니다.
    - **권한:** `hasRole('ADMIN')`
    - **Path Variable:** `creatorId` (Long).
    - **Request Body:** 없음
    - **Success Response (200 OK):**
        - **Body (`CustomApiResponse<CreatorResponse>`):** `CreatorSummaryDto`와 동일한 필드를 가지지만 더 상세한 정보가 포함될 수 있습니다.
          ```json
          {
            "success": true,
            "data": {
              "id": 1, "name": "작가 1", "bio": "상세 소개...", "jobTitle": "그래픽 디자이너",
              "profileImageUrl": "https://...", "instagramUrl": "https://..."
            },
            "timestamp": "..."
          }
          ```
    - **Error Responses:**
        - `CR001`: 작가를 찾을 수 없음.

- **`PUT /api/admin/creators/{creatorId}`**
    - **설명:** 특정 작가 정보를 수정합니다.
    - **권한:** `hasRole('ADMIN')`
    - **Path Variable:** `creatorId` (Long).
    - **Request Body (`CreatorRequest`):** (POST와 동일)
    - **Success Response (200 OK):**
        - **Body (`CustomApiResponse<Void>`):**
          ```json
          { "success": true, "data": null, "timestamp": "..." }
          ```
    - **Error Responses:**
        - `C001`: 요청 바디 유효성 검증 실패.
        - `CR001`: 작가를 찾을 수 없음.

- **`DELETE /api/admin/creators/{creatorId}`**
    - **설명:** 특정 작가를 삭제합니다.
    - **권한:** `hasRole('ADMIN')`
    - **Path Variable:** `creatorId` (Long).
    - **Request Body:** 없음
    - **Success Response (200 OK):**
        - **Body (`CustomApiResponse<Void>`):**
          ```json
          { "success": true, "data": null, "timestamp": "..." }
          ```
    - **Error Responses:**
        - `CR001`: 작가를 찾을 수 없음.

### 7.2. 노트 관리 (`/api/admin/notes`)

- **`POST /api/admin/notes`**
    - **설명:** 새 노트를 생성합니다. 노트의 모든 구성 요소(커버, 개요, 제작 과정, 회고, 질문, 외부 링크)를 포함하는 복합적인 요청 바디입니다.
    - **권한:** `hasRole('ADMIN')`
    - **Request Body (`NoteCreateRequest`):**
        - **필드 설명:**
            - `status` (String): 노트 상태. (Enum: `IN_PROGRESS`, `COMPLETED`, `ARCHIVED`)
            - `tagText` (String): 태그 텍스트. (최대 60자)
            - `creatorId` (Long): 작가 ID. (필수)
            - `cover` (Object): 노트 커버 정보. (`NoteCoverDto` 참조)
            - `overview` (Object): 노트 개요 정보. (`NoteOverviewDto` 참조)
            - `retrospect` (Object): 노트 회고 정보. (`NoteRetrospectDto` 참조)
            - `processes` (List<Object>): 노트 제작 과정. (`NoteProcessDto` 참조, 최대 2개)
            - `question` (Object): 노트 질문 정보. (`NoteQuestionDto` 참조)
            - `externalLink` (Object): 외부 링크 정보. (`NoteExternalLinkDto` 참조)
        - **`NoteCoverDto`:**
          ```json
          {
            "title": "커버 제목",      // String, @NotBlank, 최대 50자
            "teaser": "커버 티저",     // String, @NotBlank, 최대 100자
            "mainImageUrl": "https://...", // String
            "creatorName": "작가 이름",   // String, 읽기 전용으로 백엔드에서 채움
            "creatorJobTitle": "작가 직무", // String, 읽기 전용으로 백엔드에서 채움
            "category": "MURAL"      // Enum (MURAL, EMOTICON, GRAPHIC 등)
          }
          ```
        - **`NoteOverviewDto`:**
          ```json
          {
            "sectionTitle": "개요 섹션 제목", // String, @NotBlank, 최대 30자
            "bodyText": "개요 본문 내용",   // String, @NotBlank, 최대 200자
            "imageUrl": "https://..."      // String
          }
          ```
        - **`NoteRetrospectDto`:**
          ```json
          {
            "sectionTitle": "회고 섹션 제목", // String, @NotBlank, 최대 30자
            "bodyText": "회고 본문 내용"     // String, @NotBlank, 최대 200자
          }
          ```
        - **`NoteProcessDto`:**
          ```json
          {
            "position": 1,                 // short, @NotNull, 1 또는 2
            "sectionTitle": "과정 제목",     // String, @NotBlank, 최대 30자
            "bodyText": "과정 본문 내용",   // String, @NotBlank, 최대 500자
            "imageUrl": "https://..."      // String
          }
          ```
        - **`NoteQuestionDto`:**
          ```json
          {
            "questionText": "질문 텍스트" // String, 최대 100자
          }
          ```
        - **`NoteExternalLinkDto`:**
          ```json
          {
            "sourceUrl": "https://example.com/external-link" // String, 최대 500자
          }
          ```
    - **Success Response (201 Created):**
        - **Body (`CustomApiResponse<Long>`):** 새로 생성된 노트의 `id`를 반환합니다.
          ```json
          { "success": true, "data": 123, "timestamp": "..." }
          ```
    - **Error Responses:**
        - `N002`: 허용되지 않은 초기 상태.
        - `CR001`: 지정된 작가를 찾을 수 없음.
        - `C001`: 요청 바디 유효성 검증 실패.

- **`GET /api/admin/notes`**
    - **설명:** 관리자 전용 노트 목록을 페이징하여 조회합니다.
    - **권한:** `hasRole('ADMIN')`
    - **Request Body:** 없음
    - **Request Parameters:** `page`, `size`, `sort`.
    - **Success Response (200 OK):**
        - **Body (`CustomApiResponse<Page<NoteResponse>>`):** (Content는 `NoteResponse` 리스트, 상세 구조는 아래
          `GET /api/admin/notes/{noteId}` 참조)
          ```json
          {
            "success": true,
            "data": {
              "content": [ /* List<NoteResponse> */ ],
              "pageable": { ... },
              "last": false, "totalPages": 5, "totalElements": 50, "size": 10,
              "number": 0, "first": true, "numberOfElements": 10, "empty": false
            },
            "timestamp": "..."
          }
          ```
    - **Error Responses:** 없음.

- **`GET /api/admin/notes/{noteId}`**
    - **설명:** 특정 노트의 상세 정보를 관리자용으로 조회합니다.
    - **권한:** `hasRole('ADMIN')`
    - **Path Variable:** `noteId` (Long).
    - **Request Body:** 없음
    - **Success Response (200 OK):**
        - **Body (`CustomApiResponse<NoteResponse>`):**
            - **필드 설명:**
                - `id` (Long): 노트 ID.
                - `status` (String): 노트 상태. (Enum: `IN_PROGRESS`, `COMPLETED`, `PUBLISHED`, `ARCHIVED`)
                - `tagText` (String): 태그 텍스트.
                - `cover` (Object): 노트 커버 정보. (`NoteCoverResponse` 참조)
                - `overview` (Object): 노트 개요 정보. (`NoteOverviewDto` 참조)
                - `retrospect` (Object): 노트 회고 정보. (`NoteRetrospectDto` 참조)
                - `processes` (List<Object>): 노트 제작 과정. (`NoteProcessDto` 참조)
                - `question` (Object): 노트 질문 정보. (`NoteQuestionDto` 참조)
                - `answer` (Object): 사용자 답변 정보. (`NoteAnswerResponse` 참조)
                - `creatorId` (Long): 작가 ID.
                - `creatorJobTitle` (String): 작가 직무.
                - `externalLink` (Object): 외부 링크 정보. (`NoteExternalLinkDto` 참조)
                - `creator` (Object): 작가 요약 정보. (`CreatorSummaryDto` 참조)
                - `publishedAt` (LocalDate): 게시 일자.
                - `archivedAt` (LocalDateTime): 보관 시각.
                - `createdAt` (LocalDateTime): 생성 시각.
                - `updatedAt` (LocalDateTime): 최종 수정 시각.
                - `isBookmarked` (Boolean): 현재 사용자의 북마크 여부.
            - **Example:** (이전 섹션의 DTO들을 포함하는 복합 JSON)
              ```json
              {
                "success": true,
                "data": {
                  "id": 1,
                  "status": "PUBLISHED",
                  "tagText": "디자인 트렌드",
                  "cover": {
                    "title": "2025년 디자인 트렌드",
                    "teaser": "미래 디자인을 엿보다.",
                    "mainImageUrl": "https://...",
                    "creatorName": "김작가",
                    "creatorJobTitle": "UX 디자이너",
                    "publishedDate": "2025-11-24",
                    "category": { "type": "GRAPHIC", "displayName": "그래픽" }
                  },
                  "overview": {
                    "sectionTitle": "개요",
                    "bodyText": "2025년 디자인 트렌드의 주요 흐름...",
                    "imageUrl": "https://..."
                  },
                  "retrospect": {
                    "sectionTitle": "회고",
                    "bodyText": "이번 트렌드 분석을 통해 느낀 점..."
                  },
                  "processes": [
                    { "position": 1, "sectionTitle": "아이디어 구상", "bodyText": "초기 아이디어 스케치...", "imageUrl": "https://..." },
                    { "position": 2, "sectionTitle": "프로토타입 제작", "bodyText": "피드백 반영 과정...", "imageUrl": "https://..." }
                  ],
                  "question": { "questionText": "가장 인상 깊었던 트렌드는?" },
                  "answer": { "answerText": "젠스타일 미니멀리즘이었습니다." }, // 로그인 사용자의 답변
                  "creatorId": 1,
                  "creator": { "id": 1, "name": "김작가", "jobTitle": "UX 디자이너", ... },
                  "externalLink": { "sourceUrl": "https://dribbble.com/trends" },
                  "publishedAt": "2025-11-24T10:00:00",
                  "archivedAt": null,
                  "createdAt": "2025-11-20T09:00:00",
                  "updatedAt": "2025-11-24T10:00:00",
                  "isBookmarked": true
                },
                "timestamp": "..."
              }
              ```
    - **Error Responses:**
        - `N001`: 노트를 찾을 수 없음.

- **`PUT /api/admin/notes/{noteId}`**
    - **설명:** 특정 노트 정보를 수정합니다.
    - **권한:** `hasRole('ADMIN')`
    - **Path Variable:** `noteId` (Long).
    - **Request Body (`NoteUpdateRequest`):** `NoteCreateRequest`와 동일한 구조를 가지지만, 모든 필드가 선택 사항이며 부분 업데이트가 가능합니다. 이미지 URL을
      변경하지 않으려면 해당 필드를 `null`로 두거나 `uploadImage` API에서 반환된 기존 URL을 다시 전달합니다.
    - **Success Response (200 OK):** `CustomApiResponse<Void>`
    - **Error Responses:**
        - `N002`: 허용되지 않은 상태 전환 또는 `PUBLISHED`/`ARCHIVED` 상태의 노트 수정 시도.
        - `N001`: 노트를 찾을 수 없음.
        - `CR001`: 지정된 작가를 찾을 수 없음.

- **`DELETE /api/admin/notes/{noteId}`**
    - **설명:** 특정 노트를 삭제합니다.
    - **권한:** `hasRole('ADMIN')`
    - **Path Variable:** `noteId` (Long).
    - **Request Body:** 없음
    - **Success Response (204 No Content):** 응답 바디 없음.
    - **Error Responses:**
        - `N001`: 노트를 찾을 수 없음.

### 7.3. 이미지 관리 (`/api/admin/images`)

- **`POST /api/admin/images`**
    - **설명:** 이미지 파일을 AWS S3에 업로드하고, 업로드된 이미지의 공개 URL을 반환합니다.
    - **권한:** `hasRole('ADMIN')`
    - **Request Body:** `multipart/form-data`
        - `image` (File): 업로드할 이미지 파일.
    - **Success Response (200 OK):**
        - **Body (`CustomApiResponse<ImageUploadResponse>`):**
          ```json
          {
            "success": true,
            "data": {
              "imageUrl": "https://your-s3-bucket.s3.your-region.amazonaws.com/path/to/image.png" // String
            },
            "timestamp": "..."
          }
          ```
    - **Error Responses:**
        - `C006`: 파일 업로드 용량 초과.
        - `C005`: S3 업로드 중 서버 내부 오류.

### 7.4. 멤버십 관리 (관리자용)

- **`PUT /api/admin/membership-inducement-image`**
    - **설명:** 멤버십 유도 이미지의 URL을 변경합니다.
    - **권한:** `hasRole('ADMIN')`
    - **Request Body (`MembershipInducementImageUpdateRequestDto`):**
      ```json
      {
        "imageUrl": "https://new-image-url.com/image.png" // String
      }
      ```
    - **Success Response (200 OK):** `CustomApiResponse<Void>`
    - **Error Responses:**
        - `C004`: 유도 이미지 정보를 찾을 수 없음.

- **`POST /api/memberships/{userId}/ban`**
    - **설명:** 특정 사용자의 멤버십을 강제로 정지시킵니다.
    - **권한:** `hasRole('ADMIN')`
    - **Path Variable:** `userId` (Long).
    - **Request Body:** 없음
    - **Success Response (200 OK):** `CustomApiResponse<Void>`
    - **Error Responses:**
        - `A005`: 사용자를 찾을 수 없음.
        - `M001`: 멤버십 정보를 찾을 수 없음.
        - `M003`: 이미 정지된 멤버십을 정지 시도.

- **`POST /api/memberships/{userId}/unban`**
    - **설명:** 정지된 사용자 멤버십을 해제합니다. (해제 시 멤버십 상태는 `EXPIRED`로 변경됩니다.)
    - **권한:** `hasRole('ADMIN')`
    - **Path Variable:** `userId` (Long).
    - **Request Body:** 없음
    - **Success Response (200 OK):** `CustomApiResponse<Void>`
    - **Error Responses:**
        - `M001`: 정지된 멤버십을 찾을 수 없음.

### 7.5. 결제 관리 (관리자용)

- **`GET /api/admin/payments/by-user`**
    - **설명:** 특정 사용자의 모든 결제 내역을 이메일로 조회합니다.
    - **권한:** `hasRole('ADMIN')`
    - **Request Parameters:**
        - `email` (String): 조회할 사용자 이메일. (필수)
        - `page` (int, default 0): 페이지 번호.
        - `size` (int, default 10): 페이지당 항목 수.
        - `sort` (String, default `createdAt,desc`): 정렬 기준 필드 및 방향.
    - **Request Body:** 없음
    - **Success Response (200 OK):**
        - **Body (`CustomApiResponse<SliceResponseDto<PaymentHistoryDto>>`):**
          ```json
          {
            "success": true,
            "data": {
              "data": [ // List<PaymentHistoryDto>
                {
                  "paymentHistoryId": 1,      // Long
                  "paymentKey": "paymentKey_123", // String
                  "amount": 3900,             // Long
                  "orderName": "Sparki 월간 구독", // String
                  "createdAt": "2023-10-26T10:00:00", // LocalDateTime
                  "status": "SUCCESS"         // Enum (SUCCESS, FAILED, CANCELED, etc.)
                }
              ],
              "sliceInfo": { // SliceInfo (페이징 메타데이터)
                "number": 0, "size": 10, "numberOfElements": 1, "hasNext": false
              }
            },
            "timestamp": "..."
          }
          ```
    - **Error Responses:**
        - `A005`: 사용자를 찾을 수 없음.

- **`GET /api/admin/payments/details-by-user`**
    - **설명:** 특정 사용자의 종합 정보(결제 내역, 멤버십 상태, 콘텐츠 접근 기록)를 이메일로 조회합니다.
    - **권한:** `hasRole('ADMIN')`
    - **Request Parameters:**
        - `email` (String): 조회할 사용자 이메일. (필수)
        - `paymentPageable.page` (int, default 0): 결제 내역 페이지 번호.
        - `paymentPageable.size` (int, default 10): 결제 내역 페이지 크기.
        - `accessLogPageable.page` (int, default 0): 콘텐츠 접근 기록 페이지 번호.
        - `accessLogPageable.size` (int, default 10): 콘텐츠 접근 기록 페이지 크기.
    - **Request Body:** 없음
    - **Success Response (200 OK):**
        - **Body (`CustomApiResponse<AdminUserPaymentDetailsDto>`):**
          ```json
          {
            "success": true,
            "data": {
              "userId": 1,        // Long
              "email": "user@example.com", // String
              "membershipStatus": { /* MembershipStatusResponseDto 상세 */ },
              "paymentHistory": { /* SliceResponseDto<PaymentHistoryDto> 상세 */ },
              "contentAccessLogs": { /* SliceResponseDto<ContentAccessLogDto> 상세 */ }
            },
            "timestamp": "..."
          }
          ```
    - **Error Responses:**
        - `A005`: 사용자를 찾을 수 없음.

- **`POST /api/admin/payments/{paymentKey}/refund`**
    - **설명:** 특정 결제를 강제로 환불 처리합니다. (정책 검증 우회)
    - **권한:** `hasRole('ADMIN')`
    - **Path Variable:** `paymentKey` (String).
    - **Request Body (`RefundRequestDto`):**
      ```json
      {
        "reason": "관리자 수동 환불" // String, 환불 사유
      }
      ```
    - **Success Response (200 OK):**
        - **Body (`CustomApiResponse<TossPaymentCancelDto>`):** Toss Payments API 응답과 유사한 환불 처리 결과.
          ```json
          {
            "success": true,
            "data": {
              "mId": "mId_test", // String
              "version": "1.3",  // String
              "paymentKey": "paymentKey_123", // String
              "orderId": "orderId_abc", // String
              "orderName": "상품명", // String
              "status": "CANCELED", // String
              "requestedAt": "2025-11-24T12:00:00.000+09:00", // String (ISO 8601)
              "approvedAt": "2025-11-24T12:00:00.000+09:00", // String (ISO 8601)
              "totalAmount": 4900, // Long
              "canceledAmount": 4900, // Long
              "cancelReason": "관리자 수동 환불", // String
              "cancelHandleAmount": 4900, // Long
              "cancelTaxFreeAmount": 0 // Long
            },
            "timestamp": "..."
          }
          ```
    - **Error Responses:**
        - `P001`: 결제 정보를 찾을 수 없음.
        - `P004`: 이미 취소된 결제.
        - `P011`: 환불 불가능한 상태의 결제.
        - `C001`: 요청 바디 유효성 검증 실패.
        - `P010`: 토스 결제 취소 API 호출 실패.

## 8. 오류 처리 (Error Handling)

API 호출 실패 시, 공통 응답 구조의 `error` 필드를 통해 오류 코드와 메시지가 반환됩니다.

* **주요 오류 코드 (`ErrorCode.java` 기반, 예시 HTTP Status):**
    - **`C001` (400 Bad Request):** 잘못된 요청 (유효성 검증 실패, 필수 파라미터 누락 등)
    - **`C002` (401 Unauthorized):** 인증되지 않은 사용자 (유효한 JWT 없음, 토큰 만료 등)
    - **`C003` (403 Forbidden):** 접근 권한 없음 (요청된 리소스에 대한 역할 권한 부족)
    - **`C004` (404 Not Found):** 리소스를 찾을 수 없음 (요청한 ID의 엔티티 없음)
    - **`C005` (500 Internal Server Error):** 서버 내부 오류 (예상치 못한 서버 에러)
    - **`C006` (400 Bad Request):** 파일 업로드 용량 초과

    - **`A001` (409 Conflict):** 이미 가입된 이메일
    - **`A002` (401 Unauthorized):** 이메일 또는 비밀번호 불일치
    - **`A003` (401 Unauthorized):** 토큰 만료
    - **`A004` (401 Unauthorized):** 유효하지 않은 토큰
    - **`A005` (404 Not Found):** 사용자를 찾을 수 없음

    - **`N001` (404 Not Found):** 노트를 찾을 수 없음
    - **`N002` (400 Bad Request):** 허용되지 않은 노트 상태 (예: 게시된 노트 수정 시도)
    - **`N003` (403 Forbidden):** 노트 접근 권한 없음

    - **`NR001` (404 Not Found):** 리마인더 정보를 찾을 수 없음

    - **`CR001` (404 Not Found):** 작가 정보를 찾을 수 없음

    - **`M001` (404 Not Found):** 멤버십 정보를 찾을 수 없음
    - **`M002` (409 Conflict):** 이미 활성 멤버십 보유
    - **`M003` (403 Forbidden):** 정지된 멤버십
    - **`M004` (400 Bad Request):** 유효하지 않은 멤버십 상태
    - **`M005` (500 Internal Server Error):** 멤버십 활성화 실패
    - **`M006` (409 Conflict):** 취소된 멤버십 존재 (신규 결제 불가)
    - **`M007` (400 Bad Request):** 결제 실패 (내부 처리)

    - **`P001` (404 Not Found):** 결제 정보를 찾을 수 없음
    - **`P002` (400 Bad Request):** 결제 금액 불일치
    - **`P003` (500 Internal Server Error):** 결제 시스템 내부 처리 오류
    - **`P004` (400 Bad Request):** 이미 취소된 결제
    - **`P005` (500 Internal Server Error):** 환불 처리 중 오류
    - **`P006` (409 Conflict):** 이미 결제 대기 중인 멤버십
    - **`P007` (400 Bad Request):** 결제 승인 실패
    - **`P008` (409 Conflict):** 이미 처리된 결제
    - **`P009` (503 Service Unavailable):** 환불 기능 일시 비활성화
    - **`P010` (500 Internal Server Error):** 결제 취소 실패
    - **`P011` (400 Bad Request):** 환불 불가능한 결제 상태
    - **`P012` (400 Bad Request):** 환불 요청 기간 만료
    - **`P013` (409 Conflict):** 유료 콘텐츠 이미 이용
    - **`P014` (500 Internal Server Error):** Toss로부터 결제 정보 조회 실패