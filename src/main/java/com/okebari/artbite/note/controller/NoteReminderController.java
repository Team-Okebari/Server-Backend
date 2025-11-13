package com.okebari.artbite.note.controller;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.okebari.artbite.auth.vo.CustomUserDetails;
import com.okebari.artbite.common.dto.CustomApiResponse;
import com.okebari.artbite.note.dto.reminder.NoteReminderResponse;
import com.okebari.artbite.note.service.NoteReminderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notes/reminder")
@RequiredArgsConstructor
public class NoteReminderController {

	private final NoteReminderService reminderService;

	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@GetMapping("/today")
	public ResponseEntity<?> getTodayReminder(@AuthenticationPrincipal CustomUserDetails userDetails) {
		Long userId = userDetails.getUser().getId();
		Optional<NoteReminderResponse> response = reminderService.getTodayReminder(userId);
		return response
			.map(value -> ResponseEntity.ok(CustomApiResponse.success(value)))
			.orElseGet(() -> ResponseEntity.noContent().build());
	}

	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	@PostMapping("/dismiss")
	public ResponseEntity<Void> dismissToday(
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		Long userId = userDetails.getUser().getId();
		reminderService.dismissToday(userId);
		return ResponseEntity.noContent().build();
	}
}
