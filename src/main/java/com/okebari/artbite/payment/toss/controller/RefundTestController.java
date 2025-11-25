package com.okebari.artbite.payment.toss.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RefundTestController {

	@GetMapping("/refund-test-page")
	@ResponseBody
	public String getRefundTestPage() {
		return """
			<!DOCTYPE html>
			<html lang="ko">
			<head>
			    <meta charset="UTF-8">
			    <title>환불 및 구독 취소 테스트 페이지</title>
			    <style>
			        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; padding: 20px; background-color: #f8f9fa; line-height: 1.6; }
			        .page-container { max-width: 900px; margin: 20px auto; }
			        .container { background: #fff; padding: 25px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.08); margin-bottom: 20px; }
			        h1, h2, h3 { color: #343a40; }
			        h1 { text-align: center; margin-bottom: 30px; }
			        h2 { border-bottom: 2px solid #dee2e6; padding-bottom: 10px; margin-top: 20px; }
			        h3 { margin-top: 25px; color: #495057; }
			        .form-group { margin-bottom: 15px; }
			        .form-group label { display: block; margin-bottom: 5px; font-weight: 500; }
			        .form-group input, .form-group textarea { width: 100%; padding: 10px; border: 1px solid #ced4da; border-radius: 4px; box-sizing: border-box; font-size: 14px; }
			        button { display: inline-block; padding: 10px 15px; font-size: 14px; font-weight: bold; color: #fff; background-color: #007bff; border: none; border-radius: 5px; cursor: pointer; transition: background-color 0.2s; text-decoration: none; text-align: center; margin-right: 10px; margin-top: 10px; }
			        button:hover { background-color: #0056b3; }
			        button.danger { background-color: #dc3545; }
			        button.danger:hover { background-color: #c82333; }
			        button.warning { background-color: #ffc107; color: #212529; }
			        button.warning:hover { background-color: #e0a800; }
			        button.success { background-color: #28a745; }
			        button.success:hover { background-color: #218838; }
			        #api-status { margin-top: 20px; padding: 15px; background-color: #e9ecef; border-radius: 5px; word-wrap: break-word; white-space: pre-wrap; font-family: monospace; }
			        .flex-nav { display: flex; gap: 10px; margin-bottom: 20px; }
			    </style>
			</head>
			<body>
			    <div class="page-container">
			        <div class="flex-nav">
			            <a href="/login-test-page" class="button" style="background-color: #6c757d;">인증 테스트</a>
			            <a href="/payment-test-page" class="button" style="background-color: #007bff;">결제 테스트</a>
			            <a href="/membership-inducement-image-test-page" class="button" style="background-color: #6c757d;">멤버십 유도 이미지 테스트</a>
			        </div>
			        <h1>환불 및 구독 취소 테스트</h1>
			
			        <div class="container">
			            <h2>인증 정보</h2>
			            <p>이 페이지를 사용하려면 먼저 <a href="/login-test-page" target="_blank">인증 테스트 페이지</a>에서 로그인하여 Access Token을 발급받아야 합니다. ADMIN 계정 Access Token도 필요할 수 있습니다.</p>
			            <div class="form-group">
			                <label for="auth-token">User Access Token (Bearer)</label>
			                <textarea id="auth-token" rows="2" placeholder="USER 권한의 Access Token을 여기에 붙여넣으세요."></textarea>
			            </div>
			            <div class="form-group">
			                <label for="admin-auth-token">Admin Access Token (Bearer)</label>
			                <textarea id="admin-auth-token" rows="2" placeholder="ADMIN 권한의 Access Token을 여기에 붙여넣으세요. (관리자 환불 테스트용)"></textarea>
			            </div>
			            <button class="success" onclick="checkMembershipStatus()">현재 멤버십 상태 조회</button>
			        </div>
			
			        <div class="container">
			            <h2>1. 청약철회 (자동 환불) 경로 테스트</h2>
			            <p><strong>시나리오:</strong> 결제 후 7일 이내, 유료 콘텐츠를 이용하지 않은 상태에서 취소 요청 시 자동 환불되는지 확인합니다.</p>
			            <p><strong>테스트 순서:</strong></p>
			            <ol>
			                <li><a href="/payment-test-page" target="_blank">결제 테스트 페이지</a>에서 멤버십 결제 및 활성화 (최근 결제이어야 함).</li>
			                <li>이 페이지로 돌아와 User Access Token을 입력.</li>
			                <li><strong>유료 콘텐츠를 절대로 이용하지 않은 상태에서</strong> '멤버십 취소 요청' 버튼 클릭.</li>
			            </ol>
			            <button class="danger" onclick="cancelMembershipUser()">멤버십 취소 요청 (User)</button>
			        </div>
			
			        <div class="container">
			            <h2>2. 일반 구독 취소 경로 테스트</h2>
			            <p><strong>시나리오:</strong> 결제 7일 경과 또는 유료 콘텐츠를 이용한 상태에서 취소 요청 시 일반 구독 취소 처리되는지 확인합니다.</p>
			            <p><strong>테스트 순서:</strong></p>
			            <ol>
			                <li><a href="/payment-test-page" target="_blank">결제 테스트 페이지</a>에서 멤버십 결제 및 활성화.</li>
			                <li>이 페이지로 돌아와 User Access Token을 입력.</li>
			                <li>유료 콘텐츠 접근 시뮬레이션: '유료 노트 열람 시도' 버튼 클릭 (콘텐츠 이용 기록을 남김).</li>
			                <li>'멤버십 취소 요청' 버튼 클릭.</li>
			            </ol>
			            <button class="warning" onclick="accessPaidNote()">유료 노트 열람 시도 (로그 남김)</button>
			            <button class="danger" onclick="cancelMembershipUser()">멤버십 취소 요청 (User)</button>
			        </div>
			
			        <div class="container">
			            <h2>3. 관리자 수동 환불 테스트</h2>
			            <p><strong>시나리오:</strong> ADMIN 권한으로 특정 결제를 환불 요청합니다. (정책 검증 통과 상황 가정)</p>
			            <p><strong>테스트 순서:</strong></p>
			            <ol>
			                <li><a href="/payment-test-page" target="_blank">결제 테스트 페이지</a>에서 멤버십 결제 및 활성화.</li>
			                <li>이 페이지로 돌아와 User Access Token과 Admin Access Token을 입력.</li>
			                <li>아래 입력란에 환불할 결제의 PaymentKey를 입력.</li>
			                <li>'관리자 환불 요청' 버튼 클릭.</li>
			            </ol>
			            <div class="form-group">
			                <label for="payment-key-input">환불할 PaymentKey</label>
			                <input type="text" id="payment-key-input" placeholder="Toss Payments 결제 시 발급받은 PaymentKey를 입력하세요.">
			            </div>
			            <div class="form-group">
			                <label for="admin-refund-reason">환불 사유</label>
			                <input type="text" id="admin-refund-reason" value="관리자 수동 환불 테스트">
			            </div>
			            <button class="danger" onclick="adminRefundPayment()">관리자 환불 요청</button>
			                    </div>
			
			                    <div class="container">
			                        <h2>4. 관리자 사용자 정보 조회</h2>
			                        <p><strong>시나리오:</strong> ADMIN 권한으로 특정 사용자의 결제 내역, 멤버십 상태, 콘텐츠 접근 기록을 조회합니다.</p>
			                        <p><strong>테스트 순서:</strong></p>
			                        <ol>
			                            <li>위 섹션에서 USER Access Token과 Admin Access Token을 입력.</li>
			                            <li>아래 입력란에 조회할 사용자의 이메일, 페이지 번호, 페이지 크기를 입력.</li>
			                            <li>'사용자 정보 조회' 버튼 클릭.</li>
			                        </ol>
			                        <div class="form-group">
			                            <label for="admin-query-user-email">조회할 사용자 이메일</label>
			                            <input type="email" id="admin-query-user-email" placeholder="testuser@example.com">
			                        </div>
			                        <div class="form-group">
			                            <label for="query-page">페이지 번호</label>
			                            <input type="number" id="query-page" value="0">
			                        </div>
			                        <div class="form-group">
			                            <label for="query-size">페이지 크기</label>
			                            <input type="number" id="query-size" value="10">
			                        </div>
			                        <button class="success" onclick="getAdminUserDetails()">사용자 정보 조회</button>
			                    </div>
			
			                    <div class="container">
			                        <h2>API 호출 결과</h2>
			                        <pre id="api-status">API 호출 결과가 여기에 표시됩니다.</pre>
			                    </div>
			
			                    <script>
			                        const statusDiv = document.getElementById('api-status');
			                        const authTokenInput = document.getElementById('auth-token');
			                        const adminAuthTokenInput = document.getElementById('admin-auth-token');
			
			                        function displayStatus(data) {
			                            statusDiv.textContent = JSON.stringify(data, null, 2);
			                        }
			
			                        function getUserAuthHeader() {
			                            const token = authTokenInput.value.trim();
			                            if (!token) {
			                                alert('User Access Token을 입력해주세요.');
			                                throw new Error('User Token not found');
			                            }
			                            return {
			                                'Authorization': 'Bearer ' + token,
			                                'Content-Type': 'application/json'
			                            };
			                        }
			
			                        function getAdminAuthHeader() {
			                            const token = adminAuthTokenInput.value.trim();
			                            if (!token) {
			                                alert('Admin Access Token을 입력해주세요.');
			                                throw new Error('Admin Token not found');
			                            }
			                            return {
			                                'Authorization': 'Bearer ' + token,
			                                'Content-Type': 'application/json'
			                            };
			                        }
			
			                        async function apiCall(endpoint, options) {
			                            try {
			                                const response = await fetch(endpoint, options);
			                                if (response.status === 204) { // No Content
			                                    return { success: true, message: '요청이 성공적으로 처리되었습니다 (No Content).' };
			                                }
			                                const data = await response.json();
			                                if (!response.ok) {
			                                    throw (data.error || new Error('API call failed with status: ' + response.status));
			                                }
			                                return data;
			                            } catch (error) {
			                                console.error('API Error:', error);
			                                displayStatus({ success: false, error: error.message || '알 수 없는 오류 발생' });
			                                return null;
			                            }
			                        }
			
			                        async function checkMembershipStatus() {
			                            try {
			                                const headers = getUserAuthHeader();
			                                const result = await apiCall('/api/memberships/status', {
			                                    method: 'GET',
			                                    headers: headers
			                                });
			                                displayStatus(result);
			                            } catch (error) {
			                                console.error('멤버십 상태 조회 오류:', error);
			                                displayStatus({ success: false, error: error.message || '멤버십 상태 조회 중 오류 발생' });
			                            }
			                        }
			
			                        async function cancelMembershipUser() {
			                            try {
			                                const headers = getUserAuthHeader();
			                                const result = await apiCall('/api/memberships/cancel', {
			                                    method: 'POST',
			                                    headers: headers
			                                });
			                                displayStatus(result);
			                                if (result && result.success) {
			                                    alert('멤버십 취소/환불 요청이 성공적으로 처리되었습니다. 상태를 다시 조회해 확인하세요.');
			                                } else {
			                                    alert('멤버십 취소/환불 요청에 실패했습니다.');
			                                }
			                            } catch (error) {
			                                console.error('멤버십 취소/환불 요청 오류:', error);
			                                displayStatus({ success: false, error: error.message || '멤버십 취소/환불 요청 중 오류 발생' });
			                            }
			                        }
			
			                        async function accessPaidNote() {
			                            try {
			                                const headers = getUserAuthHeader();
			                                // Note: 실제 테스트를 위해 유료 노트 ID를 백엔드에서 받아오거나 직접 설정해야 합니다.
			                                // 여기서는 임시로 첫 번째 유료 노트로 가정합니다.
			                                const paidNoteId = 1; // 실제 유료 노트 ID로 변경 필요
			                                const result = await apiCall('/api/notes/published/today-detail', { // 유료 콘텐츠 API 호출
			                                    method: 'GET',
			                                    headers: headers
			                                });
			                                displayStatus(result);
			                                if (result && result.success) {
			                                    alert('유료 노트 열람 시도 성공! 콘텐츠 접근 기록이 남았습니다.');
			                                } else {
			                                    alert('유료 노트 열람 시도 실패.');
			                                }
			                            } catch (error) {
			                                console.error('유료 노트 열람 오류:', error);
			                                displayStatus({ success: false, error: error.message || '유료 노트 열람 중 오류 발생' });
			                            }
			                        }
			
			                        async function adminRefundPayment() {
			                            const paymentKey = document.getElementById('payment-key-input').value.trim();
			                            const reason = document.getElementById('admin-refund-reason').value.trim();
			                            if (!paymentKey) {
			                                alert('환불할 PaymentKey를 입력해주세요.');
			                                return;
			                            }
			                            if (!reason) {
			                                alert('환불 사유를 입력해주세요.');
			                                return;
			                            }
			
			                            try {
			                                const headers = getAdminAuthHeader();
			                                const result = await apiCall(`/api/admin/payments/${paymentKey}/refund`, {
			                                    method: 'POST',
			                                    headers: headers,
			                                    body: JSON.stringify({ reason: reason })
			                                });
			                                displayStatus(result);
			                                if (result && result.success) {
			                                    alert('관리자 환불 요청이 성공적으로 처리되었습니다!');
			                                } else {
			                                    alert('관리자 환불 요청에 실패했습니다.');
			                                }
			                            } catch (error) {
			                                console.error('관리자 환불 요청 오류:', error);
			                                displayStatus({ success: false, error: error.message || '관리자 환불 요청 중 오류 발생' });
			                            }
			                        }
			
			                        async function getAdminUserDetails() {
			                            const userEmail = document.getElementById('admin-query-user-email').value.trim();
			                            const page = document.getElementById('query-page').value.trim();
			                            const size = document.getElementById('query-size').value.trim();
			
			                            if (!userEmail) {
			                                alert('조회할 사용자 이메일을 입력해주세요.');
			                                return;
			                            }
			
			                            try {
			                                const headers = getAdminAuthHeader();
			                                const endpoint = `/api/admin/payments/details-by-user?email=${userEmail}&page=${page}&size=${size}`;
			                                const result = await apiCall(endpoint, {
			                                    method: 'GET',
			                                    headers: headers
			                                });
			                                displayStatus(result);
			                            } catch (error) {
			                                console.error('관리자 사용자 정보 조회 오류:', error);
			                                displayStatus({ success: false, error: error.message || '관리자 사용자 정보 조회 중 오류 발생' });
			                            }
			                        }
			                    </script>
			                    </body>
			                    </html>
			""";
	}
}

