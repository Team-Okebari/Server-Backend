package com.okebari.artbite.note.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.okebari.artbite.auth.vo.CustomUserDetails;
import com.okebari.artbite.common.dto.CustomApiResponse;
import com.okebari.artbite.note.dto.answer.NoteAnswerDto;
import com.okebari.artbite.note.dto.answer.NoteAnswerRequest;
import com.okebari.artbite.note.dto.answer.NoteAnswerResponse;
import com.okebari.artbite.note.service.NoteAnswerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notes/questions")
@RequiredArgsConstructor
public class NoteAnswerController {

	private final NoteAnswerService noteAnswerService;

	/**
	 * USER가 질문에 대한 답변을 최초 작성한다.
	 */
	@PreAuthorize("hasRole('USER')")
	@PostMapping("/{questionId}/answer")
	public CustomApiResponse<NoteAnswerResponse> create(
		@PathVariable Long questionId,
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@Valid @RequestBody NoteAnswerRequest request) {
		NoteAnswerDto dto = noteAnswerService.createAnswer(
			questionId,
			userDetails.getUser().getId(),
			request.answerText()
		);
		return CustomApiResponse.success(new NoteAnswerResponse(dto.answerText()));
	}

	/**
	 * USER가 기존 답변을 수정한다.
	 */
	@PreAuthorize("hasRole('USER')")
	@PutMapping("/{questionId}/answer")
	public CustomApiResponse<NoteAnswerResponse> update(
		@PathVariable Long questionId,
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@Valid @RequestBody NoteAnswerRequest request) {
		NoteAnswerDto dto = noteAnswerService.updateAnswer(
			questionId,
			userDetails.getUser().getId(),
			request.answerText()
		);
		return CustomApiResponse.success(new NoteAnswerResponse(dto.answerText()));
	}

	/**
	 * USER가 자신의 답변을 삭제한다.
	 */
	@PreAuthorize("hasRole('USER')")
	@DeleteMapping("/{questionId}/answer")
	public CustomApiResponse<Void> delete(
		@PathVariable Long questionId,
		@AuthenticationPrincipal CustomUserDetails userDetails) {
		noteAnswerService.deleteAnswer(questionId, userDetails.getUser().getId());
		return CustomApiResponse.success(null);
	}
}
