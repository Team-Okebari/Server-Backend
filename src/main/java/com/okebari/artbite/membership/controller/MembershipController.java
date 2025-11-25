package com.okebari.artbite.membership.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.okebari.artbite.auth.vo.CustomUserDetails;
import com.okebari.artbite.common.dto.CustomApiResponse;
import com.okebari.artbite.membership.dto.MembershipStatusResponseDto;
import com.okebari.artbite.membership.service.MembershipService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Membership", description = "멤버십 관리 API")
@RestController
@RequestMapping("/api/memberships")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth") // 모든 멤버십 API는 인증 필요
public class MembershipController {

	private final MembershipService membershipService;

	@Operation(summary = "멤버십 취소", description = "현재 사용자의 활성 멤버십을 취소합니다. (USER 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "멤버십 취소 성공"),
		// content = @Content(mediaType = "application/json",
		// schema = @Schema(implementation = CustomApiResponse.class),
		// examples = @ExampleObject(name = "Success", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":true,\"data\":null,\"error\":null}"))),
		@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Unauthorized", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"}}"))),
		@ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Forbidden", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C003\",\"message\":\"접근 권한이 없습니다.\"}}"))),
		@ApiResponse(responseCode = "404", description = "멤버십을 찾을 수 없음", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "MembershipNotFound", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"M001\",\"message\":\"멤버십을 찾을 수 없습니다.\"}}")))
	})
	@PostMapping("/cancel")
	@PreAuthorize("hasRole('USER')")
	public CustomApiResponse<Void> cancelMembership(
		@AuthenticationPrincipal CustomUserDetails customUserDetails) {
		membershipService.cancelMembership(customUserDetails.getUser().getId());
		return CustomApiResponse.success(null);
	}

	@Operation(summary = "취소된 멤버십 재활성화", description = "사용자의 취소된 멤버십을 재활성화합니다. (USER 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "멤버십 재활성화 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Unauthorized", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"}}"))),
		@ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Forbidden", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C003\",\"message\":\"접근 권한이 없습니다.\"}}"))),
		@ApiResponse(responseCode = "404", description = "멤버십을 찾을 수 없음", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "MembershipNotFound", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"M001\",\"message\":\"멤버십을 찾을 수 없습니다.\"}}"))),
		@ApiResponse(responseCode = "409", description = "이미 활성 멤버십 존재", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "AlreadyActive", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"M002\",\"message\":\"이미 활성 멤버십을 가지고 있습니다.\"}}")))
	})
	@PostMapping("/reactivate-canceled")
	@PreAuthorize("hasRole('USER')")
	public CustomApiResponse<MembershipStatusResponseDto> reactivateCanceledMembership(
		@AuthenticationPrincipal CustomUserDetails customUserDetails) {
		MembershipStatusResponseDto responseDto = membershipService.reactivateCanceledMembership(
			customUserDetails.getUser().getId());
		return CustomApiResponse.success(responseDto);
	}

	@Operation(summary = "멤버십 상태 조회", description = "현재 인증된 사용자의 멤버십 상태를 조회합니다. (인증된 사용자 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "멤버십 상태 조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Unauthorized", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"}}"))),
		@ApiResponse(responseCode = "404", description = "멤버십을 찾을 수 없음", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "MembershipNotFound", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"M001\",\"message\":\"멤버십을 찾을 수 없습니다.\"}}")))
	})
	@GetMapping("/status")
	@PreAuthorize("isAuthenticated()")
	public CustomApiResponse<MembershipStatusResponseDto> getMembershipStatus(
		@AuthenticationPrincipal CustomUserDetails customUserDetails) {
		MembershipStatusResponseDto responseDto = membershipService.getMembershipInfo(
			customUserDetails.getUser().getId());
		return CustomApiResponse.success(responseDto);
	}

	@Operation(summary = "멤버십 정지 (관리자용)", description = "특정 사용자의 멤버십을 정지합니다. (ADMIN 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "멤버십 정지 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Unauthorized", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"}}"))),
		@ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Forbidden", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C003\",\"message\":\"접근 권한이 없습니다.\"}}"))),
		@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "UserNotFound", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"A005\",\"message\":\"사용자를 찾을 수 없습니다.\"}}"))),
		@ApiResponse(responseCode = "409", description = "이미 정지된 멤버십", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "AlreadyBanned", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"M003\",\"message\":\"정지된 멤버십입니다.\"}}")))
	})
	@PostMapping("/{userId}/ban")
	@PreAuthorize("hasRole('ADMIN')")
	public CustomApiResponse<Void> banMembership(
		@PathVariable Long userId) {
		membershipService.banMembership(userId);
		return CustomApiResponse.success(null);
	}

	@Operation(summary = "멤버십 정지 해제 (관리자용)", description = "특정 사용자의 멤버십 정지를 해제합니다. (ADMIN 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "멤버십 정지 해제 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Unauthorized", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"}}"))),
		@ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Forbidden", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C003\",\"message\":\"접근 권한이 없습니다.\"}}"))),
		@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "UserNotFound", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"A005\",\"message\":\"사용자를 찾을 수 없습니다.\"}}"))),
		@ApiResponse(responseCode = "400", description = "유효하지 않은 멤버십 상태", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "InvalidStatus", value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"M004\",\"message\":\"유효하지 않은 멤버십 상태입니다.\"}}")))
	})
	@PostMapping("/{userId}/unban")
	@PreAuthorize("hasRole('ADMIN')")
	public CustomApiResponse<Void> unbanMembership(
		@PathVariable Long userId) {
		membershipService.unbanMembership(userId);
		return CustomApiResponse.success(null);
	}
}
