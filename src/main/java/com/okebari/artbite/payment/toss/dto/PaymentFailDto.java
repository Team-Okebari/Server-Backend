package com.okebari.artbite.payment.toss.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "토스페이먼츠 결제 실패 응답 DTO")
public class PaymentFailDto {
	@Schema(description = "에러 코드", example = "PAY_PROCESS_CANCELED")
	private String errorCode;
	@Schema(description = "에러 메시지", example = "결제창에서 결제 취소 버튼을 누르셨습니다.")
	private String errorMessage;
	@Schema(description = "주문 ID", example = "orderId_12345")
	private String orderId;
}
