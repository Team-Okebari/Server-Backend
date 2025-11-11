# Backend Troubleshooting Log (2025-11-05 기준)

## 1. API 호출 500 (C005) 연쇄 오류
- **증상**: `/api/admin/notes`, `/actuator/health` 등이 항상 `success=false`, `code=C005`로 응답.
- **시행착오**
  - Postman에서 관리자 토큰, JSON 본문을 재확인했으나 동일 증상.
  - DB에 예시 데이터를 직접 삽입하고 `GET` 호출했지만 500 지속.
- **원인**: 컨테이너가 오래된 JAR(Actuator 미포함)로 기동되어 DispatcherServlet이 `/actuator/health`, `/api/admin/notes`를 “정적 리소스”로 처리 → `NoResourceFoundException`.
- **결과/조치**
  - `./gradlew clean build -x test` 후 새 JAR 필요.
  - Docker 이미지 재빌드가 실패하며 헬스체크 `unhealthy`가 유지되어 후속 API도 모두 500.

## 2. 헬스 체크 실패 및 컨테이너 `unhealthy`
- **증상**: `docker ps`에서 `artbite-app` 상태가 `Up (unhealthy)`이며 로그에 `No static resource actuator/health`.
- **시행착오**
  - `security.whitelisted-paths`에 `/actuator/health` 추가함에도 불구하고 실패.
  - `curl http://localhost:8080/actuator/health` 결과가 항상 `C005`.
- **원인**: Actuator 엔드포인트가 포함된 JAR로 재배포되지 않은 상태. 기존 이미지(4일 전 빌드)가 계속 실행 중.
- **조치 제안**
  - 새 JAR 빌드 → `docker compose ... down` → `docker compose ... up --build`.
  - 빌드 실패 원인을 해결하지 못하면 헬스 체크도 계속 실패.

## 3. Docker 이미지 재빌드 실패
- **증상**: `docker compose ... up --build` 실행 시 `failed to resolve source metadata for docker.io/library/openjdk:21-slim` / `openjdk:21-jdk`.
- **시행착오**
  - `openjdk:21-slim` 유지 시도 → 태그가 Docker Hub에서 삭제되어 풀 실패.
  - `openjdk:21-jdk`로 교체 시도 → 역시 존재하지 않아 동일 오류.
- **원인 상세**
  - Docker Hub에서 “베이스 이미지”는 다른 이미지가 기반으로 삼는 원본 레이어(예: 운영체제+JDK)를 뜻한다. `Dockerfile`의 `FROM` 라인에서 지정하는 값이 전부 베이스 이미지이다.
  - `openjdk:21-slim`, `openjdk:21-jdk`는 과거 Oracle/Adoptium이 관리하던 공식 OpenJDK 배포판 태그였으나, 2024년 하반기 이후 Docker Hub 레지스트리에서 제거됨. → `docker pull` 시 manifest를 찾을 수 없어 `not found`.
  - 우리 로컬에는 4일 전 팀에서 빌드한 이미지가 캐시로 남아 있어 실행은 가능했지만, 새로 빌드하려 하면 더 이상 태그가 존재하지 않아 실패.
- **조치 제안**
  - 실행 단계 베이스 이미지를 현재 유지되고 있는 Java 21 런타임으로 교체 (`eclipse-temurin:21-jre`, `eclipse-temurin:21-jdk`, `amazoncorretto:21-alpine`, `bellsoft/liberica-openjdk-debian:21` 등).
  - 기존 이미지를 즉시 삭제할 필요는 없으며, 새 베이스 이미지로 만든 테스트용 이미지는 다른 태그(예: `artbite-app:temurin21`)로 관리할 수 있다.
  - 빌드 과정에서 캐시가 영향을 주면 `docker compose build --no-cache`로 강제 재빌드.

## 4. DB 마이그레이션 스크립트 재실행 오류
- **증상**: `docker exec ... < V1__create_user_social_logins_table.sql` 실행 시 `relation "user_social_logins" already exists`, `column "provider" ... does not exist`.
- **원인**: Flyway V1 스크립트를 이미 적용한 상태에서 강제로 다시 실행 → 중복 테이블/컬럼 생성 시도.
- **결과/대응**
  - V1은 재실행 금지, V2만 필요한 경우 테이블을 직접 드롭하거나 볼륨 초기화(`docker compose down -v`).
  - 현재는 테이블이 정상적으로 생성되어 있고, 직접 데이터 삽입을 통해 예시 노트를 구성.

## 5. 수동 데이터 입력 이후 API 연동
- **조치**: `notes_head`, `note_cover`, `note_process`, `note_question` 등에 예시 데이터 입력 (이미지 URL은 `picsum.photos` 활용).
- **남은 검증**: 컨테이너를 최신 이미지로 교체한 후 `/api/admin/notes`, `/api/notes/...`가 삽입한 데이터를 반환하는지 확인 예정.

---

### 향후 체크리스트
1. Dockerfile 실행 단계 베이스 이미지를 `eclipse-temurin:21-jre` 등으로 교체 후 재빌드.
2. `./gradlew clean build -x test` → `docker compose -f ... down` → `up --build -d` 순서로 최신 JAR 배포.
3. `curl http://localhost:8080/actuator/health`에서 `{"status":"UP"}` 응답 확인.
4. Postman으로 `/api/admin/notes`, `/api/notes/published/today-detail` 등을 호출해 500 해소 여부 점검.
