package com.okebari.artbite.note.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.okebari.artbite.common.exception.NoteAccessDeniedException;
import com.okebari.artbite.common.exception.NoteInvalidStatusException;
import com.okebari.artbite.common.exception.NoteNotFoundException;
import com.okebari.artbite.common.exception.UserNotFoundException;
import com.okebari.artbite.creator.domain.Creator;
import com.okebari.artbite.creator.exception.CreatorNotFoundException;
import com.okebari.artbite.creator.repository.CreatorRepository;
import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.domain.user.UserRepository;
import com.okebari.artbite.domain.user.UserRole;
import com.okebari.artbite.note.domain.Note;
import com.okebari.artbite.note.domain.NoteStatus;
import com.okebari.artbite.note.dto.note.NoteCreateRequest;
import com.okebari.artbite.note.dto.note.NoteResponse;
import com.okebari.artbite.note.dto.note.NoteUpdateRequest;
import com.okebari.artbite.note.mapper.NoteMapper;
import com.okebari.artbite.note.repository.NoteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteService {

	// 노트 CRUD를 담당하는 저장소.
	private final NoteRepository noteRepository;
	// 작가 정보 조회용 저장소. 노트와 작가를 연결할 때 사용한다.
	private final CreatorRepository creatorRepository;
	// 엔티티 ↔ DTO 변환 전담 매퍼.
	private final NoteMapper noteMapper;
	// 권한 검증 및 작성자 확인을 위한 사용자 저장소.
	private final UserRepository userRepository;

	/**
	 * ADMIN이 신규 노트를 생성한다.
	 * 1) 작성자가 ADMIN인지 확인하고, 2) 상태값이 허용 범위인지 검증한 뒤,
	 * 3) 요청 DTO를 엔티티로 변환하고 작가 연결을 수행한다.
	 * 저장이 성공하면 생성된 노트의 PK를 반환한다.
	 */
	@Transactional
	public Long create(NoteCreateRequest request, Long adminUserId) {
		User admin = userRepository.findById(adminUserId)
			.orElseThrow(() -> new UserNotFoundException());
		validateAdmin(admin);
		validateInitialStatus(request.status());

		Note note = noteMapper.toEntity(request);
		note.assignCreator(resolveCreator(request.creatorId()));

		Note saved = noteRepository.save(note);
		return saved.getId();
	}

	/**
	 * 단일 노트를 식별자로 조회해 응답 DTO로 변환한다.
	 */
	public NoteResponse get(Long noteId) {
		Note note = noteRepository.findById(noteId)
			.orElseThrow(() -> new NoteNotFoundException(noteId));
		return noteMapper.toResponse(note, null);
	}

	public NoteResponse getForAdmin(Long noteId) {
		Note note = noteRepository.findById(noteId)
			.orElseThrow(() -> new NoteNotFoundException(noteId));
		return noteMapper.toResponseForAdmin(note);
	}

	/**
	 * ADMIN이 노트를 수정한다.
	 * - 게시/보관 상태에서는 수정이 불가하므로 상태 전환 규칙을 먼저 확인한다.
	 * - 커버/개요/프로세스/질문 구조를 모두 새로 덮어쓴다.
	 * - 외부 링크 및 작가 연결 역시 요청 값으로 갱신하거나 제거한다.
	 */
	@Transactional
	public void update(Long noteId, NoteUpdateRequest request) {
		Note note = noteRepository.findById(noteId)
			.orElseThrow(() -> new NoteNotFoundException(noteId));

		validateUpdatableState(note, request.status());
		note.updateMeta(request.status(), request.tagText());

		if (note.getCover() != null && request.cover() != null) {
			note.getCover()
				.update(request.cover().title(), request.cover().teaser(), request.cover().mainImageUrl(),
					request.cover().category());
		} else if (note.getCover() == null && request.cover() != null) {
			note.assignCover(noteMapper.toCover(request.cover()));
		}

		if (note.getOverview() != null && request.overview() != null) {
			note.getOverview()
				.update(request.overview().sectionTitle(), request.overview().bodyText(),
					request.overview().imageUrl());
		} else if (note.getOverview() == null && request.overview() != null) {
			note.assignOverview(noteMapper.toOverview(request.overview()));
		}

		if (note.getRetrospect() != null && request.retrospect() != null) {
			note.getRetrospect().update(request.retrospect().sectionTitle(), request.retrospect().bodyText());
		} else if (note.getRetrospect() == null && request.retrospect() != null) {
			note.assignRetrospect(noteMapper.toRetrospect(request.retrospect()));
		}

		updateProcesses(note, request.processes());

		if (note.getQuestion() != null && request.question() != null) {
			note.getQuestion().update(request.question().questionText());
		} else if (note.getQuestion() == null && request.question() != null) {
			note.assignQuestion(noteMapper.toQuestion(request.question()));
		}

		note.updateExternalLinks(request.externalLink() != null ? request.externalLink().sourceUrl() : null);
		note.assignCreator(resolveCreator(request.creatorId()));
	}

	/**
	 * 노트를 삭제한다.
	 * 존재하지 않는 ID에 대해서는 예외를 던져 호출 측에서 상태를 알 수 있도록 한다.
	 */
	@Transactional
	public void delete(Long noteId) {
		if (!noteRepository.existsById(noteId)) {
			throw new NoteNotFoundException(noteId);
		}
		noteRepository.deleteById(noteId);
	}

	/**
	 * 페이지네이션으로 노트 목록을 조회한다.
	 * 현재는 단순 findAll 이지만 향후 권한별 필터링을 추가할 수 있다.
	 */
	public Page<NoteResponse> list(Pageable pageable) {
		return noteRepository.findAll(pageable)
			.map(note -> noteMapper.toResponse(note, null));
	}

	/**
	 * ADMIN 역할이 아니면 쓰기 작업을 거부한다.
	 */
	private void validateAdmin(User admin) {
		if (admin.getRole() != UserRole.ADMIN) {
			throw new NoteAccessDeniedException("ADMIN 권한만 노트를 등록할 수 있습니다.");
		}
	}

	/**
	 * 신규 생성 시 허용되는 초기 상태(IN_PROGRESS/COMPLETED)만 통과시킨다.
	 */
	private void validateInitialStatus(NoteStatus status) {
		if (status != NoteStatus.IN_PROGRESS && status != NoteStatus.COMPLETED && status != NoteStatus.ARCHIVED) {
			throw new NoteInvalidStatusException("신규 노트는 IN_PROGRESS, COMPLETED, ARCHIVED 상태로만 생성할 수 있습니다.");
		}
	}

	/**
	 * 수정 시 상태 전환 규칙을 강제한다.
	 * - 이미 게시(PUBLISHED)/보관(ARCHIVED)된 노트는 수정 금지
	 * - 배치에서만 다루어야 하는 상태(PUBLISHED/ARCHIVED)로의 전환 금지
	 * - COMPLETED 상태는 재수정 전 IN_PROGRESS 로 되돌려야 한다.
	 */
	private void validateUpdatableState(Note note, NoteStatus targetStatus) {
		if (note.getStatus() == NoteStatus.PUBLISHED || note.getStatus() == NoteStatus.ARCHIVED) {
			throw new NoteInvalidStatusException("PUBLISHED 또는 ARCHIVED 노트는 수정할 수 없습니다.");
		}
		if (targetStatus == NoteStatus.PUBLISHED) {
			throw new NoteInvalidStatusException("PUBLISHED 전환은 배치 작업에서만 처리됩니다.");
		}
		if (note.getStatus() == NoteStatus.COMPLETED && targetStatus == NoteStatus.COMPLETED) {
			throw new NoteInvalidStatusException("COMPLETED 상태에서 수정하려면 먼저 IN_PROGRESS로 되돌려야 합니다.");
		}
	}

	private void updateProcesses(Note note,
		java.util.List<com.okebari.artbite.note.dto.note.NoteProcessDto> processDtos) {
		if (processDtos == null) {
			note.getProcesses().clear();
			return;
		}

		java.util.Map<Short, com.okebari.artbite.note.domain.NoteProcess> existingProcesses = note.getProcesses()
			.stream()
			.collect(java.util.stream.Collectors.toMap(p -> p.getId().getPosition(), p -> p));

		java.util.List<com.okebari.artbite.note.domain.NoteProcess> toKeep = new java.util.ArrayList<>();

		for (com.okebari.artbite.note.dto.note.NoteProcessDto dto : processDtos) {
			com.okebari.artbite.note.domain.NoteProcess process = existingProcesses.get(dto.position());
			if (process != null) {
				process.update(dto.sectionTitle(), dto.bodyText(), dto.imageUrl());
				toKeep.add(process);
			} else {
				com.okebari.artbite.note.domain.NoteProcess newProcess = noteMapper.toProcess(dto);
				toKeep.add(newProcess);
			}
		}
		note.getProcesses().clear();
		for (com.okebari.artbite.note.domain.NoteProcess process : toKeep) {
			note.addProcess(process);
		}
	}

	/**
	 * 요청 필드의 creatorId를 실제 엔티티로 치환한다.
	 * creatorId는 필수이며, 존재하지 않으면 예외를 던진다.
	 */
	private Creator resolveCreator(Long creatorId) {
		if (creatorId == null) {
			throw new CreatorNotFoundException("creatorId는 필수 값입니다. ADMIN은 노트 생성/수정 시 작가를 선택해야 합니다.");
		}
		return creatorRepository.findById(creatorId)
			.orElseThrow(() -> new CreatorNotFoundException(creatorId));
	}
}
