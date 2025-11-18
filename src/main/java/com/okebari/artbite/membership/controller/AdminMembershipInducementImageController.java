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

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/membership-inducement-image")
@RequiredArgsConstructor
public class AdminMembershipInducementImageController {

	private final MembershipInducementImageService membershipInducementImageService;

	@PutMapping
	@PreAuthorize("hasRole('ADMIN')")
	public CustomApiResponse<Void> updateMembershipInducementImage(
		@Valid @RequestBody MembershipInducementImageUpdateRequestDto requestDto) {
		membershipInducementImageService.updateInducementImageUrl(requestDto.getImageUrl());
		return CustomApiResponse.success(null);
	}
}
