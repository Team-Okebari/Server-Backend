package com.okebari.artbite.note.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import com.okebari.artbite.common.exception.NoteNotFoundException;
import com.okebari.artbite.common.exception.UserNotFoundException;
import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.domain.user.UserRepository;
import com.okebari.artbite.domain.user.UserRole;
import com.okebari.artbite.note.domain.NoteAnswer;
import com.okebari.artbite.note.domain.NoteQuestion;
import com.okebari.artbite.note.dto.answer.NoteAnswerDto;
import com.okebari.artbite.note.exception.NoteAccessDeniedException;
import com.okebari.artbite.note.exception.NoteInvalidStatusException;
import com.okebari.artbite.note.mapper.NoteMapper;
import com.okebari.artbite.note.repository.NoteQuestionRepository;

@ExtendWith(MockitoExtension.class)
class NoteAnswerServiceTest {

	@Mock
	private NoteQuestionRepository noteQuestionRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private NoteMapper noteMapper;

	@InjectMocks
	private NoteAnswerService noteAnswerService;

	@Test
	void createAnswerPersistsNewEntity() {
		User user = buildUser(10L, UserRole.USER);
		NoteQuestion question = buildQuestion(5L, null);

		when(userRepository.findById(10L)).thenReturn(Optional.of(user));
		when(noteQuestionRepository.findById(5L)).thenReturn(Optional.of(question));
		when(noteQuestionRepository.save(question)).thenReturn(question);
		when(noteMapper.toAnswerDto(any())).thenReturn(new NoteAnswerDto(1L, 5L, 10L, "answer"));

		NoteAnswerDto dto = noteAnswerService.createAnswer(5L, 10L, "answer");

		assertThat(dto.answerText()).isEqualTo("answer");
		verify(noteQuestionRepository).save(question);
	}

	@Test
	void createAnswerFailsWhenAlreadyExists() {
		User user = buildUser(10L, UserRole.USER);
		NoteAnswer answer = NoteAnswer.builder()
			.respondent(user)
			.answerText("old")
			.build();
		NoteQuestion question = buildQuestion(5L, answer);

		when(userRepository.findById(10L)).thenReturn(Optional.of(user));
		when(noteQuestionRepository.findById(5L)).thenReturn(Optional.of(question));

		assertThatThrownBy(() -> noteAnswerService.createAnswer(5L, 10L, "new"))
			.isInstanceOf(NoteInvalidStatusException.class)
			.hasMessageContaining("이미 등록된 답변");
		verify(noteQuestionRepository, never()).save(any());
	}

	@Test
	void updateAnswerModifiesExistingEntity() {
		User user = buildUser(10L, UserRole.USER);
		NoteAnswer answer = NoteAnswer.builder()
			.respondent(user)
			.answerText("old")
			.build();
		NoteQuestion question = buildQuestion(7L, answer);

		when(userRepository.findById(10L)).thenReturn(Optional.of(user));
		when(noteQuestionRepository.findById(7L)).thenReturn(Optional.of(question));
		when(noteQuestionRepository.save(question)).thenReturn(question);
		when(noteMapper.toAnswerDto(question.getAnswer()))
			.thenReturn(new NoteAnswerDto(2L, 7L, 10L, "updated"));

		NoteAnswerDto dto = noteAnswerService.updateAnswer(7L, 10L, "updated");

		assertThat(dto.answerText()).isEqualTo("updated");
		verify(noteQuestionRepository).save(question);
	}

	@Test
	void updateAnswerFailsWhenMissing() {
		User user = buildUser(10L, UserRole.USER);
		NoteQuestion question = buildQuestion(7L, null);

		when(userRepository.findById(10L)).thenReturn(Optional.of(user));
		when(noteQuestionRepository.findById(7L)).thenReturn(Optional.of(question));

		assertThatThrownBy(() -> noteAnswerService.updateAnswer(7L, 10L, "updated"))
			.isInstanceOf(NoteInvalidStatusException.class)
			.hasMessageContaining("등록된 답변이 없습니다");
	}

	@Test
	void deleteAnswerRemovesAssociation() {
		User user = buildUser(10L, UserRole.USER);
		NoteAnswer answer = NoteAnswer.builder()
			.respondent(user)
			.answerText("data")
			.build();
		NoteQuestion question = buildQuestion(8L, answer);

		when(userRepository.findById(10L)).thenReturn(Optional.of(user));
		when(noteQuestionRepository.findById(8L)).thenReturn(Optional.of(question));

		noteAnswerService.deleteAnswer(8L, 10L);

		verify(noteQuestionRepository).save(question);
		assertThat(question.getAnswer()).isNull();
	}

	@Test
	void deleteAnswerRejectsOtherRespondent() {
		User user = buildUser(10L, UserRole.USER);
		User other = buildUser(20L, UserRole.USER);
		NoteAnswer answer = NoteAnswer.builder()
			.respondent(other)
			.answerText("data")
			.build();
		NoteQuestion question = buildQuestion(8L, answer);

		when(userRepository.findById(10L)).thenReturn(Optional.of(user));
		when(noteQuestionRepository.findById(8L)).thenReturn(Optional.of(question));

		assertThatThrownBy(() -> noteAnswerService.deleteAnswer(8L, 10L))
			.isInstanceOf(NoteAccessDeniedException.class);
	}

	@Test
	void createAnswerThrowsWhenUserNotFound() {
		when(userRepository.findById(1L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> noteAnswerService.createAnswer(1L, 1L, "answer"))
			.isInstanceOf(UserNotFoundException.class);
	}

	@Test
	void createAnswerThrowsWhenQuestionNotFound() {
		User user = buildUser(1L, UserRole.USER);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(noteQuestionRepository.findById(1L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> noteAnswerService.createAnswer(1L, 1L, "answer"))
			.isInstanceOf(NoteNotFoundException.class);
	}

	private User buildUser(Long id, UserRole role) {
		User user = User.builder()
			.email(id + "@test.com")
			.password("pw")
			.username("user")
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

	private NoteQuestion buildQuestion(Long id, NoteAnswer answer) {
		NoteQuestion question = NoteQuestion.builder()
			.questionText("Q?")
			.build();
		setId(question, id);
		if (answer != null) {
			question.assignAnswer(answer);
		}
		return question;
	}

	private void setId(NoteQuestion question, Long id) {
		try {
			var field = NoteQuestion.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(question, id);
		} catch (ReflectiveOperationException ignored) {
		}
	}
}
