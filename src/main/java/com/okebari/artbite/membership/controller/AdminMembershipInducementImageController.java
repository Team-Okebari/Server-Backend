package com.okebari.artbite.membership.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.okebari.artbite.common.dto.CustomApiResponse;
import com.okebari.artbite.membership.dto.MembershipInducementImageUpdateRequestDto;
import com.okebari.artbite.membership.service.MembershipInducementImageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "[Admin] Membership Inducement Image", description = "관리자용 멤버십 유도 이미지 관리 API")
@RestController
@RequestMapping("/api/admin/membership-inducement-image")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth") // 모든 ADMIN API는 인증 필요
public class AdminMembershipInducementImageController {

	private final MembershipInducementImageService membershipInducementImageService;

	@Operation(summary = "멤버십 유도 이미지 URL 업데이트", description = "메인 화면에 표시될 멤버십 유도 이미지의 URL을 업데이트합니다. (ADMIN 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "멤버십 유도 이미지 URL 업데이트 성공"),
		@ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "BadRequest", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C001\",\"message\":\"Image URL은 필수입니다.\"}}"))),
		@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Unauthorized", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"}}"))),
		@ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Forbidden", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C003\",\"message\":\"접근 권한이 없습니다.\"}}")))
	})
	@PutMapping
	@PreAuthorize("hasRole('ADMIN')")
	public CustomApiResponse<Void> updateMembershipInducementImage(
		@Valid @RequestBody MembershipInducementImageUpdateRequestDto requestDto) {
		membershipInducementImageService.updateInducementImageUrl(requestDto.getImageUrl());
		return CustomApiResponse.success(null);
	}
}
