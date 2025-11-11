package com.okebari.artbite.note.dto.note;

import java.time.LocalDateTime;

/**
 * 노트 커버 영역을 프론트에 내려줄 때 사용하는 DTO.
 * 작성자 이름/직함과 게시 시각까지 포함해 히어로 섹션을 한 번에 구성한다.
 */
public record NoteCoverResponse(
	String title,
	String teaser,
	String mainImageUrl,
	String creatorName,
	String creatorJobTitle,
	LocalDateTime publishedAt
) {
}
