package com.okebari.artbite.note.service.support;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.okebari.artbite.domain.user.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 가입한 모든 사용자 ID를 순회하려는 정책으로 변경되면서,
 * 이전처럼 북마크/답변 테이블을 스캔해 “활동 사용자만” 추리는 단계는 제거했다.
 * (23시에 모든 사용자에게 리마인드를 제공하기 때문에)
 */
@Component
@RequiredArgsConstructor
public class ReminderTargetUserReader {

	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public List<Long> fetchAllUserIds() {
		return userRepository.findAllUserIds();
	}
}
