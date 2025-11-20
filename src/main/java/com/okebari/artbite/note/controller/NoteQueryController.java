package com.okebari.artbite.note.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.okebari.artbite.auth.vo.CustomUserDetails;
import com.okebari.artbite.common.dto.CustomApiResponse;
import com.okebari.artbite.note.dto.note.ArchivedNoteViewResponse;
import com.okebari.artbite.note.dto.note.NoteCoverResponse;
import com.okebari.artbite.note.dto.note.NotePreviewResponse;
import com.okebari.artbite.note.dto.note.TodayPublishedResponse;
import com.okebari.artbite.note.dto.summary.ArchivedNoteSummaryResponse;
import com.okebari.artbite.note.service.NoteQueryService;

import lombok.RequiredArgsConstructor;

/**
 * 메인 및 지난 노트 열람 API.
 */
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteQueryController {

	private final NoteQueryService noteQueryService;

	/**
	 * 온보딩 이후 메인 화면에 노출할 금일 커버.
	 */
	@GetMapping("/published/today-cover")
	public CustomApiResponse<NoteCoverResponse> getTodayCover() {
		return CustomApiResponse.success(noteQueryService.getTodayCover());
	}

	/**
	 * 지난 노트 목록을 검색 조건과 함께 조회한다.
	 */
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@GetMapping("/archived")
	public CustomApiResponse<Page<ArchivedNoteSummaryResponse>> getArchivedNotes(
		@RequestParam(value = "keyword", required = false) String keyword,
		@PageableDefault(sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return CustomApiResponse.success(noteQueryService.getArchivedNoteList(keyword, pageable));
	}

	/**
	 * 지난 노트 상세/프리뷰를 구독 상태에 따라 제공한다.
	 */
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@GetMapping("/archived/{noteId}")
	public CustomApiResponse<ArchivedNoteViewResponse> getArchivedDetail(
		@PathVariable Long noteId,
		@AuthenticationPrincipal CustomUserDetails userDetails) {
		return CustomApiResponse.success(
			noteQueryService.getArchivedNoteView(noteId, userDetails.getUser().getId()));
	}

	/**
	 * 로그인 사용자에게 제공하는 금일 노트 미리보기.
	 */
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@GetMapping("/published/today-preview")
	public CustomApiResponse<NotePreviewResponse> getTodayPreview(
		@AuthenticationPrincipal CustomUserDetails userDetails) {
		return CustomApiResponse.success(noteQueryService.getTodayPreview(userDetails.getUser().getId()));
	}

	/**
	 * 유료 구독자를 위한 금일 게시 노트 상세.
	 */
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@GetMapping("/published/today-detail")
	public CustomApiResponse<TodayPublishedResponse> getTodayPublished(
		@AuthenticationPrincipal CustomUserDetails userDetails) {
		return CustomApiResponse.success(
			noteQueryService.getTodayPublishedDetail(userDetails.getUser().getId()));
	}
}
