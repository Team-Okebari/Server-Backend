package com.okebari.artbite.payment.toss.dto;

import java.time.LocalDateTime;

import com.okebari.artbite.domain.payment.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryDto {
	private Long paymentHistoryId;
	private Long amount;
	private String orderName;
	private PaymentStatus status;
	private LocalDateTime createdAt;
}
