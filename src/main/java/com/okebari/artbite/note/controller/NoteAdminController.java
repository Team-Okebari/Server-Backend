package com.okebari.artbite.note.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.okebari.artbite.auth.vo.CustomUserDetails;
import com.okebari.artbite.common.dto.CustomApiResponse;
import com.okebari.artbite.note.dto.note.NoteCreateRequest;
import com.okebari.artbite.note.dto.note.NoteResponse;
import com.okebari.artbite.note.dto.note.NoteUpdateRequest;
import com.okebari.artbite.note.service.NoteService;

import lombok.RequiredArgsConstructor;

/**
 * ADMIN이 노트를 관리할 때 사용하는 API 컨트롤러.
 */
@RestController
@RequestMapping("/api/admin/notes")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class NoteAdminController {

	private final NoteService noteService;

	/**
	 * 노트를 신규 작성한다.
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CustomApiResponse<Long> create(
		@AuthenticationPrincipal CustomUserDetails admin,
		@Valid @RequestBody NoteCreateRequest request) {
		Long noteId = noteService.create(request, admin.getUser().getId());
		return CustomApiResponse.success(noteId);
	}

	/**
	 * 특정 노트의 상세 정보를 조회한다.
	 */
	@GetMapping("/{noteId}")
	public CustomApiResponse<NoteResponse> get(@PathVariable Long noteId) {
		return CustomApiResponse.success(noteService.getForAdmin(noteId));
	}

	/**
	 * 노트 내용을 업데이트한다.
	 */
	@PutMapping("/{noteId}")
	public CustomApiResponse<?> update(
		@PathVariable Long noteId,
		@Valid @RequestBody NoteUpdateRequest request) {
		noteService.update(noteId, request);
		return CustomApiResponse.success();
	}

	/**
	 * 노트를 삭제한다.
	 */
	@DeleteMapping("/{noteId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long noteId) {
		noteService.delete(noteId);
	}

	/**
	 * 관리자 전용 노트 목록을 페이징 조회한다.
	 */
	@GetMapping
	public CustomApiResponse<Page<NoteResponse>> list(Pageable pageable) {
		return CustomApiResponse.success(noteService.list(pageable));
	}
}
