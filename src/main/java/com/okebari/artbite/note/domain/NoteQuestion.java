package com.okebari.artbite.note.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "note_question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoteQuestion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "note_id", nullable = false, unique = true)
	private Note note;

	@Column(name = "question_txt", nullable = false, length = 100)
	private String questionText;

	@OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<NoteAnswer> answers = new ArrayList<>();

	@Builder
	private NoteQuestion(String questionText) {
		this.questionText = questionText;
	}

	void bindNote(Note note) {
		this.note = note;
	}

	public void update(String questionText) {
		this.questionText = questionText;
	}
}
