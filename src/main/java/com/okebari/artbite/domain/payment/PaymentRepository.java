package com.okebari.artbite.domain.payment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import com.okebari.artbite.domain.user.User;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findByOrderId(String orderId);

	Optional<Payment> findByPaymentKey(String paymentKey);

	Optional<Payment> findByPaymentKeyAndUserEmail(String paymentKey, String email);

	Slice<Payment> findAllByUserEmail(String email, Pageable pageable);

	Slice<Payment> findAllByUserEmailAndStatus(String email, PaymentStatus status, Pageable pageable);

	List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime createdAt);

	List<Payment> findByStatus(PaymentStatus status);

	Optional<Payment> findTopByUserAndStatusOrderByCreatedAtDesc(User user, PaymentStatus status);

}
