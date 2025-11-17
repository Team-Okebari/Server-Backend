# NoteMapper 기반 프론트 전달 데이터 요약

| API/기능 | 최종 DTO | NoteMapper가 조립/추가하는 필드 | 비고 |
|----------|---------|--------------------------------|------|
| `GET /api/notes/published/today-detail` (구독자) <br> `GET /api/notes/archived/{id}` (`accessible=true`) | `NoteResponse` | `cover` 내부 `creatorName`, `creatorJobTitle`, `publishedDate`, `categoryBadge?` / `creatorId`, `creatorJobTitle` / `creator` summary / 타임스탬프 (`publishedAt`, `archivedAt`, `createdAt`, `updatedAt`) / `externalLink` | 노트 엔티티 + `Creator` 엔티티 + `NoteCover` 등 서브 엔티티 조합 |
| `GET /api/notes/published/today-preview` <br> `GET /api/notes/archived/{id}` (`accessible=false`) | `NotePreviewResponse` | `cover`(title/mainImage/creator/publishedDate, `categoryBadge?`, **teaser=null**) + `overview`(`sectionTitle`, `bodyText`<=100자, `imageUrl`) | 상세 대신 프리뷰 전용 데이터를 구성 |
| `GET /api/notes/published/today-cover` | `NoteCoverResponse` | `creatorName`, `creatorJobTitle`, `publishedDate` (노트·크리에이터 엔티티에서 조합) | 히어로 섹션 커버 UI |
| `GET /api/notes/archived` | `ArchivedNoteSummaryResponse` | `title`, `mainImageUrl` (NoteCover), `creatorName`, `publishedDate` (note.publishedAt) | 카드 목록 전용 요약 DTO |
| `GET /api/notes/bookmarks` | `BookmarkListItemResponse` (`NoteBookmarkResponse` → Mapper 변환) | `title`, `mainImageUrl` (NoteCover), `creatorName`, `tagText` (note.tagText) | Mapper가 `NoteBookmark` → `NoteBookmarkResponse`로 조립 후 컨트롤러에서 프론트 DTO로 변환 |
| `POST /api/notes/questions/{id}/answer` 등 | `NoteAnswerResponse` | `answerText`만 추출 | 답변 텍스트만 내려 프론트에서 바로 표시 |

## 참고
- `NoteMapper`는 `Note`, `Creator`, `NoteCover` 등 연관 엔티티를 조회해 DTO에 필요한 필드를 조립한다.
- `NoteCoverDto`의 `creatorName`/`creatorJobTitle`은 **요청 시 optional**이며, Admin UI 미리보기용. 저장 시에는 `creatorId`만 사용하지만, 응답에서는 매퍼가 `Creator` 엔티티에서 다시 채워 넣는다.
- `cover.categoryBadge`는 **today-preview / today-detail** API에서만 내려오며, 기타 API에는 포함되지 않는다.
- `categoryBadge.type`는 기획에서 정의한 실 카테고리(`MURAL`, `EMOTICON`, `GRAPHIC`, `PRODUCT`, `FASHION`, `THREE_D`, `BRANDING`, `ILLUSTRATION`, `MEDIA_ART`, `FURNITURE`, `THEATER_SIGN`, `LANDSCAPE`, `ALBUM_ARTWORK`, `VISUAL_DIRECTING`, `NONE`) 중 하나다.
- `ArchivedNoteViewResponse`는 `accessible` 플래그를 기준으로 `NoteResponse` 또는 `NotePreviewResponse` 중 하나를 채워 프론트가 단일 API 결과로 분기하도록 설계했다.
