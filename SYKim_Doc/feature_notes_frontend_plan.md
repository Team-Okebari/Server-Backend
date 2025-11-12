# 노트 기능 프론트엔드 기본 구성안

노트 도메인 API(`NoteAdminController`, `NoteQueryController`)에 대응하는 최소한의 프론트엔드 구조를 정리합니다. 기술 스택은 React + TypeScript + Vite(또는 CRA) + React Query + Axios 조합을 기준으로 합니다.

## 1. 프로젝트 구조

```
front/
├─ src/
│  ├─ api/
│  │  ├─ axiosInstance.ts      // 공통 Axios 설정 (baseURL, interceptor 등)
│  │  └─ notesApi.ts           // 노트 관련 API 래퍼
│  ├─ components/
│  │  ├─ admin/
│  │  │  ├─ NoteForm.tsx       // create/update 폼
│  │  │  └─ NoteStatusBadge.tsx
│  │  ├─ public/
│  │  │  ├─ PublishedNoteCard.tsx
│  │  │  └─ ArchivedNoteCard.tsx
│  │  └─ common/
│  │     └─ Pagination.tsx
│  ├─ pages/
│  │  ├─ admin/
│  │  │  ├─ NoteListPage.tsx   // 노트 목록
│  │  │  ├─ NoteCreatePage.tsx
│  │  │  ├─ NoteEditPage.tsx
│  │  │  ├─ CreatorListPage.tsx  // 작가 관리
│  │  │  └─ CreatorFormPage.tsx
│  │  ├─ public/
│  │  │  ├─ PublishedListPage.tsx
│  │  │  └─ ArchivedListPage.tsx
│  │  └─ auth/…                // 기존 인증 페이지 재사용
│  ├─ hooks/
│  │  ├─ useNotes.ts           // 노트 관련 React Query 훅
│  │  └─ useCreators.ts        // 작가 관리 React Query 훅
│  ├─ routes/
│  │  └─ AppRoutes.tsx         // 라우팅 구성
│  ├─ types/
│  │  └─ notes.ts              // DTO 기반 타입 정의
│  ├─ utils/
│  │  └─ formatDate.ts
│  ├─ App.tsx
│  └─ main.tsx
├─ public/
│  └─ index.html
└─ package.json
```

## 2. 공통 타입 정의 (`types/notes.ts`)

```ts
export type NoteStatus = 'IN_PROGRESS' | 'COMPLETED' | 'PUBLISHED' | 'ARCHIVED';

export interface NoteProcessDto {
  position: number;
  sectionTitle: string;
  bodyText: string;
  imageUrl: string;
}

export interface NoteCoverDto {
  title: string;
  teaser: string;
  mainImageUrl: string;
  creatorName?: string;
  creatorJobTitle?: string;
}

export interface NoteCoverResponse extends NoteCoverDto {
  publishedDate?: string;
}

export interface NoteOverviewDto {
  sectionTitle: string;
  bodyText: string;
  imageUrl: string;
}

export interface NoteRetrospectDto {
  sectionTitle: string;
  bodyText: string;
}

export interface NoteQuestionDto {
  questionText: string;
}

export interface NoteAnswerRequest {
  answerText: string;
}

export interface NoteAnswerResponse {
  answerText: string;
}

export interface NoteExternalLinkDto {
  sourceUrl?: string;
}

export interface CreatorSummaryDto {
  id: number;
  name: string;
  bio?: string;
  jobTitle?: string;
  profileImageUrl?: string;
  instagramUrl?: string;
  youtubeUrl?: string;
  behanceUrl?: string;
  xUrl?: string;
  blogUrl?: string;
  newsUrl?: string;
}

export interface CreatorRequest {
  name: string;
  bio?: string;
  jobTitle?: string;
  profileImageUrl?: string;
  instagramUrl?: string;
  youtubeUrl?: string;
  behanceUrl?: string;
  xUrl?: string;
  blogUrl?: string;
  newsUrl?: string;
}

- 관리자 폼에서는 `jobTitle`을 직함 입력란으로, `bio`는 상세 소개(긴 문장) 입력란으로 구분해 사용한다.

export type CreatorResponse = CreatorSummaryDto;

export interface NoteCreateRequest {
  status: NoteStatus;
  tagText?: string;
  cover: NoteCoverDto;
  overview: NoteOverviewDto;
  retrospect: NoteRetrospectDto;
  processes: NoteProcessDto[];
  question?: NoteQuestionDto;
  creatorId: number;
  externalLink?: NoteExternalLinkDto;
}

export interface NoteResponse {
  id: number;
  status: NoteStatus;
  tagText?: string;
  cover: NoteCoverResponse;
  overview: NoteOverviewDto;
  retrospect: NoteRetrospectDto;
  processes: NoteProcessDto[];
  question?: NoteQuestionDto;
  creatorId: number;
  creatorJobTitle?: string;
  externalLink?: NoteExternalLinkDto;
  publishedAt?: string;
  archivedAt?: string;
  createdAt: string;
  updatedAt: string;
  creator?: CreatorSummaryDto;
  answer?: NoteAnswerResponse;
}
- 상세 페이지는 `creatorId`를 통해 작가 상세/프로필을 추가 요청하므로 값이 필수다.

export interface NotePreviewResponse {
  id: number;
  cover: NoteCoverResponse;
  overview: NoteOverviewDto | null;
}

// overview.bodyText는 백엔드에서 100자로 잘려 내려오며, preview 응답에는 creator/externalLink가 포함되지 않는다.

export interface TodayPublishedResponse {
  accessible: boolean;
  note: NoteResponse | null;
  preview: NotePreviewResponse | null;
}

export interface ArchivedNoteViewResponse {
  accessible: boolean;
  note: NoteResponse | null;
  preview: NotePreviewResponse | null;
}

export interface ArchivedNoteSummaryResponse {
  id: number;
  tagText?: string;
  title?: string;
  mainImageUrl?: string;
  creatorName?: string;
  publishedDate?: string; // yyyy-MM-dd
}

export interface BookmarkListItemResponse {
  noteId: number;
  title: string;
  mainImageUrl: string;
  creatorName: string;
  tagText?: string;
}
```
- 북마크 목록은 노트 상세 이동을 위해 `noteId`만 필요하며, 작가 직함은 노출하지 않는다. `keyword` 파라미터로 제목/작가/태그 검색이 가능하며, `tagText`는 검색 결과 하이라이트용으로 활용할 수 있다.

## 3. Axios 인스턴스 (`api/axiosInstance.ts`)

```ts
import axios from 'axios';

export const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  withCredentials: true,
});

axiosInstance.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

## 4. API 래퍼 (`api/notesApi.ts`)

```ts
import { axiosInstance } from './axiosInstance';
import {
  NoteCreateRequest,
  NoteResponse,
  NoteCoverResponse,
  ArchivedNoteSummaryResponse,
  BookmarkListItemResponse,
  NoteAnswerRequest,
  NoteAnswerResponse,
} from '../types/notes';

const ADMIN_PREFIX = '/api/admin/notes';
const CREATOR_PREFIX = '/api/admin/creators';
const PUBLIC_PREFIX = '/api/notes';

export const notesApi = {
  // 관리자 API
  create: (payload: NoteCreateRequest) =>
    axiosInstance.post<number>(ADMIN_PREFIX, payload),

  get: (noteId: number) =>
    axiosInstance.get<NoteResponse>(`${ADMIN_PREFIX}/${noteId}`),

  update: (noteId: number, payload: NoteCreateRequest) =>
    axiosInstance.put<void>(`${ADMIN_PREFIX}/${noteId}`, payload),

  remove: (noteId: number) =>
    axiosInstance.delete<void>(`${ADMIN_PREFIX}/${noteId}`),

  list: (params: { page?: number; size?: number }) =>
    axiosInstance.get<{ content: NoteResponse[]; totalElements: number; totalPages: number }>(
      ADMIN_PREFIX,
      { params },
    ),

  // 공개/아카이브 API
  fetchPublishedCover: () =>
    axiosInstance.get<NoteCoverResponse>(`${PUBLIC_PREFIX}/published/today-cover`),

  fetchArchived: (params: { keyword?: string; page?: number; size?: number }) =>
    axiosInstance.get<{ content: ArchivedNoteSummaryResponse[]; totalElements: number; totalPages: number }>(
      `${PUBLIC_PREFIX}/archived`,
      { params },
    ),

  fetchArchivedDetail: (noteId: number) =>
    axiosInstance.get<NoteResponse>(`${PUBLIC_PREFIX}/archived/${noteId}`),

  toggleBookmark: (noteId: number) =>
    axiosInstance.post<{ bookmarked: boolean }>(`${PUBLIC_PREFIX}/${noteId}/bookmark`),

  fetchBookmarks: () =>
    axiosInstance.get<BookmarkListItemResponse[]>(`${PUBLIC_PREFIX}/bookmarks`),

  createAnswer: (questionId: number, payload: NoteAnswerRequest) =>
    axiosInstance.post<NoteAnswerResponse>(`${PUBLIC_PREFIX}/questions/${questionId}/answer`, payload),

  updateAnswer: (questionId: number, payload: NoteAnswerRequest) =>
    axiosInstance.put<NoteAnswerResponse>(`${PUBLIC_PREFIX}/questions/${questionId}/answer`, payload),

  deleteAnswer: (questionId: number) =>
    axiosInstance.delete<void>(`${PUBLIC_PREFIX}/questions/${questionId}/answer`),
};

export const creatorApi = {
  list: () => axiosInstance.get<CreatorResponse[]>(CREATOR_PREFIX),
  get: (creatorId: number) => axiosInstance.get<CreatorResponse>(`${CREATOR_PREFIX}/${creatorId}`),
  create: (payload: CreatorRequest) => axiosInstance.post<number>(CREATOR_PREFIX, payload),
  update: (creatorId: number, payload: CreatorRequest) =>
    axiosInstance.put<void>(`${CREATOR_PREFIX}/${creatorId}`, payload),
  remove: (creatorId: number) => axiosInstance.delete<void>(`${CREATOR_PREFIX}/${creatorId}`),
};
```

## 5. React Query 훅 (`hooks/useNotes.ts`)

```ts
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { notesApi, creatorApi } from '../api/notesApi';
import { CreatorRequest, NoteCreateRequest, NoteAnswerRequest } from '../types/notes';

export const noteKeys = {
  published: ['notes', 'published'] as const,
  archived: ['notes', 'archived'] as const,
  adminList: (page: number) => ['notes', 'admin', page] as const,
  bookmarks: ['notes', 'bookmarks'] as const,
  detail: (id: number) => ['notes', 'detail', id] as const,
  question: (id: number) => ['notes', 'question', id] as const,
};

export const creatorKeys = {
  all: ['creators'] as const,
  detail: (id: number) => ['creators', id] as const,
};

export const useCreators = () =>
  useQuery({
    queryKey: creatorKeys.all,
    queryFn: async () => (await creatorApi.list()).data,
  });

export const useCreateCreator = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreatorRequest) => creatorApi.create(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: creatorKeys.all }),
  });
};

export const useUpdateCreator = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: CreatorRequest }) =>
      creatorApi.update(id, payload),
    onSuccess: (_void, variables) => {
      queryClient.invalidateQueries({ queryKey: creatorKeys.all });
      queryClient.invalidateQueries({ queryKey: creatorKeys.detail(variables.id) });
    },
  });
};

export const useDeleteCreator = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => creatorApi.remove(id),
    onSuccess: (_void, id) => {
      queryClient.invalidateQueries({ queryKey: creatorKeys.all });
      queryClient.invalidateQueries({ queryKey: creatorKeys.detail(id) });
    },
  });
};

export const usePublishedNotes = () =>
  useQuery({
    queryKey: noteKeys.published,
    queryFn: async () => (await notesApi.fetchPublishedCover()).data,
  });

export const useArchivedNotes = (params: { keyword?: string; page?: number; size?: number }) =>
  useQuery({
    queryKey: [...noteKeys.archived, params.keyword ?? '', params.page ?? 0, params.size ?? 10],
    queryFn: async () => (await notesApi.fetchArchived(params)).data,
  });

export const useArchivedDetail = (noteId: number) =>
  useQuery({
    queryKey: noteKeys.detail(noteId),
    queryFn: async () => (await notesApi.fetchArchivedDetail(noteId)).data,
    enabled: !!noteId,
  });

export const useBookmarks = () =>
  useQuery({
    queryKey: noteKeys.bookmarks,
    queryFn: async () => (await notesApi.fetchBookmarks()).data,
  });

export const useAdminNoteList = (page: number) =>
  useQuery({
    queryKey: noteKeys.adminList(page),
    queryFn: async () => (await notesApi.list({ page, size: 10 })).data,
  });

export const useCreateNote = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: NoteCreateRequest) => notesApi.create(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: noteKeys.adminList(0) });
      queryClient.invalidateQueries({ queryKey: noteKeys.published });
    },
  });
};

export const useToggleBookmark = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (noteId: number) => notesApi.toggleBookmark(noteId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: noteKeys.bookmarks });
      queryClient.invalidateQueries({ queryKey: noteKeys.archived });
      queryClient.invalidateQueries({ queryKey: noteKeys.published });
    },
  });
};

export const useCreateAnswer = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ questionId, payload }: { questionId: number; payload: NoteAnswerRequest }) =>
      notesApi.createAnswer(questionId, payload),
    onSuccess: (_response, variables) => {
      queryClient.invalidateQueries({ queryKey: noteKeys.question(variables.questionId) });
    },
  });
};

export const useUpdateAnswer = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ questionId, payload }: { questionId: number; payload: NoteAnswerRequest }) =>
      notesApi.updateAnswer(questionId, payload),
    onSuccess: (_response, variables) => {
      queryClient.invalidateQueries({ queryKey: noteKeys.question(variables.questionId) });
    },
  });
};

export const useDeleteAnswer = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (questionId: number) => notesApi.deleteAnswer(questionId),
    onSuccess: (_response, variables) => {
      queryClient.invalidateQueries({ queryKey: noteKeys.question(variables) });
    },
  });
};
```

## 6. 관리자 목록 페이지 (`pages/admin/NoteListPage.tsx`)

```tsx
import { useState } from 'react';
import { useAdminNoteList } from '../../hooks/useNotes';
import Pagination from '../../components/common/Pagination';
import noteStatusLabel from '../../utils/noteStatusLabel';

const NoteListPage = () => {
  const [page, setPage] = useState(0);
  const { data, isLoading } = useAdminNoteList(page);

  if (isLoading) return <div>Loading...</div>;
  if (!data) return <div>No data</div>;

  return (
    <div>
      <h1>노트 목록</h1>
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Status</th>
            <th>Tag</th>
            <th>Creator</th>
            <th>Updated</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          {data.content.map((note) => (
            <tr key={note.id}>
              <td>{note.id}</td>
              <td>{noteStatusLabel(note.status)}</td>
              <td>{note.tagText}</td>
              <td>{note.creator?.name ?? '-'}</td>
              <td>{note.updatedAt}</td>
              <td>
                {/* 링크 버튼 예시 */}
                <a href={`/admin/notes/${note.id}`}>Edit</a>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <Pagination
        currentPage={page}
        totalItems={data.totalElements}
        onPageChange={setPage}
      />
    </div>
  );
};

export default NoteListPage;
```

## 7. 공개 목록 컴포넌트 (`components/public/PublishedNoteCard.tsx`)

```tsx
import { NoteCoverResponse } from '../../types/notes';

interface Props {
  cover: NoteCoverResponse;
}

const PublishedNoteCard = ({ cover }: Props) => (
  <article>
    <img src={cover.mainImageUrl} alt={cover.title} />
    <h2>{cover.title}</h2>
    <p>{cover.teaser}</p>
    <small>
      {cover.creatorName}
      {cover.creatorJobTitle ? ` · ${cover.creatorJobTitle}` : ''}
    </small>
    <small>{cover.publishedDate}</small>
  </article>
);

export default PublishedNoteCard;
```

- `creatorJobTitle`는 작가 소개 한 줄(Bio)이며, 이름 옆에 점 구분자로 함께 노출한다.

## 8. 지난 노트 목록 페이지 (`pages/public/ArchivedListPage.tsx`)

```tsx
import { useArchivedNotes } from '../../hooks/useNotes';
import ArchivedNoteCard from '../../components/public/ArchivedNoteCard';

const ArchivedListPage = () => {
  const [page, setPage] = useState(0);
  const [keyword, setKeyword] = useState('');
  const { data, isLoading } = useArchivedNotes({ keyword, page, size: 12 });

  if (isLoading) return <div>Loading...</div>;
  if (!data) return <div>No data</div>;

  return (
    <section>
      <h1>지난 노트</h1>
      <input
        value={keyword}
        onChange={(e) => setKeyword(e.target.value)}
        placeholder="제목 / 태그 / 작가 검색"
      />
      <div className="grid">
        {data.content.map((note) => (
          <ArchivedNoteCard key={note.id} note={note} />
        ))}
      </div>
      <Pagination
        currentPage={page}
        totalItems={data.totalElements}
        onPageChange={setPage}
      />
    </section>
  );
};

export default ArchivedListPage;
```

## 9. 라우팅 (`routes/AppRoutes.tsx`)

```tsx
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import NoteListPage from '../pages/admin/NoteListPage';
import NoteCreatePage from '../pages/admin/NoteCreatePage';
import NoteEditPage from '../pages/admin/NoteEditPage';
import PublishedListPage from '../pages/public/PublishedListPage';
import ArchivedListPage from '../pages/public/ArchivedListPage';
import ArchivedDetailPage from '../pages/public/ArchivedDetailPage';

const AppRoutes = () => (
  <BrowserRouter>
    <Routes>
      {/* 관리자 영역 */}
      <Route path="/admin/notes" element={<NoteListPage />} />
      <Route path="/admin/notes/new" element={<NoteCreatePage />} />
      <Route path="/admin/notes/:noteId" element={<NoteEditPage />} />

      {/* 공개 영역 */}
      <Route path="/" element={<PublishedListPage />} />
      <Route path="/notes/archived" element={<ArchivedListPage />} />
      <Route path="/notes/archived/:noteId" element={<ArchivedDetailPage />} />
    </Routes>
  </BrowserRouter>
);

export default AppRoutes;
```

## 10. 인증/권한 처리
- 관리자 페이지는 라우터 가드(HOC 또는 `RequireAuth` 컴포넌트)로 `ROLE_ADMIN` 토큰 여부 확인.
- 아카이브 상세 페이지는 상세 API 요청 시 403이 오면 구독 결제 페이지로 안내.

## 11. 스타일 가이드
- UI 라이브러리: Material UI 또는 TailwindCSS.
- 레이아웃 예시: 메인 페이지는 카드 그리드, 지난 노트는 썸네일 목록.
- 반응형: `mainImageUrl` 존재 여부에 따라 썸네일 배치.

## 12. 환경 변수
```
VITE_API_BASE_URL=http://localhost:8080
```

## 13. 빌드/배포
- `npm install`, `npm run dev` 로컬 개발.
- `npm run build` 후 `dist/` 정적 파일을 Nginx 또는 CloudFront에 배포.
- Dockerfile 예시:
```Dockerfile
FROM node:18 AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:stable-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
```

## 14. TODO
- 관리자 폼 UI 설계(프로세스 2개 입력, 이미지 업로드).
- 작가 선택 드롭다운(creatorId) 및 관련 링크 입력 필드 구성. creatorId는 필수값이므로 드롭다운 기본 선택/검증을 추가한다.
- 작가 목록 조회 API(`GET /api/admin/creators` 가정) 연동 및 신규 작가 등록 플로우 별도 정의 필요.
- 노트 상세 화면은 `GET /api/notes/published/today-preview`로 커버/개요 100자를 먼저 렌더링하고, **유료 구독자**인 경우 `GET /api/notes/archived/{noteId}` 결과로 나머지 섹션을 덮어쓴다. **무료 사용자**는 `GET /api/notes/archived/{noteId}/preview`로 전용 프리뷰(커버+개요)만 노출한 뒤 구독 CTA로 이동한다.
- 메인 홈 화면은 `GET /api/notes/published/today-cover`로 금일 게시된 노트의 커버 데이터를 받아 히어로 섹션을 구성하고, 상세 페이지 진입 시 `GET /api/notes/published/today-preview` → (구독자라면) `GET /api/notes/published/today-detail` 또는 `GET /api/notes/archived/{noteId}` / (비구독자라면) `GET /api/notes/archived/{noteId}/preview` 순으로 로딩한다.
- `GET /api/notes/published/today-detail` 응답(`TodayPublishedResponse`)에서 `accessible` 값이 `false`이면 `preview` 필드만 내려오므로, 프리뷰 전용 UI를 그대로 유지하거나 멤버십 가입 유도 동작을 수행한다. `accessible`이 `true`인 경우에만 `note` 필드의 `NoteResponse` 데이터를 상세 뷰에 바인딩한다.
- 상태 전환 UX: COMPLETED → IN_PROGRESS 버튼 제공.
- 구독 결제 연동 플로우 확인(403 처리).
- 테스트: Vitest + React Testing Library로 주요 컴포넌트 스냅샷.

## 15. 북마크 화면/훅 초안

```ts
// 북마크 응답 DTO는 note 의 요약정보를 포함한다.
export interface BookmarkListItemResponse {
  title: string;
  mainImageUrl: string;
  creatorName: string;
  creatorJobTitle?: string;
}

// API 래퍼 추가 함수 (기존 notesApi 에 주석과 함께 삽입 권장)
export const toggleBookmark = (noteId: number) =>
  axiosInstance.post<{ bookmarked: boolean }>(`/api/notes/${noteId}/bookmark`);

export const fetchBookmarks = () =>
  axiosInstance.get<BookmarkListItemResponse[]>(`/api/notes/bookmarks`);

// React Query 훅 예시
export const useBookmarks = () =>
  useQuery({
    queryKey: ['notes', 'bookmarks'],
    queryFn: async () => (await fetchBookmarks()).data,
  });

export const useToggleBookmark = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (noteId: number) => toggleBookmark(noteId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notes', 'bookmarks'] });
      queryClient.invalidateQueries({ queryKey: ['notes', 'archived'] });
      queryClient.invalidateQueries({ queryKey: ['notes', 'published'] });
    },
  });
};

// 북마크 목록 페이지 초안
const BookmarkListPage = () => {
  const { data, isLoading } = useBookmarks();
  if (isLoading) return <div>Loading...</div>;
  if (!data || data.length === 0) return <div>북마크 내역이 없습니다.</div>;

  return (
    <section>
      <h1>내 북마크</h1>
      <div className="grid">
        {data.map((item) => (
          <PublishedNoteCard key={item.bookmarkId} note={item.summary} />
        ))}
      </div>
    </section>
  );
};
```

TODO에 “북마크 화면/토글 버튼 UX” 항목을 추가해 실제 구현 시 반영합니다.
