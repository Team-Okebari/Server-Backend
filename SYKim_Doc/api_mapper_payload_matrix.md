# API ↔ Mapper Payload Matrix

> 적용 범위: `note_creator_api_spec.md`에 정의된 **노트/북마크/Admin** API.  
> 목적: 프론트엔드 개발자가 각 API 호출 시 최종적으로 어떤 DTO가 내려오고, `NoteMapper`가 어떤 엔티티 필드를 조합하는지 한 눈에 확인할 수 있도록 정리.

| API | 최종 DTO | 프론트 수신 필드 (Mapper 조립 포함) |
|-----|---------|--------------------------------|
| `GET /api/notes/published/today-cover` | `NoteCoverResponse` | `title`, `teaser`, `mainImageUrl`, `creatorName`, `creatorJobTitle`, `publishedDate` *(카테고리 배지는 미노출)* |
| `GET /api/notes/published/today-preview` | `NotePreviewResponse` | `id`, `cover { title, mainImageUrl, creatorName, creatorJobTitle, publishedDate, categoryBadge? } (teaser=null)`, `overview { sectionTitle, bodyText(<=100자), imageUrl }` |
| `GET /api/notes/published/today-detail` | `TodayPublishedResponse` | `accessible`, `note`(`NoteResponse` 전체), `preview`(`NotePreviewResponse`) |
| `GET /api/notes/archived` | `Page<ArchivedNoteSummaryResponse>` | 항목별 `id`, `tagText`, `title`, `mainImageUrl`, `creatorName`, `publishedDate` |
| `GET /api/notes/archived/{id}` | `ArchivedNoteViewResponse` | `accessible`, `note?`(`NoteResponse` 전체), `preview?`(`NotePreviewResponse` 전체) |
| `POST /api/notes/{noteId}/bookmark` | `{ bookmarked: boolean }` | 토글 결과 |
| `GET /api/notes/bookmarks?keyword` | `BookmarkListItemResponse[]` | 항목별 `noteId`, `title`, `mainImageUrl`, `creatorName`, `tagText` |
| `POST /api/admin/notes` | `Long noteId` | 생성된 ID |
| `PUT /api/admin/notes/{id}` / `GET /api/admin/notes/{id}` / `GET /api/admin/notes` | `NoteResponse` (또는 Page) | 아래 “NoteResponse 전체 필드” 참고 |
| `DELETE /api/admin/notes/{id}` | 없음 | - |
| `POST/PUT/DELETE /api/notes/questions/{id}/answer` | `NoteAnswerResponse` 또는 204 | `answerText` |
| `GET /api/notes/questions/{id}` | `NoteQuestionDto` + `NoteAnswerResponse?` | `questionText`, `answerText?` |

### NoteResponse 전체 필드
- `id`, `status`, `tagText`
- `cover { title, teaser(2.1 한정), mainImageUrl, creatorName, creatorJobTitle, publishedDate, categoryBadge? }`
- `overview { sectionTitle, bodyText, imageUrl }`
- `retrospect { sectionTitle, bodyText }`
- `processes[] { position, sectionTitle, bodyText, imageUrl }`
- `question { questionText }`
- `answer { answerText }`
- `creatorId`, `creatorJobTitle`
- `externalLink { sourceUrl }`
- `creator { id, name, jobTitle, bio, profileImageUrl, instagramUrl, youtubeUrl, behanceUrl, xUrl, blogUrl, newsUrl }`
- `publishedAt`, `archivedAt`, `createdAt`, `updatedAt`

### NotePreviewResponse 전체 필드
- `id`
- `cover { title, mainImageUrl, creatorName, creatorJobTitle, publishedDate, categoryBadge? } (teaser=null)`
- `overview { sectionTitle, bodyText(<=100자), imageUrl }`

### ArchivedNoteSummaryResponse
- `id`, `tagText`, `title`, `mainImageUrl`, `creatorName`, `publishedDate`

### BookmarkListItemResponse
- `noteId`, `title`, `mainImageUrl`, `creatorName`, `tagText`

## 참고 사항
- `NoteCoverDto`의 `creatorName/creatorJobTitle`은 **요청 시 optional**이며 Admin UI 프리뷰 용도. 서버 저장은 `creatorId`로만 처리하지만, 응답 시 매퍼가 `Creator` 엔티티에서 값을 다시 채운다.
- `cover.categoryBadge`는 today-preview/today-detail에서만 내려오며, 다른 API에서는 `null`.
- `categoryBadge.type`는 `NoteCategoryType` enum(`MURAL`, `EMOTICON`, `GRAPHIC`, `PRODUCT`, `FASHION`, `THREE_D`, `BRANDING`, `ILLUSTRATION`, `MEDIA_ART`, `FURNITURE`, `THEATER_SIGN`, `LANDSCAPE`, `ALBUM_ARTWORK`, `VISUAL_DIRECTING`, `NONE`) 중 하나다.
- `ArchivedNoteViewResponse`는 `accessible` 플래그로 프론트가 단일 API 결과를 분기할 수 있도록 설계했다.
- Bookmark 검색은 `NoteBookmarkRepository.searchByUserIdAndKeyword` → `NoteMapper.toBookmarkResponse` → Controller에서 `BookmarkListItemResponse`로 변환한다.
