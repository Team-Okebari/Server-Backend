package com.okebari.artbite.note.dto.note;

public record ArchivedNoteViewResponse(
	boolean accessible,
	NoteResponse note,
	NotePreviewResponse preview
) {
}
