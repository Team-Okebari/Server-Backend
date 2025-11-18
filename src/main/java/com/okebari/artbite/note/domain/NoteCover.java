package com.okebari.artbite.note.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "note_cover")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoteCover {

	@Id
	private Long id;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "note_id")
	private Note note;

	@Column(nullable = false, length = 30)
	private String title;

	@Column(nullable = false, length = 100)
	private String teaser;

	@Column(name = "main_image_url", nullable = false, length = 500)
	private String mainImageUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false, length = 30)
	private NoteCategoryType category = NoteCategoryType.NONE;

	@Builder
	private NoteCover(String title, String teaser, String mainImageUrl, NoteCategoryType category) {
		this.title = title;
		this.teaser = teaser;
		this.mainImageUrl = mainImageUrl;
		this.category = category != null ? category : NoteCategoryType.NONE;
	}

	void bindNote(Note note) {
		this.note = note;
		this.id = note.getId();
	}

	public void update(String title, String teaser, String mainImageUrl, NoteCategoryType category) {
		this.title = title;
		this.teaser = teaser;
		this.mainImageUrl = mainImageUrl;
		this.category = category != null ? category : NoteCategoryType.NONE;
	}
}
