package com.okebari.artbite.note.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.domain.user.UserRepository;
import com.okebari.artbite.domain.user.UserRole;
import com.okebari.artbite.note.domain.Note;
import com.okebari.artbite.note.domain.NoteBookmark;
import com.okebari.artbite.note.domain.NoteStatus;
import com.okebari.artbite.note.dto.bookmark.NoteBookmarkResponse;
import com.okebari.artbite.note.mapper.NoteMapper;
import com.okebari.artbite.note.repository.NoteBookmarkRepository;
import com.okebari.artbite.note.repository.NoteRepository;

/**
 * NoteBookmarkService 단위 테스트.
 * MockitoExtension을 사용해 목 의존성을 자동 주입한다.
 */
@ExtendWith(MockitoExtension.class)
class NoteBookmarkServiceTest {

	/**
	 * 실제 DB 없이 북마크 저장/삭제 동작을 검증하기 위해 목 리포지토리를 사용한다.
	 */
	@Mock
	private NoteBookmarkRepository bookmarkRepository;

	/**
	 * 노트 존재 여부 확인을 시뮬레이션하기 위한 목.
	 */
	@Mock
	private NoteRepository noteRepository;

	/**
	 * 사용자 조회 로직을 목으로 만들어 인증된 사용자 시나리오를 제어한다.
	 */
	@Mock
	private UserRepository userRepository;

	/**
	 * 응답 DTO 변환은 별도 책임으로 분리되어 있으므로, 실제 매퍼 호출 여부만 확인한다.
	 */
	@Mock
	private NoteMapper noteMapper;

	/**
	 * @InjectMocks는 위 목들을 주입한 NoteBookmarkService 인스턴스를 생성한다.
	 */
	@InjectMocks
	private NoteBookmarkService noteBookmarkService;

	/**
	 * [목적] 이미 북마크된 경우 다시 토글하면 삭제되어야 한다.
	 * [결과] delete가 호출되고 반환값은 false다.
	 */
	@Test
	void toggleDeletesBookmarkWhenAlreadyExists() {
		User user = buildUser(1L, UserRole.USER);
		Note note = Note.builder()
			.status(NoteStatus.IN_PROGRESS)
			.tagText("tag")
			.sourceUrl(null)
			.build();
		NoteBookmark bookmark = NoteBookmark.builder()
			.note(note)
			.user(user)
			.build();

		when(bookmarkRepository.findByNoteIdAndUserId(5L, 1L))
			.thenReturn(Optional.of(bookmark));

		boolean bookmarked = noteBookmarkService.toggle(5L, 1L);

		assertThat(bookmarked).isFalse();
		verify(bookmarkRepository).delete(bookmark);
		verify(bookmarkRepository, never()).save(any());
	}

	/**
	 * [목적] 북마크가 존재하지 않을 때 토글하면 새 북마크가 생성되어야 한다.
	 * [결과] save가 호출되고 반환값은 true다.
	 */
	@Test
	void toggleCreatesBookmarkWhenAbsent() {
		User user = buildUser(2L, UserRole.USER);
		Note note = Note.builder()
			.status(NoteStatus.IN_PROGRESS)
			.tagText("tag")
			.sourceUrl(null)
			.build();

		when(bookmarkRepository.findByNoteIdAndUserId(7L, 2L))
			.thenReturn(Optional.empty());
		when(noteRepository.findById(7L)).thenReturn(Optional.of(note));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user));

		boolean bookmarked = noteBookmarkService.toggle(7L, 2L);

		assertThat(bookmarked).isTrue();
		ArgumentCaptor<NoteBookmark> captor = ArgumentCaptor.forClass(NoteBookmark.class);
		verify(bookmarkRepository).save(captor.capture());
		NoteBookmark saved = captor.getValue();
		assertThat(saved.getNote()).isEqualTo(note);
		assertThat(saved.getUser()).isEqualTo(user);
	}

	/**
	 * [목적] 사용자 북마크 목록을 조회하면 매퍼가 만든 DTO 목록을 그대로 돌려줘야 한다.
	 * [결과] findByUserIdOrderByCreatedAtDesc 결과를 매퍼가 변환한 값이 반환된다.
	 */
	@Test
	void listReturnsResponsesFromMapper() {
		User user = buildUser(3L, UserRole.USER);
		Note note = Note.builder()
			.status(NoteStatus.IN_PROGRESS)
			.tagText("tag")
			.sourceUrl(null)
			.build();
		NoteBookmark bookmark = NoteBookmark.builder()
			.note(note)
			.user(user)
			.build();

		when(bookmarkRepository.findByUserIdOrderByCreatedAtDesc(3L))
			.thenReturn(List.of(bookmark));

		NoteBookmarkResponse response = new NoteBookmarkResponse(
			10L, 20L, "title", "image", "creator", "jobTitle", bookmark.getCreatedAt());
		when(noteMapper.toBookmarkResponse(bookmark)).thenReturn(response);

		assertThat(noteBookmarkService.list(3L))
			.containsExactly(response);
	}

	private User buildUser(Long id, UserRole role) {
		User user = User.builder()
			.email(id + "@test.com")
			.password("password")
			.username("user" + id)
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
