# Artbite API 사용 가이드

이 문서는 Artbite 백엔드 API를 사용하는 방법에 대한 기본적인 가이드를 제공합니다.

## 1. API 기본 URL

로컬 환경에서 애플리케이션이 실행 중인 경우, API의 기본 URL은 다음과 같습니다:

`http://localhost:8080`

## 2. 인증 (Authentication)

이 섹션에서는 Artbite API의 사용자 인증 및 권한 부여 메커니즘에 대해 설명합니다. JWT 기반의 일반 로그인과 OAuth2 소셜 로그인을 모두 지원하며, 토큰 관리 및 보안 관련 상세 내용을 다룹니다.

### 2.1. 회원가입 (Signup)

새로운 사용자를 등록합니다.

* **엔드포인트:** `POST /api/auth/signup`
* **권한:** `permitAll()`
* **요청 바디 (`SignupRequestDto`):**
  ```json
  {
    "email": "your_email@example.com",
    "password": "your_password",
    "username": "your_username"
  }
  ```
* **성공 응답 (`CustomApiResponse<Long>`):**
  ```json
  {
    "success": true,
    "data": 1, // 새로 생성된 사용자 ID
    "timestamp": "YYYY-MM-DDTHH:MM:SS.NNNNNNNNN"
  }
  ```

### 2.2. 로그인 (Login)

등록된 사용자로 로그인하여 `accessToken`을 발급받습니다. **Refresh Token은 HTTP-only 쿠키로 설정됩니다.**

* **엔드포인트:** `POST /api/auth/login`
* **권한:** `permitAll()`
* **요청 바디 (`LoginRequestDto`):**
  ```json
  {
    "email": "your_email@example.com",
    "password": "your_password"
  }
  ```
* **성공 응답 (JSON 바디 - `CustomApiResponse<TokenDto>`):**
  ```json
  {
    "success": true,
    "data": {
      "accessToken": "eyJhbGciOiJIUzI1NiI..."
    },
    "timestamp": "YYYY-MM-DDTHH:MM:SS.NNNNNNNNN"
  }
  ```
* **성공 응답 (응답 헤더):**
  ```
  Set-Cookie: refreshToken=a3f6747d-5d9f-4099-...; Max-Age=1209600; Path=/; Secure; HttpOnly
  ```

### 2.3. OAuth2 소셜 로그인 (OAuth2 Social Login)

사용자는 소셜 미디어 계정(카카오, 구글, 네이버)을 통해 로그인할 수 있습니다. 소셜 로그인 성공 시 Access Token은 URL 쿼리 파라미터로 전달되며, Refresh Token은 HTTP-only 쿠키로
설정됩니다.

* **시작 엔드포인트:**
    * 카카오: `GET http://localhost:8080/oauth2/authorization/kakao`
    * 구글: `GET http://localhost:8080/oauth2/authorization/google`
    * 네이버: `GET http://localhost:8080/oauth2/authorization/naver`
* **권한:** `permitAll()`
* **인증 흐름:**
    1. 사용자가 위 시작 엔드포인트 중 하나로 접근합니다.
    2. 백엔드는 해당 소셜 공급자의 인증 페이지로 사용자를 리다이렉트합니다.
    3. 사용자가 소셜 공급자에서 인증을 완료하면, 소셜 공급자는 백엔드의 콜백 URL(`http://localhost:8080/login/oauth2/code/{provider}`)로 리다이렉트합니다.
    4. 백엔드는 콜백을 처리하여 사용자 정보를 가져오고, `User` 및 `UserSocialLogin` 엔티티를 생성/업데이트하며, Access Token과 Refresh Token을 발급합니다. (
       `CustomOAuth2UserService`, `CustomOidcUserService`, `SocialAuthService` 참조)
    5. 백엔드는 최종적으로 프론트엔드의 `successRedirectUri`로 리다이렉트합니다. (`http://localhost:3000/oauth2/redirect` 또는 설정된 프론트엔드 URL)
* **성공 응답 (프론트엔드 리다이렉트 URL):**
  ```
  http://localhost:3000/oauth2/redirect?accessToken=eyJhbGciOiJIUzI1NiI...
  ```
* **성공 응답 (응답 헤더):**
  ```
  Set-Cookie: refreshToken=a3f6747d-5d9f-4099-...; Max-Age=1209600; Path=/; Secure; HttpOnly
  ```
* **토큰 처리:**
    * **Access Token**: 프론트엔드는 리다이렉트된 URL의 쿼리 파라미터에서 `accessToken`을 추출하여 로컬 스토리지 또는 애플리케이션 상태에 저장해야 합니다.
    * **Refresh Token**: `Set-Cookie` 헤더를 통해 HTTP-only 쿠키로 자동 설정됩니다.

### 2.4. 인증된 요청 (Authenticated Requests)

로그인 후 발급받은 `accessToken`을 사용하여 보호된 API 엔드포인트에 접근할 수 있습니다. `accessToken`은 모든 요청의 `Authorization` 헤더에 `Bearer` 접두사와 함께
포함되어야 합니다. Refresh Token은 브라우저가 자동으로 `Cookie` 헤더에 포함하여 전송합니다.

* **헤더 예시:**
  ```
  Authorization: Bearer eyJhbGciOiJIUzI1NiI...
  Cookie: refreshToken=a3f6747d-5d9f-4099-...
  ```

### 2.5. 토큰 재발급 (Token Reissue)

`accessToken`이 만료되었을 때, HTTP-only 쿠키에 저장된 `refreshToken`을 사용하여 새로운 `accessToken`과 `refreshToken`을 발급받을 수 있습니다. 이 과정에서 *
*Refresh Token Rotation**이 적용되어, 기존 `refreshToken`은 무효화되고 새로운 `refreshToken`이 발급됩니다. (`AuthService`,
`RefreshTokenService` 참조)

* **엔드포인트:** `POST /api/auth/reissue`
* **권한:** `isAuthenticated()`
* **요청 바디 (JSON):** (비어있는 DTO `TokenReissueRequestDto`를 사용하므로 바디는 비워둡니다.)
  ```json
  {}
  ```
* **요청 헤더:**
  ```
  Cookie: refreshToken=a3f6747d-5d9f-4099-...
  ```
* **성공 응답 (JSON 바디 - `CustomApiResponse<TokenDto>`):**
  ```json
  {
    "success": true,
    "data": {
      "accessToken": "eyJhbGciOiJIUzI1NiI..."
    },
    "timestamp": "YYYY-MM-DDTHH:MM:SS.NNNNNNNNN"
  }
  ```
* **성공 응답 (응답 헤더):** (새로운 Refresh Token이 Set-Cookie 헤더로 전달됩니다.)
  ```
  Set-Cookie: refreshToken=new_a3f6747d-5d9f-4099-...; Max-Age=1209600; Path=/; Secure; HttpOnly
  ```

### 2.6. 로그아웃 (Logout)

현재 로그인된 세션을 종료하고 Access Token과 Refresh Token을 무효화합니다. 카카오 소셜 로그인 사용자의 경우, 카카오 계정에서도 로그아웃할 수 있는 옵션을 제공하기 위해 카카오 로그아웃 페이지로
리다이렉트될 수 있습니다. (`AuthService`, `SocialAuthService` 참조)

* **엔드포인트:** `POST /api/auth/logout`
* **권한:** `isAuthenticated()`
* **요청 바디 (JSON):** (비어있는 DTO를 사용하므로 바디는 비워둡니다.)
  ```json
  {}
  ```
* **요청 헤더:**
  ```
  Authorization: Bearer eyJhbGciOiJIUzI1NiI...
  Cookie: refreshToken=a3f6747d-5d9f-4099-...
  ```
* **성공 응답 (JSON 바디 - `CustomApiResponse<String>`):**
    * **일반 로그아웃:**
      ```json
      {
        "success": true,
        "data": null,
        "timestamp": "YYYY-MM-DDTHH:MM:SS.NNNNNNNNN"
      }
      ```
    * **카카오 소셜 로그아웃 (리다이렉트 필요):**
      ```json
      {
        "success": true,
        "data": "https://kauth.kakao.com/oauth/logout?client_id=...&logout_redirect_uri=...", // 카카오 로그아웃 페이지 URL
        "timestamp": "YYYY-MM-DDTHH:MM:SS.NNNNNNNNN"
      }
      ```
* **성공 응답 (응답 헤더):** (Refresh Token 쿠키가 삭제됩니다.)
  ```
  Set-Cookie: refreshToken=; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:10 GMT; Path=/; Secure; HttpOnly
  ```
* **프론트엔드 처리:**
    * 응답 `data` 필드에 URL이 포함되어 있다면, 해당 URL로 브라우저를 리다이렉트하여 소셜 로그아웃 흐름을 완료해야 합니다.
    * `data` 필드가 `null`이라면, 일반 로그아웃이므로 프론트엔드에서 로컬 상태를 정리합니다.

### 2.7. 토큰 무효화 (Token Invalidation)

보안 강화를 위해, 사용자의 비밀번호 변경과 같은 중요한 보안 이벤트 발생 시 해당 사용자의 모든 기존 Refresh Token이 자동으로 무효화됩니다. 이 경우, 기존 Refresh Token을 사용한 재발급
요청은 실패하게 됩니다. 이는 탈취된 토큰의 위험을 최소화하기 위한 조치입니다. (`User` 엔티티의 `tokenVersion` 필드 참조)

## 3. 멤버십 API (Membership API)

이 섹션에서는 Artbite 서비스의 멤버십 구독 및 관리를 위한 API 엔드포인트를 설명합니다. 사용자의 멤버십 상태 조회, 취소, 재활성화 및 관리자용 멤버십 정지/해제 기능을 제공합니다.

### 3.1. 멤버십 상태 조회

현재 로그인한 사용자의 멤버십 상태를 조회합니다.

* **엔드포인트:** `GET /api/memberships/status`
* **권한:** `isAuthenticated()` (인증된 모든 사용자)
* **성공 응답 (`CustomApiResponse<MembershipStatusResponseDto>`):**
  ```json
  {
    "success": true,
    "data": {
      "status": "ACTIVE", // 멤버십 상태 (ACTIVE, CANCELED, EXPIRED, BANNED)
      "planType": "DEFAULT_MEMBER_PLAN", // 멤버십 플랜 유형
      "startDate": "2023-10-26T10:00:00",
      "endDate": "2023-11-26T10:00:00",
      "consecutiveMonths": 1, // 연속 구독 개월 수
      "autoRenew": true // 자동 갱신 여부
    },
    "timestamp": "YYYY-MM-DDTHH:MM:SS.NNNNNNNNN"
  }
  ```

### 3.2. 멤버십 취소

현재 로그인한 사용자의 활성 멤버십을 취소합니다.

* **엔드포인트:** `POST /api/memberships/cancel`
* **권한:** `hasRole('USER')`
* **요청 바디 (JSON):** (비어있는 DTO `CancelMembershipRequestDto`를 사용하므로 바디는 비워둡니다.)
  ```json
  {}
  ```
* **성공 응답 (`CustomApiResponse<Void>`):**
  ```json
  {
    "success": true,
    "data": null,
    "timestamp": "YYYY-MM-DDTHH:MM:SS.NNNNNNNNN"
  }
  ```

### 3.3. 취소된 멤버십 재활성화

현재 로그인한 사용자의 취소된 멤버십을 재활성화합니다.

* **엔드포인트:** `POST /api/memberships/reactivate-canceled`
* **권한:** `hasRole('USER')`
* **요청 바디 (JSON):** (비어있는 DTO `RenewMembershipRequestDto`를 사용하므로 바디는 비워둡니다.)
  ```json
  {}
  ```
* **성공 응답 (`CustomApiResponse<MembershipStatusResponseDto>`):**
  ```json
  {
    "success": true,
    "data": {
      "status": "ACTIVE",
      "planType": "DEFAULT_MEMBER_PLAN",
      "startDate": "2023-10-26T10:00:00",
      "endDate": "2023-11-26T10:00:00",
      "consecutiveMonths": 2,
      "autoRenew": true
    },
    "timestamp": "YYYY-MM-DDTHH:MM:SS.NNNNNNNNN"
  }
  ```

### 3.4. 멤버십 정지 (관리자용)

특정 사용자의 멤버십을 정지시킵니다.

* **엔드포인트:** `POST /api/memberships/{userId}/ban`
* **권한:** `hasRole('ADMIN')`
* **경로 변수:** `userId` (정지할 사용자의 ID)
* **성공 응답 (`CustomApiResponse<Void>`):**
  ```json
  {
    "success": true,
    "data": null,
    "timestamp": "YYYY-MM-DDTHH:MM:SS.NNNNNNNNN"
  }
  ```

### 3.5. 멤버십 정지 해제 (관리자용)

특정 사용자의 정지된 멤버십을 해제합니다. (해제 시 멤버십 상태는 `EXPIRED`로 변경됩니다.)

* **엔드포인트:** `POST /api/memberships/{userId}/unban`
* **권한:** `hasRole('ADMIN')`
* **경로 변수:** `userId` (정지 해제할 사용자의 ID)
* **성공 응답 (`CustomApiResponse<Void>`):**
  ```json
  {
    "success": true,
    "data": null,
    "timestamp": "YYYY-MM-DDTHH:MM:SS.NNNNNNNNN"
  }
  ```

## 4. 결제 API (Payment API - Toss Payments)

이 섹션에서는 토스페이먼츠를 통한 결제 처리에 관련된 API 엔드포인트를 설명합니다. 결제 요청부터 성공/실패 콜백 처리, 결제 내역 조회까지의 과정을 다룹니다.

### 4.1. 결제 요청 시작

클라이언트에서 결제창을 띄우기 전에 백엔드에 결제 정보를 요청합니다. 이 요청을 통해 `orderId`가 생성되고, 토스페이먼츠 결제창 호출에 필요한 정보가 반환됩니다.

* **엔드포인트:** `POST /api/payments/toss`
* **권한:** `isAuthenticated()`
* **요청 바디 (`PaymentDto`):**
  ```json
  {
    "payType": "CARD", // 결제 수단 (CARD, VIRTUAL_ACCOUNT, TRANSFER, MOBILE_PHONE)
    "amount": 4900, // 결제 금액
    "orderName": "Sparki 월간 구독", // 주문명
    "membershipPlanType": "DEFAULT_MEMBER_PLAN" // 멤버십 플랜 유형
  }
  ```
* **성공 응답 (`CustomApiResponse<PaymentResDto>`):**
  ```json
  {
    "success": true,
    "data": {
      "payType": "카드", // 결제 수단 설명
      "amount": 4900,
      "orderName": "Sparki 월간 구독",
      "orderId": "a1b2c3d4-e5f6-7890-1234-567890abcdef", // 백엔드에서 생성된 고유 주문 ID
      "customerEmail": "user@example.com",
      "customerName": "사용자이름",
      "successUrl": "http://localhost:8080/api/payments/toss/success", // 토스 결제 성공 시 리다이렉트될 백엔드 URL
      "failUrl": "http://localhost:8080/api/payments/toss/fail", // 토스 결제 실패 시 리다이렉트될 백엔드 URL
      "createdAt": "2023-10-26T10:00:00.123456789"
    },
    "timestamp": "YYYY-MM-DDTHH:MM:SS.NNNNNNNNN"
  }
  ```
* **클라이언트 처리 흐름:**
    1. 클라이언트는 이 응답을 받아 토스페이먼츠 SDK의 `tossPayments.requestPayment()` 메서드를 호출합니다.
    2. `successUrl`과 `failUrl`은 토스페이먼츠 결제창에서 결제 완료 후 백엔드로 리다이렉트될 URL입니다.

### 4.2. 결제 성공 콜백 처리

토스페이먼츠 결제창에서 결제가 성공적으로 완료되면, 토스페이먼츠는 백엔드의 `successUrl`로 리다이렉트합니다. 백엔드는 이 요청을 받아 결제를 최종 승인하고 멤버십을 활성화합니다.

* **엔드포인트:** `GET /api/payments/toss/success`
* **권한:** `permitAll()` (화이트리스트에 등록되어 있음)
* **요청 파라미터:**
    * `paymentKey`: 토스페이먼츠에서 발급한 결제 키
    * `orderId`: 백엔드에서 생성한 주문 ID
    * `amount`: 결제 금액
* **처리 흐름:**
    1. 백엔드는 `paymentKey`, `orderId`, `amount`를 사용하여 토스페이먼츠 `confirm` API를 호출하여 결제를 최종 승인합니다.
    2. DB에 결제 상태를 `SUCCESS`로 업데이트하고, 해당 사용자의 멤버십을 활성화합니다.
    3. 최종적으로 프론트엔드의 `frontendSuccessUrl`로 리다이렉트됩니다.
* **성공 응답:** 프론트엔드 `frontendSuccessUrl`로 리다이렉트 (예: `http://localhost:3000/payment/success`)

### 4.3. 결제 실패 콜백 처리

토스페이먼츠 결제창에서 결제가 실패하면, 토스페이먼츠는 백엔드의 `failUrl`로 리다이렉트합니다. 백엔드는 이 요청을 받아 실패 정보를 기록합니다.

* **엔드포인트:** `GET /api/payments/toss/fail`
* **권한:** `permitAll()` (화이트리스트에 등록되어 있음)
* **요청 파라미터:**
    * `code`: 토스페이먼츠 오류 코드
    * `message`: 토스페이먼츠 오류 메시지
    * `orderId`: 백엔드에서 생성한 주문 ID
* **처리 흐름:**
    1. 백엔드는 `orderId`에 해당하는 결제 정보를 찾아 상태를 `FAILED`로 업데이트하고 실패 사유를 기록합니다.
    2. 최종적으로 프론트엔드의 `frontendFailUrl`로 리다이렉트됩니다.
* **성공 응답:** 프론트엔드 `frontendFailUrl`로 리다이렉트 (예: `http://localhost:3000/payment/fail?code=...&message=...&orderId=...`)

### 4.4. 결제 취소

결제를 취소합니다. 현재는 환불 기능이 일시적으로 비활성화되어 있습니다.

* **엔드포인트:** `POST /api/payments/toss/cancel`
* **권한:** `isAuthenticated()`
* **요청 바디 (`PaymentCancelDto`):**
  ```json
  {
    "paymentKey": "토스페이먼츠 결제 키",
    "cancelReason": "취소 사유"
  }
  ```
* **성공 응답 (`CustomApiResponse<PaymentSuccessDto>`):**
  ```json
  {
    "success": true,
    "data": {
      // 취소된 결제 정보 (PaymentSuccessDto와 유사)
    },
    "timestamp": "YYYY-MM-DDTHH:MM:SS.NNNNNNNNN"
  }
  ```
* **현재 상태:** `REFUND_TEMPORARILY_DISABLED` 오류가 반환됩니다.

### 4.5. 결제 내역 조회

현재 로그인한 사용자의 결제 내역을 페이지네이션하여 조회합니다.

* **엔드포인트:** `GET /api/payments/toss/history`
* **권한:** `isAuthenticated()`
* **요청 파라미터:**
    * `page`: 페이지 번호 (0부터 시작)
    * `size`: 페이지당 항목 수
    * `sort`: 정렬 기준 (예: `createdAt,desc`)
* **성공 응답 (`CustomApiResponse<SliceResponseDto<PaymentHistoryDto>>`):**
  ```json
  {
    "success": true,
    "data": {
      "data": [
        {
          "paymentHistoryId": 1,
          "amount": 4900,
          "orderName": "Sparki 월간 구독",
          "status": "SUCCESS",
          "createdAt": "2023-10-26T10:00:00.123456789"
        }
      ],
      "sliceInfo": {
        "getNumber": 0, // 현재 페이지 번호
        "getSize": 10, // 페이지 크기
        "getNumberOfElements": 1, // 현재 페이지의 요소 수
        "hasNext": false, // 다음 페이지 존재 여부
        "hasPrevious": false // 이전 페이지 존재 여부
      }
    },
    "timestamp": "YYYY-MM-DDTHH:MM:SS.NNNNNNNNN"
  }
  ```

## 5. 설정 API (Config API)

이 섹션에서는 프론트엔드에서 백엔드의 특정 설정 정보를 조회하는 API 엔드포인트를 설명합니다. 주로 결제 모듈과 같은 외부 서비스 연동에 필요한 설정 값을 제공합니다.

### 5.1. 결제 설정 조회

토스페이먼츠 연동에 필요한 클라이언트 키, 주문명, 멤버십 금액 등 결제 관련 설정 정보를 조회합니다.

* **엔드포인트:** `GET /api/config/payment`
* **권한:** `permitAll()`
* **성공 응답 (`CustomApiResponse<PaymentConfigResponseDto>`):**
  ```json
  {
    "success": true,
    "data": {
      "clientKey": "토스페이먼츠 클라이언트 API 키",
      "orderName": "Sparki 월간 구독",
      "amount": 4900
    },
    "timestamp": "YYYY-MM-DDTHH:MM:SS.NNNNNNNNN"
  }
  ```

## 6. API 문서 (Swagger UI)

이 섹션에서는 Artbite 백엔드 API의 상세 스펙을 확인하고 테스트할 수 있는 Swagger UI 사용법을 안내합니다.

* **Swagger UI 접근:**
  `http://localhost:8080/swagger-ui/index.html`

### 6.1. Swagger UI에서 인증하기

보호된 엔드포인트를 테스트하려면 Swagger UI에서 인증 정보를 설정해야 합니다.

1. Swagger UI 페이지 우측 상단의 **`Authorize`** 버튼을 클릭합니다.
2. `bearerAuth` 섹션에 로그인 시 발급받은 `accessToken`을 `Bearer ` 접두사 없이 입력합니다. (예: `eyJhbGciOiJIUzI1NiI...`)
3. **`Authorize`** 버튼을 클릭한 후 팝업을 닫습니다.

이제 보호된 엔드포인트를 테스트할 수 있습니다.

## 7. 오류 처리 (Error Handling)

이 섹션에서는 Artbite API의 일관된 오류 응답 형식과 주요 오류 코드에 대해 설명합니다. 클라이언트는 이 정보를 바탕으로 백엔드에서 발생하는 다양한 오류 상황에 효과적으로 대응할 수 있습니다.

* **오류 응답 예시:**
  ```json
  {
    "success": false,
    "error": {
      "code": "A001", // ErrorCode.java에 정의된 코드
      "message": "이미 가입된 이메일입니다." // ErrorCode.java에 정의된 메시지 또는 커스텀 메시지
    },
    "timestamp": "YYYY-MM-DDTHH:MM:SS.NNNNNNNNN"
  }
  ```
* **주요 오류 코드 (예시):**
    * `COMMON_BAD_REQUEST` (400): 잘못된 요청 (유효성 검증 실패 등)
    * `COMMON_UNAUTHORIZED` (401): 인증되지 않은 사용자 (토큰 없음/만료/유효하지 않음)
    * `COMMON_FORBIDDEN` (403): 접근 권한 없음
    * `COMMON_NOT_FOUND` (404): 리소스를 찾을 수 없음
    * `COMMON_INTERNAL_SERVER_ERROR` (500): 서버 내부 오류
    * `AUTH_EMAIL_ALREADY_EXISTS` (409): 이미 가입된 이메일
    * `MEMBERSHIP_ALREADY_ACTIVE` (409): 이미 활성 멤버십 보유
    * `PAYMENT_AMOUNT_MISMATCH` (400): 결제 금액 불일치
    * `REFUND_TEMPORARILY_DISABLED` (503): 환불 기능 일시 비활성화

---