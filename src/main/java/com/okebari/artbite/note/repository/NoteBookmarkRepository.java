package com.okebari.artbite.note.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

	/**
	 * 북마크 목록 검색. 제목/태그/작가명에 부분 일치하는 결과를 최신 저장 순으로 반환한다.
	 */
	@Query("""
		select nb from NoteBookmark nb
		join nb.note n
		left join n.cover c
		left join n.creator cr
		where nb.user.id = :userId
		and (
			lower(c.title) like lower(concat('%', :keyword, '%'))
			or lower(n.tagText) like lower(concat('%', :keyword, '%'))
			or lower(cr.name) like lower(concat('%', :keyword, '%'))
		)
		order by nb.createdAt desc
		""")
	List<NoteBookmark> searchByUserIdAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);
}
