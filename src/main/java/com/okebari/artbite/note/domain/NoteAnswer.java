package com.okebari.artbite.note.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.okebari.artbite.domain.common.BaseTimeEntity;
import com.okebari.artbite.domain.user.User;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "note_answer", uniqueConstraints = {
	@UniqueConstraint(columnNames = {"question_id", "user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoteAnswer extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "question_id", nullable = false)
	private NoteQuestion question;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User respondent;

	@Column(name = "answer_txt", length = 200)
	private String answerText;

	@Builder
	private NoteAnswer(User respondent, String answerText) {
		this.respondent = respondent;
		this.answerText = answerText;
	}

	public void bindQuestion(NoteQuestion question) {
		this.question = question;
	}

	void clearQuestion() {
		this.question = null;
	}

	public void update(String answerText) {
		this.answerText = answerText;
	}
}
