package com.okebari.artbite.note.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.okebari.artbite.note.domain.NoteQuestion;

public interface NoteQuestionRepository extends JpaRepository<NoteQuestion, Long> {
}
