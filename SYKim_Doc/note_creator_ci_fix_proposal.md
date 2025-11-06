# CI 실패 대응 제안 (2025-11-06)

## 문제 요약
- `./gradlew checkstyleMain` 실행 시 다수의 경고가 발생했습니다.  
  - `NoteMapper.java`, `NoteQueryService.java`의 import 정렬 순서가 규칙과 다릅니다.  
  - `NoteProcessDto.java`, `NoteQueryService.java`에 공백 기반 들여쓰기가 포함되어 Checkstyle이 요구하는 탭 들여쓰기를 위반합니다.
- `./gradlew test` 단계에서 `NoteRedisIntegrationTest.completedNoteIsPublishedAndCached()`가 실패했습니다.  
  - `publishedAt` 비교 시, 테스트 코드가 시스템 기본 시간대(UTC) `LocalDateTime.now()`와 KST 기준으로 저장된 값을 비교하여 9시간 차이로 인해 `isBeforeOrEqualTo` 검증이 깨진 것으로 보입니다.

## 권장 조치

### 1. Checkstyle 경고 정리
- `NoteMapper.java`, `NoteQueryService.java`에서 import 구문을 알파벳 순 / 그룹 순으로 재정렬합니다.  
  - IDE에서 `Optimize Imports` 실행 또는 수동 정렬.  
- `NoteProcessDto.java`, `NoteQueryService.java`의 들여쓰기를 탭으로 교정합니다.  
  - `.editorconfig` 또는 Checkstyle 설정에 맞춰 탭/공백 규칙을 확인 후 전체 파일 재포맷 권장.

### 2. NoteRedisIntegrationTest 안정화
- `assertThat(published.getPublishedAt()).isBeforeOrEqualTo(LocalDateTime.now());` 대신 시간대 차이를 고려한 비교로 교체합니다.  
  - 예: `assertThat(published.getPublishedAt().atZone(ZoneId.of("Asia/Seoul")).toInstant())` vs `Instant.now()` 비교.  
  - 혹은 KST 기준 `LocalDateTime.now(ZoneId.of("Asia/Seoul"))` 사용.  
- 테스트의 핵심이 “PUBLISHED 전환 + 캐싱 여부”이므로, 단순히 `publishedAt`이 null 아님/markPublished 호출 여부를 검증하는 방향으로 완화도 고려할 수 있습니다.

### 3. 재검증
- 위 수정 후 `./gradlew checkstyleMain`과 `./gradlew test`를 로컬/CI 모두에서 재실행하여 성공 여부를 확인합니다.  
- 필요 시 Redis/DB 컨테이너가 정상 기동하도록 `NoteContainerBaseTest` 기반 테스트 환경도 함께 확인합니다.

## 예상 코드 수정 사항
- `src/main/java/com/okebari/artbite/note/mapper/NoteMapper.java`, `src/main/java/com/okebari/artbite/note/service/NoteQueryService.java`  
  - import 순서를 Checkstyle 규칙에 맞게 조정 (`java.*` → `javax.*` → `org.*` → `com.*`)  
  - 불필요한 공백 라인 제거 및 정렬
- `src/main/java/com/okebari/artbite/note/dto/note/NoteProcessDto.java`, `src/main/java/com/okebari/artbite/note/service/NoteQueryService.java`  
  - 들여쓰기 탭 적용 (`\t`)  
  - 필요 시 `.editorconfig` 설정 확인 후 전체 파일 Reformat
- `src/test/java/com/okebari/artbite/note/integration/NoteRedisIntegrationTest.java`  
  - KST 상수(`private static final ZoneId KST = ZoneId.of("Asia/Seoul");`) 추가  
  - `assertThat(published.getPublishedAt()).isBeforeOrEqualTo(LocalDateTime.now());` → `assertThat(published.getPublishedAt()).isBeforeOrEqualTo(LocalDateTime.now(KST));` 변경  
  - (선택) 캐싱 검증에서 `publishedAt` NotNull, 상태 전환 여부 중심으로 단언식 단순화
- 변경 후 `./gradlew checkstyleMain` / `./gradlew test` 재실행 및 GitHub Actions에서 성공 여부 확인

### 예시: 수정 전/후 코드 스냅샷

1. **NoteMapper import 정렬**

```java
// Before
import java.util.List;

import org.springframework.stereotype.Component;

import com.okebari.artbite.creator.domain.Creator;
import com.okebari.artbite.creator.dto.CreatorSummaryDto;
import com.okebari.artbite.creator.mapper.CreatorMapper;
import com.okebari.artbite.note.domain.Note;
...
```

```java
// After
import java.util.List;

import com.okebari.artbite.creator.domain.Creator;
import com.okebari.artbite.creator.dto.CreatorSummaryDto;
import com.okebari.artbite.creator.mapper.CreatorMapper;
import com.okebari.artbite.note.domain.Note;
...

import org.springframework.stereotype.Component;
```

2. **NoteProcessDto 들여쓰기**

```java
// Before
public record NoteProcessDto(
    @NotNull short position,
    @NotBlank String sectionTitle,
    @NotBlank String bodyText,
    @NotBlank String imageUrl
) {
}
```

```java
// After
public record NoteProcessDto(
	@NotNull short position,
	@NotBlank String sectionTitle,
	@NotBlank String bodyText,
	@NotBlank String imageUrl
) {
}
```

3. **NoteQueryService 메서드 들여쓰기**

```java
// Before
 	public TodayPublishedResponse getTodayPublishedDetail(Long userId) {
 		Note note = findTodayPublishedNote();
 		boolean accessible = subscriptionService.isActiveSubscriber(userId);
 		if (!accessible) {
 			NotePreviewResponse preview = noteMapper.toPreview(note, OVERVIEW_PREVIEW_LIMIT);
 			return new TodayPublishedResponse(false, null, preview);
 		}
 		return new TodayPublishedResponse(true, noteMapper.toResponse(note), null);
 	}
```

```java
// After
	public TodayPublishedResponse getTodayPublishedDetail(Long userId) {
		Note note = findTodayPublishedNote();
		boolean accessible = subscriptionService.isActiveSubscriber(userId);
		if (!accessible) {
			NotePreviewResponse preview = noteMapper.toPreview(note, OVERVIEW_PREVIEW_LIMIT);
			return new TodayPublishedResponse(false, null, preview);
		}
		return new TodayPublishedResponse(true, noteMapper.toResponse(note), null);
	}
```

4. **NoteRedisIntegrationTest 시간대 보정**

```java
// Before
assertThat(published.getPublishedAt()).isBeforeOrEqualTo(LocalDateTime.now());
```

```java
// After
private static final ZoneId KST = ZoneId.of("Asia/Seoul");
...
assertThat(published.getPublishedAt()).isBeforeOrEqualTo(LocalDateTime.now(KST));
```

## 기대 효과 및 후속 조치
- Checkstyle 경고를 제거하여 코드 컨벤션을 준수하고, 빌드 로그 가독성을 개선합니다.  
- 통합 테스트가 시간대에 관계없이 안정적으로 동작하여 CI 파이프라인이 실패하지 않도록 합니다.  
- 수정 후 CI가 통과하는지 GitHub Actions에서 확인하고, 필요 시 Master/Dev 브랜치에 반영합니다.
