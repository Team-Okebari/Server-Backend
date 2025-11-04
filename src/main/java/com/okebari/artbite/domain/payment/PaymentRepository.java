package com.okebari.artbite.domain.payment;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findByOrderId(String orderId);

	Optional<Payment> findByPaymentKey(String paymentKey);

	Optional<Payment> findByPaymentKeyAndUserEmail(String paymentKey, String email);

	Slice<Payment> findAllByUserEmail(String email, Pageable pageable);

}
