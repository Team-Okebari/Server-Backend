package com.okebari.artbite.note.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.okebari.artbite.auth.vo.CustomUserDetails;
import com.okebari.artbite.common.dto.CustomApiResponse;
import com.okebari.artbite.note.dto.bookmark.BookmarkListItemResponse;
import com.okebari.artbite.note.dto.bookmark.NoteBookmarkResponse;
import com.okebari.artbite.note.service.NoteBookmarkService;

import lombok.RequiredArgsConstructor;

/**
 * 노트 북마크 토글/조회 API.
 */
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteBookmarkController {

	private final NoteBookmarkService noteBookmarkService;

	/**
	 * 노트 북마크 상태를 토글한다.
	 */
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@PostMapping("/{noteId}/bookmark")
	public CustomApiResponse<Map<String, Boolean>> toggle(
		@PathVariable Long noteId,
		@AuthenticationPrincipal CustomUserDetails userDetails) {
		boolean bookmarked = noteBookmarkService.toggle(noteId, userDetails.getUser().getId());
		return CustomApiResponse.success(Map.of("bookmarked", bookmarked));
	}

	/**
	 * 현재 사용자가 저장한 북마크 목록을 조회한다.
	 */
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@GetMapping("/bookmarks")
	public CustomApiResponse<List<BookmarkListItemResponse>> bookmarks(
		@AuthenticationPrincipal CustomUserDetails userDetails) {
		List<NoteBookmarkResponse> raw = noteBookmarkService.list(userDetails.getUser().getId());
		List<BookmarkListItemResponse> payload = raw.stream()
			.map(dto -> new BookmarkListItemResponse(
				dto.title(),
				dto.mainImageUrl(),
				dto.creatorName(),
				dto.creatorJobTitle()))
			.toList();
		return CustomApiResponse.success(payload);
	}
}
