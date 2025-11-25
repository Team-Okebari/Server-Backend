package com.okebari.artbite.membership.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.okebari.artbite.common.exception.NotFoundException;
import com.okebari.artbite.domain.membership.MembershipInducementImage;
import com.okebari.artbite.domain.membership.MembershipInducementImageRepository;
import com.okebari.artbite.membership.dto.MembershipInducementImageResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MembershipInducementImageService {

	private final MembershipInducementImageRepository membershipInducementImageRepository;

	public MembershipInducementImageResponseDto getInducementImageUrl() {
		MembershipInducementImage image = membershipInducementImageRepository.findById(1L)
			.orElseThrow(() -> new NotFoundException("Membership inducement image not found."));
		return MembershipInducementImageResponseDto.builder()
			.imageUrl(image.getImageUrl())
			.build();
	}

	@Transactional
	public void updateInducementImageUrl(String newImageUrl) {
		MembershipInducementImage image = membershipInducementImageRepository.findById(1L)
			.orElseThrow(() -> new NotFoundException("Membership inducement image not found."));
		image.updateImageUrl(newImageUrl);
		// No need to call save explicitly as it's a managed entity within a transactional context
	}
}
