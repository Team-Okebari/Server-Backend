package com.okebari.artbite.note.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.okebari.artbite.note.domain.NoteReminder;

public interface NoteReminderRepository extends JpaRepository<NoteReminder, Long> {

	Optional<NoteReminder> findByUserIdAndReminderDate(Long userId, LocalDate reminderDate);

	void deleteByUserIdAndReminderDate(Long userId, LocalDate reminderDate);

	@Query("""
		select r from NoteReminder r
		where r.reminderDate = :date
		order by r.userId
		""")
	Stream<NoteReminder> streamAllByReminderDate(@Param("date") LocalDate date);
}
