package com.okebari.artbite.note.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.okebari.artbite.common.exception.NoteNotFoundException;
import com.okebari.artbite.note.domain.Note;
import com.okebari.artbite.note.domain.NoteStatus;
import com.okebari.artbite.note.dto.note.ArchivedNoteViewResponse;
import com.okebari.artbite.note.dto.note.NoteCoverResponse;
import com.okebari.artbite.note.dto.note.NotePreviewResponse;
import com.okebari.artbite.note.dto.note.NoteResponse;
import com.okebari.artbite.note.dto.note.TodayPublishedResponse;
import com.okebari.artbite.note.dto.note.NoteOverviewDto;
import com.okebari.artbite.note.mapper.NoteMapper;
import com.okebari.artbite.note.repository.NoteRepository;

@ExtendWith(MockitoExtension.class)
class NoteQueryServiceTest {

	@Mock
	private NoteRepository noteRepository;

	@Mock
	private NoteMapper noteMapper;

	@Mock
	private SubscriptionService subscriptionService;

	@InjectMocks
	private NoteQueryService noteQueryService;

	@Test
	void getTodayPreviewReturnsMappedResponse() {
		Note note = Note.builder()
			.status(NoteStatus.PUBLISHED)
			.build();
		when(noteRepository.findFirstByStatusAndPublishedAtBetween(eq(NoteStatus.PUBLISHED), any(), any()))
			.thenReturn(java.util.Optional.of(note));

		NoteOverviewDto overview = new NoteOverviewDto("섹션", "미리보기", null);
		NotePreviewResponse preview = new NotePreviewResponse(1L, null, overview);
		when(noteMapper.toPreviewWithCategory(note, 100)).thenReturn(preview);

		NotePreviewResponse result = noteQueryService.getTodayPreview();

		assertThat(result).isEqualTo(preview);
	}

	@Test
	void getTodayPreviewThrowsWhenNoPublishedNote() {
		when(noteRepository.findFirstByStatusAndPublishedAtBetween(eq(NoteStatus.PUBLISHED), any(), any()))
			.thenReturn(java.util.Optional.empty());

		assertThatThrownBy(() -> noteQueryService.getTodayPreview())
			.isInstanceOf(NoteNotFoundException.class);
	}

	@Test
	void getTodayCoverReturnsMapperResult() {
		Note note = Note.builder()
			.status(NoteStatus.PUBLISHED)
			.build();
		when(noteRepository.findFirstByStatusAndPublishedAtBetween(eq(NoteStatus.PUBLISHED), any(), any()))
			.thenReturn(java.util.Optional.of(note));

		NoteCoverResponse cover = new NoteCoverResponse(
			"title",
			"teaser",
			"https://img",
			"creator",
			"jobTitle",
			LocalDate.now(),
			null
		);
		when(noteMapper.toCoverResponse(note)).thenReturn(cover);

		NoteCoverResponse result = noteQueryService.getTodayCover();

		assertThat(result).isEqualTo(cover);
	}

	@Test
	void getTodayCoverThrowsWhenNoPublishedNote() {
		when(noteRepository.findFirstByStatusAndPublishedAtBetween(eq(NoteStatus.PUBLISHED), any(), any()))
			.thenReturn(java.util.Optional.empty());

		assertThatThrownBy(() -> noteQueryService.getTodayCover())
			.isInstanceOf(NoteNotFoundException.class);
	}

	@Test
	void getTodayPublishedDetailChecksSubscription() {
		when(subscriptionService.isActiveSubscriber(1L)).thenReturn(true);
		Note note = Note.builder().status(NoteStatus.PUBLISHED).build();
		when(noteRepository.findFirstByStatusAndPublishedAtBetween(eq(NoteStatus.PUBLISHED), any(), any()))
			.thenReturn(java.util.Optional.of(note));
		NoteResponse response = mock(NoteResponse.class);
		when(noteMapper.toResponseWithCoverCategory(note)).thenReturn(response);

		TodayPublishedResponse result = noteQueryService.getTodayPublishedDetail(1L);

		assertThat(result.accessible()).isTrue();
		assertThat(result.note()).isEqualTo(response);
		assertThat(result.preview()).isNull();
	}

	@Test
	void getTodayPublishedDetailReturnsPreviewForNonSubscriber() {
		when(subscriptionService.isActiveSubscriber(2L)).thenReturn(false);
		Note note = Note.builder().status(NoteStatus.PUBLISHED).build();
		when(noteRepository.findFirstByStatusAndPublishedAtBetween(eq(NoteStatus.PUBLISHED), any(), any()))
			.thenReturn(java.util.Optional.of(note));
		NoteOverviewDto overview2 = new NoteOverviewDto("섹션", "preview", null);
		NotePreviewResponse preview = new NotePreviewResponse(1L, null, overview2);
		when(noteMapper.toPreviewWithCategory(note, 100)).thenReturn(preview);

		TodayPublishedResponse result = noteQueryService.getTodayPublishedDetail(2L);

		assertThat(result.accessible()).isFalse();
		assertThat(result.note()).isNull();
		assertThat(result.preview()).isEqualTo(preview);
	}

	@Test
	void getTodayPublishedDetailThrowsWhenNoNote() {
		when(noteRepository.findFirstByStatusAndPublishedAtBetween(eq(NoteStatus.PUBLISHED), any(), any()))
			.thenReturn(java.util.Optional.empty());

		assertThatThrownBy(() -> noteQueryService.getTodayPublishedDetail(1L))
			.isInstanceOf(NoteNotFoundException.class);
	}

	@Test
	void getArchivedNoteViewReturnsDetailForSubscriber() {
		Note note = Note.builder()
			.status(NoteStatus.ARCHIVED)
			.build();
		when(noteRepository.findById(5L)).thenReturn(java.util.Optional.of(note));
		when(subscriptionService.isActiveSubscriber(1L)).thenReturn(true);
		NoteResponse response = mock(NoteResponse.class);
		when(noteMapper.toResponse(note)).thenReturn(response);

		ArchivedNoteViewResponse result = noteQueryService.getArchivedNoteView(5L, 1L);

		assertThat(result.accessible()).isTrue();
		assertThat(result.note()).isEqualTo(response);
		assertThat(result.preview()).isNull();
	}

	@Test
	void getArchivedNoteViewReturnsPreviewForNonSubscriber() {
		Note note = Note.builder()
			.status(NoteStatus.ARCHIVED)
			.build();
		when(noteRepository.findById(6L)).thenReturn(java.util.Optional.of(note));
		when(subscriptionService.isActiveSubscriber(2L)).thenReturn(false);
		NoteOverviewDto overview = new NoteOverviewDto("섹션", "archived preview", null);
		NotePreviewResponse preview = new NotePreviewResponse(6L, null, overview);
		when(noteMapper.toPreview(note, 100)).thenReturn(preview);

		ArchivedNoteViewResponse result = noteQueryService.getArchivedNoteView(6L, 2L);

		assertThat(result.accessible()).isFalse();
		assertThat(result.note()).isNull();
		assertThat(result.preview()).isEqualTo(preview);
	}
}
