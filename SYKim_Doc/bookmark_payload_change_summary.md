# Bookmark Payload Change Summary

---

## 1. 배경
- 북마크 목록 API (`GET /api/notes/bookmarks`)에서 **작업노트 ID가 누락**되어 후속 액션(노트 상세 이동 등)에 추가 조회가 필요했다.
- 반면, `creatorJobTitle`은 실제 UI에서 더 이상 사용하지 않아 불필요한 데이터가 지속적으로 전송되고 있었다.
- 이에 따라 페이로드를 정리해 **필수 정보만 포함**하도록 스펙을 조정했다.

---

## 2. 요구사항 요약
1. `BookmarkListItemResponse`에 `noteId`를 포함하여 프론트가 바로 노트 상세 URL을 구성할 수 있게 한다.
2. `creatorJobTitle` 필드는 전 파이프라인에서 제거한다. (DTO, 매퍼, 컨트롤러, 테스트 모두)
3. 서비스 로직(`NoteBookmarkService`)이나 레포지토리에는 변화가 없어야 한다.

---

## 3. 변경 내용 상세
| 구분 | 이전 | 이후 |
|------|------|------|
| DTO (`BookmarkListItemResponse`) | `title`, `mainImageUrl`, `creatorName`, `creatorJobTitle` | `noteId`, `title`, `mainImageUrl`, `creatorName` |
| DTO (`NoteBookmarkResponse`) | `bookmarkId`, `noteId`, `title`, `mainImageUrl`, `creatorName`, `creatorJobTitle`, `bookmarkedAt` | `bookmarkId`, `noteId`, `title`, `mainImageUrl`, `creatorName`, `bookmarkedAt` |
| 매퍼 (`NoteMapper.toBookmarkResponse`) | Creator 직함까지 포함해 DTO 구성 | 노트 ID와 기본 정보만 반환 |
| 컨트롤러 (`NoteBookmarkController.bookmarks`) | 프론트 전용 DTO 생성 시 직함 포함 | `noteId` 포함, 직함 제외 |
| 테스트 (`NoteBookmarkServiceTest.listReturnsResponsesFromMapper`) | 직함 포함 DTO를 검증 | 변경된 DTO 구조에 맞게 수정 |

---

## 4. 수정된 파일
- `src/main/java/com/okebari/artbite/note/dto/bookmark/BookmarkListItemResponse.java`
- `src/main/java/com/okebari/artbite/note/dto/bookmark/NoteBookmarkResponse.java`
- `src/main/java/com/okebari/artbite/note/controller/NoteBookmarkController.java`
- `src/main/java/com/okebari/artbite/note/mapper/NoteMapper.java`
- `src/test/java/com/okebari/artbite/note/service/NoteBookmarkServiceTest.java`

각 파일의 주요 변경 사항은 §3 표와 동일하다.

---

## 5. API 영향
- `GET /api/notes/bookmarks` 응답 JSON에서
  - `noteId` 필드가 새로 추가된다.
  - `creatorJobTitle` 필드는 제거된다.
- Swagger/문서(`NoteCreator API Spec` 등)도 이후 동일하게 업데이트 필요.

---

## 6. QA 및 후속 조치
1. **프론트엔드**: 북마크 목록 화면에서 `noteId`를 사용하도록 수정하고, `creatorJobTitle` 참조 로직 제거.
2. **문서화**: API/디자인 스펙에 반영 (예: `note_creator_api_spec.md`, `note_creator_backend_frontend_integration.md`).
3. **테스트**: 현재 단위테스트(`NoteBookmarkServiceTest`)는 통과. E2E 테스트나 Postman 스펙 검증을 통해 실제 응답 구조를 재확인 필요.

---

## 7. 참고
- 추가 릴리즈 노트 작성 시 “Bookmark payload simplification (noteId added, creatorJobTitle removed)” 항목으로 기록.
- 기존 캐시나 클라이언트 저장 데이터는 새 필드를 자동으로 수용하므로 마이그레이션 불필요하나, 배포 전 프론트 코드가 준비됐는지 체크해야 함.
- 이후 검색/`tagText` 확장 내용은 `SYKim_Doc/bookmark_search_change_summary.md` 참고.
