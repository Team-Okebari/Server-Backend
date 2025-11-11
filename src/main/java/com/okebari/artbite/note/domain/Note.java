package com.okebari.artbite.note.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.okebari.artbite.creator.domain.Creator;
import com.okebari.artbite.domain.common.BaseTimeEntity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * notes_head 테이블에 대응하는 루트 애그리거트.
 * 노트의 상태, 작성 정보, 외부 링크, 하위 섹션(커버/프로세스/질문)을 한 곳에 모아 관리한다.
 */
@Entity
@Table(name = "notes_head")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Note extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 노트 작성 진행 단계. IN_PROGRESS → COMPLETED → PUBLISHED → ARCHIVED 순으로 전환한다.
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private NoteStatus status;

	// 태그형 키워드. 검색/필터에 활용하며 입력은 선택 사항이다.
	@Column(name = "tag_text", length = 60)
	private String tagText;

	// 노트가 게시된 시각. 자정 배포 스케줄러가 설정한다.
	@Column(name = "published_at")
	private LocalDateTime publishedAt;

	// 게시 후 24시간이 지나면 자동으로 보관 상태가 되며 아카이브 목록에서만 노출한다.
	@Column(name = "archived_at")
	private LocalDateTime archivedAt;

	// REQ_106: Admin이 입력하는 외부 참고 링크(관련 자료/포트폴리오 URL)를 보관한다.
	@Column(name = "source_url", length = 255)
	private String sourceUrl;

	// 작가(Creator) FK. 동일 작가가 여러 노트를 가질 수 있으므로 N:1 관계로 둔다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "creator_id")
	private Creator creator;

	// 표지(타이틀·티저·대표 이미지)
	@OneToOne(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private NoteCover cover;

	// 작업 개요
	@OneToOne(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private NoteOverview overview;

	// 회고 섹션
	@OneToOne(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private NoteRetrospect retrospect;

	// 프로세스 1/2 정보를 position 으로 구분한다.
	@OneToMany(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<NoteProcess> processes = new ArrayList<>();

	// 질문·답변 묶음. 질문은 노트가 생성될 때 ADMIN이 입력한다.
	@OneToOne(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private NoteQuestion question;

	@Builder
	private Note(NoteStatus status, String tagText, String sourceUrl) {
		this.status = status;
		this.tagText = tagText;
		this.sourceUrl = sourceUrl;
	}

	public void updateMeta(NoteStatus status, String tagText) {
		this.tagText = tagText;
		if (status == NoteStatus.IN_PROGRESS) {
			revertToInProgress();
		} else if (status == NoteStatus.COMPLETED) {
			this.status = NoteStatus.COMPLETED;
		}
	}

	public void updateExternalLinks(String sourceUrl) {
		this.sourceUrl = sourceUrl;
	}

	/**
	 * 노트와 작가를 연결한다. null 로 전달하면 참조를 제거해 독립 노트로 만든다.
	 */
	public void assignCreator(Creator creator) {
		this.creator = creator;
	}

	/**
	 * 표지를 교체하고 상호 연관관계를 고정한다.
	 */
	public void assignCover(NoteCover cover) {
		this.cover = cover;
		if (cover != null) {
			cover.bindNote(this);
		}
	}

	/**
	 * 개요 섹션을 교체한다.
	 */
	public void assignOverview(NoteOverview overview) {
		this.overview = overview;
		if (overview != null) {
			overview.bindNote(this);
		}
	}

	/**
	 * 회고 섹션을 교체한다.
	 */
	public void assignRetrospect(NoteRetrospect retrospect) {
		this.retrospect = retrospect;
		if (retrospect != null) {
			retrospect.bindNote(this);
		}
	}

	/**
	 * 프로세스 리스트를 새로 채워 넣는다. 기존 항목은 모두 제거된다.
	 */
	public void replaceProcesses(List<NoteProcess> processes) {
		this.processes.clear();
		if (processes != null) {
			for (NoteProcess process : processes) {
				this.addProcess(process);
			}
		}
	}

	/**
	 * 단일 프로세스를 추가하면서 연관관계를 맺는다.
	 */
	public void addProcess(NoteProcess process) {
		if (process == null) {
			return;
		}
		this.processes.add(process);
		process.bindNote(this);
	}

	/**
	 * 노트에 질문을 연결한다. 질문이 null 이면 기존 연결을 끊는다.
	 */
	public void assignQuestion(NoteQuestion question) {
		this.question = question;
		if (question != null) {
			question.bindNote(this);
		}
	}

	/**
	 * 게시 상태로 전환하고 게시 시각을 기록한다.
	 */
	public void markPublished(LocalDateTime publishedAt) {
		this.status = NoteStatus.PUBLISHED;
		this.publishedAt = publishedAt;
	}

	/**
	 * 24시간 노출 이후 아카이브 상태로 전환한다.
	 */
	public void markArchived(LocalDateTime archivedAt) {
		this.status = NoteStatus.ARCHIVED;
		this.archivedAt = archivedAt;
	}

	/**
	 * 다시 작성 가능하도록 상태를 IN_PROGRESS로 되돌리고 게시/보관 타임스탬프를 초기화한다.
	 */
	public void revertToInProgress() {
		this.status = NoteStatus.IN_PROGRESS;
		this.publishedAt = null;
		this.archivedAt = null;
	}
}
