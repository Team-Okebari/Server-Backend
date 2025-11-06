package com.okebari.artbite.note.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.okebari.artbite.note.domain.NoteBookmark;

public interface NoteBookmarkRepository extends JpaRepository<NoteBookmark, Long> {

	/**
	 * 특정 사용자가 특정 노트를 북마크했는지 여부를 확인한다.
	 */
	Optional<NoteBookmark> findByNoteIdAndUserId(Long noteId, Long userId);

	/**
	 * 사용자별 북마크 목록을 최신 순으로 돌려준다.
	 */
	List<NoteBookmark> findByUserIdOrderByCreatedAtDesc(Long userId);
}
