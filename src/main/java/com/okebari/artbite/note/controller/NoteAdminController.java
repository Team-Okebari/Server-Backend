package com.okebari.artbite.note.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.okebari.artbite.auth.vo.CustomUserDetails;
import com.okebari.artbite.common.dto.CustomApiResponse;
import com.okebari.artbite.note.dto.note.NoteCreateRequest;
import com.okebari.artbite.note.dto.note.NoteResponse;
import com.okebari.artbite.note.dto.note.NoteUpdateRequest;
import com.okebari.artbite.note.service.NoteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * ADMIN이 노트를 관리할 때 사용하는 API 컨트롤러.
 */
@Tag(name = "[Admin] Notes", description = "관리자용 노트 관리 API")
@RestController
@RequestMapping("/api/admin/notes")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class NoteAdminController {

	private final NoteService noteService;

	@Operation(summary = "노트 신규 작성", description = "새로운 노트 정보를 생성합니다. (ADMIN 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "노트 생성 성공",
			content = @Content(mediaType = "application/json",
				schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "Success", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":true,\"data\":1,\"error\":null}"))),
		@ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "BadRequest", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C001\",\"message\":\"입력값이 유효하지 않습니다.\"}}"))),
		@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Unauthorized", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"}}"))),
		@ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Forbidden", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C003\",\"message\":\"접근 권한이 없습니다.\"}}")))
	})
	/**
	 * 노트를 신규 작성한다.
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CustomApiResponse<Long> create(
		@AuthenticationPrincipal CustomUserDetails admin,
		@Valid @RequestBody NoteCreateRequest request) {
		Long noteId = noteService.create(request, admin.getUser().getId());
		return CustomApiResponse.success(noteId);
	}

	@Operation(summary = "특정 노트 상세 조회", description = "ID로 특정 노트의 상세 정보를 조회합니다. (ADMIN 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "노트 상세 조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Unauthorized", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"}}"))),
		@ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Forbidden", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C003\",\"message\":\"접근 권한이 없습니다.\"}}"))),
		@ApiResponse(responseCode = "404", description = "노트를 찾을 수 없음", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "NoteNotFound", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"N001\",\"message\":\"노트를 찾을 수 없습니다.\"}}")))
	})
	/**
	 * 특정 노트의 상세 정보를 조회한다.
	 */
	@GetMapping("/{noteId}")
	public CustomApiResponse<NoteResponse> get(@PathVariable Long noteId) {
		return CustomApiResponse.success(noteService.getForAdmin(noteId));
	}

	@Operation(summary = "노트 내용 업데이트", description = "ID로 특정 노트의 내용을 업데이트합니다. (ADMIN 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "노트 업데이트 성공"),
		@ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "BadRequest", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C001\",\"message\":\"입력값이 유효하지 않습니다.\"}}"))),
		@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Unauthorized", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"}}"))),
		@ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Forbidden", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C003\",\"message\":\"접근 권한이 없습니다.\"}}"))),
		@ApiResponse(responseCode = "404", description = "노트를 찾을 수 없음", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "NoteNotFound", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"N001\",\"message\":\"노트를 찾을 수 없습니다.\"}}")))
	})
	/**
	 * 노트 내용을 업데이트한다.
	 */
	@PutMapping("/{noteId}")
	public CustomApiResponse<?> update(
		@PathVariable Long noteId,
		@Valid @RequestBody NoteUpdateRequest request) {
		noteService.update(noteId, request);
		return CustomApiResponse.success();
	}

	@Operation(summary = "노트 삭제", description = "ID로 특정 노트를 삭제합니다. (ADMIN 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "204", description = "노트 삭제 성공", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Success", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":true,\"data\":null,\"error\":null}"))),
		@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Unauthorized", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"}}"))),
		@ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Forbidden", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C003\",\"message\":\"접근 권한이 없습니다.\"}}"))),
		@ApiResponse(responseCode = "404", description = "노트를 찾을 수 없음", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "NoteNotFound", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"N001\",\"message\":\"노트를 찾을 수 없습니다.\"}}")))
	})
	/**
	 * 노트를 삭제한다.
	 */
	@DeleteMapping("/{noteId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long noteId) {
		noteService.delete(noteId);
	}

	@Operation(summary = "관리자 전용 노트 목록 페이징 조회", description = "관리자용 노트 목록을 페이징하여 조회합니다. (ADMIN 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "노트 목록 조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Unauthorized", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"}}"))),
		@ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Forbidden", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C003\",\"message\":\"접근 권한이 없습니다.\"}}")))
	})
	/**
	 * 관리자 전용 노트 목록을 페이징 조회한다.
	 */
	@GetMapping
	public CustomApiResponse<Page<NoteResponse>> list(Pageable pageable) {
		return CustomApiResponse.success(noteService.list(pageable));
	}
}
