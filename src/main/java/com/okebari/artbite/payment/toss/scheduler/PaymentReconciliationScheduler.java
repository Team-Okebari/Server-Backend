package com.okebari.artbite.payment.toss.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.okebari.artbite.common.service.MdcLogging;
import com.okebari.artbite.domain.payment.Payment;
import com.okebari.artbite.domain.payment.PaymentRepository;
import com.okebari.artbite.domain.payment.PaymentStatus;
import com.okebari.artbite.payment.toss.dto.PaymentSuccessDto;
import com.okebari.artbite.payment.toss.service.TossPaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class PaymentReconciliationScheduler {

	private static final Set<String> TOSS_TERMINAL_FAILURE_STATUSES = Set.of("ABORTED", "EXPIRED", "CANCELED");
	private final PaymentRepository paymentRepository;
	private final TossPaymentService tossPaymentService;
	@Value("${payment.toss.confirming-timeout-minutes:5}")
	private int confirmingTimeoutMinutes;

	/**
	 * 5분에 한 번씩, 'CONFIRMING' 상태에 머물러 있는 결제를 찾아 최종 상태를 동기화합니다.
	 */
	@Scheduled(fixedRateString = "${payment.toss.reconciliation-interval-ms:300000}") // 5 minutes
	public void reconcileConfirmingPayments() {
		log.info("결제 상태 동기화 작업을 시작합니다 (CONFIRMING 상태 확인).");

		LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(confirmingTimeoutMinutes);
		List<Payment> stuckPayments = paymentRepository.findByStatusAndCreatedAtBefore(PaymentStatus.CONFIRMING,
			timeoutThreshold);

		if (stuckPayments.isEmpty()) {
			log.info("동기화 대상 결제가 없습니다.");
			return;
		}

		log.info("{}건의 동기화 대상 결제를 처리합니다.", stuckPayments.size());

		for (Payment payment : stuckPayments) {
			try (var ignored = MdcLogging.withContext("paymentId", payment.getId().toString())) {
				PaymentSuccessDto tossPayment = tossPaymentService.fetchPaymentByOrderId(payment.getOrderId());
				String tossStatus = tossPayment.getStatus();

				log.info("Toss 결제 상태 확인: orderId={}, tossStatus={}", payment.getOrderId(), tossStatus);

				if ("DONE".equalsIgnoreCase(tossStatus)) {
					log.info("Toss 상태 'DONE' 확인. 로컬 DB를 SUCCESS로 업데이트합니다.");
					// processPaymentSuccess는 내부적으로 CONFIRMING 상태를 확인하므로 안전하게 호출 가능
					tossPaymentService.processPaymentSuccess(payment.getOrderId(), payment.getPaymentKey());
				} else if (TOSS_TERMINAL_FAILURE_STATUSES.contains(tossStatus.toUpperCase())) {
					log.warn("Toss 상태가 최종 실패로 확인됨. 로컬 DB를 FAILED로 업데이트합니다. tossStatus={}", tossStatus);
					tossPaymentService.failPayment(payment.getOrderId(), "Toss 상태 불일치 복구: " + tossStatus);
				} else {
					log.info("Toss 상태가 아직 처리 중입니다. 다음 스케줄까지 대기합니다. tossStatus={}", tossStatus);
				}

			} catch (Exception e) {
				log.error("결제 상태 동기화 처리 중 오류가 발생했습니다. orderId: {}. Error: {}", payment.getOrderId(), e.getMessage(), e);
				// 개별 실패가 다른 건에 영향을 주지 않도록 루프 계속
			}
		}
		log.info("결제 상태 동기화 작업을 종료합니다.");
	}
}
