package com.okebari.artbite.s3.service;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3Service {

	private final S3Client s3Client;
	private final String bucket;

	public S3Service(S3Client s3Client, @Value("${cloud.aws.s3.bucket}") String bucket) {
		this.s3Client = s3Client;
		this.bucket = bucket;
	}

	public String uploadFile(MultipartFile multipartFile) throws IOException {
		String originalFileName = multipartFile.getOriginalFilename();
		String extension = "";
		String baseName = originalFileName;

		if (originalFileName != null && originalFileName.contains(".")) {
			int lastDot = originalFileName.lastIndexOf(".");
			extension = originalFileName.substring(lastDot); // e.g., .png
			baseName = originalFileName.substring(0, lastDot);
		}

		String uniqueFileName = "uploads/" + baseName + "-" + UUID.randomUUID().toString() + extension;

		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
			.bucket(bucket)
			.key(uniqueFileName)
			.contentType(multipartFile.getContentType())
			.build();

		s3Client.putObject(putObjectRequest,
			RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize()));

		GetUrlRequest getUrlRequest = GetUrlRequest.builder()
			.bucket(bucket)
			.key(uniqueFileName)
			.build();

		URL url = s3Client.utilities().getUrl(getUrlRequest);

		return url.toString();
	}
}