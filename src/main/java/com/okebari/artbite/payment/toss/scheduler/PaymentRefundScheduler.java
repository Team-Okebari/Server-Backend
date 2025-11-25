package com.okebari.artbite.payment.toss.scheduler;

import java.util.List;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.okebari.artbite.common.service.MdcLogging;
import com.okebari.artbite.domain.payment.Payment;
import com.okebari.artbite.domain.payment.PaymentRepository;
import com.okebari.artbite.domain.payment.PaymentStatus;
import com.okebari.artbite.payment.toss.service.TossPaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class PaymentRefundScheduler {

	private static final String AUTO_REFUND_REASON = "서버 내부 처리 오류로 인한 자동 환불";
	private final PaymentRepository paymentRepository;
	private final TossPaymentService tossPaymentService;

	/**
	 * 1시간에 한 번씩, 내부 처리 실패(PROCESSING_FAILED) 상태의 결제를 자동으로 환불 처리합니다.
	 */
	@Scheduled(fixedRate = 3600000) // 1 hour
	public void refundProcessingFailedPayments() {
		log.info("내부 처리 실패 결제 자동 환불 작업을 시작합니다.");

		List<Payment> failedPayments = paymentRepository.findByStatus(PaymentStatus.PROCESSING_FAILED);

		if (failedPayments.isEmpty()) {
			log.info("자동 환불 대상 결제가 없습니다.");
			return;
		}

		log.info("{}건의 자동 환불 대상 결제를 처리합니다.", failedPayments.size());

		for (Payment payment : failedPayments) {
			try (var ignored = MdcLogging.withContext("paymentId", payment.getId().toString())) {
				log.info("결제 환불을 시도합니다. PaymentKey: {}", payment.getPaymentKey());
				tossPaymentService.executeRefund(payment.getPaymentKey(), AUTO_REFUND_REASON);
			} catch (Exception e) {
				log.error("자동 환불 처리 중 오류가 발생했습니다. PaymentKey: {}. Error: {}", payment.getPaymentKey(), e.getMessage(),
					e);
				// 개별 환불 실패가 다른 결제 건 처리에 영향을 주지 않도록 루프를 계속 진행합니다.
			}
		}
		log.info("내부 처리 실패 결제 자동 환불 작업을 종료합니다.");
	}
}
