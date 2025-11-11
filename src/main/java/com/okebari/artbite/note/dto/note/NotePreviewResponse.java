package com.okebari.artbite.note.dto.note;

import com.okebari.artbite.creator.dto.CreatorSummaryDto;

/**
 * 무료 사용자용 노트 미리보기 응답 DTO.
 * 커버 영역과 개요 일부만 내려 사용자가 구독 여부를 판단하도록 한다.
 */
public record NotePreviewResponse(
	Long id,
	NoteCoverResponse cover,
	String overviewPreview,
	NoteExternalLinkDto externalLink,
	CreatorSummaryDto creator
) {
}
