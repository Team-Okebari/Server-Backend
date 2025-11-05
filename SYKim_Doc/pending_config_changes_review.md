# 로컬 미반영 변경 사항 점검

아래 파일들은 현재 워킹 트리에 수정된 상태입니다. 배포·커밋 전에 내용 검토 후 이상 없으면 유지, 문제가 있을 경우 되돌리시길 바랍니다.

## Dockerfile
- **변경 내용**: 런타임 이미지를 `openjdk:21-slim`에서 `eclipse-temurin:21-jre`로 교체.
- **검토 포인트**
  - 새 이미지에서 애플리케이션이 정상 구동되는지 확인.
  - 이미지 교체 후 용량/기동 속도 및 인증 관련 기본 설정 차이 여부 확인.

## build.gradle
- **변경 내용**: `spring-boot-starter-actuator` 의존성 추가.
- **검토 포인트**
  - Actuator 엔드포인트 노출 범위를 `application.yml`에서 적절히 제한했는지 확인.
  - 운영 배포 시 권한 또는 방화벽 설정 필요 여부 검토.

## src/main/java/com/okebari/artbite/ArtbiteApplication.java
- **변경 내용**: `@EnableScheduling` 추가로 스케줄러 기능 활성화.
- **검토 포인트**
  - 스케줄링 관련 빈이 실제 존재하는지, 예상치 못한 주기로 동작하지 않는지 확인.
  - 로컬/운영 환경에서 스케줄러 구동이 안전한지 점검.

## src/main/java/com/okebari/artbite/common/exception/ErrorCode.java
- **변경 내용**: 노트/크리에이터 관련 에러 코드(`N001~N003`, `CR001`) 추가.
- **검토 포인트**
  - 서비스/컨트롤러 계층에서 해당 코드들을 사용하도록 맞춰졌는지 확인.
  - 기존 에러 코드와 중복되는 구간 없는지 검증.

## src/main/resources/application-dev.yml
- **변경 내용**: `security.whitelisted-paths`에 `/api/notes/published/today-cover`, `/actuator/health` 추가.
- **검토 포인트**
  - 화이트리스트 확장으로 인한 인증 우회 경로가 없는지 확인.
  - Actuator 노출 범위를 `management.endpoints.web.exposure.include` 등으로 제한했는지 검토.

## 데이터베이스 (note_creator.job_title)
- **변경 내용**: `note_creator` 테이블에 `job_title` 컬럼 추가 (`V3__add_job_title_to_note_creator.sql`) 및 기존 `bio` 값을 기본 복사.
- **검토 포인트**
  - Flyway가 새로운 마이그레이션을 정상 인식하는지 확인.
  - Creator 도메인/DTO/매퍼가 `jobTitle` 필드를 올바르게 처리하는지 API 응답으로 검증.

---

> 위 변경 사항 중 문제가 발견되면 `git checkout -- <파일경로>` 등으로 개별 되돌릴 수 있습니다. 검토 후 이상 없으면 진행해 주세요.
