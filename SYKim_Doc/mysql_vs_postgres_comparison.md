# MySQL vs PostgreSQL 문법 및 적용 가이드

## 1. 주요 문법 차이

| 항목 | MySQL | PostgreSQL |
|------|-------|------------|
| 자동 증가 PK | `BIGINT AUTO_INCREMENT` | `BIGSERIAL`, `SERIAL`, `GENERATED ALWAYS AS IDENTITY` |
| 날짜/시간 타입 | `DATETIME`, `TIMESTAMP`(기본 UTC) | `TIMESTAMP [WITHOUT/WITH] TIME ZONE` |
| 기본 값 표현 | `DEFAULT NOW()` | `DEFAULT CURRENT_TIMESTAMP` |
| 주석 | `--`, `#`, `/* */` | `--`, `/* */` (# 미지원) |
| 외래 키 삭제 규칙 | `ON DELETE CASCADE` 동일 | 동일 |
| 인덱스 생성 | `CREATE INDEX ... ON ...` 동일 | 동일 |

## 2. 멤버십 테이블 적용 예

- **MySQL**: `AUTO_INCREMENT`, `DATETIME` 등을 사용.
- **PostgreSQL**: `BIGSERIAL`, `TIMESTAMP`, `INTEGER`, `CURRENT_TIMESTAMP` 사용.  
  → `src/main/resources/db/migration/V2__create_memberships_table.sql`에 Postgres 버전으로 정리 완료.

## 3. 어떤 DB를 언제 쓸까?

| 구분 | MySQL | PostgreSQL |
|------|-------|------------|
| 트래픽 중심 서비스 | 읽기 위주의 단순 쿼리가 많고, 복제/샤딩 경험이 있다면 선택 가능. | 복잡한 쿼리/함수/CTE가 많거나 JSONB, GIS 등을 활용할 때 유리. |
| 표준 SQL 호환성 | 제한적 (기능 대비 간단) | ANSI SQL 준수도가 높아 이식성이 좋음. |
| 확장 기능 | 파티셔닝·리플리케이션 위주 | 윈도우 함수, CTE, 트리거, 확장 모듈(PostGIS 등) 풍부 |
| 라이선스/커뮤니티 | GPL 기반, Aurora MySQL 등 클라우드 옵션 다양 | 진보된 기능을 무료로 제공, 기업용 배포(Postgres Pro 등)도 존재 |

### 추천
- **분석성 쿼리·JSONB·지리정보** → PostgreSQL
- **간단 CRUD 중심 + 이미 MySQL 운영 경험** → MySQL
- 현재 프로젝트는 **PostgreSQL**을 사용하므로, 마이그레이션 스크립트는 해당 문법에 맞춰 작성해야 한다.

## 4. 실무 팁
1. 마이그레이션 스크립트를 작성할 때 타겟 DB의 문법을 명시 (파일 상단 주석 등).
2. Flyway/Spring 설정에서 `spring.jpa.database-platform`도 올바르게 지정해 SQL Dialect 미스매치를 방지.
3. 로컬 개발자는 Docker Compose로 같은 종류의 DB를 띄워 테스트한다.
