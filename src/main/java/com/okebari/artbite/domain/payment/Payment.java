package com.okebari.artbite.domain.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.okebari.artbite.domain.common.BaseTimeEntity;
import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.payment.toss.dto.PayType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "payments", indexes = {
	@Index(name = "idx_payment_user", columnList = "user_id"),
	@Index(name = "idx_payment_orderId", columnList = "orderId", unique = true),
	@Index(name = "idx_payment_paymentKey", columnList = "paymentKey")
})
public class Payment extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "payment_id")
	private Long id;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, name = "pay_type")
	private PayType payType;

	@Column(nullable = false, name = "pay_amount")
	private Long amount;

	@Column(nullable = false, name = "order_name")
	private String orderName;

	@Column(nullable = false, unique = true, name = "order_id")
	private String orderId; // 서비스에서 정한 주문 고유번호

	@Column(nullable = false, name = "pay_success_yn")
	private boolean paySuccessYN; // 결제 성공 여부

	@Column(name = "payment_key")
	private String paymentKey; // 토스페이먼츠에서 정한 결제 구분용 키

	@Column(name = "fail_reason")
	private String failReason; // 실패 이유

	@Column(name = "cancel_yn")
	private boolean cancelYN; // 취소 여부

	@Column(name = "cancel_reason")
	private String cancelReason; // 취소 이유

	// --- 비즈니스 로직 --- //
	public void success(String paymentKey) {
		this.paymentKey = paymentKey;
		this.paySuccessYN = true;
	}

	public void fail(String failReason) {
		this.failReason = failReason;
		this.paySuccessYN = false;
	}

	public void cancel(String cancelReason) {
		this.cancelReason = cancelReason;
		this.cancelYN = true;
	}
}
