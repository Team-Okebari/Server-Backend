package com.okebari.artbite.note.domain;

import lombok.Getter;

/**
 * 노트가 속한 카테고리를 나타내는 enum.
 * 기획에서 사용 중인 실제 카테고리 명칭을 그대로 Enum 값으로 관리한다.
 */
@Getter
public enum NoteCategoryType {
	MURAL("벽화"),
	EMOTICON("이모티콘"),
	GRAPHIC("그래픽"),
	PRODUCT("제품"),
	FASHION("패션"),
	THREE_D("3D"),
	BRANDING("브랜딩"),
	ILLUSTRATION("일러스트"),
	MEDIA_ART("미디어아트"),
	FURNITURE("가구"),
	THEATER_SIGN("극장 손간판"),
	LANDSCAPE("조경"),
	ALBUM_ARTWORK("음반 아트워크"),
	VISUAL_DIRECTING("비주얼 디렉팅"),
	NONE("카테고리 없음");

	private final String label;

	NoteCategoryType(String label) {
		this.label = label;
	}
}
