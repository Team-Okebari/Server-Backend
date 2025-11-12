# Archived Note Payload Change Summary

---

## 1. 변경 배경
- 지난 노트(ARCHIVED) 목록 응답에 포함되던 `teaser` 필드는 실제 UX에서 활용도가 낮고, 오히려 카드에서 **작가 이름과 게시일** 정보를 강조해 달라는 요청이 있었다.
- 프론트에서는 카드 내 서브텍스트 자리에 “작가명 · 게시일(년/월/일)” 조합을 표시하고 싶었으나, 기존 DTO로는 해당 값을 계산할 수 없었다.
- 이에 `ArchivedNoteSummaryResponse` 구조를 수정해 **`creatorName`과 `publishedDate`(yyyy-MM-dd)** 를 제공하도록 변경했다.

---

## 2. 변경 사항 요약
| 항목 | 이전 | 이후 |
|------|------|------|
| DTO 필드 | `id`, `tagText`, `title`, `mainImageUrl`, `teaser` | `id`, `tagText`, `title`, `mainImageUrl`, `creatorName`, `publishedDate` |
| Mapper (`NoteMapper.toArchivedSummary`) | 커버 티저 문자열을 내려줌 | `Creator.name`과 `publishedAt.toLocalDate()`를 내려줌 |
| 문서 · 스펙 | “teaser” 사용 안내 | “creatorName + publishedDate” 표기 방식 |
| 프론트 타입 정의 | `teaser?: string` | `creatorName?: string`, `publishedDate?: string` |

---

## 3. 수정된 파일 목록
1. **백엔드 코드**
   - `src/main/java/com/okebari/artbite/note/dto/summary/ArchivedNoteSummaryResponse.java`
   - `src/main/java/com/okebari/artbite/note/mapper/NoteMapper.java`
2. **문서/스펙**
   - `SYKim_Doc/note_creator_backend_frontend_integration.md`
   - `SYKim_Doc/note_creator_api_spec.md`
   - `SYKim_Doc/note_query_architecture.md`
   - `SYKim_Doc/feature_notes_dev_plan.md`
   - `SYKim_Doc/feature_notes_frontend_plan.md`
   - `SYKim_Doc/feature_notes_code_draft.md`

모든 문서에서 JSON 예시, DTO 설명, UX 각주를 최신 구조에 맞춰 갱신했다.

---

## 4. API/프론트 영향
- `GET /api/notes/archived` 응답 JSON에 **`creatorName`, `publishedDate`** 필드가 추가되며, `teaser`는 제거된다.
- 프론트 카드 UI는 `creatorName`과 `publishedDate`를 합쳐 서브텍스트로 노출하면 된다. (예: `김작가 · 2024-06-01`)
- 정렬/검색 로직에는 변화가 없으며, `tagText`와 키워드 검색 파라미터는 그대로 유지된다.

---

## 5. 확인/QA 포인트
1. **프론트 타입**: `ArchivedNoteSummaryResponse` 인터페이스가 새 필드를 사용하도록 이미 수정되었는지 확인.
2. **날짜 포맷**: 백엔드에서 `LocalDate`로 내려주므로 직렬화 결과가 ISO `yyyy-MM-dd`인지 Postman으로 점검.
3. **레거시 UI**: teaser 문자열을 참조하던 레거시 컴포넌트가 있다면 제거/대체 필요.
4. **분석 로직**: 카드 클릭 이벤트 등에서 teaser를 로그에 보내던 경우 새로운 필드로 업데이트.

---

## 6. 향후 조치
- 멤버십/분석 팀과 협의해 `creatorName`, `publishedDate` 기반 KPI 정의 여부 확인.
- teaser가 완전히 불필요하다면 NoteCover 엔티티의 `teaser` 활용처도 정리하는 후속 작업을 검토.
