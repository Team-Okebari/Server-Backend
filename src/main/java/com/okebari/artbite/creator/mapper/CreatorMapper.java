package com.okebari.artbite.creator.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.okebari.artbite.creator.domain.Creator;
import com.okebari.artbite.creator.dto.CreatorRequest;
import com.okebari.artbite.creator.dto.CreatorResponse;
import com.okebari.artbite.creator.dto.CreatorSummaryDto;

@Component
/**
 * Creator ↔ DTO 변환 전담. 서비스 계층에서 반복되는 매핑 로직을 한 곳에 모은다.
 */
public class CreatorMapper {

	public Creator toEntity(CreatorRequest request) {
		return Creator.builder()
			.name(request.name())
			.bio(request.bio())
			.jobTitle(request.jobTitle())
			.profileImageUrl(request.profileImageUrl())
			.instagramUrl(request.instagramUrl())
			.youtubeUrl(request.youtubeUrl())
			.behanceUrl(request.behanceUrl())
			.xUrl(request.xUrl())
			.blogUrl(request.blogUrl())
			.newsUrl(request.newsUrl())
			.build();
	}

	public void updateEntity(Creator creator, CreatorRequest request) {
		creator.updateProfile(request.name(), request.bio(), request.jobTitle(), request.profileImageUrl());
		creator.updateSocialLinks(
			request.instagramUrl(),
			request.youtubeUrl(),
			request.behanceUrl(),
			request.xUrl(),
			request.blogUrl(),
			request.newsUrl()
		);
	}

	public CreatorResponse toResponse(Creator creator) {
		return new CreatorResponse(
			creator.getId(),
			creator.getName(),
			creator.getBio(),
			creator.getJobTitle(),
			creator.getProfileImageUrl(),
			creator.getInstagramUrl(),
			creator.getYoutubeUrl(),
			creator.getBehanceUrl(),
			creator.getXUrl(),
			creator.getBlogUrl(),
			creator.getNewsUrl()
		);
	}

	public CreatorSummaryDto toSummary(Creator creator) {
		return new CreatorSummaryDto(
			creator.getId(),
			creator.getName(),
			creator.getBio(),
			creator.getJobTitle(),
			creator.getProfileImageUrl(),
			creator.getInstagramUrl(),
			creator.getYoutubeUrl(),
			creator.getBehanceUrl(),
			creator.getXUrl(),
			creator.getBlogUrl(),
			creator.getNewsUrl()
		);
	}

	public List<CreatorSummaryDto> toSummaryList(List<Creator> creators) {
		return creators.stream()
			.map(creator -> this.toSummary(creator))
			.toList();
	}
}
