package com.okebari.artbite.payment.toss.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.okebari.artbite.payment.toss.config.TossPaymentConfig;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class TossPaymentTestController {

	private final TossPaymentConfig tossPaymentConfig;

	@GetMapping("/payment-test-page")
	@ResponseBody
	public String getPaymentTestPage() {
		String clientKey = tossPaymentConfig.getTestClientApiKey();
		String htmlContent = """
			<!DOCTYPE html>
			<html lang="ko">
			<head>
			    <meta charset="UTF-8">
			    <title>결제 테스트 페이지</title>
			    <script src="https://js.tosspayments.com/v1/payment"></script>
			    <style>
			        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen-Sans, Ubuntu, Cantarell, "Helvetica Neue", sans-serif; padding: 20px; background-color: #f8f9fa; }
			        .container { max-width: 600px; margin: 40px auto; background: #fff; padding: 30px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
			        h1 { text-align: center; }
			        #payment-button { width: 100%; padding: 15px; font-size: 18px; font-weight: bold; color: #fff; background-color: #007bff; border: none; border-radius: 5px; cursor: pointer; transition: background-color 0.2s; }
			        #payment-button:hover { background-color: #0056b3; }
			        #payment-button:disabled { background-color: #cccccc; cursor: not-allowed; }
			        .form-group { margin-bottom: 20px; }
			        .form-group label { display: block; margin-bottom: 8px; font-weight: 500; }
			        .form-group textarea, .form-group input { width: 100%; padding: 10px; border: 1px solid #ced4da; border-radius: 4px; box-sizing: border-box; font-size: 14px; }
			        #api-status { margin-top: 20px; padding: 15px; background-color: #e9ecef; border-radius: 5px; word-wrap: break-word; }
			    </style>
			</head>
			<body>
			    <div class="container">
			        <h1>Toss Payments 테스트</h1>
			        <p><strong>주의:</strong> 이 페이지를 사용하려면 먼저 <a href="/login-test-page" target="_blank">인증 테스트 페이지</a>에서 로그인하여 Access Token을 발급받아야 합니다.</p>
			
			        <div class="form-group">
			            <label for="auth-token">Access Token</label>
			            <textarea id="auth-token" rows="4" placeholder="여기에 Access Token을 붙여넣으세요..."></textarea>
			        </div>
			
			        <div class="form-group">
			            <label for="order-name">주문명</label>
			            <input type="text" id="order-name" value="테스트 멤버십">
			        </div>
			
			        <div class="form-group">
			            <label for="amount">결제 금액</label>
			            <input type="number" id="amount" value="1500">
			        </div>
			
			        <button id="payment-button">결제하기</button>
			
			        <div id="api-status">
			            <h3>API 상태</h3>
			            <pre>API 호출 결과가 여기에 표시됩니다.</pre>
			        </div>
			    </div>
			
			    <script>
			        const clientKey = '__CLIENT_KEY__';
			        const tossPayments = TossPayments(clientKey);
			        const paymentButton = document.getElementById('payment-button');
			        const authTokenInput = document.getElementById('auth-token');
			        const statusDiv = document.getElementById('api-status');
			
			        paymentButton.addEventListener('click', async function () {
			            const token = authTokenInput.value.trim();
			            if (!token) {
			                alert('Access Token을 입력해주세요.');
			                return;
			            }
			
			            paymentButton.disabled = true;
			            statusDiv.innerHTML = '<h3>API 상태</h3><pre>결제 정보 생성 중...</pre>';
			
			            try {
			                // 1단계: 백엔드에 결제 정보 생성을 요청합니다.
			                const response = await fetch('/api/payments/toss', {
			                    method: 'POST',
			                    headers: {
			                        'Content-Type': 'application/json',
			                        'Authorization': 'Bearer ' + token
			                    },
			                    body: JSON.stringify({
			                        payType: 'CARD',
			                        amount: document.getElementById('amount').value,
			                        orderName: document.getElementById('order-name').value
			                    })
			                });
			
			                const responseData = await response.json();
			                statusDiv.innerHTML = `<h3>API 상태</h3><pre>${JSON.stringify(responseData, null, 2)}</pre>`;
			
			                if (!response.ok) {
			                    throw new Error('결제 정보 생성 실패: ' + (responseData.error ? responseData.error.message : '알 수 없는 오류'));
			                }
			
			                const paymentData = responseData.data;
			
			                // 2단계: 백엔드에서 받은 정보로 Toss 결제창을 호출합니다.
			                tossPayments.requestPayment(paymentData.payType, {
			                    amount: paymentData.amount,
			                    orderId: paymentData.orderId,
			                    orderName: paymentData.orderName,
			                    customerName: paymentData.customerName,
			                    customerEmail: paymentData.customerEmail,
			                    successUrl: paymentData.successUrl,
			                    failUrl: paymentData.failUrl
			                });
			
			            } catch (error) {
			                console.error('결제 요청 실패:', error);
			                statusDiv.innerHTML = `<h3>API 상태</h3><pre>오류: ${error.message}</pre>`;
			            } finally {
			                paymentButton.disabled = false;
			            }
			        });
			    </script>
			</body>
			</html>
			""";
		return htmlContent.replace("__CLIENT_KEY__", clientKey);
	}
}
