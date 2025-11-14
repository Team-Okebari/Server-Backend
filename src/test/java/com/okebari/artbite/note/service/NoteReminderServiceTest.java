package com.okebari.artbite.note.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.okebari.artbite.note.domain.NoteReminder;
import com.okebari.artbite.note.domain.NoteReminderPayload;
import com.okebari.artbite.note.domain.ReminderSourceType;
import com.okebari.artbite.note.dto.reminder.NoteReminderResponse;
import com.okebari.artbite.note.dto.reminder.SurfaceHint;
import com.okebari.artbite.note.mapper.NoteReminderMapper;
import com.okebari.artbite.note.repository.NoteReminderRepository;
import com.okebari.artbite.note.service.support.ReminderCacheClient;
import com.okebari.artbite.note.service.support.NoteReminderCacheValue;
import com.okebari.artbite.note.service.support.ReminderStateMachine;

@ExtendWith(MockitoExtension.class)
class NoteReminderServiceTest {

	@Mock
	private NoteReminderRepository reminderRepository;

	@Mock
	private ReminderCacheClient cacheClient;

	private NoteReminderMapper reminderMapper;

	private Clock fixedClock;

	private NoteReminderService reminderService;
	private ReminderStateMachine reminderStateMachine;

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final LocalDate FIXED_DATE = LocalDate.of(2025, 1, 1);
	private static final Instant FIXED_INSTANT = FIXED_DATE.atStartOfDay(KST).toInstant();

	@BeforeEach
	void setUp() {
		this.reminderMapper = new NoteReminderMapper();
		this.fixedClock = Clock.fixed(FIXED_INSTANT, KST);
		this.reminderStateMachine = new ReminderStateMachine();
		this.reminderService =
			new NoteReminderService(reminderRepository, cacheClient, reminderStateMachine, reminderMapper, fixedClock);
	}

	@Test
	void returnsEmptyWhenReminderDoesNotExist() {
		when(cacheClient.get(1L, FIXED_DATE)).thenReturn(Optional.empty());
		when(reminderRepository.findByUserIdAndReminderDate(1L, FIXED_DATE)).thenReturn(Optional.empty());

		Optional<NoteReminderResponse> result = reminderService.getTodayReminder(1L);

		assertThat(result).isEmpty();
		verify(cacheClient, never()).save(any());
	}

	@Test
	void firstVisitMarksDeferredAndPersists() {
		NoteReminder reminder = createReminder();
		when(cacheClient.get(1L, FIXED_DATE)).thenReturn(Optional.empty());
		when(reminderRepository.findByUserIdAndReminderDate(1L, FIXED_DATE)).thenReturn(Optional.of(reminder));
		when(reminderRepository.save(any(NoteReminder.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Optional<NoteReminderResponse> result = reminderService.getTodayReminder(1L);

		assertThat(result).isPresent();
		assertThat(result.get().surfaceHint()).isEqualTo(SurfaceHint.DEFERRED);
		verify(cacheClient, times(2)).save(any(NoteReminder.class));
	}

	@Test
	void subsequentVisitReturnsBanner() {
		NoteReminder reminder = createReminder();
		LocalDateTime firstVisit = LocalDateTime.now(fixedClock).minusMinutes(10);
		reminder.markFirstVisit(firstVisit);
		NoteReminderCacheValue cacheValue = NoteReminderCacheValue.from(reminder);

		when(cacheClient.get(1L, FIXED_DATE)).thenReturn(Optional.of(cacheValue));
		when(reminderRepository.findByUserIdAndReminderDate(1L, FIXED_DATE)).thenReturn(Optional.of(reminder));
		when(reminderRepository.save(any(NoteReminder.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Optional<NoteReminderResponse> result = reminderService.getTodayReminder(1L);

		assertThat(result).isPresent();
		assertThat(result.get().surfaceHint()).isEqualTo(SurfaceHint.BANNER);
		verify(cacheClient).save(any(NoteReminder.class));
	}

	private NoteReminder createReminder() {
		NoteReminderPayload payload = NoteReminderPayload.builder()
			.noteId(10L)
			.title("Mock title")
			.build();
		return NoteReminder.create(1L, FIXED_DATE, ReminderSourceType.BOOKMARK, 10L, payload);
	}
}
