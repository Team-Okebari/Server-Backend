package com.okebari.artbite.s3.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "이미지 업로드 응답 DTO")
public class ImageUploadResponse {
	@Schema(description = "업로드된 이미지의 URL", example = "https://your-s3-bucket.s3.ap-northeast-2.amazonaws.com/image_uuid.jpg")
	private String imageUrl;
}
