package com.okebari.artbite.tracking.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.okebari.artbite.domain.tracking.ContentAccessLog;
import com.okebari.artbite.domain.tracking.ContentAccessLogRepository;
import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.note.domain.Note;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContentAccessLogService {

	private final ContentAccessLogRepository contentAccessLogRepository;

	/**
	 * 사용자의 유료 노트 접근 기록을 저장합니다.
	 * 호출한 서비스의 트랜잭션과 별개로, 항상 새로운 쓰기 트랜잭션에서 실행됩니다.
	 * @param user 접근한 사용자
	 * @param note 접근된 노트
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void logNoteAccess(User user, Note note) {
		ContentAccessLog log = ContentAccessLog.builder()
			.user(user)
			.note(note)
			.build();
		contentAccessLogRepository.save(log);
	}

	/**
	 * 특정 시점 이후에 사용자가 유료 콘텐츠에 접근한 기록이 있는지 확인합니다.
	 * @param user 확인할 사용자
	 * @param timestamp 기준 시점
	 * @return 접근 기록이 있으면 true, 없으면 false
	 */
	@Transactional(readOnly = true)
	public boolean hasUserAccessedContentSince(User user, LocalDateTime timestamp) {
		if (timestamp == null) {
			return false; // 기준 시점이 없으면 확인 불가
		}
		return contentAccessLogRepository.existsByUserAndAccessedAtAfter(user, timestamp);
	}
}
