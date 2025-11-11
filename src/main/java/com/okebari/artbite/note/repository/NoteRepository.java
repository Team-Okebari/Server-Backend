package com.okebari.artbite.note.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.okebari.artbite.note.domain.Note;
import com.okebari.artbite.note.domain.NoteStatus;

public interface NoteRepository extends JpaRepository<Note, Long> {

	/**
	 * 작업 중이거나 작성이 완료된 노트를 최신 작성 순으로 돌려준다.
	 * 관리자 화면에서 임시 저장본과 작성 완료본을 함께 확인할 때 사용한다.
	 */
	@Query("select n from Note n where n.status in ('IN_PROGRESS','COMPLETED') order by n.createdAt desc")
	List<Note> findAllDrafts();

	/**
	 * 메인 화면 노출을 위해 게시(PUBLISHED) 상태의 노트를 게시 시각 역순으로 조회한다.
	 */
	@Query("select n from Note n where n.status = 'PUBLISHED' order by n.publishedAt desc")
	List<Note> findAllPublished();

	/**
	 * 지난 노트 전용 목록을 위해 아카이브(ARCHIVED) 상태 노트를 페이징한다.
	 */
	@Query("select n from Note n where n.status = 'ARCHIVED' order by n.archivedAt desc")
	Page<Note> findAllArchived(Pageable pageable);

	/**
	 * 지난 노트 검색. 제목/태그/작가명에 부분 일치하는 결과를 최신 아카이브 순으로 반환한다.
	 */
	@Query("""
		select n from Note n
		left join n.cover c
		left join n.creator cr
		where n.status = 'ARCHIVED'
		and (
			lower(c.title) like lower(concat('%', :keyword, '%'))
			or lower(n.tagText) like lower(concat('%', :keyword, '%'))
			or lower(cr.name) like lower(concat('%', :keyword, '%'))
		)
		order by n.archivedAt desc
		""")
	Page<Note> searchArchived(@Param("keyword") String keyword, Pageable pageable);

	/**
	 * 특정 상태의 노트를 전부 조회한다. 스케줄러에서 상태 전환 대상을 가져올 때 사용한다.
	 */
	List<Note> findByStatus(NoteStatus status);

	/**
	 * 작성 완료 상태 노트를 가장 오래된 업데이트 순으로 정렬해 가져온다.
	 * 자정 자동 배포 시 가장 먼저 작성된 노트를 우선 선택하기 위함.
	 */
	@Query("select n from Note n where n.status = 'COMPLETED' order by n.updatedAt asc")
	List<Note> findCompletedOrderByUpdatedAtAsc();

	/**
	 * 지정한 시각 이전에 게시된 노트 목록을 조회한다.
	 * 게시 후 24시간이 지난 노트를 찾아 아카이브 전환할 때 활용한다.
	 */
	@Query("select n from Note n where n.status = 'PUBLISHED' and n.publishedAt <= :before")
	List<Note> findPublishedBefore(@Param("before") LocalDateTime before);

	/**
	 * 특정 일자(KST 기준)의 게시 노트를 조회한다.
	 */
	Optional<Note> findFirstByStatusAndPublishedAtBetween(
		NoteStatus status,
		LocalDateTime startInclusive,
		LocalDateTime endExclusive
	);
}
