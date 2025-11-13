package com.okebari.artbite.note.service.support;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.okebari.artbite.common.exception.NoteNotFoundException;
import com.okebari.artbite.note.domain.Note;
import com.okebari.artbite.note.domain.NoteReminderPayload;
import com.okebari.artbite.note.domain.ReminderSourceType;
import com.okebari.artbite.note.mapper.NoteReminderMapper;
import com.okebari.artbite.note.repository.NoteAnswerRepository;
import com.okebari.artbite.note.repository.NoteBookmarkRepository;
import com.okebari.artbite.note.repository.NoteRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NoteReminderSelector {

	private final NoteBookmarkRepository bookmarkRepository;
	private final NoteAnswerRepository answerRepository;
	private final NoteRepository noteRepository;
	private final NoteReminderMapper reminderMapper;

	@Transactional(readOnly = true)
	public Optional<ReminderCandidate> pickCandidate(Long userId, LocalDate targetDate) {
		Set<Long> bookmarkNoteIds = new LinkedHashSet<>(bookmarkRepository.findNoteIdsByUserId(userId));
		Set<Long> answerNoteIds = new LinkedHashSet<>(answerRepository.findNoteIdsByRespondentId(userId));

		if (bookmarkNoteIds.isEmpty() && answerNoteIds.isEmpty()) {
			return Optional.empty();
		}

		List<Long> candidates = mergeCandidates(bookmarkNoteIds, answerNoteIds);
		Long selectedNoteId = findBestCandidate(userId, targetDate, candidates);

		ReminderSourceType sourceType = bookmarkNoteIds.contains(selectedNoteId)
			? ReminderSourceType.BOOKMARK
			: ReminderSourceType.ANSWER;

		Note note = noteRepository.findWithCoverAndCreator(selectedNoteId)
			.orElseThrow(() -> new NoteNotFoundException(selectedNoteId));

		NoteReminderPayload payload = reminderMapper.toPayload(note);
		return Optional.of(new ReminderCandidate(selectedNoteId, sourceType, payload));
	}

	private List<Long> mergeCandidates(Set<Long> bookmarks, Set<Long> answers) {
		List<Long> merged = new ArrayList<>(bookmarks);
		for (Long answerNoteId : answers) {
			if (!bookmarks.contains(answerNoteId)) {
				merged.add(answerNoteId);
			}
		}
		return merged;
	}

	private Long findBestCandidate(Long userId, LocalDate targetDate, List<Long> candidates) {
		if (candidates.isEmpty()) {
			throw new IllegalStateException("No reminder candidate found");
		}
		Random random = new Random(generateSeed(userId, targetDate));
		int index = random.nextInt(candidates.size());
		return candidates.get(index);
	}

	private long generateSeed(Long userId, LocalDate targetDate) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			String raw = userId + "-" + targetDate;
			byte[] digest = messageDigest.digest(raw.getBytes(StandardCharsets.UTF_8));
			return ByteBuffer.wrap(digest).getLong();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("MD5 algorithm is not available", e);
		}
	}

	public record ReminderCandidate(Long noteId, ReminderSourceType sourceType, NoteReminderPayload payload) {
	}
}
