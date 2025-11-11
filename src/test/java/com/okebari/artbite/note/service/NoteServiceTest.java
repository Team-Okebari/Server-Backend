package com.okebari.artbite.note.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.domain.user.UserRepository;
import com.okebari.artbite.domain.user.UserRole;
import com.okebari.artbite.note.domain.Note;
import com.okebari.artbite.creator.domain.Creator;
import com.okebari.artbite.creator.repository.CreatorRepository;
import com.okebari.artbite.note.exception.NoteAccessDeniedException;
import com.okebari.artbite.note.exception.NoteInvalidStatusException;
import com.okebari.artbite.note.repository.NoteRepository;
import com.okebari.artbite.note.domain.NoteStatus;
import com.okebari.artbite.note.dto.note.NoteCoverDto;
import com.okebari.artbite.note.dto.note.NoteCreateRequest;
import com.okebari.artbite.note.dto.note.NoteExternalLinkDto;
import com.okebari.artbite.note.dto.note.NoteOverviewDto;
import com.okebari.artbite.note.dto.note.NoteProcessDto;
import com.okebari.artbite.note.dto.note.NoteRetrospectDto;
import com.okebari.artbite.note.dto.note.NoteUpdateRequest;
import com.okebari.artbite.note.dto.question.NoteQuestionDto;
import com.okebari.artbite.note.mapper.NoteMapper;

/**
 * NoteService 핵심 시나리오를 검증하는 단위 테스트.
 * MockitoExtension을 통해 @Mock/@InjectMocks 필드를 초기화한다.
 */
@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

	/**
	 * 노트 저장소를 목으로 대체해 DB 없이 서비스 로직만 검증한다.
	 */
	@Mock
	private NoteRepository noteRepository;

	/**
	 * 작가 조회 역시 DB 접근 없이 상황을 구성하기 위해 목 객체로 준비한다.
	 */
	@Mock
	private CreatorRepository creatorRepository;

	/**
	 * 매퍼 로직은 별도 검증 대상이므로, 여기서는 서비스에서 호출되는지만 확인한다.
	 */
	@Mock
	private NoteMapper noteMapper;

	/**
	 * 호출자가 ADMIN인지 여부를 시뮬레이션하기 위해 UserRepository도 목 객체로 둔다.
	 */
	@Mock
	private UserRepository userRepository;

	/**
	 * @InjectMocks는 위 목 객체들을 주입한 NoteService 인스턴스를 만들어 준다.
	 */
	@InjectMocks
	private NoteService noteService;

	/**
	 * [목적] ADMIN이 아닌 사용자가 노트를 생성하려 하면 예외가 발생해야 한다.
	 * [결과] AccessDeniedException이 던져지고 저장 로직은 수행되지 않는다.
	 */
	@Test
	void createThrowsWhenCallerIsNotAdmin() {
		User normalUser = buildUser(UserRole.USER, 10L);
		when(userRepository.findById(10L)).thenReturn(Optional.of(normalUser));

        NoteCreateRequest request = createNoteRequest(NoteStatus.IN_PROGRESS, 1L);

		assertThatThrownBy(() -> noteService.create(request, 10L))
			.isInstanceOf(NoteAccessDeniedException.class);

		verify(noteRepository, never()).save(any());
	}

	/**
	 * [목적] 허용되지 않은 상태(IN_PROGRESS/COMPLETED 외)로 생성 요청 시 예외가 나와야 한다.
	 * [결과] IllegalArgumentException이 발생하고 매퍼는 호출되지 않는다.
	 */
	@Test
	void createThrowsWhenStatusIsNotAllowed() {
		User admin = buildUser(UserRole.ADMIN, 1L);
		when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        NoteCreateRequest request = createNoteRequest(NoteStatus.PUBLISHED, 1L);

		assertThatThrownBy(() -> noteService.create(request, 1L))
			.isInstanceOf(NoteInvalidStatusException.class)
			.hasMessageContaining("신규 노트는 IN_PROGRESS 또는 COMPLETED 상태로만 생성할 수 있습니다.");

		verify(noteMapper, never()).toEntity(any());
	}

	/**
	 * [목적] 정상 ADMIN이 허용 상태로 요청하면 노트가 저장되어 ID를 돌려줘야 한다.
	 * [결과] save가 호출되고, 반환된 ID가 기대값(123L)과 일치한다.
	 */
	@Test
	void createSavesNoteAndReturnsId() {
		User admin = buildUser(UserRole.ADMIN, 1L);
		when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

		Long creatorId = 99L;
		NoteCreateRequest request = createNoteRequest(NoteStatus.IN_PROGRESS, creatorId);

	Creator creator = Creator.builder()
		.name("Creator")
		.bio("Bio")
		.jobTitle("Job")
		.build();
		ReflectionTestUtils.setField(creator, "id", creatorId);
		when(creatorRepository.findById(creatorId)).thenReturn(Optional.of(creator));

		Note mapped = Note.builder()
			.status(NoteStatus.IN_PROGRESS)
			.tagText("tag")
			.sourceUrl("https://source")
			.build();
		when(noteMapper.toEntity(request)).thenReturn(mapped);

		ReflectionTestUtils.setField(mapped, "id", 123L);
		when(noteRepository.save(mapped)).thenReturn(mapped);

		Long noteId = noteService.create(request, 1L);

		assertThat(noteId).isEqualTo(123L);
		assertThat(mapped.getCreator()).isEqualTo(creator);
		verify(noteRepository).save(mapped);
	}

	/**
	 * [목적] 이미 게시(PUBLISHED)된 노트를 수정하려 하면 금지되어야 한다.
	 * [결과] IllegalStateException이 발생한다.
	 */
	@Test
	void updateThrowsWhenNoteIsPublished() {
		Note note = Note.builder()
			.status(NoteStatus.PUBLISHED)
			.tagText("tag")
			.sourceUrl(null)
			.build();
		when(noteRepository.findById(5L)).thenReturn(Optional.of(note));

        NoteUpdateRequest request = createNoteUpdateRequest(NoteStatus.IN_PROGRESS, 1L);

		assertThatThrownBy(() -> noteService.update(5L, request))
			.isInstanceOf(NoteInvalidStatusException.class)
			.hasMessageContaining("PUBLISHED 또는 ARCHIVED 노트는 수정할 수 없습니다.");
	}

	private NoteCreateRequest createNoteRequest(NoteStatus status, Long creatorId) {
		return new NoteCreateRequest(
			status,
			"tag",
			new NoteCoverDto("title", "teaser", "https://img.main"),
			new NoteOverviewDto("overview", "overview body", "https://img.overview"),
			new NoteRetrospectDto("retro", "retro body"),
			List.of(
			new NoteProcessDto((short)1, "process 1", "body 1", "https://img.process1"),
			new NoteProcessDto((short)2, "process 2", "body 2", "https://img.process2")
		),
			new NoteQuestionDto("question?"),
			creatorId,
			new NoteExternalLinkDto("https://source")
	);
	}

	private NoteUpdateRequest createNoteUpdateRequest(NoteStatus status, Long creatorId) {
		return new NoteUpdateRequest(
			status,
			"tag",
			new NoteCoverDto("title", "teaser", "https://img.main"),
			new NoteOverviewDto("overview", "overview body", "https://img.overview"),
			new NoteRetrospectDto("retro", "retro body"),
			List.of(
			new NoteProcessDto((short)1, "process 1", "body 1", "https://img.process1"),
			new NoteProcessDto((short)2, "process 2", "body 2", "https://img.process2")
		),
			new NoteQuestionDto("question?"),
			creatorId,
			new NoteExternalLinkDto("https://source")
	);
	}

	private User buildUser(UserRole role, Long id) {
		User user = User.builder()
			.email(role.name().toLowerCase() + "@test.com")
			.password("password")
			.username(role.name().toLowerCase())
			.role(role)
			.enabled(true)
			.accountNonExpired(true)
			.accountNonLocked(true)
			.credentialsNonExpired(true)
			.tokenVersion(0)
			.build();
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}
}
