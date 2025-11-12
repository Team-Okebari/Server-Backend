# Presentation: Daily Reminder Note Digest

---

## 1. 목적과 배경
- **핵심 목표**: 북마크하거나 질문에 답했던 과거 작업노트를 하루 한 번 랜덤으로 다시 노출해 재참여를 유도한다.
- **사용자 가치**
  - 하루 한 번 “잊고 있던 작업노트” 배너를 *두 번째 접속부터* 노출해 흐름을 방해하지 않는다.
  - 당일에는 앱 재접속 시에도 동일한 콘텐츠가 유지되어 집중도를 높임.
  - “오늘은 그만 보기”로 제어권을 제공, 다음날 자정에 자동 리셋.
- **업데이트 주기**: 00:00 (Asia/Seoul) 기준으로 사용자별 하나의 노트를 선정하여 24시간 캐싱.

---

## 2. 데일리 타임라인
| 시점 | 시스템 동작 | 노출/상태 |
|------|-------------|-----------|
| 23:00 | `NoteReminderSelector`가 북마크/답변 후보를 조회해 `note_reminder_pot (reminder_date=다음날)`에 랜덤 1건 upsert | 다음날 노출 데이터 미리 준비 |
| 00:00 | `NoteReminderScheduler`가 `reminder_date=오늘` 데이터를 Redis에 SETNX + TTL 24h 저장, 이전 날짜 데이터 정리 | 당일 배너 고정 |
| 첫 접속 | `GET /api/notes/reminder/today` 호출, `surfaceHint=DEFERRED`, `firstVisitAt` 기록, 배너 노출 안 함 | 조용히 상태만 세팅 |
| 두 번째 접속 | 동일 API 호출, `firstVisitAt` 존재 & `bannerSeenAt` 미설정 → `surfaceHint=BANNER`, `bannerSeenAt` 기록 | 상단 배너 최초 노출 (X 버튼 포함) |
| 이후 접속 | `bannerSeenAt` 존재 & `dismissed=false`면 계속 `surfaceHint=BANNER` 반환, 클라이언트 로컬 상태에 따라 UI 제어 | 배너 유지 |
| 배너 X 클릭 | “오늘은 그만 보기” 확인 모달 노출 (취소/닫기) | UI 상단 고정, 아직 dismissed 아님 |
| 모달 - 취소 | 배너 유지 (`surfaceHint=BANNER`), `modalClosedAt`만 선택적으로 기록 | 재접속 시 계속 노출 |
| 모달 - 닫기 (오늘은 그만 보기) | `POST /dismiss` → `dismissed=true`, Redis 동기화 | 당일 노출 중지 |
| 배너 CTA(Call To Action) 클릭 | `noteId` 포함 딥링크 `/notes/archived/{noteId}`로 이동 | 프론트 라우터에서 상세 페이지 재사용 |
| 24h 경과 | 다음 자정 배치가 새 노트를 배포, `reminder_date<오늘` 데이터 정리 | 새로운 콘텐츠 |

---

## 3. 시스템 구성 및 책임
| 컴포넌트 | 역할 | 세부 내용 |
|----------|------|-----------|
| `NoteReminderScheduler` | 매일 00:00에 배치 실행, 후보군 chunk 처리 | `@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")` + ShedLock |
| `NoteReminderSelector` | 북마크/답변 데이터로 후보 리스트 구성 및 랜덤 샘플링 | 가중치 적용, 최근 활동 우선 |
| `NoteReminderService` | 조회/숨김 비즈니스 로직, 캐시 동기화 | firstVisit·bannerSeen·dismiss 상태 관리 |
| `NoteReminderController` | `GET /today`, `POST /dismiss` API 제공 | JWT 인증 |
| `NoteReminderRepository` | `note_reminder_pot` JPA 접근 | `findByUserIdAndReminderDate` |
| Redis 캐시 | 당일 노트를 TTL 24h로 저장 | 키 `note:reminder:{userId}:{yyyymmdd}` |

---

## 4. 디렉토리 구조 & 수정 범위
```
src/main/java/com/okebari/artbite/note
├── controller
│   ├── NoteReminderController.java (+)
│   └── NoteBookmarkController.java (기존)
├── domain
│   └── NoteReminder.java (+)
├── dto
│   ├── reminder/NoteReminderResponse.java (+)
│   ├── reminder/NoteReminderDismissRequest.java (+)
│   └── reminder/NoteReminderPayload.java (+)   # 캐시/DB 스냅샷
├── mapper
│   └── NoteReminderMapper.java (+)
├── repository
│   ├── NoteReminderRepository.java (+)
│   └── NoteBookmarkRepository.java (랜덤 후보 쿼리 추가)
├── scheduler
│   └── NoteReminderScheduler.java (+)
├── service
│   ├── NoteReminderService.java (+)
│   └── NoteBookmarkService.java (북마크 삭제 이벤트 발행)
└── support
    └── NoteReminderSelector.java (+)

src/main/resources/db/migration
└── V7__create_note_reminder_pot_table.sql (+)

src/test/java/com/okebari/artbite/note
├── integration/NoteReminderIntegrationTest.java (+)
└── support/NoteReminderSelectorTest.java (+)
```
`(+)=신규`, 기존 파일은 이벤트 발행/쿼리 추가 외에는 영향 없음.

---

## 5. 데이터 모델
| 컬럼 | 타입 | 설명 |
|------|------|------|
| `id` | BIGINT PK | auto increment |
| `user_id` | BIGINT FK | 대상 사용자 |
| `note_id` | BIGINT FK | 노출 노트 |
| `source_type` | ENUM(`BOOKMARK`,`ANSWER`) | 선택 근거 |
| `reminder_date` | DATE | 자정 기준 날짜 |
| `payload_snapshot` | JSONB | 제목·이미지·직함·질문 요약 등 |
| `first_visit_at` | TIMESTAMP | 당일 API를 최초 호출한 시각(배너 지연 트리거) |
| `banner_seen_at` | TIMESTAMP | 상단 배너를 최초 노출한 시각(=두 번째 접속 이후) |
| `modal_closed_at` | TIMESTAMP | “오늘은 그만 보기” 확인 모달에서 **취소(계속 보기)** 를 선택해 창을 닫은 시각 (선택) |
| `dismissed` | BOOLEAN | 오늘 숨김 여부 |
| `dismissed_at` | TIMESTAMP | “오늘은 그만 보기” 처리 시각 |

제약조건
- UNIQUE (`user_id`, `reminder_date`)
- INDEX (`reminder_date`)
- `payload_snapshot`에는 “당일 고정 데이터”를 담아 노트 수정보다 노출 일관성을 우선.

### 5.1 `source_type` Enum을 유지하는 이유
- `source_type`(see `SYKim_Doc/note_reminder_architecture.md:80-89`) is there mainly to preserve **why** a 노트가 선택됐는지. 단순히 데이터 fetch 목적만이라면 북마크/답변 각각의 레포지토리를 호출하면 되지만, 리마인더 테이블 한 곳에서 “오늘 이 노트가 사용자에게 노출된 이유”를 추적하려면 정규화된 출처 값이 필요해요. 그래야 이후에 “답변 기반 노트가 더 참여율이 높은가?”, “북마크만 있는 유저에게는 어떤 비율로 섞어서 보냈나?” 같은 분석이나 A/B 실험을 바로 돌릴 수 있습니다. 레코드 하나로 일자·사용자·노트·출처를 묶어두면 운영에서 오류 조사도 쉬워요.
- 배치가 북마크 + 답변 후보를 합쳐 샘플링하는 구조상, `note_reminder_pot`에는 이미 “최종 선택 결과”만 저장됩니다. 이때 출처가 없으면, 이후 쿼리로 거슬러 올라가 “오늘 보여준 노트가 북마크에서 왔는지 답변에서 왔는지”를 찾으려면 다시 두 테이블을 뒤져봐야 하고, 삭제/탈퇴 등으로 원본 행이 사라진 경우에는 출처 자체를 잃어버릴 수 있습니다.
- 물론 레포지토리에 “답변이 달린 작업노트 목록” 같은 기능을 추가하는 건 여전히 필요합니다(후보군 구성 단계). 다만, **후보군을 가져오는 과정**과 **최종 선정 결과를 기록·분석하는 과정**을 분리하려면 digest 테이블에 `source_type`을 남겨두는 편이 데이터 신뢰성과 후처리 모두에 유리합니다.
- 정리하면, 레포지토리 확장으로 후보 리스트를 가져오는 건 맞지만, `source_type` 필드는 “당일 노출 내역의 provenance”를 기록해 운영·분석·오류 추적을 가능하게 하려는 선택입니다. 만약 사용 사례가 없다고 판단되면 제거도 가능하지만, 그럼 향후 리포트/실험에서 매번 원본 테이블을 조인해 출처를 추적해야 하니 그 비용을 고려해 결정하면 됩니다.

추가적으로, `source_type` 기준으로 메트릭을 분리해 로그/데이터 파이프라인에서 전송하면 “BOOKMARK 기반 노출 대비 ANSWER 기반 노출의 클릭률/숨김률” 같은 세부 KPI를 즉시 산출할 수 있으므로, 실제 운영 단계에서 기획∙PM팀이 가정을 검증하는 속도를 높일 수 있다.

### 5.2 Daily Pot Upsert 상세
| 질문 | 답변 |
|------|------|
| **언제 랜덤을 뽑아 DB에 upsert하나?** | 매일 23:00(KST)에 북마크/답변 데이터를 스냅샷해 `note_reminder_pot`에 다음날(`reminder_date=다음날`)용 레코드를 미리 저장한다. 자정(00:00) 배치는 이 스냅샷을 Redis에 워밍만 한다. |
| **왜 굳이 테이블에 고정시키나?** | UX 요구가 “오늘 재접속해도 같은 노트”이기 때문. 조회할 때마다 랜덤 뽑으면 배너 내용이 계속 바뀌어 혼란스럽다. 23시에 미리 뽑아두면 자정 직후 배치 부하를 줄이고, 하루 동안 동일 payload를 보장할 수 있다. |
| **`note_reminder_pot`에는 무엇을 저장하나?** | `user_id`, `note_id`, `source_type`, `reminder_date`, `payload_snapshot(제목/이미지/작가 직함 등)`, `firstVisitAt`, `bannerSeenAt`, `modalClosedAt`, `dismissed`, `dismissedAt` 등 “오늘의 노출 상태” 전체를 담는다. |
| **랜덤 후보는 어떻게 모으나?** | **확정안**: 23시에 사용자별 북마크·답변 후보 뷰를 만든 뒤, SQL `ORDER BY md5(:userId || :targetDate || note_id)` + `LIMIT 1`로 한 건을 확정한다. **옵션**: 후보 수가 적거나 커스텀 가중치가 필요하면 `SecureRandom/ThreadLocalRandom`으로 서비스 레이어에서 추첨한다(정책 변경 시 적용). |
| **24시간 뒤 데이터는 어떻게 되나?** | `reminder_date`로 필터링 가능하므로 분석이나 장애 조사에 활용 가능하고, 일정 기간이 지나면 배치로 삭제할 수 있다. 자정 배치가 새 값을 upsert하면서 사실상 “매일 리셋되는 저장소” 역할을 한다. |
| **Redis는 왜 병행하나?** | DB upsert 직후 Redis `note:reminder:{userId}:{yyyymmdd}` 키를 TTL 24h로 저장해 조회 속도를 높이고, dismiss/노트 삭제 이벤트 시 캐시 갱신만으로 즉시 반영하기 위해서다. 캐시 miss 시에는 DB에서 다시 조회해 저장한다. |

> 요약: `note_reminder_pot`은 “사용자별 오늘의 리마인드 노트 + 노출 상태”를 하루 동안 보관하는 저장소다. 자정 배치가 이 테이블을 업데이트해 하루 캐시를 만들고, API는 이 레코드를 기반으로 배너를 노출하거나 숨긴다.

---

## 6. 핵심 개념 및 고려 사항
- **23시 후보군 스냅샷**: 매일 23:00에 북마크/답변 데이터를 조회해 다음날 노출 후보를 미리 확정한다. 자정 직후 배치 부하를 줄이고, 실패 시 재시도 여유(1시간)를 확보한다.
- **자정 Redis 워밍**: 00:00에 `note_reminder_pot`(reminder_date=당일)을 읽어 Redis `note:reminder:{userId}:{yyyymmdd}` 키로 SETNX + TTL 24h 저장한다. 하루 동안 동일 payload 유지.
- **랜덤 샘플링 전략(확정)**: `ORDER BY md5(concat(:userId, :date, note_id)) LIMIT 1`로 하루 한 건을 고정한다.
- **랜덤 샘플링 옵션**: Java `SecureRandom/ThreadLocalRandom`으로 후보 리스트를 직접 샘플링하거나, `ORDER BY random()`으로 매 호출마다 노트를 바꾸는 방식을 정책 변경 시에만 사용한다.
- **캐싱 전략**: Redis `SETNX` + TTL 24h, 캐시 miss 시 DB fallback 후 다시 저장.
- **Idempotent Upsert**: `INSERT ... ON CONFLICT (user_id, reminder_date) DO UPDATE`로 재실행에도 동일 결과 보장.
- **상태 전이**: `firstVisitAt` 없으면 첫 호출로 기록, `bannerSeenAt` 없으면 두 번째 호출에서 배너 노출, `dismissed=true`면 204 반환. `modalClosedAt`은 모달에서 “취소”를 택한 흔적만 남기는 용도로 사용한다.
- **이벤트 기반 무효화**: 현재 노트 비공개/자동 무효화 기능은 요구되지 않으며, ADMIN이 노트를 삭제할 경우 DB FK(`ON DELETE CASCADE`)로 북마크·답변·지난 노트 목록이 자동 정리되므로 추가 리스너는 사용하지 않는다.

### 6.0.1 Idempotent Upsert 상세
- **왜 필요한가?**  
  1. 23시 배치가 중간에 실패해 재실행되더라도 동일 사용자·날짜 조합이 중복 삽입되지 않는다.  
  2. 자정 Redis 워밍 배치가 실패해 다시 돌릴 때도 같은 데이터를 그대로 덮어쓰면서 복구할 수 있다.  
  3. “각 사용자·각 날짜에 한 건만 존재”라는 비즈니스 규칙을 DB Unique 제약 + upsert로 강제한다.
- **SQL 예시**
  ```sql
  INSERT INTO note_reminder_pot (user_id, reminder_date, note_id, payload_snapshot)
  VALUES (:userId, :targetDate, :noteId, :payload)
  ON CONFLICT (user_id, reminder_date)
  DO UPDATE SET
      note_id = EXCLUDED.note_id,
      payload_snapshot = EXCLUDED.payload_snapshot,
      source_type = EXCLUDED.source_type,
      updated_at = now();
  ```
  - 1차 실행에서 `userId=42, targetDate=2025-11-12, noteId=3`가 저장된 후 재시작했을 때, 같은 쿼리가 다시 돌면 동일 row만 덮어쓴다.
  - selector가 두 번째 실행에서 noteId=7을 뽑았다면, 위 upsert가 noteId=7로 갱신해 “마지막으로 성공한 선택 결과”만 남긴다.
- **Kotlin JDBC 스니펫**
  ```kotlin
  fun upsertReminder(userId: Long, targetDate: LocalDate, candidate: ReminderCandidate) {
      jdbcTemplate.update(
          """
          INSERT INTO note_reminder_pot (user_id, reminder_date, note_id, source_type, payload_snapshot)
          VALUES (?, ?, ?, ?, ?::jsonb)
          ON CONFLICT (user_id, reminder_date)
          DO UPDATE SET
              note_id = EXCLUDED.note_id,
              source_type = EXCLUDED.source_type,
              payload_snapshot = EXCLUDED.payload_snapshot,
              updated_at = now()
          """.trimIndent(),
          userId,
          targetDate,
          candidate.noteId,
          candidate.sourceType.name,
          objectMapper.writeValueAsString(candidate.payload)
      )
  }
  ```
  - `ReminderScheduler`가 재시도 루프를 돌며 이 함수를 호출해도 항상 동일 키 한 건만 갱신된다.
  - 애플리케이션 레이어는 `runCatching { upsertReminder(...) }` 재시도만 하면 되고, 별도 “중복 여부” 체크 쿼리를 호출할 필요가 없다.

### 6.0 랜덤 전략 결정표
| 항목 | 확정안(기본) | 옵션(정책 변경 시) |
|------|--------------|--------------------|
| 선정 타이밍 | 매일 23시에 후보를 스냅샷하고 자정에 Redis 워밍. | 실시간 호출 시마다 후보를 조회하거나, 배너 노출 시점에 즉시 추첨. |
| 추첨 로직 | SQL `ORDER BY md5(:userId || :targetDate || note_id) LIMIT 1` (하루 1건 고정). | Java `SecureRandom/ThreadLocalRandom` 가중치 추첨, 혹은 `ORDER BY random()`으로 완전 무작위. |
| 결과 유지 | `note_reminder_pot` + Redis TTL 24h, 당일 동일 payload 유지. | 캐시 TTL을 짧게 두거나 매 요청마다 새로 추첨(당일 고정 보장 안 됨). |
| 사용 목적 | “두 번째 접속부터 동일 노트를 보여주는” 현재 요구사항. | “접속할 때마다 다른 노트를 보여달라” 등 정책 변경 혹은 실험. |
| 추가 고려 | 23시~자정 사이에 실패해도 재시도 여유 확보. | 후보 수가 적거나 가중치 정책/실험을 빠르게 적용하고 싶을 때. |

### 6.1 랜덤 샘플링 구현 상세
1. **후보 조회**: 23시에 `note_bookmark`, `note_answer`에서 **각 사용자별** 후보 리스트를 `UNION ALL`로 가져온다. 즉, 쿼리는 `WHERE user_id = :userId` 조건으로 사용자 하나씩 처리하며, 다른 사용자의 후보와 섞지 않는다. 필요하면 projection에 `weight` 컬럼을 추가해 최근 활동에 가중치를 둘 수 있다.
2. **SQL 기반 랜덤**: PostgreSQL에서 안정적인 랜덤 순서를 만들기 위해 사용자 ID·날짜·노트 ID를 합쳐 해시를 계산한다.
   ```sql
   SELECT note_id, source_type, payload
   FROM reminder_candidates
   WHERE user_id = :userId
   ORDER BY md5(:userId || :targetDate || note_id::text)
   LIMIT 1;
   ```
   같은 입력(유저/날짜/노트)에는 항상 동일한 순서가 나오므로 재실행해도 결과가 바뀌지 않는다.
   - 예시) userId=42, noteId 후보가 1~3이라고 가정하면 `targetDate=2025-11-12`일 때 해시 값이 가장 작은 noteId=2가 선택될 수 있다. 다음날 `targetDate=2025-11-13`이 되면 입력 문자열이 `"42-2025-11-13-3"`처럼 바뀌어 해시 값도 달라지므로 이번에는 noteId=3이 선택될 수 있다. 즉 하루 동안은 고정되지만 날짜가 바뀌면 자동으로 다른 noteId가 뽑힌다.
   - 예시: userId=42, noteId 후보가 1~10일 때 `targetDate=2025-11-12`이면 해시 값이 가장 작은 noteId=3이 선택될 수 있다. 다음날 `targetDate=2025-11-13`이 되면 같은 noteId=3이라도 입력 문자열이 `"42-2025-11-13-3"`으로 바뀌어 해시 값이 달라지므로, 이번에는 noteId=7의 해시가 가장 작아져 7이 선택될 수 있다. 즉 하루 동안은 고정되지만 날짜가 바뀌면 자동으로 다른 noteId가 뽑힌다.
3. **(옵션) Java 기반 가중치**: 후보 수가 적거나 정책상 매 호출마다 결과가 바뀌어야 한다면 `NoteReminderSelector`에서 `SecureRandom` or `ThreadLocalRandom`을 사용해 직접 샘플링한다. (예: 최근 활동 노트는 weight 2, 오래된 노트는 weight 1).
4. **예외 처리**: 후보가 없으면 `Optional.empty()`를 반환해 해당 사용자는 당일 배너가 노출되지 않도록 한다.
5. **결과 저장 & 재시도**: 뽑힌 노트를 `note_reminder_pot`에 `INSERT ... ON CONFLICT`로 upsert 한다. 랜덤 셀렉터가 후보를 고르지 못하거나 DB/캐시 I/O 중 오류가 나면 “성공할 때까지” 재시도를 수행하되, 무한 루프를 막기 위해 최대 N회(예: 3회) 시도 후 실패 시 해당 사용자 row를 건너뛰고 알람을 쏜다. 다음 배치가 다시 실행되면 동일 사용자에 대해 재선정을 시도한다. 자정에는 성공적으로 저장된 데이터를 그대로 Redis에 옮긴다.
6. **Retry 루프 예시**
   ```kotlin
   repeat(MAX_RETRY) { attempt ->
       val candidate = selector.pick(userId, targetDate) ?: return@repeat
       runCatching { repository.upsert(userId, targetDate, candidate) }
           .onSuccess { return } // 성공 시 즉시 탈출
           .onFailure { log.warn("reminder upsert 실패 attempt=$attempt", it) }
   }
   alarmService.notify("Reminder pick 실패", userId, targetDate)
   ```
   - `selector.pick` 단계에서 후보를 찾지 못하면 즉시 반복문 상단으로 올라가 다시 랜덤 추첨을 시도한다.
   - upsert가 성공할 때까지 반복하므로, 일시적인 DB·네트워크 오류로 인한 “당일 리마인드 부재”를 최소화한다.

### 6.1.1 MAX_RETRY & 알람 채널 구현 예시
- **권장 기본값**: `MAX_RETRY=3`, 알람 채널은 `note-reminder-alert` (Slack)로 가정. 배치 시간 안에 충분히 재시도할 수 있다면 5회까지 늘릴 수 있으나, 실패 시 다음 배치로 넘길 수 있는 시간이 줄어든다.
- **프로퍼티 정의**: 운영 환경마다 다르게 가져가기 위해 설정으로 분리한다.
  ```kotlin
  @ConfigurationProperties("note.reminder")
  data class NoteReminderProperties(
      val maxRetry: Int = 3,
      val alarmChannel: String = "note-reminder-alert"
  )
  ```
- **서비스 적용 코드**
  ```kotlin
  @Component
  class ReminderScheduler(
      private val selector: NoteReminderSelector,
      private val repository: NoteReminderRepository,
      private val alarmService: AlarmService,
      private val props: NoteReminderProperties
  ) {
      fun assignReminder(userId: Long, targetDate: LocalDate) {
          repeat(props.maxRetry) { attempt ->
              val candidate = selector.pick(userId, targetDate)
                  ?: return@repeat // 후보 없음 → 다음 attempt에서 다시 pick

              runCatching { repository.upsert(userId, targetDate, candidate) }
                  .onSuccess { return } // 성공 시 함수 종료
                  .onFailure {
                      log.warn("reminder upsert 실패 userId=$userId attempt=${attempt + 1}", it)
                      Thread.sleep(200L) // 짧은 back-off
                  }
          }

          alarmService.notify(
              channel = props.alarmChannel,
              title = "Reminder pick 실패",
              message = "userId=$userId targetDate=$targetDate 후보 확정 실패 (maxRetry=${props.maxRetry})"
          )
      }
  }
  ```
  - 알람 서비스는 사내 표준 경보 채널(Slack, PagerDuty 등)로 라우팅한다.
  - `Thread.sleep`은 예시일 뿐이며, 실제 구현에서는 `RetryTemplate`나 Reactor `retryBackoff` 등을 써도 된다.

#### 단위 테스트 예시
1. **재시도 성공 케이스**
   ```kotlin
   @Test
   fun `upsert가 2번째에 성공하면 알람을 쏘지 않는다`() {
       whenever(selector.pick(42L, targetDate)).thenReturn(candidate)
       whenever(repository.upsert(any(), any(), any()))
           .thenThrow(DataAccessResourceFailureException("boom"))
           .thenReturn(Unit)

       scheduler.assignReminder(42L, targetDate)

       verify(repository, times(2)).upsert(42L, targetDate, candidate)
       verify(alarmService, never()).notify(any(), any(), any())
   }
   ```
2. **모든 재시도 실패 케이스**
   ```kotlin
   @Test
   fun `max retry 이후 실패하면 알람을 전송한다`() {
       whenever(selector.pick(42L, targetDate)).thenReturn(candidate)
       whenever(repository.upsert(any(), any(), any()))
           .thenThrow(DataAccessResourceFailureException("boom"))

       scheduler.assignReminder(42L, targetDate)

       verify(repository, times(props.maxRetry)).upsert(42L, targetDate, candidate)
       verify(alarmService).notify(
           eq(props.alarmChannel),
           contains("Reminder pick 실패"),
           contains("userId=42")
       )
   }
   ```
3. **후보 없음 케이스**: `selector.pick`이 `null`을 반환하면 `upsert`가 호출되지 않고 함수가 바로 끝나는지 검증한다.

- 테스트에서 `NoteReminderSelector`와 `NoteReminderRepository`를 Mock으로 두고, 재시도 횟수와 알람 여부만 집중적으로 검증한다.
- 통합 테스트에서는 실제 DB/Redis를 붙여 두 가지 시나리오를 확인한다.
  1. `note_reminder_pot`에 이미 row가 있을 때 upsert가 idempotent하게 동작하는지.
  2. `alarmService` 대체 구현(예: InMemory sink)을 두어 실패 시 메시지가 기록되는지.

### 6.2 대량 사용자(예: 10만 명) 처리 전략
| 전략 | 설명 | 장점 | 주의 사항 |
|------|------|------|-----------|
| **기본 순차 배치** | 한 스레드가 모든 사용자 ID를 순회 | 구현 단순, 디버깅 용이 | 10만 명 이상이면 23시/자정 배치 시간이 길어짐 |
| **청크 병렬 처리** | 사용자 ID를 청크(예: 1,000명)로 나눠 고정 쓰레드 풀에서 병렬 실행 | 처리 시간 단축, 특정 청크만 재시도 가능 | DB/Redis 커넥션 풀 크기 조정 필요, 모니터링 필수 |
| **워커 큐 분산** | 23시 배치가 사용자 파티션 메시지를 Kafka/SQS에 넣고 워커가 비동기로 처리 | 수평 확장 용이, 자정 부하 분산 | 큐 운영 비용, 순서/중복 처리 로직 필요 |
| **Redis 파이프라이닝** | 자정 Redis 워밍 시 SETNX 호출을 파이프라인으로 묶어 전송 | 네트워크 RTT 감소, 대량 set에 유리 | 파이프라인 내 실패 항목 재시도 로직 필요 |

#### 청크 병렬 처리 예시 (Kotlin)
```kotlin
@Component
class ReminderSnapshotJob(
    private val activeUserProvider: ActiveUserProvider,
    private val reminderScheduler: ReminderScheduler, // 6.1.1 로직 재사용
    @Value("\${note.reminder.chunk-size:1000}") private val chunkSize: Int,
    private val executor: ExecutorService
) {
    fun snapshotAt23(targetDate: LocalDate) {
        activeUserProvider.fetchActiveUserIds()                  // e.g. 100,000 IDs
            .chunked(chunkSize)                                  // [1..1000], [1001..2000], ...
            .map { chunk -> CompletableFuture.runAsync({
                chunk.forEach { userId ->
                    reminderScheduler.assignReminder(userId, targetDate)
                }
            }, executor) }
            .forEach { it.join() }                               // 전체 완료까지 대기
    }
}
```
- `executor`는 CPU 수/커넥션 풀 크기에 맞춰 제한하고, 실패한 청크는 `CompletableFuture` 예외를 모아 재실행한다.
- chunk-size를 줄이면 실패 범위를 좁힐 수 있고, 늘리면 컨텍스트 스위칭을 줄일 수 있다.

#### Redis 파이프라이닝 예시 (Lettuce)
```kotlin
fun warmupRedis(today: LocalDate, reminders: List<ReminderPayload>) {
    val sync = redisConnection.sync()
    sync.setAutoFlushCommands(false)
    reminders.chunked(5_000).forEach { bucket ->
        bucket.forEach { reminder ->
            sync.setex(
                "note:reminder:${reminder.userId}:$today",
                Duration.ofHours(24).seconds,
                reminder.payloadJson
            )
        }
        sync.flushCommands() // 파이프라인 전송
    }
    sync.setAutoFlushCommands(true)
}
```
- 파이프라인 단위로 `flushCommands()`를 호출해 1회 왕복으로 수천 건을 밀어 넣을 수 있다.
- 실패한 bucket은 로그로 남기고, 동일 bucket만 다시 실행하면 된다.

> 결론: 23시 후보 확정과 자정 Redis 워밍 모두 사용자 수가 커질수록 **청크 병렬 처리 + 파이프라이닝** 전략을 적용해야 SLA를 지킬 수 있다. 단, 커넥션 풀·모니터링·재시도 정책을 함께 조정해 과부하를 방지한다.

#### 해시 기반 샘플링 도식
```mermaid
flowchart TD
    Bookmark["Bookmark 후보"] --> U["후보 뷰\n(UNION ALL)"]
    Answer["Answer 후보"] --> U
    U -->|"userId별"| HashStep["md5(userId || targetDate || noteId)"]
    HashStep -->|"ORDER BY asc LIMIT 1"| PickedNote["선정된 noteId"]
    PickedNote --> PotWrite["INSERT INTO note_reminder_pot\n(reminder_date = targetDate)"]
```
- `targetDate = LocalDate.now(Asia/Seoul).plusDays(1)` (다음날).
- 동일 사용자 + 동일 날짜에 대해서는 항상 같은 순서를 만들기 때문에 재실행해도 결과가 바뀌지 않는다.
- 날짜가 바뀌면 `targetDate` 값도 변해 새로운 순서가 생성되므로, 다음날에는 다른 노트가 선택된다.
- `ORDER BY random()`처럼 모든 후보를 완전 무작위로 섞지 않고도 “하루 동안 고정 + 다음날 새 랜덤” 요구를 충족하므로 비용이 훨씬 낮다. 하루에 한 번만 정렬해 결과를 저장하면 되므로, 사용자가 조회할 때마다 랜덤 작업을 반복하지 않아도 된다.
- `ORDER BY random()`처럼 모든 후보를 완전 무작위로 섞지 않고도 “하루 동안 고정 + 다음날 새 랜덤” 요구를 충족하므로 비용이 훨씬 낮다. 하루에 한 번만 해시 정렬을 실행해 결과를 저장하면 되므로, 사용자가 조회할 때마다 랜덤 작업을 반복하지 않아도 된다.
- 만약 사용자가 접속할 때마다 결과가 달라져야 하는 시나리오라면, `ORDER BY random()` 또는 Java `ThreadLocalRandom`으로 매 호출마다 랜덤을 돌리고, 배너가 보여질 때마다 다시 선택하는 구조로 바꿔야 한다. 이 경우 하루 동안 동일 노트를 유지하기 어렵고 – 사용자 경험상 불필요한 변동이 생기므로 현재 설계에서는 해시 기반(one per day) 전략을 채택한다.
- `ORDER BY random()`처럼 전체 후보를 완전 무작위로 섞지 않고도 “하루 동안 고정 + 다음날 새 랜덤” 요구를 충족하므로 비용이 훨씬 낮다.

---

## 7. 시각화 자료
### 7.1 23시 스냅샷 & 자정 배포 플로우
```mermaid
sequenceDiagram
    participant Snapshot as 23시 Selector
    participant Repo as ReminderRepository
    participant Scheduler as 00시 Scheduler
    participant Redis

    Snapshot->>Repo: upsert(userId, remindDate=다음날, noteId, payload)
    Scheduler->>Repo: fetch remindDate = 오늘
    Scheduler->>Redis: SETNX note:reminder:{user}:{today} payload TTL=24h
    Redis-->>API: GET /notes/reminder/today 응답
    API-->>User: 배너 데이터
```

### 7.2 사용자 세션 & 배너 지연 노출 흐름
```mermaid
flowchart TD
    Start["첫 접속"] --> API1["GET /api/notes/reminder/today"]
    API1 -->|surfaceHint=DEFERRED| NoBanner["배너 미노출\n(firstVisitAt 기록)"]
    NoBanner --> Revisit["두 번째 접속"]
    Revisit --> API2["GET /api/notes/reminder/today"]
    API2 -->|dismissed=true| Hidden["당일 숨김 상태"]
    API2 -->|surfaceHint=BANNER| Banner["상단 배너 노출\n(bannerSeenAt 저장)"]
    Banner --> CTA["오늘의 작업노트 보러가기\n(Call To Action, noteId 기반 상세 이동)"]
    CTA --> DetailRoute["라우터 이동: /notes/archived/{noteId}"]
    DetailRoute --> DetailAPI["GET /api/notes/archived/{noteId}"]
    DetailAPI -->|accessible=true| FullView["상세 화면 (NoteResponse)"]
    DetailAPI -->|accessible=false| PreviewView["프리뷰 화면 + 구독 CTA"]
    Banner --> CloseX["X 버튼 클릭 → 확인 모달"]
    CloseX --> ModalNo["아니요 선택\n(배너 유지)"]
    CloseX --> ModalYes["예 선택\n(오늘은 그만 보기)"]
    ModalYes --> Dismiss["POST /dismiss"]
    Dismiss --> CacheUpdate["Redis/DB 업데이트"]
    CacheUpdate --> Hidden

```

---

## 8. API 컨트랙트
| Endpoint | Method | 설명 | 응답 |
|----------|--------|------|------|
| `/api/notes/reminder/today` | GET | 당일 리마인드 노트 조회 | 200: `NoteReminderResponse`, 204: 노출 없음 |
| `/api/notes/reminder/dismiss` | POST | “오늘은 그만 보기” 처리 | 204, 바디 `{ "reason": "USER_DISMISS" }` (선택) |
| `/api/notes/reminder/modal-close` | POST | (선택) X 버튼 클릭 후 모달에서 “취소”를 선택해 닫은 기록(`modalClosedAt`) 저장 | 204 |

> 버튼별 API: “취소”(모달 닫기)는 필요 시 `/modal-close`로 로그만 남기고, “닫기”(오늘 그만 보기)는 기존 `/dismiss`를 호출해 `dismissed=true` 상태를 만든다. 별도 API는 추가하지 않는다.

`NoteReminderResponse`
```json
{
  "surfaceHint": "BANNER",     // or DEFERRED (첫 접속), NONE(=dismissed)
  "noteId": 123,
  "title": "잊고 있는 작업노트",
  "excerpt": "노트 타이틀을 절대 한 줄...",
  "creatorName": "작가명",
  "jobTitle": "직함",
  "sourceType": "BOOKMARK",
  "reminderDate": "2024-06-20",
  "dismissed": false
}
```

`surfaceHint` enum
- `DEFERRED`: 첫 번째 접속, 배너 노출 없이 상태만 세팅
- `BANNER`: 배너 노출 필요
- `NONE`: 후보 없음 혹은 `dismissed=true`

> **명명 배경**: `surfaceHint`는 외부 표준이 아닌 리마인드 API 전용 커스텀 필드다. 서버가 프론트에 “이번 응답을 어떤 방식으로 노출할지” 힌트를 주기 위해 정의했다.  
> - `DEFERRED`: “노출을 미루고(first visit) 상태만 기록하라.” 첫 접속임에도 로컬 캐시가 꼬였을 때도 서버가 강제로 배너를 숨길 수 있다.  
> - `BANNER`: “즉시 배너를 띄워라.” 두 번째 접속 이상, `firstVisitAt`이 기록된 상태에서 내려보내 UX를 통일한다.  
> - `NONE`: “이번에는 아무 것도 띄우지 말라.” 후보가 없거나 사용자가 `dismissed=true` 상태일 때 내려보내, 프론트가 local state에 기대지 않고 노출 여부를 결정할 수 있게 한다.  
> Enum 값을 명확히 정의해 두면, 이후 요구사항(예: 모달, 풀스크린 등) 추가 시에도 동일 필드를 확장해 나갈 수 있고, 서버와 프론트 간 상태 불일치로 인한 UI 꼬임을 방지할 수 있다.

### 8.1 배너 클릭 → 상세 페이지 이동 흐름
1. **리마인드 조회**: `GET /api/notes/reminder/today` 응답에 `noteId` 포함.
2. **배너 CTA 클릭**: 프론트는 `noteId`를 들고 앱 라우터 경로 `/notes/archived/{noteId}`(혹은 동일 의미의 공통 상세 경로)로 이동.
3. **상세 API 호출**: 해당 화면 진입 시 백엔드 `GET /api/notes/archived/{noteId}` 호출.
4. **백엔드 판단**: API가 구독 상태를 확인해
   - 유료 사용자는 `accessible=true`, `note` 필드에 전체 `NoteResponse`
   - 무료 사용자는 `accessible=false`, `preview` 필드에 `NotePreviewResponse`
   를 반환.
5. **프론트 렌더링**: 응답의 `accessible` 값에 따라 본문/프리뷰 UI를 선택해 렌더링.  

> 즉, 배너/북마크/지난 노트 등 진입 경로가 달라도 상세 페이지 호출은 `GET /api/notes/archived/{noteId}` 하나로 통일되며, 라우팅은 단지 “어느 noteId로 이동할지”만 결정하고 유·무료 분기는 백엔드가 담당한다.

---

## 9. UX 규칙 (와이어프레임 연계)
- 첫 방문: 리마인드 배너를 **노출하지 않고** 백그라운드에서 `firstVisitAt`만 저장, 사용자는 기존 콘텐츠를 그대로 소비.
- 두 번째 방문 이후: 상단 배너를 노출하고 `bannerSeenAt`을 기록. 배너에는 X 버튼과 “오늘의 작업노트 보러가기”(클릭 시 `noteId`로 `/notes/archived/{noteId}` 상세 이동), “오늘은 그만 보기” CTA(Call To Action)가 포함된다.
- X 버튼: “오늘은 그만 보기” 여부를 묻는 예/아니오 모달을 띄운다. **취소**(계속 보기)를 선택하면 배너가 유지되고 선택적으로 `modalClosedAt`만 기록한다(필요 시 `POST /modal-close`). **닫기**를 선택하면 `POST /dismiss`를 호출한다.
- “오늘은 그만 보기” 확인 후 예를 선택하면 서버 `dismiss` API를 호출해 당일 전체 노출을 중지한다.
- 배너를 닫았더라도 사용자가 다시 새로고침하면 동일 노트가 재노출되며, `dismissed=true`가 되었을 때만 완전히 숨겨진다.
- 자정 리셋: `firstVisitAt`, `bannerSeenAt`, `modalClosedAt`, `dismissed` 모두 초기화되어 다음날 새로운 노트가 동일 흐름으로 노출.

---

## 10. 구현 로드맵 & 테스트
1. **DB 스키마**: Flyway `V7__create_note_reminder_pot_table.sql` 추가, enum/인덱스 정의.
2. **도메인/레포지토리**: `NoteReminder` 엔티티 + `NoteReminderRepository`, QueryDSL 커스텀 구현.
3. **후보군 셀렉터**: `NoteReminderSelector`에서 북마크·답변 projection과 랜덤 로직 작성, 유닛 테스트로 분포 검증.
4. **스케줄러**: ShedLock 적용, chunk 처리, idempotent upsert 구현. 통합 테스트(`NoteReminderIntegrationTest.publishOnePerUser`).
5. **서비스/컨트롤러**: 조회/숨김/firstVisit·bannerSeen 업데이트, Redis 캐시 연동. MockMvc 테스트로 API 계약 검증.
6. **이벤트 처리**: 노트 삭제/비공개, 북마크 해제 이벤트 시 캐시 무효화 로직 연결 (추후 결정).
7. **모니터링**: 배치 처리 수, 실패율, dismiss율을 `MeterRegistry`와 로그로 남겨 Grafana 대시보드에 노출.

---

## 11. 리스크 & 대응
- **대량 사용자 처리 시간**: batch chunk + 비동기 스트림, 필요시 “lazy selection (첫 조회 시 선택)” fallback 설계.
- **후보 고갈**: 북마크/답변이 모두 없는 사용자는 204 반환 후 “오늘의 작업노트 모으기” CTA(Call To Action)로 대체 경험 제공.
- **노트 삭제/수정**: `payload_snapshot` 기반 노출로 당일 안정성 확보, 삭제 이벤트 시 즉시 dismissed 처리.
- **시간대 불일치**: 모든 날짜 계산을 `Clock` 주입 + `ZoneId.of("Asia/Seoul")`로 테스트 가능하게 구성.
- **랜덤성 검증**: Selector 테스트에서 χ² 유사 검증으로 편향 감시, 메트릭으로 노트 노출 편중 확인.

### 11.1 장애 시나리오별 운영 전략

| 시나리오 | 감지 포인트 | 대응 전략 |
|----------|-------------|-----------|
| **① 스케줄러 지연/실패로 자정 이후 리마인드 미생성** | - `SchedulerSuccessCount` 메트릭이 0<br>- `note_reminder_pot`에서 `reminder_date = today` 행 미존재<br>- Grafana 알람 (자정+5분 이후 still 0) | 1) 자동 재시도: ShedLock이 실패하면 5분 간격 재시도 태스크 실행<br>2) 운영자 수동 버튼: Admin API `/admin/reminders/retry?date=`로 특정 일자만 재생성<br>3) Lazy selection fallback: 조회 API가 pot miss 감지 시, 즉시 `NoteReminderSelector`를 호출해 단건 생성 후 캐시/DB 저장<br>4) 장애 보고: Slack 알람 + Notion 장애 기록 남기기 |
| **② 첫/두 번째 접속 판단 오류로 배너가 잘못 노출** | - `surfaceHint=BANNER`이면서 `firstVisitAt` null<br>- `surfaceHint=DEFERRED`인데 `firstVisitAt`이 이미 존재 | 1) 서버를 단일 근거로 사용: 첫 호출 시 `firstVisitAt` 없으면 무조건 기록 + `DEFERRED` 반환, 이후 요청에서는 기록 값 기준으로 분기<br>2) 백필 로직: `firstVisitAt`가 null인데 `bannerSeenAt`가 set되어 있으면 over-write 후 즉시 `BANNER` 반환<br>3) 모니터링: “오늘 firstVisit만 있고 bannerSeen 없음” 비율을 대시보드에 표시, 임계치 초과 시 알림<br>4) 재설정 API: 운영자가 사용자별 상태(`firstVisitAt`,`bannerSeenAt`,`dismissed`)를 초기화 할 수 있는 `/admin/reminders/reset` 제공 |
| **③ 사용자가 ‘오늘은 그만 보기’ 눌렀는데 dismiss 상태가 반영되지 않음** | - 클라이언트에서 200/204 응답 미수신 로그<br>- 같은 날 `dismissed=false`인데 `/dismiss` 호출 수가 1 이상 | 1) API 응답 전 DB와 Redis 모두 업데이트하고, 실패 시 전체 작업을 재시도(재시도 3회, backoff)<br>2) 클라이언트 재시도 가이드: 실패 시 “다시 시도” 토스트 + 최대 3회 재요청<br>3) 조회 API에서 `dismissed_at` 타임스탬프와 현재 시간이 5분 내인데도 `dismissed=false`면 강제로 true로 보정 (idempotent update)<br>4) 실패 로그 적재: dismiss 실패 시 사용자·노트·오류코드를 Kibana에 남겨 수동 보정 가능하게 함 |
| **④ 하루 종일 배너가 보여야 하는데 전혀 안 보이는 경우** | - `surfaceHint=NONE` 비율이 비정상적으로 높음<br>- `dismissed=true`가 자정 이후에도 유지 | 1) 조회 API에서 `reminder_date < today` 인 레코드는 자동 초기화(자정 리셋이 실패했을 때 self-heal)<br>2) Redis 캐시 장애 대비: 캐시 miss 시 DB fallback 후 다시 SET<br>3) “오늘 다시 보기” 수동 버튼 제공 → 해당 버튼은 `/dismiss` 반대 동작으로 상태 초기화 |

> 위 전략은 전부 운영 리플레이 가능한 로그/메트릭을 전제로 한다. `NoteReminderAudit` 테이블(사용자, 날짜, 상태 변경)을 추가해 추후 “왜 배너가 떴/안 떴는지” 역추적 가능하도록 설계한다.

---

## 12. 다음 액션
1. PM/디자인과 API 컨트랙트(`surfaceHint`, CTA(Call To Action) 문구, dismiss 흐름) 확정.
2. DB/Redis 작업 순서 및 배포 전략 정리 (마이그레이션 선적용 → 코드 배포).
3. 스케줄러 모니터링용 대시보드 초안 작성.

---

## 13. 캐싱 전략 옵션 상세 비교

| 옵션 | 개요 | 장점 | 단점 | 구현 포인트 |
|------|------|------|------|-------------|
| **A. 24시간 Redis 고정 캐시** | 자정 배치 직후 `SETNX note:reminder:{user}:{date}`로 하루 동안 동일 payload 유지 | - 모든 조회가 메모리 hit → DB 부하 최소화<br>- 멀티 인스턴스에서도 응답 일관성 보장<br>- 캐시 invalidation으로 사용자 상태(숨김 등) 즉시 반영 가능 | - Redis 장애 시 폴백 로직 필요<br>- 하루 동안 콘텐츠 변경이 어려움(노트 삭제 이벤트 처리 필요)<br>- 운영 복잡도(모니터링/용량) 증가 | - 스케줄러에서 upsert 성공 시 Redis 저장<br>- `dismiss` 시 Redis에 즉시 업데이트<br>- TTL=24h + 자정 키 네이밍으로 자동 만료<br>- 장애 대비: 캐시 miss 시 DB fallback 후 다시 저장 |
| **B. DB 단독 조회 (No Cache)** | 모든 요청이 `note_reminder_pot`를 인덱스 조회하여 응답 | - 아키텍처 단순, Redis 의존 제거<br>- 데이터 즉시 최신 상태 반영(첫 방문/배너 노출/숨김 상태가 DB 한 곳에만 존재)<br>- 장애 포인트 감소 | - DAU/접속 빈도가 높으면 DB read 부하 증가<br>- 멀티 인스턴스에서 동일 레코드 반복 조회, 커넥션 점유 | - `(user_id, reminder_date)` 복합 인덱스로 단건 조회 O(1)+디스크 hit 최소화<br>- 서비스 레이어에서 `NoteReminder` → DTO 변환만 수행<br>- dismiss 처리 후에도 별도 캐시 동기화 불필요<br>- 트래픽 증가 시 read replica 도입 고려 |
| **C. 짧은 TTL 캐시 (1~4h)** | 배치 직후 캐시하되 TTL을 몇 시간으로 제한, 만료 시 DB에서 다시 로딩 | - Redis 캐시 혜택 + 자가 복구력 확보(캐시 파기되도 수 시간 후 자동 복구)<br>- 당일 중간에 노트 교체(예: 콘텐츠 비활성화) 필요 시 캐시 만료를 기다리면 반영 가능 | - TTL 간격 동안은 Redis miss→DB 복구 비용 발생<br>- 일부 시점엔 캐시/DB 데이터가 어긋날 수 있어 일관성 관리 필요 | - 스케줄러는 TTL 짧게 설정(`Duration.ofHours(4)` 등), `dismiss` 시 캐시 갱신<br>- 만료 후 첫 조회 시 DB에서 재조회→Redis 재적재<br>- 노트 삭제 이벤트 시 Redis 키 삭제만 해도 몇 시간 후 자동 로딩<br>- 모니터링에서 캐시 hit rate 기준치를 설정해 이상 감지 |

### 선택 가이드
1. **트래픽/DB 부하**: DAU 5만+, 평균 하루 3회 이상 접속이라면 옵션 A가 가장 효율적. DAU 수천/DB 여유가 크다면 옵션 B도 충분.
2. **운영 복잡도**: Redis 운영 부담을 줄이고 싶다면 옵션 B. 이미 토큰/세션 때문에 Redis를 사용 중이고 모니터링 체계가 있다면 옵션 A/C가 자연스럽다.
3. **콘텐츠 갱신 요구**: 당일에도 노트를 교체하거나 긴급 차단이 자주 필요하면 옵션 C가 유연. (A는 명시적 invalidation 로직이 필수)
4. **장애 복원**: 옵션 C는 캐시 만료만 기다리면 자동으로 재구축되므로 운영자介入이 덜 필요. 옵션 A는 장애 시 fallback·재워밍 스크립트가 있어야 한다.

### 구현 과정 특징
- **옵션 A (24h)**: 스케줄러 → DB upsert → Redis SETNX. 서비스는 Redis hit 우선, miss 시 DB 조회 후 다시 캐시. dismiss/노트 삭제 이벤트는 Redis delete + DB 업데이트 쌍으로 처리.
- **옵션 B (DB only)**: 스케줄러는 DB만 업데이트. 서비스는 항상 `NoteReminderRepository.findByUserIdAndReminderDate`. dismiss는 단순 update. 인프라 단계를 줄일 수 있어 배포/테스트가 간단.
- **옵션 C (Short TTL)**: 옵션 A의 로직을 기반으로 TTL만 짧게 설정하고, 캐시 miss 빈도·패턴을 모니터링. 재적재 시 레이스 컨디션을 피하기 위해 `SET` 대신 `SETEX` 사용, 또는 `CacheManager`를 활용해 스프링 캐시 추상화로 구현.

#### 13.1 옵션 A: 24시간 Redis 고정 캐시
```mermaid
sequenceDiagram
    participant Scheduler
    participant DB as ReminderRepository
    participant Redis
    participant API as ReminderAPI
    participant User

    Scheduler->>DB: upsert reminder(row)
    Scheduler->>Redis: SETNX note:reminder:{user}:{date} payload TTL=24h
    User->>API: GET /reminder/today
    API->>Redis: GET key
    alt cache hit
        Redis-->>API: payload
    else cache miss
        API->>DB: findByUserIdAndDate
        DB-->>API: row
        API->>Redis: SET key payload TTL=24h
    end
    API-->>User: response (banner/popup)
    User->>API: POST /dismiss
    API->>DB: update dismissed
    API->>Redis: set dismissed flag or delete key
```

#### 13.2 옵션 B: DB 단독 조회
```mermaid
flowchart LR
    SubgraphScheduler[자정 배치]
    SubgraphScheduler --> DBWrite[(note_reminder_pot upsert)]
    end

    UserSession[사용자 요청] --> Controller[NoteReminderController]
    Controller --> RepoQuery[(findByUserIdAndDate)]
    RepoQuery --> Controller
    Controller --> Response[(NoteReminderResponse)]
    UserSession -->|dismiss| DismissController[POST /dismiss]
    DismissController --> RepoUpdate[(update dismissed)]
```

#### 13.3 옵션 C: 짧은 TTL 캐시
```mermaid
sequenceDiagram
    participant Scheduler
    participant DB as ReminderRepository
    participant Redis
    participant API as ReminderAPI

    Scheduler->>DB: upsert reminder(row)
    Scheduler->>Redis: SETEX key payload TTL=4h
    loop 사용자 요청
        API->>Redis: GET key
        alt hit
            Redis-->>API: payload
        else miss/expired
            API->>DB: findByUserIdAndDate
            DB-->>API: row
            API->>Redis: SETEX key payload TTL=4h
        end
        API-->>User: response
        opt dismiss
            API->>DB: update dismissed
            API->>Redis: update or delete key
        end
    end
```

---

## 14. Reference Code Snippets

> 실제 구현 시 참고할 골격 코드. 필드/예외 처리/로그는 상황에 맞게 확장한다.

### 14.1 엔티티 & 레포지토리

```java
@Entity
@Table(name = "note_reminder_pot",
	uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "reminder_date"}))
public class NoteReminder {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "note_id", nullable = false)
	private Long noteId;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_type", nullable = false)
	private ReminderSourceType sourceType;

	@Column(name = "reminder_date", nullable = false)
	private LocalDate reminderDate;

	@Column(name = "payload_snapshot", columnDefinition = "jsonb")
	private String payloadSnapshot;

	private LocalDateTime firstVisitAt;
	private LocalDateTime bannerSeenAt;
	private LocalDateTime modalClosedAt;
	private boolean dismissed;
	private LocalDateTime dismissedAt;

	public void markFirstVisit(LocalDateTime now) {
		if (this.firstVisitAt == null) {
			this.firstVisitAt = now;
		}
	}

	public void markBannerSeen(LocalDateTime now) {
		if (this.bannerSeenAt == null) {
			this.bannerSeenAt = now;
		}
	}

	public void dismiss(LocalDateTime now) {
		this.dismissed = true;
		this.dismissedAt = now;
	}
}
```

```java
public interface NoteReminderRepository extends JpaRepository<NoteReminder, Long> {

	Optional<NoteReminder> findByUserIdAndReminderDate(Long userId, LocalDate reminderDate);

	@Query("""
		select r from NoteReminder r
		where r.reminderDate = :date
		order by r.userId
	""")
	Stream<NoteReminder> streamAllByDate(LocalDate date);
}
```

### 14.2 후보 셀렉터 (옵션 시나리오)
> 기본 배포 환경에서는 6.1에서 정의한 **SQL md5 정렬**을 사용한다. 아래 예시는 “정책이 바뀌어 서비스 레이어에서 랜덤 추첨을 하고 싶을 때” 적용할 수 있는 대안 구현이다.

```java
@Component
@RequiredArgsConstructor
public class NoteReminderSelector {

	private final NoteBookmarkRepository bookmarkRepository;
	private final NoteAnswerRepository answerRepository;
	// 옵션 경로: 서비스 레이어에서 무작위 추첨이 필요할 때만 SecureRandom 사용
	private final Random random = new SecureRandom();

	public Optional<ReminderCandidate> pickOne(Long userId) {
		List<ReminderCandidate> candidates = new ArrayList<>();
		candidates.addAll(bookmarkRepository.findReminderCandidates(userId));
		candidates.addAll(answerRepository.findReminderCandidates(userId));

		if (candidates.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(candidates.get(random.nextInt(candidates.size())));
	}

	public record ReminderCandidate(Long noteId, ReminderSourceType sourceType, NoteReminderPayload payload) { }
}
```

### 14.3 스케줄러

```java
@Component
@RequiredArgsConstructor
public class NoteReminderScheduler {

	private final ActiveUserProvider activeUserProvider;
	private final NoteReminderSelector selector;
	private final NoteReminderRepository reminderRepository;
	private final NoteReminderCachePort cachePort;

	@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
	@SchedulerLock(name = "noteReminderScheduler", lockAtLeastFor = "PT1M", lockAtMostFor = "PT5M")
	@Transactional
	public void publishDailyDigest() {
		LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
		activeUserProvider.streamUserIds()
			.forEach(userId -> selector.pickOne(userId).ifPresent(candidate -> upsert(userId, today, candidate)));
	}

	private void upsert(Long userId, LocalDate today, ReminderCandidate candidate) {
		NoteReminder reminder = reminderRepository.findByUserIdAndReminderDate(userId, today)
			.orElseGet(NoteReminder::new);
		reminder.setUserId(userId);
		reminder.setReminderDate(today);
		reminder.setNoteId(candidate.noteId());
		reminder.setSourceType(candidate.sourceType());
		reminder.setPayloadSnapshot(candidate.payload().toJson());
		reminderRepository.save(reminder);
		cachePort.save(reminder); // Redis SETNX
	}
}
```

### 14.4 서비스 & 컨트롤러

```java
@Service
@RequiredArgsConstructor
public class NoteReminderService {

	private final NoteReminderRepository reminderRepository;
	private final NoteReminderCachePort cachePort;
	private final Clock clock;

	@Transactional(readOnly = true)
	public Optional<NoteReminderResponse> getTodayReminder(Long userId) {
		LocalDate today = LocalDate.now(clock);
		NoteReminder reminder = cachePort.get(userId, today)
			.orElseGet(() -> reminderRepository.findByUserIdAndReminderDate(userId, today)
				.map(cachePort::save)
				.orElse(null));
		if (reminder == null) {
			return Optional.empty();
		}

		LocalDateTime now = LocalDateTime.now(clock);
		if (reminder.getFirstVisitAt() == null) {
			reminder.markFirstVisit(now);
			reminderRepository.save(reminder);
			return Optional.of(NoteReminderResponse.deferred(reminder));
		}
		if (reminder.isDismissed()) {
			return Optional.of(NoteReminderResponse.none());
		}
		if (reminder.getBannerSeenAt() == null) {
			reminder.markBannerSeen(now);
			reminderRepository.save(reminder);
		}
		return Optional.of(NoteReminderResponse.banner(reminder));
	}

	@Transactional
	public void dismiss(Long userId, LocalDate today) {
		NoteReminder reminder = reminderRepository.findByUserIdAndReminderDate(userId, today)
			.orElseThrow(() -> new ReminderNotFoundException(userId, today));
		reminder.dismiss(LocalDateTime.now(clock));
		cachePort.delete(userId, today);
	}
}
```

```java
@RestController
@RequestMapping("/api/notes/reminder")
@RequiredArgsConstructor
public class NoteReminderController {

	private final NoteReminderService reminderService;

	@GetMapping("/today")
	public ResponseEntity<?> getToday(@AuthenticationPrincipal CustomUserDetails user) {
		return reminderService.getTodayReminder(user.getUser().getId())
			.<ResponseEntity<?>>map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.noContent().build());
	}

	@PostMapping("/dismiss")
	public ResponseEntity<Void> dismiss(
		@AuthenticationPrincipal CustomUserDetails user,
		@RequestBody(required = false) NoteReminderDismissRequest request) {
		reminderService.dismiss(user.getUser().getId(), LocalDate.now());
		return ResponseEntity.noContent().build();
	}
}
```

```java
public record NoteReminderDismissRequest(String reason) { }
```

### 14.5 Redis 포트 (옵션 A/C)

```java
@Component
@RequiredArgsConstructor
public class NoteReminderCachePort {

	private final RedisTemplate<String, String> redisTemplate;
	private final ObjectMapper objectMapper;

	public Optional<NoteReminder> get(Long userId, LocalDate date) {
		String key = buildKey(userId, date);
		String value = redisTemplate.opsForValue().get(key);
		return value == null ? Optional.empty() : Optional.of(deserialize(value));
	}

	public NoteReminder save(NoteReminder reminder) {
		String key = buildKey(reminder.getUserId(), reminder.getReminderDate());
		redisTemplate.opsForValue().set(key, serialize(reminder), Duration.ofHours(24));
		return reminder;
	}

	public void delete(Long userId, LocalDate date) {
		redisTemplate.delete(buildKey(userId, date));
	}

	private String buildKey(Long userId, LocalDate date) {
		return "note:reminder:%d:%s".formatted(userId, date);
	}
}
```

위 코드 조각을 기반으로 서비스/테스트를 구현하면 문서에서 설명한 플로우를 그대로 재현할 수 있다.
