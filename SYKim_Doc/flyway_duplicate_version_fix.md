# Flyway 중복 버전 정리 가이드

> **상황**: `src/main/resources/db/migration` 안에 `V2__*.sql` 파일이 두 개 있고, 이미 프로덕션 DB에 모두 적용된 상태다. Flyway는 동일 버전을 허용하지 않으므로, DB를 초기화하지 않고도 히스토리를 정리하는 절차가 필요하다.

---

## 0. 사전 준비
1. **DB 백업**  
   - 운영 DB라면 전체 백업 또는 최소한 `flyway_schema_history` 테이블을 백업한다. 예)  
     ```bash
     pg_dump -h <host> -U <user> -d <database> -t flyway_schema_history > flyway_schema_history_backup.sql
     ```
2. **접속 정보 확인**  
   - 애플리케이션에서 사용하는 DB와 동일한 접속 정보(호스트, 포트, DB명, 계정)를 준비한다.
3. **DB 접속 도구 선택**  
   - `psql`, DataGrip, DBeaver 등 SQL을 실행할 수 있는 툴. 아래 예시는 `psql` 기준.

---

## 1. 적용 순서와 파일 현황 파악
**DBeaver 사용 시**
1. 좌측 Database Navigator에서 해당 DB → `Schemas` → `public`(또는 실제 스키마) → `Tables` → `flyway_schema_history`를 더블클릭해 데이터 탭을 연다.
2. `Data` 탭에서 “Order by installed_rank” 정렬을 적용한 뒤, `version = '2'`인 행을 확인한다. (필요하면 `Filter` → `version = '2'`를 사용)
3. 동일 탭 상단의 SQL 아이콘을 눌러 자동 생성된 쿼리를 확인하거나, 직접 SQL 콘솔을 열어 아래 쿼리를 실행해도 된다.
   ```sql
   SELECT installed_rank, version, description, script, checksum, installed_on
     FROM flyway_schema_history
    ORDER BY installed_rank;
   ```

---

## 2. 파일명 재정렬
1. 실제 적용 순서에 맞춰 마이그레이션 파일명을 정리한다. 예시:
   - `V2__create_memberships_table.sql` (그대로 유지)
   - `V2__create_notes_tables.sql` → `V3__create_notes_tables.sql`
   - `V3__add_job_title_to_note_creator.sql` → `V4__add_job_title_to_note_creator.sql`
2. IDE나 터미널에서 파일명을 변경한다.  
   ```bash
   mv src/main/resources/db/migration/V2__create_notes_tables.sql \
      src/main/resources/db/migration/V3__create_notes_tables.sql
   mv src/main/resources/db/migration/V3__add_job_title_to_note_creator.sql \
      src/main/resources/db/migration/V4__add_job_title_to_note_creator.sql
   ```

---

## 3. Schema History 업데이트
> 이미 실행된 마이그레이션의 버전이 `flyway_schema_history`에 기록되어 있으므로, 파일명 변경에 맞춰 테이블도 수정해야 한다.

1. (선택) 트랜잭션 시작 및 백업  
   - DBeaver SQL 콘솔에서 다음을 실행한다.
     ```sql
     BEGIN;
     CREATE TABLE IF NOT EXISTS flyway_schema_history_backup AS
     SELECT * FROM flyway_schema_history;
     ```
2. 중복 버전 행 업데이트  
   - `flyway_schema_history` 데이터 탭에서 직접 셀을 수정해도 되지만, SQL로 실행하는 편이 명확하다.  
     예시: `V2__create_notes_tables.sql`을 `V3__...`로 바꾼 경우
     ```sql
     UPDATE flyway_schema_history
        SET version = '3',
            description = 'create notes tables',
            script = 'V3__create_notes_tables.sql'
      WHERE version = '2'
        AND description ILIKE '%notes%';
     ```
   - 기존 `V3` 행(예: `add job title...`)은 `version='4'`, `script='V4__add_job_title_to_note_creator.sql'` 로 수정.
3. 트랜잭션 커밋  
   ```sql
   COMMIT;
   ```

---

## 4. Flyway Repair 실행
파일명과 히스토리를 수정하면 checksum이 기존 기록과 달라진다. Flyway의 `repair` 명령으로 checksum을 재계산한다.

```bash
./gradlew flywayRepair   # 또는 flyway repair
```

> 이 명령은 `flyway_schema_history`의 checksum만 재작성하므로 데이터 테이블에는 영향이 없다.

---

## 5. 검증
1. `./gradlew flywayMigrate` 를 실행해 더 이상 “Duplicate migration” 또는 checksum 오류가 나지 않는지 확인한다.
2. 향후 새 마이그레이션을 만들 때는 정리된 버전 다음 번호부터 사용한다. (예: 이번에 `V4`까지 사용했다면 다음 파일은 `V5__...`)

---

## 6. 요약
1. DB 접속 → `flyway_schema_history` 조회로 실행 순서를 파악한다.  
2. 실제 파일명을 순차적으로 재정렬한다.  
3. `flyway_schema_history`의 해당 행을 새 버전에 맞게 업데이트 후 커밋한다.  
4. `flyway repair` 로 checksum을 재계산한다.  
5. `flyway migrate` 로 검증하고 이후 버전 번호는 누락 없이 증가시킨다.

이 과정을 거치면 DB를 초기화하지 않고도 중복 버전 문제를 안전하게 해결할 수 있다.
