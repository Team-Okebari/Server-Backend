package com.okebari.artbite.note.dto.summary;

public record ArchivedNoteSummaryResponse(
	Long id,
	String tagText,
	String title,
	String mainImageUrl,
	String teaser
) {
}
