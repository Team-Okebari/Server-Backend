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

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/memberships")
@RequiredArgsConstructor
public class MembershipController {

	private final MembershipService membershipService;

	@PostMapping("/cancel")
	@PreAuthorize("hasRole('USER')") // 멤버만 자신의 멤버십을 취소할 수 있습니다.
	public CustomApiResponse<Void> cancelMembership(
		@AuthenticationPrincipal CustomUserDetails customUserDetails) {
		membershipService.cancelMembership(customUserDetails.getUser().getId());
		return CustomApiResponse.success(null);
	}

	@GetMapping("/status")
	@PreAuthorize("isAuthenticated()") // 인증된 모든 사용자는 자신의 멤버십 상태를 확인할 수 있습니다.
	public CustomApiResponse<MembershipStatusResponseDto> getMembershipStatus(
		@AuthenticationPrincipal CustomUserDetails customUserDetails) {
		MembershipStatusResponseDto responseDto = membershipService.getMembershipInfo(
			customUserDetails.getUser().getId());
		return CustomApiResponse.success(responseDto);
	}

	@PostMapping("/{userId}/ban")
	@PreAuthorize("hasRole('ADMIN')") // 관리자만 사용자의 멤버십을 정지할 수 있습니다.
	public CustomApiResponse<Void> banMembership(
		@PathVariable Long userId) {
		membershipService.banMembership(userId);
		return CustomApiResponse.success(null);
	}

	@PostMapping("/{userId}/unban")
	@PreAuthorize("hasRole('ADMIN')") // 관리자만 사용자의 멤버십 정지를 해제할 수 있습니다.
	public CustomApiResponse<Void> unbanMembership(
		@PathVariable Long userId) {
		membershipService.unbanMembership(userId);
		return CustomApiResponse.success(null);
	}
}
