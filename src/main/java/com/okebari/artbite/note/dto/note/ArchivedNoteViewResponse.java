package com.okebari.artbite.note.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "아카이브된 노트 상세/미리보기 응답 DTO")
public record ArchivedNoteViewResponse(
	@Schema(description = "노트 접근 가능 여부", example = "true")
	boolean accessible,
	@Schema(description = "노트 상세 정보 (accessible이 true일 경우)")
	NoteResponse note,
	@Schema(description = "노트 미리보기 정보 (accessible이 false일 경우)")
	NotePreviewResponse preview
) {
}
