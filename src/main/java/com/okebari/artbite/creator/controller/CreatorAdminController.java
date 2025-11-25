package com.okebari.artbite.creator.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.okebari.artbite.common.dto.CustomApiResponse;
import com.okebari.artbite.creator.dto.CreatorRequest;
import com.okebari.artbite.creator.dto.CreatorResponse;
import com.okebari.artbite.creator.dto.CreatorSummaryDto;
import com.okebari.artbite.creator.service.CreatorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "[Admin] Creators", description = "관리자용 작가 관리 API")
@RestController
@RequestMapping("/api/admin/creators")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
/**
 * ADMIN 작가 관리용 REST 컨트롤러.
 * 노트 작성 화면이 참조할 목록/상세 API와 관리자 CRUD를 제공한다.
 */
public class CreatorAdminController {

	private final CreatorService creatorService;

	@Operation(summary = "작가 생성", description = "새로운 작가 정보를 생성합니다. (ADMIN 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "작가 생성 성공", content = @Content(mediaType = "application/json",
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
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CustomApiResponse<Long> create(@Valid @RequestBody CreatorRequest request) {
		return CustomApiResponse.success(creatorService.create(request));
	}

	@Operation(summary = "모든 작가 목록 조회", description = "모든 작가 정보 목록을 요약하여 조회합니다. (ADMIN 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "작가 목록 조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Unauthorized", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"}}"))),
		@ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Forbidden", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C003\",\"message\":\"접근 권한이 없습니다.\"}}")))
	})
	@GetMapping
	public CustomApiResponse<List<CreatorSummaryDto>> list() {
		return CustomApiResponse.success(creatorService.list());
	}

	@Operation(summary = "특정 작가 상세 조회", description = "ID로 특정 작가의 상세 정보를 조회합니다. (ADMIN 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "작가 상세 조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Unauthorized", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"}}"))),
		@ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Forbidden", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C003\",\"message\":\"접근 권한이 없습니다.\"}}"))),
		@ApiResponse(responseCode = "404", description = "작가를 찾을 수 없음", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "CreatorNotFound", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"CR001\",\"message\":\"작가 정보를 찾을 수 없습니다.\"}}")))
	})
	@GetMapping("/{creatorId}")
	public CustomApiResponse<CreatorResponse> get(@PathVariable Long creatorId) {
		return CustomApiResponse.success(creatorService.get(creatorId));
	}

	@Operation(summary = "작가 정보 수정", description = "ID로 특정 작가 정보를 수정합니다. (ADMIN 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "작가 수정 성공"),
		@ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "BadRequest", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C001\",\"message\":\"입력값이 유효하지 않습니다.\"}}"))),
		@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Unauthorized", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"}}"))),
		@ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Forbidden", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C003\",\"message\":\"접근 권한이 없습니다.\"}}"))),
		@ApiResponse(responseCode = "404", description = "작가를 찾을 수 없음", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "CreatorNotFound", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"CR001\",\"message\":\"작가 정보를 찾을 수 없습니다.\"}}")))
	})
	@PutMapping("/{creatorId}")
	public CustomApiResponse<Void> update(
		@PathVariable Long creatorId,
		@Valid @RequestBody CreatorRequest request) {
		creatorService.update(creatorId, request);
		return CustomApiResponse.success(null);
	}

	@Operation(summary = "작가 정보 삭제", description = "ID로 특정 작가 정보를 삭제합니다. (ADMIN 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "작가 삭제 성공",
			content = @Content(mediaType = "application/json",
				schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "Success", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":true,\"data\":null,\"error\":null}"))),
		@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Unauthorized", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"}}"))),
		@ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Forbidden", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C003\",\"message\":\"접근 권한이 없습니다.\"}}"))),
		@ApiResponse(responseCode = "404", description = "작가를 찾을 수 없음", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "CreatorNotFound", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"CR001\",\"message\":\"작가 정보를 찾을 수 없습니다.\"}}")))
	})
	@DeleteMapping("/{creatorId}")
	public CustomApiResponse<Void> delete(@PathVariable Long creatorId) {
		creatorService.delete(creatorId);
		return CustomApiResponse.success(null);
	}
}

