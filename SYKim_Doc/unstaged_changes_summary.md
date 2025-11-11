# Unstaged Change Summary (2025-11-06)

## src/main/java/com/okebari/artbite/creator/dto/CreatorRequest.java
- 형식 통일: 레코드 선언 닫는 괄호 앞 공백을 제거해 다른 DTO와 동일한 스타일을 유지했습니다. (실제 로직 변경 없음)

## src/main/java/com/okebari/artbite/note/dto/note/NoteProcessDto.java
- Checkstyle 경고 해소: 들여쓰기를 탭으로 교체해 프로젝트 규칙(탭 기반 들여쓰기)을 준수했습니다.

## src/main/java/com/okebari/artbite/note/mapper/NoteMapper.java
- Checkstyle 경고 해소: import 순서를 알파벳/그룹 기준으로 재정렬했습니다. 기능상 변화는 없습니다.

## src/main/java/com/okebari/artbite/note/service/NoteQueryService.java
- Checkstyle 경고 해소: `getTodayPublishedDetail` 메서드 들여쓰기를 탭으로 정리해 컨벤션에 맞췄습니다.

## src/test/java/com/okebari/artbite/note/integration/NoteRedisIntegrationTest.java
- 시간대 보정: 테스트에서 `LocalDateTime.now()` 대신 KST 기준 `LocalDateTime.now(ZoneId.of("Asia/Seoul"))`을 사용하도록 변경해 KST로 저장된 `publishedAt` 비교 시 실패하던 문제를 방지했습니다.  
- 관련해 `ZoneId` 상수를 추가했습니다.
