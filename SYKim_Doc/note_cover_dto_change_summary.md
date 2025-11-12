# NoteCoverDto 확장 변경 요약

---

## 1. 변경 배경
- 프론트에서는 커버 정보(제목/티저/대표 이미지)뿐 아니라 **작가 이름과 직함**을 한 번에 참조하고 싶지만, 기존 `NoteCoverDto`는 해당 필드가 없어 별도의 `creator` 객체를 뒤져야 했다.
- 관리자(Admin) 화면에서 노트를 생성/수정할 때도 커버 폼과 작가 메타 정보를 함께 보여주기 위해 같은 DTO를 재활용하고자 했으나 구조가 달라 재사용성이 떨어졌다.
- 이에 `NoteCoverDto`에 `creatorName`, `creatorJobTitle`을 추가해 커버 단위에서 필요한 정보를 묶어 전달하도록 변경했다.

---

## 2. 코드 변경
| 파일 | 주요 수정 |
|------|-----------|
| `src/main/java/com/okebari/artbite/note/dto/note/NoteCoverDto.java` | `creatorName`, `creatorJobTitle` 필드를 추가. (검증은 적용하지 않음, 서버에서 채움) |
| `src/test/java/com/okebari/artbite/note/service/NoteServiceTest.java` | 신규 필드에 맞춰 `NoteCoverDto` 생성자 호출부 갱신. |

> **참고**: 서버에서는 여전히 노트 생성 시 `NoteCoverDto`의 기본 3개 필드(title/teaser/mainImageUrl)만 사용하고, 새로운 필드는 응답/프론트 전용으로 `null` 허용 상태다.

---

## 3. 문서/프론트 타입 반영
| 문서 | 내용 |
|------|------|
| `SYKim_Doc/feature_notes_frontend_plan.md` | `NoteCoverDto` 타입 정의에 `creatorName?`, `creatorJobTitle?` 추가. `NoteCoverResponse`는 더 이상 별도 필드를 선언하지 않고 `NoteCoverDto`를 그대로 확장. |
| `SYKim_Doc/feature_notes_code_draft.md` | Java record 정의, `toCoverDto` 구현, 샘플 요청(`NoteCreateRequest`, `NoteUpdateRequest`) 코드 모두 새로운 필드에 맞춰 업데이트. |

---

## 4. 영향 및 확인 사항
1. **API 요청**: Admin에서 노트 생성/수정 요청을 보낼 때 새 필드 전달은 선택 사항이며, 미입력 시 서버에서 `null`로 처리된다.
2. **응답 구조**: `NoteCoverResponse`는 기존과 동일하게 작가 이름/직함을 포함하지만, 이제 `NoteCoverDto`만으로도 동일 정보를 표현할 수 있어 타입 공유가 수월해진다.
3. **프론트 구현**: 커버 카드/히어로 섹션에서 `cover.creatorName`, `cover.creatorJobTitle`을 바로 사용 가능. (단, 값이 없을 수 있으므로 optional 처리)
4. **추가 검토**: 추후 Admin UI에서 커버 정보와 작가 정보를 동시에 편집할지 여부에 따라 validation 정책을 조정할 수 있다.

---

## 5. 다음 단계
- 기존 프론트 코드에서 `creator` 객체를 통해 이름/직함을 얻고 있었다면, 필요 시 `cover.creatorName`으로 치환할 수 있는지 검토.
- `NoteCoverDto`를 소비하는 다른 문서나 SDK가 있다면 동일 업데이트를 반영.
