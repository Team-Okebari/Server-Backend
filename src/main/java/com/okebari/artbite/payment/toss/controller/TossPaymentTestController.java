package com.okebari.artbite.payment.toss.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
		String orderName = tossPaymentConfig.getOrderName();
		Long amount = tossPaymentConfig.getMembershipAmount();
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
			        <a href="/login-test-page" style="display: inline-block; margin-bottom: 20px; padding: 10px 15px; background-color: #6c757d; color: white; text-decoration: none; border-radius: 5px;">로그인 테스트 페이지로 이동</a>
			        <h1>Toss Payments 테스트</h1>
			        <p><strong>주의:</strong> 이 페이지를 사용하려면 먼저 <a href="/login-test-page" target="_blank">인증 테스트 페이지</a>에서 로그인하여 Access Token을 발급받아야 합니다.</p>
			
			        <div class="form-group">
			            <label for="auth-token">Access Token</label>
			            <textarea id="auth-token" rows="4" placeholder="여기에 Access Token을 붙여넣으세요..."></textarea>
			        </div>
			
			        <div class="form-group">
			            <label for="pay-type">결제 수단</label>
			            <select id="pay-type">
			                <option value="CARD" selected>카드</option>
			                <option value="VIRTUAL_ACCOUNT">가상계좌</option>
			                <option value="TRANSFER">계좌이체</option>
			                <option value="MOBILE_PHONE">휴대폰</option>
			            </select>
			        </div>
			
			        <div class="form-group">
			            <label for="order-name">주문명</label>
			            <input type="text" id="order-name" value="__ORDER_NAME__">
			        </div>
			
			        <div class="form-group">
			            		            <label for="amount">결제 금액</label>
			            		            <input type="number" id="amount" value="__AMOUNT__">
			            		        </div>
			
			            		        		        <button id="payment-button">결제하기</button>
			
			            		        		        <hr style="margin: 30px 0;">
			
			            		        		        <h2>멤버십 액션</h2>
			            		        		        <div class="action-buttons">
			            		        		            <button id="cancel-membership-button" style="background-color: #dc3545;">멤버십 취소</button>
			            		        		            <button id="reactivate-canceled-membership-button" style="background-color: #ffc107; color: #212529;">취소된 멤버십 재활성화</button>
			            		        		        </div>
			
			            		        		        <hr style="margin: 30px 0;">
			
			            		        		        <h2>관리자 액션 (userId 필요)</h2>
			            		        		        <div class="form-group">
			            		        		            <label for="admin-user-id">대상 User ID</label>
			            		        		            <input type="number" id="admin-user-id" placeholder="대상 사용자의 ID 입력">
			            		        		        </div>
			            		        		        <div class="action-buttons">
			            		        		            <button id="ban-membership-button" style="background-color: #fd7e14;">멤버십 정지 (Ban)</button>
			            		        		            <button id="unban-membership-button" style="background-color: #20c997;">멤버십 정지 해제 (Unban)</button>
			            		        		        </div>
			
			            		        		        <hr style="margin: 30px 0;">
			
			            		        		        <div id="api-status">
			            		        		            <h3>API 상태</h3>
			            		        		            <pre>API 호출 결과가 여기에 표시됩니다.</pre>
			            		        		        </div>			            			    </div>
			
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
			            			                        payType: document.getElementById('pay-type').value,
			            			                        amount: document.getElementById('amount').value,
			            			                        orderName: document.getElementById('order-name').value,
			            			                        membershipPlanType: 'DEFAULT_MEMBER_PLAN'
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
			
			        // --- Membership Actions ---
			        async function callMembershipApi(endpoint, method, statusTitle, userId = null) {
			            const token = authTokenInput.value.trim();
			            if (!token) {
			                alert('Access Token을 입력해주세요.');
			                return null;
			            }
			            statusDiv.innerHTML = `<h3>API 상태</h3><pre>${statusTitle} 중...</pre>`;
			            try {
			                let url = `/api/memberships`;
			                if (userId) {
			                    url += `/${userId}`;
			                }
			                url += endpoint;
			
			                const response = await fetch(url, {
			                    method: method,
			                    headers: {
			                        'Authorization': 'Bearer ' + token
			                    }
			                });
			                const data = await response.json();
			                if (!response.ok) {
			                    throw data; // Throw error object from backend
			                }
			                statusDiv.innerHTML = `<h3>API 상태</h3><pre>${statusTitle} 결과: ${JSON.stringify(data, null, 2)}</pre>`;
			                return data;
			            } catch (error) {
			                console.error(`${statusTitle} 오류:`, error);
			                statusDiv.innerHTML = `<h3>API 상태</h3><pre>${statusTitle} 오류: ${JSON.stringify(error, null, 2)}</pre>`;
			                return null;
			            }
			        }
			
			        document.getElementById('cancel-membership-button').addEventListener('click', async function () {
			            await callMembershipApi('/cancel', 'POST', '멤버십 취소');
			        });
			
			        document.getElementById('reactivate-canceled-membership-button').addEventListener('click', async function () {
			            await callMembershipApi('/reactivate-canceled', 'POST', '취소된 멤버십 재활성화');
			        });
			
			        // --- Admin Actions ---
			        document.getElementById('ban-membership-button').addEventListener('click', async function () {
			            const userId = document.getElementById('admin-user-id').value.trim();
			            if (!userId) {
			                alert('대상 User ID를 입력해주세요.');
			                return;
			            }
			            await callMembershipApi('/ban', 'POST', '멤버십 정지', userId);
			        });
			
			        document.getElementById('unban-membership-button').addEventListener('click', async function () {
			            const userId = document.getElementById('admin-user-id').value.trim();
			            if (!userId) {
			                alert('대상 User ID를 입력해주세요.');
			                return;
			            }
			            await callMembershipApi('/unban', 'POST', '멤버십 정지 해제', userId);
			        });
			    </script>
			</body>
			</html>
			""";
		return htmlContent
			.replace("__CLIENT_KEY__", clientKey)
			.replace("__ORDER_NAME__", orderName)
			.replace("__AMOUNT__", String.valueOf(amount));
	}

	@GetMapping("/payment/success")
	@ResponseBody
	public String getSuccessPage() {
		return """
			<!DOCTYPE html>
			<html lang="ko">
			<head>
			    <meta charset="UTF-8">
			    <title>결제 성공</title>
			    <style>
			        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen-Sans, Ubuntu, Cantarell, "Helvetica Neue", sans-serif; padding: 20px; background-color: #f8f9fa; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }
			        .container { max-width: 600px; text-align: center; background: #fff; padding: 40px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
			        h1 { color: #28a745; }
			        p { font-size: 1.2rem; }
			    </style>
			</head>
			<body>
			    <div class="container">
			        <h1>결제 성공</h1>
			        <p>결제가 성공적으로 완료되었습니다.</p>
			    </div>
			</body>
			</html>
			""";
	}

	@GetMapping("/payment/fail")
	@ResponseBody
	public String getFailPage(@RequestParam(required = false) String message,
		@RequestParam(required = false) String code) {
		return String.format("""
			<!DOCTYPE html>
			<html lang="ko">
			<head>
			    <meta charset="UTF-8">
			    <title>결제 실패</title>
			    <style>
			        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen-Sans, Ubuntu, Cantarell, "Helvetica Neue", sans-serif; padding: 20px; background-color: #f8f9fa; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }
			        .container { max-width: 600px; text-align: center; background: #fff; padding: 40px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
			        h1 { color: #dc3545; }
			        .reason { background-color: #e9ecef; padding: 15px; border-radius: 5px; margin-top: 20px; text-align: left; }
			    </style>
			</head>
			<body>
			    <div class="container">
			        <h1>결제 실패</h1>
			        <p>결제 처리 중 오류가 발생했습니다.</p>
			        <div class="reason">
			            <strong>사유:</strong> %s <br>
			            <strong>에러 코드:</strong> %s
			        </div>
			    </div>
			</body>
			</html>
			""", message, code);
	}
}
