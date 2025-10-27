# Artbite Backend

## 프로젝트 개요

Artbite는 디자이너를 위한 영감 및 레퍼런스 관리 플랫폼의 백엔드 서비스입니다. 사용자 인증, 레퍼런스 저장 및 관리, 검색, 추천 등 다양한 기능을 제공하여 디자이너의 작업 효율성을 높이고 창의적인 활동을 지원합니다.

## 기술 스택

### 백엔드
-   **언어**: Java 21
-   **프레임워크**: Spring Boot 3.5.6
-   **데이터베이스**: PostgreSQL (운영), H2 Database (개발/테스트)
-   **캐시/메시지 브로커**: Redis
-   **인증/권한**: Spring Security, JWT (JSON Web Token)
-   **ORM**: Spring Data JPA, Hibernate
-   **빌드 도구**: Gradle
-   **테스트**: JUnit 5, Mockito, Testcontainers
-   **유틸리티**: Lombok

### 개발 환경
-   **컨테이너**: Docker, Docker Compose

## 로컬 개발 환경 설정

### 1. 필수 도구 설치
-   **Java Development Kit (JDK)**: 21 버전
-   **Gradle**: 8.x 버전 (Gradle Wrapper 사용 권장)
-   **Docker Desktop**: Docker Engine 및 Docker Compose 포함

### 2. 프로젝트 클론
```bash
git clone [프로젝트-레포지토리-URL]
cd artbite
```

### 3. `.env` 파일 설정
프로젝트 루트에 `.env-example` 파일을 참고하여 `.env` 파일을 생성하고, 필요한 환경 변수들을 설정합니다.

```bash
cp .env-example .env
# nano .env 또는 VS Code 등으로 .env 파일 열어서 값 설정
```

**주요 환경 변수:**
-   `CORS_URL`: 프론트엔드 애플리케이션의 URL (예: `http://localhost:3000,https://www.artbite.com`)
-   `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB_URL`: PostgreSQL 연결 정보
-   `JWT_SECRET_KEY`: JWT 토큰 서명에 사용될 Base64 인코딩된 비밀 키 (예: `openssl rand -base64 32`로 생성)

### 4. Docker 환경 시작 (PostgreSQL, Redis, Spring Boot 애플리케이션)
PostgreSQL, Redis 및 Spring Boot 애플리케이션 컨테이너를 시작합니다.
```bash
./infra/scripts/start.sh
```

### 5. 애플리케이션 실행 (선택 사항: Docker 없이 로컬에서 실행)
Docker 컨테이너를 사용하지 않고 로컬에서 Spring Boot 애플리케이션만 실행하려면 다음 명령어를 사용합니다.
```bash
./gradlew bootRun
```

### 6. Docker 리소스 정리
불필요한 Docker 빌드 캐시 및 사용하지 않는 빌더 데이터를 정리하여 디스크 공간을 확보합니다.
```bash
docker builder prune -f
```

## Docker 환경 설정 및 실행

### 1. Docker Compose 파일
-   **`infra/database/docker-compose.yml`**: PostgreSQL 및 Redis 서비스를 정의합니다.
-   **`docker-compose.yml` (루트)**: Spring Boot 애플리케이션 서비스를 정의하고, `infra/database`의 서비스들과 연동합니다.
-   **`infra/elk/docker-compose.yml`**: Elasticsearch, Logstash, Kibana 및 Filebeat 서비스를 정의합니다.

### 2. Docker 환경 시작
모든 인프라 서비스(DB, Redis, ELK)와 Spring Boot 애플리케이션을 시작합니다.
```bash
./infra/scripts/start.sh
```

### 3. Docker 환경 중지
모든 인프라 서비스와 Spring Boot 애플리케이션을 중지합니다.
```bash
./infra/scripts/stop.sh
```