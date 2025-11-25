package com.okebari.artbite.note.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

@Tag(name = "Note Answer", description = "노트 질문에 대한 답변 관리 API")
@RestController
@RequestMapping("/api/notes/questions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class NoteAnswerController {

	private final NoteAnswerService noteAnswerService;

	@Operation(summary = "질문에 대한 답변 작성", description = "특정 질문에 대해 사용자가 답변을 최초로 작성합니다. (USER 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "답변 작성 성공",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "Success", value = "{\"success\":true,\"data\":{\"answerText\":\"새로운 답변 내용\"},\"error\":null,\"timestamp\":\"2025-11-25T10:00:00\"}"))),
		@ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "BadRequest", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C001\",\"message\":\"답변 내용은 비워둘 수 없습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"))),
		@ApiResponse(responseCode = "401", description = "인증 실패",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "Unauthorized", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"))),
		@ApiResponse(responseCode = "404", description = "질문을 찾을 수 없음",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "QuestionNotFound", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"N001\",\"message\":\"노트를 찾을 수 없습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}")))
	})
	/**
	 * USER가 질문에 대한 답변을 최초 작성한다.
	 */
	@PreAuthorize("hasRole('USER')")
	@PostMapping("/{questionId}/answer")
	public CustomApiResponse<NoteAnswerResponse> create(
		@Parameter(description = "답변을 작성할 질문의 ID", required = true) @PathVariable Long questionId,
		@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
		@Valid @RequestBody NoteAnswerRequest request) {
		NoteAnswerDto dto = noteAnswerService.createAnswer(
			questionId,
			userDetails.getUser().getId(),
			request.answerText()
		);
		return CustomApiResponse.success(new NoteAnswerResponse(dto.answerText()));
	}

	@Operation(summary = "작성된 답변 수정", description = "사용자가 기존에 작성했던 답변을 수정합니다. (USER 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "답변 수정 성공",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "Success", value = "{\"success\":true,\"data\":{\"answerText\":\"수정된 답변 내용\"},\"error\":null,\"timestamp\":\"2025-11-25T10:00:00\"}"))),
		@ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "BadRequest", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C001\",\"message\":\"답변 내용은 비워둘 수 없습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"))),
		@ApiResponse(responseCode = "401", description = "인증 실패",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "Unauthorized", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"))),
		@ApiResponse(responseCode = "404", description = "답변 또는 질문을 찾을 수 없음",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "AnswerNotFound", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C004\",\"message\":\"리소스를 찾을 수 없습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}")))
	})
	/**
	 * USER가 기존 답변을 수정한다.
	 */
	@PreAuthorize("hasRole('USER')")
	@PutMapping("/{questionId}/answer")
	public CustomApiResponse<NoteAnswerResponse> update(
		@Parameter(description = "수정할 답변이 달린 질문의 ID", required = true) @PathVariable Long questionId,
		@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
		@Valid @RequestBody NoteAnswerRequest request) {
		NoteAnswerDto dto = noteAnswerService.updateAnswer(
			questionId,
			userDetails.getUser().getId(),
			request.answerText()
		);
		return CustomApiResponse.success(new NoteAnswerResponse(dto.answerText()));
	}

	@Operation(summary = "작성된 답변 삭제", description = "사용자가 자신의 답변을 삭제합니다. (USER 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "답변 삭제 성공",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "Success", value = "{\"success\":true,\"data\":null,\"error\":null,\"timestamp\":\"2025-11-25T10:00:00\"}"))),
		@ApiResponse(responseCode = "401", description = "인증 실패",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "Unauthorized", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"))),
		@ApiResponse(responseCode = "404", description = "삭제할 답변을 찾을 수 없음",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "AnswerNotFound", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C004\",\"message\":\"리소스를 찾을 수 없습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}")))
	})
	/**
	 * USER가 자신의 답변을 삭제한다.
	 */
	@PreAuthorize("hasRole('USER')")
	@DeleteMapping("/{questionId}/answer")
	public CustomApiResponse<Void> delete(
		@Parameter(description = "삭제할 답변이 달린 질문의 ID", required = true) @PathVariable Long questionId,
		@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
		noteAnswerService.deleteAnswer(questionId, userDetails.getUser().getId());
		return CustomApiResponse.success(null);
	}

	@Operation(summary = "작성된 답변 조회", description = "사용자가 자신의 답변을 조회합니다. 답변이 없으면 204 No Content를 반환합니다. (USER 권한 필요)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "답변 조회 성공",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "Success", value = "{\"success\":true,\"data\":{\"answerText\":\"내가 작성한 답변 내용\"},\"error\":null,\"timestamp\":\"2025-11-25T10:00:00\"}"))),
		@ApiResponse(responseCode = "204", description = "작성된 답변 없음", content = @Content),
		@ApiResponse(responseCode = "401", description = "인증 실패",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "Unauthorized", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"C002\",\"message\":\"인증되지 않은 사용자입니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}"))),
		@ApiResponse(responseCode = "404", description = "질문을 찾을 수 없음",
			content = @Content(schema = @Schema(implementation = CustomApiResponse.class),
				examples = @ExampleObject(name = "QuestionNotFound", value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"N001\",\"message\":\"노트를 찾을 수 없습니다.\"},\"timestamp\":\"2025-11-25T10:00:00\"}")))
	})
	/**
	 * USER가 자신의 답변을 조회한다.
	 * 답변이 없으면 204 No Content를 반환한다.
	 */
	@PreAuthorize("hasRole('USER')")
	@GetMapping("/{questionId}/answer")
	public ResponseEntity<CustomApiResponse<NoteAnswerResponse>> get(
		@Parameter(description = "조회할 답변이 달린 질문의 ID", required = true) @PathVariable Long questionId,
		@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
		NoteAnswerDto dto = noteAnswerService.getAnswer(questionId, userDetails.getUser().getId());
		if (dto == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		return ResponseEntity.ok(CustomApiResponse.success(new NoteAnswerResponse(dto.answerText())));
	}
}
