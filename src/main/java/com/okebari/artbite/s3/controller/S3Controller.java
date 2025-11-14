package com.okebari.artbite.s3.controller;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.okebari.artbite.s3.dto.ImageUploadResponse;
import com.okebari.artbite.s3.service.S3Service;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/images")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class S3Controller {

	private final S3Service s3Service;

	@PostMapping
	public ResponseEntity<ImageUploadResponse> uploadImage(@RequestParam("image") MultipartFile image) {
		try {
			String imageUrl = s3Service.uploadFile(image);
			return ResponseEntity.ok(new ImageUploadResponse(imageUrl));
		} catch (IOException e) {
			// Consider a more specific error response
			return ResponseEntity.internalServerError().build();
		}
	}
}
