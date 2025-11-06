# Docker 이미지 빌드 & Compose 구성 가이드

Docker를 처음 다루는 사람도 이 문서만 읽으면 애플리케이션 이미지를 만들고 `docker-compose`로 서비스를 기동할 수 있도록 단계별로 정리했습니다. 예시는 `Server-Backend` (Spring Boot + Gradle) 프로젝트를 기준으로 작성했습니다.

---

## 1. 사전 준비
- Docker Desktop 또는 Docker Engine 설치
- Docker Compose 플러그인 (Docker Desktop은 기본 포함)
- Git, Java 21, Gradle Wrapper(`./gradlew`) 사용 가능 여부 확인
- 레지스트리에 푸시할 계획이라면 Docker Hub 계정 혹은 사내 레지스트리 정보 준비

> 설치 확인  
> ```bash
> docker --version
> docker compose version
> ```

---

## 2. Dockerfile 작성
### 2.1 기본 구조
멀티 스테이지 빌드로 **빌드 단계**와 **실행 단계**를 분리하면, 최종 이미지 크기를 줄이고 빌드 캐시를 효율적으로 활용할 수 있습니다.

```dockerfile
# Dockerfile
# 1. 빌드 단계: Gradle + JDK 환경에서 jar 생성
FROM gradle:8.8.0-jdk21-alpine AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# 의존성 캐시
RUN ./gradlew dependencies

# 소스 복사 후 빌드
COPY src ./src
RUN ./gradlew build -x test --no-daemon

# 2. 실행 단계: 경량 JRE 이미지에서 jar 실행
FROM eclipse-temurin:21-jre
WORKDIR /app

ENV TZ=Asia/Seoul

# 비루트 계정 생성 (보안 권장)
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring

COPY --from=build --chown=spring:spring /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 왜 `eclipse-temurin:21-jre`?
2024년 이후 Docker Hub에서 `openjdk:21-slim`, `openjdk:21-jdk` 등이 삭제되었습니다. Temurin(Adoptium), Amazon Corretto, Liberica 등 유지되는 배포판 중 하나를 선택해야 새 이미지를 받을 수 있습니다.

---

## 3. 로컬에서 이미지 빌드하기
1. 애플리케이션 jar 생성  
   ```bash
   ./gradlew clean build -x test
   ```
2. 도커 이미지 빌드  
   ```bash
   docker build -t artbite-app:local .
   ```
   - 캐시를 완전히 무시하려면 `--no-cache` 옵션 추가
   - 특정 파일만 고쳐서 다시 빌드할 때는 전 단계가 캐시되어 빠르게 빌드됩니다.

3. 빌드 결과 확인  
   ```bash
   docker images artbite-app
   ```

---

## 4. 단일 컨테이너로 실행해 보기
```bash
docker run --rm \
  --env-file .env \
  -p 8080:8080 \
  artbite-app:local
```
- `.env` 파일을 사용하면 애플리케이션에서 필요한 DB, JWT, CORS 설정을 한 번에 주입할 수 있습니다.
- 추가로 Postgres나 Redis를 로컬에 직접 띄워두어야 하므로, 단독 실행보다는 Compose 사용을 권장합니다.

> 실행 중인 컨테이너의 로그 확인  
> ```bash
> docker logs -f <container_id>
> ```

---

## 5. docker-compose.yml 작성
### 5.1 기본 Compose 파일 (애플리케이션 + DB + Redis)
```yaml
version: "3.9"

services:
  app:
    image: artbite-app:local
    build: .
    container_name: artbite-app
    ports:
      - "8080:8080"
    env_file: .env
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    networks:
      - artbite-net
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s

  postgres:
    image: postgres:13-alpine
    container_name: artbite-postgres
    environment:
      POSTGRES_USER: artbite_user
      POSTGRES_PASSWORD: artbite_password
      POSTGRES_DB: artbite_db
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U artbite_user"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - artbite-net

  redis:
    image: redis:6-alpine
    container_name: artbite-redis
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - artbite-net

networks:
  artbite-net:
    driver: bridge

volumes:
  pgdata:
```

### 5.2 분리된 Compose 파일 사용
현재 프로젝트처럼 `docker-compose.yml`(앱)과 `infra/database/docker-compose.yml`(DB/Redis)을 분리해 관리하면, 환경에 따라 필요한 서비스만 선택적으로 띄울 수 있습니다.

---

## 6. Compose 명령어 요약
| 명령어 | 설명 |
|--------|------|
| `docker compose up -d` | 백그라운드로 컨테이너 기동 |
| `docker compose down` | 컨테이너, 네트워크, 익명 볼륨 정리 |
| `docker compose up --build -d` | 이미지 재빌드 후 실행 (코드가 수정된 경우 필수) |
| `docker compose start/stop` | 이미 생성된 컨테이너만 실행/중지 |
| `docker compose ps` | 현재 상태 확인 |
| `docker compose logs -f app` | 특정 서비스 로그 스트리밍 |
| `docker compose exec app bash` | 컨테이너 내부 셸 접속 |

> 분리된 Compose 파일 사용 예  
> ```bash
> docker compose -f docker-compose.yml -f infra/database/docker-compose.yml up --build -d
> ```

---

## 7. 변경 사항이 있을 때는?
- **코드 수정**: `./gradlew clean build -x test` → `docker compose ... up --build -d`
- **Dockerfile 수정**: 동일하게 `up --build`
- **환경 변수만 수정**: `.env` 변경 후 `docker compose up -d`(재시작) 또는 `docker compose restart app`
- **베이스 이미지 태그 변경**: Dockerfile 수정 후 새로 빌드 (`--no-cache` 권장)

왜 매번 `--build`가 필요할까?  
→ 이미지는 빌드 시점에 jar를 포함하므로, 코드가 달라져도 rebuild하지 않으면 구 버전 jar가 계속 실행됩니다.

---

## 8. Docker로 백엔드를 실행한다는 의미 (개념 & 플로우)

Docker는 애플리케이션을 실행하는 데 필요한 OS, 라이브러리, 실행 파일 등을 **이미지**에 묶어 놓고, 그 이미지를 실행한 **컨테이너**로 서비스가 돌아가도록 도와주는 플랫폼입니다. Spring Boot 백엔드가 Docker로 실행된다는 말은 “jar를 포함한 커스텀 이미지가 만들어져 있고, 그 이미지를 컨테이너로 띄워서 서버가 동작한다”는 뜻입니다.

```
[ Local Source ]
      │
      ▼  (build.gradle / gradlew)
 [ ./gradlew build ]  →  build/libs/app.jar  ──┐
                                              │  Docker build
                                              ▼
                                   +-------------------------+
                                   | Dockerfile              |
                                   |  - Gradle stage         |
                                   |  - Temurin JRE stage    |
                                   +-------------------------+
                                              │
                                              ▼
                            [ artbite-app 이미지 생성 ]
                                              │  docker run / docker compose up
                                              ▼
                           [ 컨테이너 (artbite-app) 실행 중 ]
                             ├─ 내부에서 java -jar app.jar 실행
                             ├─ Postgres, Redis 컨테이너와 네트워크 연결
                             └─ Health Check로 상태 모니터링
```

### 플로우 요약
1. **애플리케이션 빌드**: `./gradlew build` → `build/libs/app.jar`
2. **이미지 생성**: Dockerfile을 통해 jar와 런타임(JRE)을 한 이미지로 패키징
3. **컨테이너 실행**: `docker run` 또는 `docker compose up`으로 이미지에서 컨테이너 생성
4. **연동 구성**: Postgres, Redis 같은 의존 서비스도 컨테이너로 띄우고 네트워크 연결
5. **접근**: `localhost:8080`으로 요청을 보내면 컨테이너 안에서 앱이 응답

이 과정을 통해 “환경에 상관없이 동일한 실행 환경”을 팀원끼리 공유할 수 있고, 배포 서버에서도 동일한 이미지를 가져다 실행하기만 하면 됩니다.

---

## 9. 건강한 컨테이너 상태 확인
- `docker compose ps`에서 `State`가 `healthy`인지 확인
- Health Check 로그  
  ```bash
  docker logs artbite-app --since 30s | grep "actuator/health"
  ```
  `No static resource actuator/health`가 나오면 JAR에 Actuator가 포함되지 않은 상태로 실행된 것입니다.

---

## 10. 오류 & 대응 팁
| 오류 메시지 | 원인 | 해결 |
|-------------|------|------|
| `failed to resolve source metadata for docker.io/library/openjdk:21-slim` | 해당 베이스 이미지 태그 삭제 | Dockerfile을 Temurin/Corretto 등 살아 있는 태그로 교체 |
| `No static resource api/...` | 새 컨트롤러 변경 사항이 이미지에 반영되지 않음 | `./gradlew clean build -x test` 후 `docker compose ... up --build` |
| DB 스키마 중복 생성 오류 | 마이그레이션 스크립트 재실행 | 테이블 드롭 후 실행 or 볼륨 초기화 |
| 컨테이너가 `starting`에서 멈춤 | Health Check 실패 | 앱 로그 확인 → 환경 변수/DB 연결 정보 점검 |

---

## 11. 레지스트리에 푸시하려면
1. 이미지 태그 지정  
   ```bash
   docker build -t registry.example.com/project/artbite-app:2025-11-05 .
   ```
2. 로그인 후 푸시  
   ```bash
   docker login registry.example.com
   docker push registry.example.com/project/artbite-app:2025-11-05
   ```
3. 서버에서는 `docker pull` 후 Compose에서 `image:`를 해당 태그로 지정하면 됩니다.

---

## 12. 권장 워크플로우 요약
1. 코드 수정 → `./gradlew clean build -x test`
2. Docker 이미지 재빌드 → `docker compose ... up --build -d`
3. Actuator, API 호출로 정상 동작 확인
4. 필요 시 Docker Hub/사내 레지스트리로 푸시

위 절차를 따르면 Docker를 처음 접하는 사람도 이미지 빌드부터 Compose 배포까지 무리 없이 수행할 수 있습니다. 질문이 생기면 `backend_troubleshooting_log.md` 문서를 참고하거나 팀에 문의하세요.
