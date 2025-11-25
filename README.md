# Sparki Backend

- [Sparki Frontend 바로가기](https://github.com/Team-Okebari/Frontend)

> **디자이너를 위한 영감 및 레퍼런스 관리 플랫폼 'Sparki'의 백엔드** 레포지토리입니다. </br>
> Spring Boot, JPA, 다양한 클라우드 서비스를 기반으로 사용자 인증, 콘텐츠 관리, 구독 및 결제 API를 제공합니다.
<img width="1920" height="1080" alt="1" src="https://github.com/user-attachments/assets/60f75e58-1df1-44ce-9a81-4b3c909a53df" />
<img width="1920" height="1080" alt="29" src="https://github.com/user-attachments/assets/594e2d14-d2d5-4505-b4f8-3684f60a4848" />
<img width="1920" height="1080" alt="27" src="https://github.com/user-attachments/assets/08778bcd-c552-48d8-b616-83dfffd2c3ce" />
<img width="1920" height="1080" alt="30" src="https://github.com/user-attachments/assets/4d711107-0339-4c02-96d0-577405f2ff76" />
<img width="1920" height="1080" alt="11" src="https://github.com/user-attachments/assets/1c2ecb58-7e70-4721-bf20-0025aeaae75e" />
<img width="1920" height="1080" alt="12" src="https://github.com/user-attachments/assets/243cdfe6-71ac-44b6-bde5-b7c787002b14" />
<img width="1920" height="1080" alt="13" src="https://github.com/user-attachments/assets/b0d53d89-b761-4bc1-a3cd-fd93760fad2b" />
<img width="1920" height="1080" alt="14" src="https://github.com/user-attachments/assets/0a75766b-8bdd-4b55-a3d6-318109543b6a" />
<img width="1920" height="1080" alt="15" src="https://github.com/user-attachments/assets/41d2e7cd-9826-4416-a2ef-49640fcdd79f" />
<img width="1920" height="1080" alt="21" src="https://github.com/user-attachments/assets/c7b056e1-da0d-42b2-bf28-c2a1f2f7e278" />
<img width="1920" height="1080" alt="22" src="https://github.com/user-attachments/assets/f34b2072-d8c9-4f27-b486-8ae6ddcc6ee9" />
<img width="1920" height="1080" alt="23" src="https://github.com/user-attachments/assets/a37ceff2-fb56-4e83-8ce7-63342075fa06" />
<img width="1920" height="1080" alt="24" src="https://github.com/user-attachments/assets/a3772eb7-b09f-4027-9cfb-e5ba8786a4d6" />
<img width="1920" height="1080" alt="31" src="https://github.com/user-attachments/assets/e8edd416-e4a6-4994-b7fe-ab87ebf32170" />
<img width="1920" height="1080" alt="32" src="https://github.com/user-attachments/assets/5a0492d4-fd4e-485c-84db-e5c8f063127a" />

</br>

## 1. 프로젝트 개요

이 레포지토리는 디자이너의 창작 활동을 지원하기 위한 영감 및 레퍼런스 관리 플랫폼의 백엔드 시스템으로, 다음과 같은 핵심 기능을 제공합니다:

- **사용자 인증 및 권한**: 로컬 회원가입, JWT 토큰 기반 인증, Google/Kakao/Naver OAuth2 소셜 로그인 연동.
- **콘텐츠 관리 (노트)**: 관리자 전용 어드민 페이지를 통한 노트 콘텐츠 생성, 조회, 수정, 삭제(CRUD) 기능.
- **구독 및 결제**: Toss Payments를 연동한 멤버십 구독 모델, 자동 갱신 및 '스마트 취소'(환불 정책 적용) 기능.
- **동적 콘텐츠 제공**: 사용자의 구독 상태에 따라 노트 콘텐츠를 전체 또는 미리보기로 차등 제공.
- **사용자 참여 기능**: 노트에 대한 질문/답변, 북마크, 개인화된 리마인더(알림) 기능.
- **파일 스토리지**: AWS S3를 활용한 이미지 파일 관리.
- **로깅 및 모니터링**: ELK 스택(개발) 및 AWS OpenSearch(운영)를 활용한 중앙화된 로깅 시스템.

</br>

## 2. 기술 스택

### **Backend**

- Java 21
- Spring Boot 3.5.6
    - Spring Security (OAuth 2.0, JWT)
    - Spring Data JPA (Hibernate)
    - Spring Cloud AWS (S3 연동)
    - Spring Cache
    - Spring Actuator
- JPA / Hibernate
- Flyway (DB Migration)

### **Storage & Cache**

- PostgreSQL (AWS RDS - 운영, Docker - 개발)
- Redis (AWS ElastiCache, SSL - 운영, Docker - 개발)
- Caffeine

### **External Integrations**

- AWS S3 (이미지 등 파일 저장)
- Toss Payments API
- Google/Kakao/Naver OAuth2 API

### **Infrastructure / Deployment**

- Docker & Docker Compose
- GitHub Actions (CI/CD)
- Self-Hosted Runner on EC2 (CD)

### **Logging & Monitoring**

- ELK Stack (Elasticsearch, Logstash, Kibana, Filebeat) - 개발
- Fluent Bit + AWS OpenSearch - 운영

### **Development & Testing Tools**

- Gradle
- Lombok
- Jackson
- Testcontainers
- JUnit 5 & Mockito
- Checkstyle (Naver)
- SpringDoc (Swagger UI)

</br>

## 3. 프로젝트 구조

```
artbite-Backend/
├── src/main/java/com/okebari/artbite/
│   ├── auth/         # 인증 및 권한 (JWT, OAuth2)
│   ├── creator/      # 작가 관리 (Admin)
│   ├── membership/   # 멤버십 및 구독 관리
│   ├── note/         # 노트 콘텐츠 관리 (핵심 도메인)
│   ├── payment/      # 결제 (Toss Payments 연동)
│   ├── s3/           # 이미지 업로드
│   ├── tracking/     # 유료 컨텐츠 이용 여부 트래킹
│   ├── common/       # 공통 모듈 (예외 처리, 응답 DTO 등)
│   └── config/       # 애플리케이션 전역 설정
├── src/main/resources/
│   ├── application.yml         # 메인 설정
│   ├── application-dev.yml     # 개발 환경 설정
│   ├── application-prod.yml    # 운영 환경 설정
│   └── db/migration/           # Flyway DB 마이그레이션 스크립트
├── infra/                      # 인프라 설정 (IaC)
│   ├── database/               # 데이터베이스 (PostgreSQL, Redis)
│   ├── elk/                    # ELK 스택 (Elasticsearch, Logstash, Kibana)
│   ├── fluent-bit/             # Fluent Bit (운영용 로그 포워더)
│   └── scripts/                # 실행/중지 스크립트
├── logs/                       # 로그 파일
├── build.gradle                # Gradle 빌드 설정
└── Dockerfile                  # 애플리케이션 Dockerfile
```

</br>

## 4. 주요 기능

- **사용자 인증 및 권한**
  - 일반 회원가입/로그인
  - Google/Kakao/Naver OAuth2 소셜 로그인
  - Refresh Token Rotation을 적용한 안전한 JWT 인증
  - 역할 기반 접근 제어 (`@PreAuthorize`)
- **멤버십 관리**
  - 사용자 멤버십 구독 상태 관리 (활성화, 취소, 재활성화, 금지)
  - 환불 정책 (7일 이내, 콘텐츠 미사용)이 적용된 '스마트 취소'
  - 스케줄러를 통한 멤버십 만료/갱신 처리
- **작가(Creator) 관리**
  - 관리자 전용 작가 정보 (이름, 약력, 직무, 소셜 링크) CRUD 기능
- **노트 콘텐츠 관리**
    - **노트 CRUD**: 관리자 전용 노트 (메타데이터, 커버, 개요, 제작 과정, 회고 등 모듈화된 구조) 생성/조회/수정/삭제
    - **질문 및 답변**: 노트별 질문에 대한 사용자 답변 생성/수정/삭제/조회
    - **북마크**: 사용자별 노트 북마크 토글 및 목록 조회 (키워드 검색 지원)
    - **리마인더**: 상태 머신 기반의 개인화된 일일 노트 리마인더 (캐시 활용)
    - **동적 콘텐츠 제공**: 사용자의 구독 상태에 따라 노트 콘텐츠를 전체 또는 미리보기로 차등 제공
- **결제 시스템**
  - Toss Payments 연동을 통한 결제 요청/승인/실패 처리, 환불, 스케줄러를 통한 미완료 결제 처리
- **파일 스토리지**
  - AWS S3를 활용한 이미지 등 파일 저장 및 관리

</br>

## 5. API 엔드포인트

(주요 엔드포인트 목록이며, 전체 목록은 `http://{}/swagger-ui/index.html` 에서 확인 가능)

### 클라이언트 API (`/api`)

- `POST /api/auth/signup`: 회원가입
- `POST /api/auth/login`: 로그인 (Access Token 발급)
- `POST /api/auth/reissue`: Access Token 재발급
- `POST /api/auth/logout`: 로그아웃
- `GET /api/notes/published/today-cover`: 금일 노트 커버 조회 (공개)
- `GET /api/notes/published/today-preview`: 금일 노트 미리보기 (로그인 사용자)
- `GET /api/notes/published/today-detail`: 금일 노트 상세 조회 (멤버십 구독자)
- `GET /api/notes/archived`: 지난 노트 목록 조회 (로그인 사용자)
- `GET /api/notes/archived/{noteId}`: 지난 노트 상세 조회 (멤버십 구독자)
- `POST /api/notes/{noteId}/bookmark`: 노트 북마크 토글
- `GET /api/notes/bookmarks`: 내 북마크 목록 조회
- `POST /api/notes/questions/{questionId}/answer`: 질문에 답변하기
- `GET /api/memberships/status`: 내 멤버십 상태 조회
- `POST /api/memberships/cancel`: 멤버십 취소 (환불 또는 구독 해지)

### 관리자 API (`/api/admin`)

- `POST /api/admin/creators`: 작가 생성
- `GET, PUT, DELETE /api/admin/creators/{creatorId}`: 작가 조회, 수정, 삭제
- `POST /api/admin/notes`: 노트 생성
- `GET, PUT, DELETE /api/admin/notes/{noteId}`: 노트 조회, 수정, 삭제
- `PUT /api/admin/membership-inducement-image`: 멤버십 유도 이미지 변경
- `POST /api/memberships/{userId}/ban`: 특정 사용자 멤버십 정지

</br>

## 6. 시작하기

### 1. 필수 도구 설치

- **Java Development Kit (JDK)**: 21 버전
- **Gradle**: 8.x 버전 (Gradle Wrapper 사용 권장)
- **Docker Desktop**: Docker Engine 및 Docker Compose 포함

### 2. 프로젝트 클론

```bash
git clone https://github.com/okebari/artbite-Backend.git
cd artbite-Backend
```

### 3. `.env` 파일 설정

프로젝트 루트에 `.env-example` 파일을 참고하여 `.env` 파일을 생성하고, 필요한 환경 변수들을 설정합니다.

```bash
cp .env-example .env
# nano .env 또는 VS Code 등으로 .env 파일 열어서 값 설정
```

### 4. Docker 환경 시작 (DB, Redis, ELK, App)

PostgreSQL, Redis, ELK Stack 및 Spring Boot 애플리케이션 컨테이너를 한 번에 시작합니다.

```bash
./infra/scripts/start.sh local
```

### 5. Docker 환경 중지

```bash
# 모든 서비스 중지 및 네트워크 정리
./infra/scripts/stop.sh local
```

### 6. 테스트 페이지 접속

애플리케이션이 실행되면, 개발 편의성을 위한 테스트 페이지에 접속하여 주요 기능을 확인하고 API를 테스트할 수 있습니다.

- **인증 테스트 페이지**: `http://{}/login-test-page`
- **Swagger UI (API 문서)**: `http://{}/swagger-ui/index.html`

</br>

## 7. 테스트 전략

Artbite 백엔드는 견고한 테스트 전략을 통해 코드 품질과 안정성을 확보합니다.

- **단위 테스트 (Unit Test)**: Mockito를 활용하여 각 서비스 계층의 비즈니스 로직을 격리하여 테스트합니다. (예: `AuthServiceTest`, `MembershipServiceTest`)
- **통합 테스트 (Integration Test)**: Testcontainers를 사용하여 실제 PostgreSQL 및 Redis 컨테이너를 기반으로 통합 테스트를 수행합니다. 이를 통해 데이터베이스 및 캐시
  연동을 포함한 실제 환경과 유사한 조건에서 컴포넌트 간의 상호 작용을 검증합니다. (예: `AuthIntegrationTest`, `NoteRedisIntegrationTest`)
- **개발자 도구**: 개발 편의성을 위해 HTML 기반의 테스트 페이지들을 제공하여 주요 API 엔드포인트(인증, 작가 관리, 노트 관리 등)를 웹 인터페이스에서 쉽게 수동으로 테스트하고 디버깅할 수 있도록
  합니다. (예: `/login-test-page`, `/note-admin-test-page`)
