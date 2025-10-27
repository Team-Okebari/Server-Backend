package com.okebari.artbite.common.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.MDC;

/**
 * try-with-resources 구문을 사용하여 MDC 컨텍스트를 안전하게 추가하고 제거하는 유틸리티 클래스.
 * 예시:
 * try (var ignored = MDCLogging.withContext("orderId", order.getId().toString())) {
 *     log.info("주문 처리 중");
 * }
 */
public final class MdcLogging {

	private MdcLogging() {
		// 유틸리티 클래스이므로 인스턴스화 방지
	}

	public static MdcContext withContext(String key, String value) {
		return new MdcContext(Map.of(key, value));
	}

	public static MdcContext withContexts(Map<String, String> contextMap) {
		return new MdcContext(contextMap);
	}

	public static class MdcContext implements AutoCloseable {
		private final Map<String, String> previousValues = new HashMap<>();

		/**
		 * 생성자에서 새로운 컨텍스트를 MDC에 추가하고, 이전 값들을 저장합니다.
		 * @param contextMap MDC에 추가할 키-값 맵
		 */
		public MdcContext(Map<String, String> contextMap) {
			contextMap.forEach((key, value) -> {
				previousValues.put(key, MDC.get(key));
				MDC.put(key, value);
			});
		}

		/**
		 * AutoCloseable.close() 구현.
		 * try-with-resources 블록이 끝날 때 호출되어 MDC 상태를 이전으로 복원합니다.
		 */
		@Override
		public void close() {
			previousValues.forEach((key, value) -> {
				if (value != null) {
					MDC.put(key, value); // 이전 값이 있었으면 복원
				} else {
					MDC.remove(key);     // 이전 값이 없었으면 제거
				}
			});
		}
	}
}
