package com.okebari.artbite.note.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.okebari.artbite.note.domain.NoteAnswer;

public interface NoteAnswerRepository extends JpaRepository<NoteAnswer, Long> {

	@Query("select distinct na.respondent.id from NoteAnswer na where na.respondent is not null")
	List<Long> findDistinctRespondentIds();

	@Query("""
		select q.note.id from NoteAnswer na
		join na.question q
		where na.respondent.id = :userId
		""")
	List<Long> findNoteIdsByRespondentId(@Param("userId") Long userId);
}
