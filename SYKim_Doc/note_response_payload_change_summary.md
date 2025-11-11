# NoteResponse Payload Change Summary

---

## 1. 변경 배경
- 상세/관리 화면에서 **작가 직함(creatorJobTitle)** 정보를 별도로 보여달라는 요구가 있었으나, `NoteResponse`에는 `creatorId`만 존재하고 직함은 커버 DTO나 `CreatorSummaryDto`를 통해서만 우회적으로 접근해야 했다.
- API 계약상 `NoteResponse`가 모든 상세 데이터를 책임지고 있으므로, 직함을 직접 내려주는 편이 프론트/어드민 코드 재사용에 유리하다고 판단해 필드를 추가했다.

---

## 2. 코드 변경
| 파일 | 수정 내용 |
|------|-----------|
| `src/main/java/com/okebari/artbite/note/dto/note/NoteResponse.java` | `String creatorJobTitle` 필드를 추가. |
| `src/main/java/com/okebari/artbite/note/mapper/NoteMapper.java` | `Creator` 엔티티에서 직함을 읽어 새 필드에 전달. |

추가 로직은 단순한 필드 전달이라 서비스/레포지토리 계층에는 영향이 없다.

---

## 3. 문서/타입 정의
| 문서 | 반영 내용 |
|------|-----------|
| `SYKim_Doc/feature_notes_frontend_plan.md` | `NoteResponse` 인터페이스에 `creatorJobTitle?: string`을 추가해 프론트 타입을 동기화. |
| `SYKim_Doc/feature_notes_code_draft.md` | Java record 정의를 동일하게 갱신해 아키텍처 문서와 코드 초안이 일치하도록 유지. |

---

## 4. QA 체크리스트
1. **API 응답**: `GET /api/notes/published/today-detail`, `GET /api/notes/archived/{id}`, `GET /api/admin/notes/{id}` 등 `NoteResponse`를 반환하는 엔드포인트에서 `creatorJobTitle`이 직렬화되어 오는지 Postman으로 확인.
2. **프론트 연동**: 기존에 `cover.creatorJobTitle`을 사용하던 UI가 `note.creatorJobTitle`도 참조할 수 있도록 리팩터링할지 여부 검토.
3. **호환성**: 새 필드는 optional이므로 구버전 클라이언트에도 영향이 없지만, 사용 여부에 따라 타입 정의 업데이트가 필요하다.

---

## 5. 추가 메모
- `creatorId`는 여전히 응답에 포함되며, 프론트에서도 해당 값을 사용해 작가 상세 데이터를 요청한다. 따라서 새 필드 추가로 인해 기존 흐름이 바뀌지는 않는다.
- 추후 Admin 노트 편집 화면에서 `creatorJobTitle`을 즉시 확인하거나 다른 언어 버전을 표시해야 할 때 본 필드를 재활용할 수 있다.
