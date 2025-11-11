package com.okebari.artbite.creator.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.okebari.artbite.common.dto.CustomApiResponse;
import com.okebari.artbite.creator.dto.CreatorRequest;
import com.okebari.artbite.creator.dto.CreatorResponse;
import com.okebari.artbite.creator.dto.CreatorSummaryDto;
import com.okebari.artbite.creator.service.CreatorService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/creators")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
/**
 * ADMIN 작가 관리용 REST 컨트롤러.
 * 노트 작성 화면이 참조할 목록/상세 API와 관리자 CRUD를 제공한다.
 */
public class CreatorAdminController {

	private final CreatorService creatorService;

	@PostMapping
	public CustomApiResponse<Long> create(@Valid @RequestBody CreatorRequest request) {
		return CustomApiResponse.success(creatorService.create(request));
	}

	@GetMapping
	public CustomApiResponse<List<CreatorSummaryDto>> list() {
		return CustomApiResponse.success(creatorService.list());
	}

	@GetMapping("/{creatorId}")
	public CustomApiResponse<CreatorResponse> get(@PathVariable Long creatorId) {
		return CustomApiResponse.success(creatorService.get(creatorId));
	}

	@PutMapping("/{creatorId}")
	public CustomApiResponse<Void> update(
		@PathVariable Long creatorId,
		@Valid @RequestBody CreatorRequest request) {
		creatorService.update(creatorId, request);
		return CustomApiResponse.success(null);
	}

	@DeleteMapping("/{creatorId}")
	public CustomApiResponse<Void> delete(@PathVariable Long creatorId) {
		creatorService.delete(creatorId);
		return CustomApiResponse.success(null);
	}
}

