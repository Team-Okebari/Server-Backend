package com.okebari.artbite.note.controller;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.okebari.artbite.auth.vo.CustomUserDetails;
import com.okebari.artbite.common.dto.CustomApiResponse;
import com.okebari.artbite.note.dto.reminder.NoteReminderResponse;
import com.okebari.artbite.note.service.NoteReminderService;

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

@Tag(name = "Note Reminder", description = "노트 리마인더 관리 API")
@RestController
@RequestMapping("/api/notes/reminder")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class NoteReminderController {

	private final NoteReminderService reminderService;

	@Operation(summary = "금일 리마인더 조회", description = "사용자에게 오늘 노출할 리마인더 정보를 조회합니다. (USER, ADMIN 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "리마인더 조회 성공"),
		@ApiResponse(responseCode = "204", description = "오늘 노출할 리마인더 없음 (리마인더가 없거나 이미 숨김 처리됨)",
			content = @Content),
		@ApiResponse(responseCode = "401", description = "인증 실패",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "Unauthorized", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"),
				mediaType = "application/json")),
		@ApiResponse(responseCode = "403", description = "권한 없음",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "Forbidden", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C003\",\"message\":\"접근 권한이 없습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"),
				mediaType = "application/json"))
	})
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@GetMapping("/today")
	public ResponseEntity<?> getTodayReminder(
		@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
		Long userId = userDetails.getUser().getId();
		Optional<NoteReminderResponse> response = reminderService.getTodayReminder(userId);
		return response
			.map(value -> ResponseEntity.ok(CustomApiResponse.success(value)))
			.orElseGet(() -> ResponseEntity.noContent().build());
	}

	@Operation(summary = "금일 리마인더 해제", description = "오늘 노출된 리마인더를 다시 보지 않도록 설정합니다. (USER, ADMIN 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "204", description = "리마인더 해제 성공",
			content = @Content),
		@ApiResponse(responseCode = "401", description = "인증 실패",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "Unauthorized", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"),
				mediaType = "application/json")),
		@ApiResponse(responseCode = "403", description = "권한 없음",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "Forbidden", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C003\",\"message\":\"접근 권한이 없습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"),
				mediaType = "application/json"))
	})
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@PostMapping("/dismiss")
	public ResponseEntity<Void> dismissToday(
		@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
	) {
		Long userId = userDetails.getUser().getId();
		reminderService.dismissToday(userId);
		return ResponseEntity.noContent().build();
	}
}
