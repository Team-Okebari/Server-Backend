package com.okebari.artbite.note.service.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.okebari.artbite.note.domain.Note;
import com.okebari.artbite.note.domain.NoteCover;
import com.okebari.artbite.note.domain.NoteStatus;
import com.okebari.artbite.note.domain.ReminderSourceType;
import com.okebari.artbite.note.mapper.NoteReminderMapper;
import com.okebari.artbite.note.repository.NoteAnswerRepository;
import com.okebari.artbite.note.repository.NoteBookmarkRepository;
import com.okebari.artbite.note.repository.NoteRepository;

@ExtendWith(MockitoExtension.class)
class NoteReminderSelectorTest {

	private static final Long USER_ID = 100L;
	private static final LocalDate TARGET_DATE = LocalDate.of(2025, 1, 1);

	@Mock
	private NoteBookmarkRepository bookmarkRepository;

	@Mock
	private NoteAnswerRepository answerRepository;

	@Mock
	private NoteRepository noteRepository;

	private NoteReminderSelector selector;

	@BeforeEach
	void setUp() {
		this.selector = new NoteReminderSelector(
			bookmarkRepository,
			answerRepository,
			noteRepository,
			new NoteReminderMapper()
		);
	}

	@Test
	void pickCandidate_returnsDeterministicResultWithRandomSeed() {
		List<Long> bookmarkIds = List.of(10L, 20L, 50L);
		List<Long> answerIds = List.of(20L, 30L, 60L);
		when(bookmarkRepository.findNoteIdsByUserId(USER_ID)).thenReturn(bookmarkIds);
		when(answerRepository.findNoteIdsByRespondentId(USER_ID)).thenReturn(answerIds);

		List<Long> expectedCandidates = mergeForTest(bookmarkIds, answerIds);
		Long expectedNoteId = pickWithSeed(USER_ID, TARGET_DATE, expectedCandidates);
		ReminderSourceType expectedSourceType =
			bookmarkIds.contains(expectedNoteId) ? ReminderSourceType.BOOKMARK : ReminderSourceType.ANSWER;

		when(noteRepository.findWithCoverAndCreator(expectedNoteId))
			.thenReturn(java.util.Optional.of(createNote(expectedNoteId)));

		var first = selector.pickCandidate(USER_ID, TARGET_DATE);
		var second = selector.pickCandidate(USER_ID, TARGET_DATE);

		assertThat(first).isPresent();
		assertThat(first.get().noteId()).isEqualTo(expectedNoteId);
		assertThat(first.get().sourceType()).isEqualTo(expectedSourceType);
		assertThat(second).isPresent();
		assertThat(second.get().noteId()).isEqualTo(expectedNoteId);

		verify(noteRepository, times(2)).findWithCoverAndCreator(expectedNoteId);
	}

	@Test
	void pickCandidate_returnsEmptyWhenNoBookmarksOrAnswers() {
		when(bookmarkRepository.findNoteIdsByUserId(USER_ID)).thenReturn(List.of());
		when(answerRepository.findNoteIdsByRespondentId(USER_ID)).thenReturn(List.of());

		var result = selector.pickCandidate(USER_ID, TARGET_DATE);

		assertThat(result).isEmpty();
		verify(noteRepository, never()).findWithCoverAndCreator(org.mockito.ArgumentMatchers.anyLong());
	}

	private Note createNote(Long noteId) {
		Note note = Note.builder()
			.status(NoteStatus.PUBLISHED)
			.tagText("tag")
			.sourceUrl("url")
			.build();
		ReflectionTestUtils.setField(note, "id", noteId);
		NoteCover cover = NoteCover.builder()
			.title("title-" + noteId)
			.teaser("teaser")
			.mainImageUrl("image")
			.build();
		note.assignCover(cover);
		return note;
	}

	private List<Long> mergeForTest(List<Long> bookmarks, List<Long> answers) {
		LinkedHashSet<Long> bookmarkSet = new LinkedHashSet<>(bookmarks);
		LinkedHashSet<Long> answerSet = new LinkedHashSet<>(answers);
		List<Long> merged = new ArrayList<>(bookmarkSet);
		for (Long answer : answerSet) {
			if (!bookmarkSet.contains(answer)) {
				merged.add(answer);
			}
		}
		return merged;
	}

	private Long pickWithSeed(Long userId, LocalDate date, List<Long> candidates) {
		long seed = generateSeed(userId, date);
		return candidates.get(new Random(seed).nextInt(candidates.size()));
	}

	private long generateSeed(Long userId, LocalDate date) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			String raw = userId + "-" + date;
			byte[] digest = messageDigest.digest(raw.getBytes(StandardCharsets.UTF_8));
			return ByteBuffer.wrap(digest).getLong();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
