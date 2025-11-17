package com.okebari.artbite.note.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "note_overview")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoteOverview {

	@Id
	private Long id;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "note_id")
	private Note note;

	@Column(nullable = false, length = 30)
	private String sectionTitle;

	@Column(nullable = false, length = 200)
	private String bodyText;

	@Column(nullable = false, length = 500)
	private String imageUrl;

	@Builder
	private NoteOverview(String sectionTitle, String bodyText, String imageUrl) {
		this.sectionTitle = sectionTitle;
		this.bodyText = bodyText;
		this.imageUrl = imageUrl;
	}

	void bindNote(Note note) {
		this.note = note;
		this.id = note.getId();
	}

	public void update(String sectionTitle, String bodyText, String imageUrl) {
		this.sectionTitle = sectionTitle;
		this.bodyText = bodyText;
		this.imageUrl = imageUrl;
	}
}
