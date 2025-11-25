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
 * 메인 및 지난 노트 열람 API.
 */
@Tag(name = "Note Query", description = "메인 및 지난 노트 열람 API")
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteQueryController {

	private final NoteQueryService noteQueryService;

	@Operation(summary = "금일 발행 노트 커버 조회", description = "온보딩 이후 메인 화면(비로그인)에 노출할 금일 발행된 노트의 커버 정보를 조회합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "404", description = "금일 발행된 노트 없음",
			content = @Content(mediaType = "application/json",
				schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "NoteNotFound",
					value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"N001\",\"message\":\"노트를 찾을 수 없습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}")))
	})
	/**
	 * 온보딩 이후 메인 화면에 노출할 금일 커버.
	 */
	@GetMapping("/published/today-cover")
	public CustomApiResponse<NoteCoverResponse> getTodayCover() {
		return CustomApiResponse.success(noteQueryService.getTodayCover());
	}

	@Operation(summary = "아카이브된 노트 목록 조회", description = "지난 노트(아카이브) 목록을 검색 조건과 함께 페이지네이션하여 조회합니다. (USER, ADMIN 권한 필요)")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(examples = @ExampleObject(name = "Unauthorized", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"))),
		@ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(examples = @ExampleObject(name = "Forbidden", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C003\",\"message\":\"접근 권한이 없습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}")))
	})
	/**
	 * 지난 노트 목록을 검색 조건과 함께 조회한다.
	 */
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@GetMapping("/archived")
	public CustomApiResponse<Page<ArchivedNoteSummaryResponse>> getArchivedNotes(
		@Parameter(description = "검색할 키워드 (제목 또는 작가명)", example = "디자인") @RequestParam(value = "keyword", required = false) String keyword,
		@Parameter(hidden = true) @PageableDefault(sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return CustomApiResponse.success(noteQueryService.getArchivedNoteList(keyword, pageable));
	}

	@Operation(summary = "아카이브된 노트 상세 조회", description = "특정 아카이브 노트의 상세 정보를 조회합니다. 사용자의 구독 상태에 따라 전체 내용 또는 미리보기만 제공됩니다. (USER, ADMIN 권한 필요)")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(examples = @ExampleObject(name = "Unauthorized", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"))),
		@ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(examples = @ExampleObject(name = "Forbidden", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C003\",\"message\":\"접근 권한이 없습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"))),
		@ApiResponse(responseCode = "404", description = "노트 없음", content = @Content(examples = @ExampleObject(name = "NoteNotFound", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"N001\",\"message\":\"노트를 찾을 수 없습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}")))
	})
	/**
	 * 지난 노트 상세/프리뷰를 구독 상태에 따라 제공한다.
	 */
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@GetMapping("/archived/{noteId}")
	public CustomApiResponse<ArchivedNoteViewResponse> getArchivedDetail(
		@Parameter(description = "조회할 노트의 ID", required = true) @PathVariable Long noteId,
		@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
		return CustomApiResponse.success(
			noteQueryService.getArchivedNoteView(noteId, userDetails.getUser().getId()));
	}

	@Operation(summary = "금일 발행 노트 미리보기 조회", description = "로그인한 사용자를 위해 금일 발행된 노트의 미리보기 정보를 조회합니다. (USER, ADMIN 권한 필요)")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(examples = @ExampleObject(name = "Unauthorized", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"))),
		@ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(examples = @ExampleObject(name = "Forbidden", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C003\",\"message\":\"접근 권한이 없습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"))),
		@ApiResponse(responseCode = "404", description = "노트 없음", content = @Content(examples = @ExampleObject(name = "NoteNotFound", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"N001\",\"message\":\"노트를 찾을 수 없습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}")))
	})
	/**
	 * 로그인 사용자에게 제공하는 금일 노트 미리보기.
	 */
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@GetMapping("/published/today-preview")
	public CustomApiResponse<NotePreviewResponse> getTodayPreview(
		@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
		return CustomApiResponse.success(noteQueryService.getTodayPreview(userDetails.getUser().getId()));
	}

	@Operation(summary = "금일 발행 노트 상세 조회 (구독자용)", description = "유료 구독자를 위해 금일 발행된 노트의 전체 상세 정보를 조회합니다. 사용자의 구독 상태에 따라 전체 내용 또는 미리보기가 제공됩니다. (USER, ADMIN 권한 필요)")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(examples = @ExampleObject(name = "Unauthorized", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"))),
		@ApiResponse(responseCode = "403", description = "구독하지 않은 사용자", content = @Content(examples = @ExampleObject(name = "Forbidden", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"N003\",\"message\":\"노트에 접근할 권한이 없습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"))),
		@ApiResponse(responseCode = "404", description = "노트 없음", content = @Content(examples = @ExampleObject(name = "NoteNotFound", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"N001\",\"message\":\"노트를 찾을 수 없습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}")))
	})
	/**
	 * 유료 구독자를 위한 금일 게시 노트 상세.
	 */
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@GetMapping("/published/today-detail")
	public CustomApiResponse<TodayPublishedResponse> getTodayPublished(
		@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
		return CustomApiResponse.success(
			noteQueryService.getTodayPublishedDetail(userDetails.getUser().getId()));
	}
}
