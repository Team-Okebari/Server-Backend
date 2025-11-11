package com.okebari.artbite.creator.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.okebari.artbite.creator.domain.Creator;
import com.okebari.artbite.creator.dto.CreatorRequest;
import com.okebari.artbite.creator.dto.CreatorResponse;
import com.okebari.artbite.creator.dto.CreatorSummaryDto;
import com.okebari.artbite.creator.exception.CreatorNotFoundException;
import com.okebari.artbite.creator.mapper.CreatorMapper;
import com.okebari.artbite.creator.repository.CreatorRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
/**
 * 관리자 작가 관리 유스케이스를 담당하는 서비스.
 * (등록/조회/수정/삭제 + 목록 조회)
 */
public class CreatorService {

	private final CreatorRepository creatorRepository;
	private final CreatorMapper creatorMapper;

	public Long create(CreatorRequest request) {
		Creator creator = creatorMapper.toEntity(request);
		return creatorRepository.save(creator).getId();
	}

	@Transactional(readOnly = true)
	public CreatorResponse get(Long creatorId) {
		Creator creator = findCreator(creatorId);
		return creatorMapper.toResponse(creator);
	}

	@Transactional(readOnly = true)
	public List<CreatorSummaryDto> list() {
		return creatorMapper.toSummaryList(creatorRepository.findAll());
	}

	public void update(Long creatorId, CreatorRequest request) {
		Creator creator = findCreator(creatorId);
		creatorMapper.updateEntity(creator, request);
	}

	public void delete(Long creatorId) {
		if (!creatorRepository.existsById(creatorId)) {
			throw new CreatorNotFoundException(creatorId);
		}
		creatorRepository.deleteById(creatorId);
	}

	private Creator findCreator(Long creatorId) {
		return creatorRepository.findById(creatorId)
			.orElseThrow(() -> new CreatorNotFoundException(creatorId));
	}
}
