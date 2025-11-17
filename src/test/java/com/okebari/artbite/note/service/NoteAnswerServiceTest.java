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

import com.okebari.artbite.common.exception.NoteInvalidStatusException;
import com.okebari.artbite.common.exception.NoteNotFoundException;
import com.okebari.artbite.common.exception.UserNotFoundException;
import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.domain.user.UserRepository;
import com.okebari.artbite.domain.user.UserRole;
import com.okebari.artbite.note.domain.NoteAnswer;
import com.okebari.artbite.note.domain.NoteQuestion;
import com.okebari.artbite.note.dto.answer.NoteAnswerDto;
import com.okebari.artbite.note.mapper.NoteMapper;
import com.okebari.artbite.note.repository.NoteAnswerRepository;
import com.okebari.artbite.note.repository.NoteQuestionRepository;

@ExtendWith(MockitoExtension.class)
class NoteAnswerServiceTest {

	@Mock
	private NoteQuestionRepository noteQuestionRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private NoteAnswerRepository noteAnswerRepository; // Added

	@Mock
	private NoteMapper noteMapper;

	@InjectMocks
	private NoteAnswerService noteAnswerService;

	@Test
	void createAnswerPersistsNewEntity() {
		User user = buildUser(10L, UserRole.USER);
		NoteQuestion question = buildQuestion(5L);
		NoteAnswer newAnswer = NoteAnswer.builder().respondent(user).answerText("answer").build();
		ReflectionTestUtils.setField(newAnswer, "id", 1L); // Set ID for the new answer

		when(userRepository.findById(10L)).thenReturn(Optional.of(user));
		when(noteQuestionRepository.findById(5L)).thenReturn(Optional.of(question));
		when(noteAnswerRepository.findByQuestionIdAndRespondentId(5L, 10L)).thenReturn(Optional.empty());
		when(noteAnswerRepository.save(any(NoteAnswer.class))).thenReturn(newAnswer);
		when(noteMapper.toAnswerDto(any(NoteAnswer.class))).thenReturn(new NoteAnswerDto(1L, 5L, 10L, "answer"));

		NoteAnswerDto dto = noteAnswerService.createAnswer(5L, 10L, "answer");

		assertThat(dto.answerText()).isEqualTo("answer");
		verify(noteAnswerRepository).save(any(NoteAnswer.class));
	}

	@Test
	void createAnswerFailsWhenAlreadyExists() {
		User user = buildUser(10L, UserRole.USER);
		NoteAnswer existingAnswer = NoteAnswer.builder().respondent(user).answerText("old").build();
		NoteQuestion question = buildQuestion(5L);

		when(userRepository.findById(10L)).thenReturn(Optional.of(user));
		when(noteQuestionRepository.findById(5L)).thenReturn(Optional.of(question));
		when(noteAnswerRepository.findByQuestionIdAndRespondentId(5L, 10L)).thenReturn(Optional.of(existingAnswer));

		assertThatThrownBy(() -> noteAnswerService.createAnswer(5L, 10L, "new"))
			.isInstanceOf(NoteInvalidStatusException.class)
			.hasMessageContaining("이미 등록된 답변이 있습니다");
		verify(noteAnswerRepository, never()).save(any());
	}

	@Test
	void updateAnswerModifiesExistingEntity() {
		User user = buildUser(10L, UserRole.USER);
		NoteAnswer existingAnswer = NoteAnswer.builder().respondent(user).answerText("old").build();
		NoteQuestion question = buildQuestion(7L);

		when(userRepository.findById(10L)).thenReturn(Optional.of(user));
		when(noteQuestionRepository.findById(7L)).thenReturn(Optional.of(question));
		when(noteAnswerRepository.findByQuestionIdAndRespondentId(7L, 10L)).thenReturn(Optional.of(existingAnswer));
		when(noteAnswerRepository.save(any(NoteAnswer.class))).thenReturn(existingAnswer); // Return the updated answer
		when(noteMapper.toAnswerDto(any(NoteAnswer.class))).thenReturn(new NoteAnswerDto(2L, 7L, 10L, "updated"));

		NoteAnswerDto dto = noteAnswerService.updateAnswer(7L, 10L, "updated");

		assertThat(dto.answerText()).isEqualTo("updated");
		verify(noteAnswerRepository).save(existingAnswer);
		assertThat(existingAnswer.getAnswerText()).isEqualTo("updated");
	}

	@Test
	void updateAnswerFailsWhenMissing() {
		User user = buildUser(10L, UserRole.USER);
		NoteQuestion question = buildQuestion(7L);

		when(userRepository.findById(10L)).thenReturn(Optional.of(user));
		when(noteQuestionRepository.findById(7L)).thenReturn(Optional.of(question));
		when(noteAnswerRepository.findByQuestionIdAndRespondentId(7L, 10L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> noteAnswerService.updateAnswer(7L, 10L, "updated"))
			.isInstanceOf(NoteInvalidStatusException.class)
			.hasMessageContaining("등록된 답변이 없습니다");
		verify(noteAnswerRepository, never()).save(any());
	}

	@Test
	void deleteAnswerRemovesEntity() {
		User user = buildUser(10L, UserRole.USER);
		NoteAnswer existingAnswer = NoteAnswer.builder().respondent(user).answerText("data").build();
		NoteQuestion question = buildQuestion(8L);

		when(userRepository.findById(10L)).thenReturn(Optional.of(user));
		when(noteQuestionRepository.findById(8L)).thenReturn(Optional.of(question));
		when(noteAnswerRepository.findByQuestionIdAndRespondentId(8L, 10L)).thenReturn(Optional.of(existingAnswer));
		doNothing().when(noteAnswerRepository).delete(existingAnswer);

		noteAnswerService.deleteAnswer(8L, 10L);

		verify(noteAnswerRepository).delete(existingAnswer);
	}

	@Test
	void deleteAnswerFailsWhenMissing() {
		User user = buildUser(10L, UserRole.USER);
		NoteQuestion question = buildQuestion(8L);

		when(userRepository.findById(10L)).thenReturn(Optional.of(user));
		when(noteQuestionRepository.findById(8L)).thenReturn(Optional.of(question));
		when(noteAnswerRepository.findByQuestionIdAndRespondentId(8L, 10L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> noteAnswerService.deleteAnswer(8L, 10L))
			.isInstanceOf(NoteInvalidStatusException.class)
			.hasMessageContaining("삭제할 답변이 존재하지 않습니다");
		verify(noteAnswerRepository, never()).delete(any());
	}

	@Test
	void getAnswerReturnsExistingEntity() {
		User user = buildUser(10L, UserRole.USER);
		NoteAnswer existingAnswer = NoteAnswer.builder().respondent(user).answerText("data").build();
		NoteQuestion question = buildQuestion(9L);
		NoteAnswerDto expectedDto = new NoteAnswerDto(3L, 9L, 10L, "data");

		when(userRepository.findById(10L)).thenReturn(Optional.of(user));
		when(noteQuestionRepository.findById(9L)).thenReturn(Optional.of(question));
		when(noteAnswerRepository.findByQuestionIdAndRespondentId(9L, 10L)).thenReturn(Optional.of(existingAnswer));
		when(noteMapper.toAnswerDto(any(NoteAnswer.class))).thenReturn(expectedDto);

		NoteAnswerDto dto = noteAnswerService.getAnswer(9L, 10L);

		assertThat(dto).isNotNull();
		assertThat(dto.answerText()).isEqualTo(expectedDto.answerText());
		assertThat(dto.questionId()).isEqualTo(expectedDto.questionId());
		assertThat(dto.respondentId()).isEqualTo(expectedDto.respondentId());
	}

	@Test
	void getAnswerReturnsNullWhenMissing() {
		User user = buildUser(10L, UserRole.USER);
		NoteQuestion question = buildQuestion(9L);

		when(userRepository.findById(10L)).thenReturn(Optional.of(user));
		when(noteQuestionRepository.findById(9L)).thenReturn(Optional.of(question));
		when(noteAnswerRepository.findByQuestionIdAndRespondentId(9L, 10L)).thenReturn(Optional.empty());

		NoteAnswerDto dto = noteAnswerService.getAnswer(9L, 10L);

		assertThat(dto).isNull();
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

	private NoteQuestion buildQuestion(Long id) {
		NoteQuestion question = NoteQuestion.builder()
			.questionText("Q?")
			.build();
		setId(question, id);
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
