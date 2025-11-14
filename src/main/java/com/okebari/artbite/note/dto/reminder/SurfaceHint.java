package com.okebari.artbite.note.dto.reminder;

/**
 * 프론트에 노출 방향을 알려주는 힌트.
 */
public enum SurfaceHint {
	DEFERRED, // 첫 접속: 배너 노출하지 않고 firstVisitAt만 기록
	BANNER,   // 배너 노출: 두 번째 접속 이상 또는 이미 본 상태
	NONE      // 노출 없음: dismiss 처리 or 후보 없음
}
