package com.okebari.artbite.membership.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.okebari.artbite.common.dto.CustomApiResponse;
import com.okebari.artbite.membership.dto.MembershipInducementImageResponseDto;
import com.okebari.artbite.membership.service.MembershipInducementImageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Membership Inducement Image", description = "멤버십 유도 이미지 조회 API")
@RestController
@RequestMapping("/api/membership-inducement-image")
@RequiredArgsConstructor
public class MembershipInducementImageController {

	private final MembershipInducementImageService membershipInducementImageService;

	@Operation(summary = "멤버십 유도 이미지 URL 조회", description = "메인 화면에 표시될 멤버십 유도 이미지의 URL을 조회합니다. (인증 불필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "멤버십 유도 이미지 URL 조회 성공", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = {
				@ExampleObject(name = "Success", summary = "이미지 URL 존재",
					value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":true,\"data\":{\"imageUrl\":\"https://example.com/inducement.png\"},\"error\":null}"),
				@ExampleObject(name = "SuccessNoImage", summary = "이미지 URL 없음",
					value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":true,\"data\":{\"imageUrl\":null},\"error\":null}")
			}))
	})
	@GetMapping
	public CustomApiResponse<MembershipInducementImageResponseDto> getMembershipInducementImage() {
		MembershipInducementImageResponseDto responseDto = membershipInducementImageService.getInducementImageUrl();
		return CustomApiResponse.success(responseDto);
	}
}
