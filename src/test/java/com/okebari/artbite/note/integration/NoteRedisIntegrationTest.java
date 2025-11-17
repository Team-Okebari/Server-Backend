package com.okebari.artbite.note.integration;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.domain.user.UserRepository;
import com.okebari.artbite.domain.user.UserRole;
import com.okebari.artbite.note.domain.Note;
import com.okebari.artbite.note.domain.NoteStatus;
import com.okebari.artbite.note.repository.NoteRepository;
import com.okebari.artbite.note.scheduler.NoteStatusScheduler;

import software.amazon.awssdk.services.s3.S3Client;

/**
 * Docker 기반 PostgreSQL + Redis 컨테이너를 활용한 노트 도메인 통합 테스트 예시.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "cloud.aws.s3.bucket=dummy-bucket")
class NoteRedisIntegrationTest extends NoteContainerBaseTest {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul"); // Re-added KST definition
	@MockitoBean
	private S3Client s3Client;
	@Autowired
	private NoteRepository noteRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private NoteStatusScheduler noteStatusScheduler;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	@Test
	void completedNoteIsPublishedAndCached() {
		// given: Postgres에 ADMIN 사용자와 COMPLETED 노트를 저장한다.
		User admin = userRepository.save(
			User.builder()
				.email("admin@test.com")
				.password("pw")
				.username("admin")
				.role(UserRole.ADMIN)
				.enabled(true)
				.accountNonExpired(true)
				.accountNonLocked(true)
				.credentialsNonExpired(true)
				.tokenVersion(0)
				.build()
		);

		Note note = noteRepository.save(
			Note.builder()
				.status(NoteStatus.COMPLETED)
				.tagText("daily-tag")
				.sourceUrl("https://source")
				.build()
		);

		// when: 자정 배포 스케줄러를 직접 실행해 COMPLETED → PUBLISHED 전환을 유도한다.
		noteStatusScheduler.publishCompletedNotes();

		// then: 실제 DB에서 노트 상태가 PUBLISHED로 변했는지 확인한다.
		Note published = noteRepository.findById(note.getId()).orElseThrow();
		assertThat(published.getStatus()).isEqualTo(NoteStatus.PUBLISHED);
		assertThat(published.getPublishedAt()).isBeforeOrEqualTo(LocalDateTime.now(KST));

		// and: Redis에 노트 요약 정보를 캐싱하고 즉시 조회해 본다.
		String cacheKey = "notes:published:" + published.getId();
		stringRedisTemplate.opsForHash().put(cacheKey, "title", "오늘의 작업노트");
		stringRedisTemplate.opsForHash().put(cacheKey, "tag", published.getTagText());

		assertThat(stringRedisTemplate.opsForHash().entries(cacheKey))
			.containsEntry("title", "오늘의 작업노트")
			.containsEntry("tag", "daily-tag");
	}
}
