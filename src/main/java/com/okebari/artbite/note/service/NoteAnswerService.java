package com.okebari.artbite.note.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.okebari.artbite.common.exception.NoteAccessDeniedException;
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
import com.okebari.artbite.note.repository.NoteQuestionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class NoteAnswerService {

	private final NoteQuestionRepository questionRepository;
	private final UserRepository userRepository;
	private final NoteMapper noteMapper;

	/**
	 * USER 롤 사용자가 질문에 대한 답변을 최초 작성한다.
	 * 기존 답변이 존재하면 예외를 던진다.
	 */
	public NoteAnswerDto createAnswer(Long questionId, Long userId, String answerText) {
		User user = loadUser(userId);
		NoteQuestion question = loadQuestion(questionId);

		if (question.getAnswer() != null) {
			throw new NoteInvalidStatusException("이미 등록된 답변이 있습니다. 수정 API를 사용하세요.");
		}

		NoteAnswer answer = NoteAnswer.builder()
			.respondent(user)
			.answerText(answerText)
			.build();
		question.assignAnswer(answer);

		return noteMapper.toAnswerDto(questionRepository.save(question).getAnswer());
	}

	/**
	 * USER 롤 사용자가 기존 답변을 수정한다.
	 * 답변이 없으면 예외를 던진다.
	 */
	public NoteAnswerDto updateAnswer(Long questionId, Long userId, String answerText) {
		loadUser(userId); // 권한 검증
		NoteQuestion question = loadQuestion(questionId);

		NoteAnswer answer = question.getAnswer();
		if (answer == null) {
			throw new NoteInvalidStatusException("등록된 답변이 없습니다. 먼저 생성하세요.");
		}
		if (answer.getRespondent() != null && !answer.getRespondent().getId().equals(userId)) {
			throw new NoteAccessDeniedException("다른 사용자의 답변은 수정할 수 없습니다.");
		}

		answer.update(answerText);
		return noteMapper.toAnswerDto(questionRepository.save(question).getAnswer());
	}

	/**
	 * USER 롤 사용자가 자신의 답변을 삭제한다.
	 */
	public void deleteAnswer(Long questionId, Long userId) {
		loadUser(userId); // 권한 검증
		NoteQuestion question = loadQuestion(questionId);

		NoteAnswer answer = question.getAnswer();
		if (answer == null) {
			throw new NoteInvalidStatusException("삭제할 답변이 존재하지 않습니다.");
		}
		if (answer.getRespondent() != null && !answer.getRespondent().getId().equals(userId)) {
			throw new NoteAccessDeniedException("다른 사용자의 답변은 삭제할 수 없습니다.");
		}

		question.assignAnswer(null);
		questionRepository.save(question);
	}

	private User loadUser(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new UserNotFoundException());
		if (user.getRole() != UserRole.USER) {
			throw new NoteAccessDeniedException("USER 권한만 답변을 작성할 수 있습니다.");
		}
		return user;
	}

	private NoteQuestion loadQuestion(Long questionId) {
		return questionRepository.findById(questionId)
			.orElseThrow(() -> new NoteNotFoundException("해당 질문을 찾을 수 없습니다."));
	}
}
