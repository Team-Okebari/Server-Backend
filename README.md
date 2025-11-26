# 디자이너를 위한 영감 및 레퍼런스 관리 플랫폼 Sparki (Backend)

|||
|----------|----------|
| Sparki Frontend | [Sparki Frontend 바로가기](https://github.com/Team-Okebari/Frontend) |
| Demo (YouTube) | [Demo ▶️ Youtube 바로가기](https://youtube.com/shorts/tryKJCuCI2Y) |
| 배포 링크 | [배포 링크 🌐 바로가기](https://www.sparki-today.com/) |

> 디자이너의 인사이트를 위한 영감 및 레퍼런스 관리 플랫폼

**디자이너를 위한 영감 및 레퍼런스 관리 플랫폼 'Sparki'의 백엔드** 레포지토리입니다. </br>
Spring Boot, JPA, 다양한 클라우드 서비스를 기반으로 사용자 인증, 콘텐츠 관리, 구독 및 결제 API를 제공합니다.

<img width="1920" height="1080" alt="1" src="https://github.com/user-attachments/assets/60f75e58-1df1-44ce-9a81-4b3c909a53df" />
<img width="1920" height="1080" alt="11" src="https://github.com/user-attachments/assets/1c2ecb58-7e70-4721-bf20-0025aeaae75e" />
<img width="1920" height="1080" alt="12" src="https://github.com/user-attachments/assets/243cdfe6-71ac-44b6-bde5-b7c787002b14" />
<img width="1920" height="1080" alt="13" src="https://github.com/user-attachments/assets/b0d53d89-b761-4bc1-a3cd-fd93760fad2b" />
<img width="1920" height="1080" alt="14" src="https://github.com/user-attachments/assets/0a75766b-8bdd-4b55-a3d6-318109543b6a" />
<img width="1920" height="1080" alt="15" src="https://github.com/user-attachments/assets/41d2e7cd-9826-4416-a2ef-49640fcdd79f" />

</br>
</br>

## 1. 프로젝트 개요

이 레포지토리는 디자이너의 창작 활동을 지원하기 위한 영감 및 레퍼런스 관리 플랫폼의 백엔드 시스템으로, 다음과 같은 핵심 기능을 제공합니다.

## 팀원 소개 (Backend Roles)

| 이름 | 담당 역할 |
|------|-----------|
| 임은택 | 인증/회원관리 (JWT / OAuth2) 구현, Toss Payments 결제 및 멤버십 구현, 인프라 (AWS / CI / CD) 구축 및 운영, QA 및 성능 개선 |
| 김소연 | REST API 서버, RDBMS (Postgres) 설계 및 구현, Note 로직 (CRUD / 북마크 / 리마인더) 구현, QA 및 전체 모듈 디버깅 |

## 주요 기능/기술 목표

| **카테고리** | **핵심 기능** | **구현 기술 및 목표** |
|--------------|----------------|-------------------------|
| **사용자 인증 및 권한** | - 일반 회원가입/로그인<br>- Google/Kakao/Naver OAuth2 소셜 로그인<br>- Refresh Token Rotation 적용 안전한 JWT 인증<br>- 역할 기반 접근 제어 (`@PreAuthorize`) | - Spring Security + JWT 기반 인증 구조<br>- OAuth2 Client 연동 소셜 로그인<br>- BCrypt 비밀번호 암호화<br>- Refresh Token 관리 전략 |
| **멤버십 관리 / 결제** | - 사용자 멤버십 구독 상태 관리 (활성화, 취소, 재활성화, 정지)<br>- 스마트 취소 (7일 이내, 콘텐츠 미사용 환불)<br>- 스케줄러 기반 멤버십 만료/갱신 처리<br>- Toss Payments 결제 연동 (정기 결제는 MVP에서 제외) | - Toss Payments API 연동 (결제 요청/승인/실패/환불)<br>- 구독 상태 기반 Access Control<br>- 자동 갱신 처리 및 스케줄러 구현 |
| **작가(Creator) 관리** | - 관리자 전용 작가 정보 CRUD (이름, 약력, 직무, 소셜 링크) | - 관리자 전용 Backoffice 구현<br>- 권한 기반 접근 제어 |
| **노트 콘텐츠 관리** | - 노트 CRUD (메타데이터, 커버, 개요, 제작 과정, 회고 등 모듈화)<br>- 질문/답변 CRUD<br>- 북마크 토글 및 목록 조회<br>- 리마인더 (상태 머신 기반, 개인화, 캐시 활용)<br>- 구독 상태에 따른 미리보기/전체 콘텐츠 제공 | - 노트 도메인 설계 및 권한 기반 접근 제어<br>- Q&A/북마크/리마인더 도메인 및 API 개발<br>- Personalized Content 제공 로직 구현 |
| **파일 스토리지** | - 이미지 업로드/조회/삭제 | - AWS S3 업로드/서명 URL/삭제 기능 구현<br>- 이미지 리사이즈/검증 (MVP 제외) |
| **로깅 및 모니터링** | - 중앙화된 로그 수집/조회 | - 개발 환경: ELK 스택<br>- 운영 환경: AWS OpenSearch 기반 로그 분석 |


<img width="1920" height="1080" alt="30" src="https://github.com/user-attachments/assets/4d711107-0339-4c02-96d0-577405f2ff76" />

</br>
</br>

## 기술 스택 (Technical Stack)

| **구분** | **기술 스택** | **설명** |
|----------|---------------|-----------|
| **Backend** | Java 21, Spring Boot 3.5.6, Spring Security(OAuth2, JWT), Spring Data JPA(Hibernate), Spring Cloud AWS(S3), Spring Cache, Spring Actuator, Flyway | 인증/인가, ORM, 파일 스토리지 연동, 캐싱, 모니터링 등 핵심 백엔드 기능 구성 |
| **Storage & Cache** | PostgreSQL(AWS RDS), Redis(ElastiCache, SSL), Caffeine | 영속 데이터 저장, 세션/토큰 및 캐시 관리, 로컬 고성능 캐시 제공 |
| **External Integrations** | AWS S3, Toss Payments API, Google/Kakao/Naver OAuth2 API | 파일 저장, 결제/정기 결제 처리, 소셜 로그인 연동 |
| **Infra & Deployment** | Docker, Docker Compose, GitHub Actions(CI/CD), EC2 Self-Hosted Runner | 일관된 컨테이너 환경 구성 및 자동화된 배포 파이프라인 구축 |
| **Logging & Monitoring** | ELK Stack, Fluent Bit + AWS OpenSearch | 개발·운영 환경에 따른 중앙화 로깅 및 모니터링 |
| **Dev & Test Tools** | Gradle, Lombok, Jackson, Testcontainers, JUnit5, Mockito, Checkstyle(Naver), SpringDoc(Swagger UI) | 개발 생산성 향상, 테스트 자동화, API 문서화 지원 |

   
<img width="1920" height="1080" alt="29" src="https://github.com/user-attachments/assets/594e2d14-d2d5-4505-b4f8-3684f60a4848" />

</br>
</br>

## 프로젝트 구조

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

## API 엔드포인트

(주요 엔드포인트 목록이며, 전체 목록은 `http://{}/swagger-ui/index.html` 에서 확인 가능)

### 클라이언트 API

| **Resource** | **Method** | **URI (엔드포인트)** | **설명** | **권한** |
|--------------|------------|---------------------|----------|----------------|
| 인증 | POST | /api/auth/signup | 회원가입 | 공개 |
| | POST | /api/auth/login | 로그인 (Access Token 발급) | 공개 |
| | POST | /api/auth/reissue | Access Token 재발급 | 로그인 필요 |
| | POST | /api/auth/logout | 로그아웃 | 로그인 필요 |
| 노트 | GET | /api/notes/published/today-cover | 금일 노트 커버 조회 | 공개 |
| | GET | /api/notes/published/today-preview | 금일 노트 미리보기 | 로그인 필요 |
| | GET | /api/notes/published/today-detail | 금일 노트 상세 조회 | 멤버십 구독자 |
| | GET | /api/notes/archived | 지난 노트 목록 조회 | 로그인 필요 |
| | GET | /api/notes/archived/{noteId} | 지난 노트 상세 조회 | 멤버십 구독자 |
| 북마크 | POST | /api/notes/{noteId}/bookmark | 노트 북마크 토글 | 로그인 필요 |
| | GET | /api/notes/bookmarks | 내 북마크 목록 조회 | 로그인 필요 |
| 질문/답변 | POST | /api/notes/questions/{questionId}/answer | 질문에 답변하기 | 로그인 필요 |
| 멤버십 | GET | /api/memberships/status | 내 멤버십 상태 조회 | 로그인 필요 |
| | POST | /api/memberships/cancel | 멤버십 취소 (환불 또는 구독 해지) | 로그인 필요 |

---

### 관리자 API

| **Resource** | **Method** | **URI (엔드포인트)** | **설명** | **권한** |
|--------------|------------|---------------------|----------|----------------|
| 작가 관리 | POST | /api/admin/creators | 작가 생성 | 관리자 |
| | GET | /api/admin/creators/{creatorId} | 작가 조회 | 관리자 |
| | PUT | /api/admin/creators/{creatorId} | 작가 수정 | 관리자 |
| | DELETE | /api/admin/creators/{creatorId} | 작가 삭제 | 관리자 |
| 노트 관리 | POST | /api/admin/notes | 노트 생성 | 관리자 |
| | GET | /api/admin/notes/{noteId} | 노트 조회 | 관리자 |
| | PUT | /api/admin/notes/{noteId} | 노트 수정 | 관리자 |
| | DELETE | /api/admin/notes/{noteId} | 노트 삭제 | 관리자 |
| 멤버십 관리 | PUT | /api/admin/membership-inducement-image | 멤버십 유도 이미지 변경 | 관리자 |
| | POST | /api/memberships/{userId}/ban | 특정 사용자 멤버십 정지 | 관리자 |


</br>

## 시작하기

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

## 테스트 전략

Artbite 백엔드는 견고한 테스트 전략을 통해 코드 품질과 안정성을 확보합니다.

- **단위 테스트 (Unit Test)**: Mockito를 활용하여 각 서비스 계층의 비즈니스 로직을 격리하여 테스트합니다. (예: `AuthServiceTest`, `MembershipServiceTest`)
- **통합 테스트 (Integration Test)**: Testcontainers를 사용하여 실제 PostgreSQL 및 Redis 컨테이너를 기반으로 통합 테스트를 수행합니다. 이를 통해 데이터베이스 및 캐시
  연동을 포함한 실제 환경과 유사한 조건에서 컴포넌트 간의 상호 작용을 검증합니다. (예: `AuthIntegrationTest`, `NoteRedisIntegrationTest`)
- **개발자 도구**: 개발 편의성을 위해 HTML 기반의 테스트 페이지들을 제공하여 주요 API 엔드포인트(인증, 작가 관리, 노트 관리 등)를 웹 인터페이스에서 쉽게 수동으로 테스트하고 디버깅할 수 있도록
  합니다. (예: `/login-test-page`, `/note-admin-test-page`)

</br>

## UT

<img width="1920" height="1080" alt="21" src="https://github.com/user-attachments/assets/c7b056e1-da0d-42b2-bf28-c2a1f2f7e278" />
<img width="1920" height="1080" alt="22" src="https://github.com/user-attachments/assets/f34b2072-d8c9-4f27-b486-8ae6ddcc6ee9" />
<img width="1920" height="1080" alt="23" src="https://github.com/user-attachments/assets/a37ceff2-fb56-4e83-8ce7-63342075fa06" />
<img width="1920" height="1080" alt="24" src="https://github.com/user-attachments/assets/a3772eb7-b09f-4027-9cfb-e5ba8786a4d6" />

</br>
</br>

## 회고

<img width="1920" height="1080" alt="31" src="https://github.com/user-attachments/assets/e8edd416-e4a6-4994-b7fe-ab87ebf32170" />
<img width="1920" height="1080" alt="32" src="https://github.com/user-attachments/assets/5a0492d4-fd4e-485c-84db-e5c8f063127a" />
