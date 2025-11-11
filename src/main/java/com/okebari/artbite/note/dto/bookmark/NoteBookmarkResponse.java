package com.okebari.artbite.note.dto.bookmark;

import java.time.LocalDateTime;

public record NoteBookmarkResponse(
	Long bookmarkId,
	Long noteId,
	String title,
	String mainImageUrl,
	String creatorName,
	String creatorJobTitle,
	LocalDateTime bookmarkedAt
) {
}
