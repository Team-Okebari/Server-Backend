package com.okebari.artbite.s3.controller;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.okebari.artbite.common.dto.CustomApiResponse;
import com.okebari.artbite.s3.dto.ImageUploadResponse;
import com.okebari.artbite.s3.service.S3Service;

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

@Tag(name = "S3", description = "S3 이미지 업로드 API (관리자용)")
@RestController
@RequestMapping("/api/admin/images")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class S3Controller {

	private final S3Service s3Service;

	@Operation(summary = "이미지 업로드", description = "단일 이미지를 S3에 업로드하고 업로드된 이미지의 URL을 반환합니다. (ADMIN 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "이미지 업로드 성공"),
		@ApiResponse(responseCode = "400", description = "파일 업로드 용량 초과",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "FileSizeExceeded", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C006\",\"message\":\"파일 업로드 용량을 초과했습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"),
				mediaType = "application/json")),
		@ApiResponse(responseCode = "401", description = "인증 실패",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "Unauthorized", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"),
				mediaType = "application/json")),
		@ApiResponse(responseCode = "403", description = "권한 없음",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "Forbidden", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C003\",\"message\":\"접근 권한이 없습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"),
				mediaType = "application/json")),
		@ApiResponse(responseCode = "500", description = "이미지 업로드 실패 또는 서버 내부 오류",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "InternalServerError", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C005\",\"message\":\"서버 내부 오류가 발생했습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"),
				mediaType = "application/json"))
	})
	@PostMapping
	public ResponseEntity<ImageUploadResponse> uploadImage(
		@Parameter(description = "업로드할 이미지 파일 (MultipartFile)", required = true) @RequestParam("image") MultipartFile image) {
		try {
			String imageUrl = s3Service.uploadFile(image);
			return ResponseEntity.ok(new ImageUploadResponse(imageUrl));
		} catch (IOException e) {
			// Consider a more specific error response
			return ResponseEntity.internalServerError().build();
		}
	}
}
