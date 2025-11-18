package com.okebari.artbite.membership.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.okebari.artbite.common.dto.CustomApiResponse;
import com.okebari.artbite.membership.dto.MembershipInducementImageResponseDto;
import com.okebari.artbite.membership.service.MembershipInducementImageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/membership-inducement-image")
@RequiredArgsConstructor
public class MembershipInducementImageController {

	private final MembershipInducementImageService membershipInducementImageService;

	@GetMapping
	public CustomApiResponse<MembershipInducementImageResponseDto> getMembershipInducementImage() {
		MembershipInducementImageResponseDto responseDto = membershipInducementImageService.getInducementImageUrl();
		return CustomApiResponse.success(responseDto);
	}
}
