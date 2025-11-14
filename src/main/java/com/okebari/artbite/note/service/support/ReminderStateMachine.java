package com.okebari.artbite.note.service.support;

import org.springframework.stereotype.Component;

import com.okebari.artbite.note.dto.reminder.SurfaceHint;

/**
 * 배너 상태 전이를 전담하는 공통 추상화.
 * - Q3/Q4 응답: dismissed/firstVisit/bannerSeen 조건문이 군데군데 흩어지는 문제를 줄인다.
 */
@Component
public class ReminderStateMachine {

	public TransitionDecision decide(ReminderStateSnapshot snapshot) {
		if (snapshot.dismissed()) {
			return TransitionDecision.dismissed();
		}
		if (!snapshot.hasFirstVisit()) {
			return TransitionDecision.markFirstVisit();
		}
		if (!snapshot.hasBannerSeen()) {
			return TransitionDecision.markBannerSeen();
		}
		return TransitionDecision.keepShowing();
	}

	public TransitionDecision decide(NoteReminderCacheValue cacheValue) {
		return decide(cacheValue.state());
	}

	public enum StateAction {
		NONE,
		MARK_FIRST_VISIT,
		MARK_BANNER_SEEN
	}

	public record TransitionDecision(StateAction action, SurfaceHint hint) {

		public static TransitionDecision dismissed() {
			return new TransitionDecision(StateAction.NONE, SurfaceHint.NONE);
		}

		public static TransitionDecision markFirstVisit() {
			return new TransitionDecision(StateAction.MARK_FIRST_VISIT, SurfaceHint.DEFERRED);
		}

		public static TransitionDecision markBannerSeen() {
			return new TransitionDecision(StateAction.MARK_BANNER_SEEN, SurfaceHint.BANNER);
		}

		public static TransitionDecision keepShowing() {
			return new TransitionDecision(StateAction.NONE, SurfaceHint.BANNER);
		}

		public boolean requiresPersistence() {
			return action == StateAction.MARK_FIRST_VISIT || action == StateAction.MARK_BANNER_SEEN;
		}
	}
}
