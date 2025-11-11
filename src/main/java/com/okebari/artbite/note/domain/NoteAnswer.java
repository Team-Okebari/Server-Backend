package com.okebari.artbite.note.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.okebari.artbite.domain.common.BaseTimeEntity;
import com.okebari.artbite.domain.user.User;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "note_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoteAnswer extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "question_id", nullable = false, unique = true)
	private NoteQuestion question;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User respondent;

	@Column(name = "answer_txt", length = 200)
	private String answerText;

	@Builder
	private NoteAnswer(User respondent, String answerText) {
		this.respondent = respondent;
		this.answerText = answerText;
	}

	void bindQuestion(NoteQuestion question) {
		this.question = question;
	}

	void clearQuestion() {
		this.question = null;
	}

	public void update(String answerText) {
		this.answerText = answerText;
	}
}
