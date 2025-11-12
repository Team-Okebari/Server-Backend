# Bookmark Search & Tag Text Update Summary

---

## 1. 변경 배경
- 사용자 북마크가 늘어나면서 **제목/작가/태그 기반 검색** 요구가 생겼지만, 기존 API는 전체 목록만 반환해 클라이언트에서 필터링할 근거가 없었다.
- `BookmarkListItemResponse`에도 `tagText` 정보가 없어 검색 결과 강조/필터 구현이 어려웠다.
- 따라서 지난 노트 목록과 동일한 검색 전략(제목/태그/작가명 LIKE)을 북마크에도 적용하고, DTO에 태그 텍스트를 추가했다.

---

## 2. 코드 변경
| 영역 | 파일 | 주요 수정 |
|------|------|-----------|
| Repository | `NoteBookmarkRepository` | `searchByUserIdAndKeyword` JPQL 추가 (노트/커버/작가 조인, LIKE 검색) |
| Service | `NoteBookmarkService` | `list(userId, keyword)`로 확장, 키워드 유무에 따라 검색/전체 목록 분기 |
| Controller | `NoteBookmarkController` | `keyword` 쿼리 파라미터 지원, DTO 매핑 시 `tagText` 전달 |
| Mapper | `NoteMapper.toBookmarkResponse` | `tagText` 값 세팅 |
| DTO | `NoteBookmarkResponse`, `BookmarkListItemResponse` | `tagText` 필드 추가 |
| Test | `NoteBookmarkServiceTest` | 기존 목록 테스트 수정 + 검색 전용 테스트 추가 |

---

## 3. API/문서 반영
- `GET /api/notes/bookmarks`가 `keyword` 선택 파라미터를 지원하며, 응답 필드에 `tagText`가 포함됨.
- 문서 업데이트
  - `SYKim_Doc/note_creator_api_spec.md`
  - `SYKim_Doc/note_creator_backend_frontend_integration.md`
  - `SYKim_Doc/note_bookmark_architecture.md`
  - `SYKim_Doc/feature_notes_frontend_plan.md`
  - `SYKim_Doc/feature_notes_code_draft.md`
- 프론트 가이드는 제목·작가명·대표 이미지만 노출하되, `tagText`는 검색 하이라이트/필터 용도로 활용하도록 안내.

---

## 4. QA 체크리스트
1. **검색 정확도**: `keyword`에 대해 제목·태그·작가명이 모두 LIKE 조건으로 조회되는지 Postman으로 검증.
2. **공백 처리**: 빈 문자열/공백만 전달 시 전체 목록과 동일한 결과가 나오는지 확인.
3. **정렬**: 검색 결과도 저장 순(`createdAt desc`)을 유지하는지 확인.
4. **호환성**: 기존 클라이언트는 `keyword`를 보내지 않아도 동작하며, `tagText` 필드는 optional이므로 역호환성 문제 없음.

---

## 5. 후속 과제
- 태그 기반 필터(예: chips)나 최근 검색 키워드 저장 등 UX 개선을 기획과 논의.
- 서버 캐싱/페이지네이션 요구가 생길 경우, 조회 페이징과 캐시 전략을 도입.
