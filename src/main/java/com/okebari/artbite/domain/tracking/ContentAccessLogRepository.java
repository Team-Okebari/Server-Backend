package com.okebari.artbite.domain.tracking;

import java.time.LocalDateTime;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import com.okebari.artbite.domain.user.User;

public interface ContentAccessLogRepository extends JpaRepository<ContentAccessLog, Long> {

	/**
	 * 특정 사용자
	 * @param user a {@link com.okebari.artbite.domain.user.User} object.
	 * @param timestamp a {@link java.time.LocalDateTime} object.
	 * @return a boolean.
	 */
	boolean existsByUserAndAccessedAtAfter(User user, LocalDateTime timestamp);

	/**
	 * 특정 사용자의 모든 콘텐츠 접근 로그를 최신순으로 조회합니다.
	 * @param user 조회할 사용자
	 * @param pageable 페이징 정보
	 * @return 콘텐츠 접근 로그 슬라이스
	 */
	Slice<ContentAccessLog> findByUserOrderByAccessedAtDesc(User user, Pageable pageable);
}
