package com.okebari.artbite.note.dto.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 제작 과정 DTO는 노트 작성/수정 응답에서 함께 사용되므로 note 하위 패키지에 배치한다.
 */
public record NoteProcessDto(
    @NotNull short position,
    @NotBlank String sectionTitle,
    @NotBlank String bodyText,
    @NotBlank String imageUrl
) {
}
