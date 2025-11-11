package com.okebari.artbite.note.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "note_process")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoteProcess {

	@EmbeddedId
	private NoteProcessId id;

	@MapsId("noteId")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "note_id")
	private Note note;

	@Column(name = "section_title", nullable = false, length = 30)
	private String sectionTitle;

	@Column(name = "body_text", nullable = false, length = 500)
	private String bodyText;

	@Column(name = "image_url", nullable = false, length = 255)
	private String imageUrl;

	@Builder
	private NoteProcess(short position, String sectionTitle, String bodyText, String imageUrl) {
		this.id = new NoteProcessId(null, position);
		this.sectionTitle = sectionTitle;
		this.bodyText = bodyText;
		this.imageUrl = imageUrl;
	}

	void bindNote(Note note) {
		this.note = note;
		this.id = new NoteProcessId(note.getId(), this.id.getPosition());
	}

	public void update(String sectionTitle, String bodyText, String imageUrl) {
		this.sectionTitle = sectionTitle;
		this.bodyText = bodyText;
		this.imageUrl = imageUrl;
	}
}
