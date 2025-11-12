package com.okebari.artbite.note.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.okebari.artbite.common.exception.NoteNotFoundException;
import com.okebari.artbite.common.exception.UserNotFoundException;
import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.domain.user.UserRepository;
import com.okebari.artbite.note.domain.Note;
import com.okebari.artbite.note.domain.NoteBookmark;
import com.okebari.artbite.note.dto.bookmark.NoteBookmarkResponse;
import com.okebari.artbite.note.mapper.NoteMapper;
import com.okebari.artbite.note.repository.NoteBookmarkRepository;
import com.okebari.artbite.note.repository.NoteRepository;

import lombok.RequiredArgsConstructor;

/**
 * 노트 북마크 토글 및 조회를 담당하는 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class NoteBookmarkService {

	private final NoteBookmarkRepository bookmarkRepository;
	private final NoteRepository noteRepository;
	private final UserRepository userRepository;
	private final NoteMapper noteMapper;

	/**
	 * 북마크 상태를 토글한다. 이미 북마크 되어 있으면 삭제(해제)하고 false,
	 * 없으면 새로 등록하고 true를 반환한다. 프론트는 반환값으로 최종 상태를 판단한다.
	 */
	public boolean toggle(Long noteId, Long userId) {
		NoteBookmark existing = bookmarkRepository.findByNoteIdAndUserId(noteId, userId).orElse(null);
		if (existing != null) {
			bookmarkRepository.delete(existing);
			return false;
		}

		Note note = noteRepository.findById(noteId)
			.orElseThrow(() -> new NoteNotFoundException(noteId));
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new UserNotFoundException());

		bookmarkRepository.save(NoteBookmark.builder()
			.note(note)
			.user(user)
			.build());
		return true;
	}

	/**
	 * 사용자의 북마크 목록을 최신 순으로 조회한다.
	 * 서비스 DTO(`NoteBookmarkResponse`)를 반환하고, 컨트롤러에서 프론트 전용 DTO로 변환한다.
	 */
	@Transactional(readOnly = true)
	public List<NoteBookmarkResponse> list(Long userId, String keyword) {
		List<NoteBookmark> bookmarks = (keyword == null || keyword.isBlank())
			? bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId)
			: bookmarkRepository.searchByUserIdAndKeyword(userId, keyword.trim());
		return bookmarks.stream()
			.map(bookmark -> noteMapper.toBookmarkResponse(bookmark))
			.toList();
	}
}
