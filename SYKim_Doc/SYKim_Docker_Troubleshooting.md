# Docker 기반 실행 시행착오 정리 – SYKim

## 1. 4시간 동안 겪은 문제 원인 분석

| 증상 | 근본 원인 | 해결 방법 |
| --- | --- | --- |
| `./gradlew bootRun` 시 `PlaceholderResolutionException: Could not resolve placeholder 'JWT_SECRET_KEY'` | 로컬 IDE/터미널에서 `.env` 파일을 자동으로 읽지 않음. 환경 변수 `JWT_SECRET_KEY`, `JWT_ACCESS_TOKEN_EXPIRE_TIME`, `JWT_REFRESH_TOKEN_EXPIRE_TIME` 등이 설정되지 않아 `JwtProvider` 빈 생성 실패 | 실행 전 셸에서 `export` 하거나 IntelliJ Run Configuration 환경 변수에 값 입력. `SPRING_PROFILES_ACTIVE=local` 로 실행해 기본 시크릿 사용 가능 |
| Docker 컨테이너 `artbite-app` 이 `unhealthy` → `Restarting` 반복 | ① 이미지에 `curl` 미설치라 healthcheck 명령 실패 ② `SPRING_DATASOURCE_URL`, `SPRING_DATA_REDIS_HOST` 가 `localhost` 로 덮어써져 DB/Redis 접속 실패 | ① Dockerfile 에 `curl` 설치 or healthcheck 를 다른 엔드포인트로 조정 ② 로컬에 export 했던 값을 `unset` 하고 다시 `docker compose up -d` 실행 |
| `http://localhost:8080/login-test-page` 접속 불가 | `artbite-app` 컨테이너가 DB 연결 실패로 재기동. 8080 포트가 열리지 않음 | `.env` 환경 변수(`artbite-postgres`, `artbite-redis`)를 사용하도록 초기화 후 컨테이너 재기동 |

### 핵심 교훈
- `.env` 는 Docker Compose 에서만 자동 로드된다. 로컬 실행 시에는 반드시 환경 변수를 수동으로 주입해야 한다.
- 로컬 개발을 위해 export 한 값이 Docker 컨테이너에도 전달되면, 컨테이너 내부에서 `localhost` 를 바라보게 되어 통신이 끊긴다.
- 헬스체크는 컨테이너 내부에서 실행되므로 필요한 도구(`curl` 등)가 이미지에 포함되어야 한다.

## 2. Docker 를 통해 웹 엔드포인트에 접근한다는 의미

1. `docker-compose` 로 `app`, `postgres`, `redis` 컨테이너를 동시에 기동한다.  
   - `.env` 값이 각 컨테이너 환경 변수로 주입되어, 애플리케이션이 내부 네트워크(`artbite-postgres`, `artbite-redis`)를 통해 의존 서비스에 연결한다.
2. `app` 컨테이너가 기동되면 내부에서 `java -jar app.jar` 가 실행되어 Spring Boot 애플리케이션이 8080 포트를 리슨한다.
3. `docker-compose.yml` 의 `ports: "8080:8080"` 설정 덕분에, 호스트 PC 의 `localhost:8080` 이 컨테이너의 8080 포트와 바인딩된다.
4. 브라우저에서 `http://localhost:8080/login-test-page` 를 호출하면, 요청은 호스트 → Docker 브리지 네트워크 → `app` 컨테이너로 전달되고, 컨테이너가 응답을 반환한다.

### 왜 접속이 가능한가?
- Docker 가 애플리케이션과 의존 서비스(DB, Redis)를 모두 가상화된 네트워크에 띄우고, 포트 바인딩으로 호스트 ↔ 컨테이너 간 트래픽을 연결해주기 때문이다.
- 애플리케이션 내부 로직은 컨테이너 안에서도 그대로 실행되며, 외부에서 접근하기 위한 최소 조건은 **컨테이너가 정상 실행**되고 **포트가 노출**되어 있는 것이다.
- `.env` 값을 올바르게 주입하면 컨테이너는 의존 서비스에 접근 가능하고, Spring Boot 가 정상적으로 부팅되어 웹 요청을 처리한다.

## 3. 실행 순서 체크리스트

1. 로컬에서 export 했던 값이 있으면 `unset` 처리 (`SPRING_DATASOURCE_URL`, `SPRING_DATA_REDIS_HOST`, JWT 관련 변수 등).
2. `docker compose -f docker-compose.yml -f infra/database/docker-compose.yml up -d` 실행.
3. `docker compose ... ps` 로 `artbite-app` 이 `Up ... (healthy)` 인지 확인.
4. 브라우저에서 `http://localhost:8080/login-test-page` 접속하여 로그인/토큰 플로우 테스트.
5. 문제 발생 시 `docker logs artbite-app` 로 오류 메시지 확인.
