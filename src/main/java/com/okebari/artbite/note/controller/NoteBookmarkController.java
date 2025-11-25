package com.okebari.artbite.note.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.okebari.artbite.auth.vo.CustomUserDetails;
import com.okebari.artbite.common.dto.CustomApiResponse;
import com.okebari.artbite.note.dto.bookmark.BookmarkListItemResponse;
import com.okebari.artbite.note.dto.bookmark.NoteBookmarkResponse;
import com.okebari.artbite.note.service.NoteBookmarkService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 노트 북마크 토글/조회 API.
 */
@Tag(name = "Note Bookmark", description = "노트 북마크 토글 및 조회 API")
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class NoteBookmarkController {

	private final NoteBookmarkService noteBookmarkService;

	@Operation(summary = "노트 북마크 토글", description = "특정 노트의 북마크 상태를 토글합니다. (USER, ADMIN 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "북마크 상태 토글 성공",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "Bookmarked", value = "{\"success\":true,\"data\":{\"bookmarked\":true},\"error\":null,\"timestamp\":\"2025-11-25T10:00:00\"}"),
				mediaType = "application/json")),
		@ApiResponse(responseCode = "401", description = "인증 실패",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "Unauthorized", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"),
				mediaType = "application/json")),
		@ApiResponse(responseCode = "403", description = "권한 없음",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "Forbidden", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C003\",\"message\":\"접근 권한이 없습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"),
				mediaType = "application/json")),
		@ApiResponse(responseCode = "404", description = "노트를 찾을 수 없음",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "NoteNotFound", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"N001\",\"message\":\"노트를 찾을 수 없습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"),
				mediaType = "application/json"))
	})
	/**
	 * 노트 북마크 상태를 토글한다.
	 */
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@PostMapping("/{noteId}/bookmark")
	public CustomApiResponse<Map<String, Boolean>> toggle(
		@Parameter(description = "북마크 상태를 토글할 노트의 ID", required = true) @PathVariable Long noteId,
		@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
		boolean bookmarked = noteBookmarkService.toggle(noteId, userDetails.getUser().getId());
		return CustomApiResponse.success(Map.of("bookmarked", bookmarked));
	}

	@Operation(summary = "사용자 북마크 목록 조회", description = "현재 사용자가 북마크한 노트 목록을 조회합니다. 검색 키워드를 통해 필터링할 수 있습니다. (USER, ADMIN 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "북마크 목록 조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "Unauthorized", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"),
				mediaType = "application/json")),
		@ApiResponse(responseCode = "403", description = "권한 없음",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "Forbidden", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C003\",\"message\":\"접근 권한이 없습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"),
				mediaType = "application/json"))
	})
	/**
	 * 현재 사용자가 저장한 북마크 목록을 조회한다.
	 */
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@GetMapping("/bookmarks")
	public CustomApiResponse<List<BookmarkListItemResponse>> bookmarks(
		@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
		@Parameter(description = "검색할 키워드 (노트 제목 또는 작가명)", example = "디자인") @RequestParam(required = false) String keyword) {
		List<NoteBookmarkResponse> raw = noteBookmarkService.list(userDetails.getUser().getId(), keyword);
		List<BookmarkListItemResponse> payload = raw.stream()
			.map(dto -> new BookmarkListItemResponse(
				dto.noteId(),
				dto.title(),
				dto.mainImageUrl(),
				dto.creatorName(),
				dto.tagText()))
			.toList();
		return CustomApiResponse.success(payload);
	}
}
