package com.okebari.artbite.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class LoginTestController {

	@GetMapping("/login-test-page")
	@ResponseBody
	public String getLoginTestPage() {
		return """
			<!DOCTYPE html>
			<html lang="ko">
			<head>
			    <meta charset="UTF-8">
			    <meta name="viewport" content="width=device-width, initial-scale=1.0">
			    <title>인증 테스트 페이지</title>
			    <style>
			        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen-Sans, Ubuntu, Cantarell, "Helvetica Neue", sans-serif; margin: 0; padding: 0; background-color: #f8f9fa; color: #212529; }
			        .page-wrapper { max-width: 800px; margin: 0 auto; padding: 40px; }
			        .grid-container { display: grid; grid-template-columns: 1fr 1fr; gap: 40px; margin-top: 20px; }
			        .container { background-color: #ffffff; padding: 30px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
			        h1 { font-size: 28px; color: #343a40; margin-bottom: 20px; text-align: center; }
			        h2 { font-size: 20px; color: #495057; border-bottom: 1px solid #dee2e6; padding-bottom: 10px; margin-top: 30px; }
			        p { line-height: 1.6; }
			        .login-buttons { display: flex; flex-direction: column; gap: 15px; margin-top: 25px; }
			        a.button, button { display: block; width: 100%; padding: 15px 20px; border-radius: 5px; text-decoration: none; font-weight: 500; font-size: 16px; text-align: center; transition: all 0.2s ease-in-out; border: none; cursor: pointer; box-sizing: border-box; }
			        .google { background-color: #4285F4; color: white; }
			        .kakao { background-color: #FEE500; color: #191919; }
			        .naver { background-color: #03C75A; color: white; }
			        a.button:hover, button:hover { opacity: 0.9; transform: translateY(-2px); }
			                            #token-display, #api-status { margin-top: 30px; padding: 20px; background-color: #e9ecef; border: 1px solid #ced4da; border-radius: 5px; word-wrap: break-word; font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, Courier, monospace; font-size: 14px; width: 100%; max-width: 278px; }
			                            #token-display h3, #api-status h3 { margin-top: 0; }
			        #token-display pre, #api-status pre { white-space: pre-wrap; }
			        .form-group { margin-bottom: 15px; }
			        .form-group label { display: block; margin-bottom: 5px; font-weight: 500; }
			        .form-group input { width: 100%; padding: 10px; border: 1px solid #ced4da; border-radius: 4px; box-sizing: border-box; }
			        .action-buttons { display: flex; gap: 10px; margin-top: 20px; }
			        #login-status { margin-bottom: 20px; padding: 15px; background-color: #d4edda; border: 1px solid #c3e6cb; border-radius: 5px; color: #155724; font-weight: bold; text-align: center; }
			        #login-status.logged-out { background-color: #f8d7da; border-color: #f5c6cb; color: #721c24; }
			    </style>
			    <script>
			        let currentAccessToken = '';
			
			        function getUrlParameter(name) {
			            name = name.replace(/[[\\]]/g, '\\$&');
			            var regex = new RegExp('[\\?&]' + name + '=([^&#]*)');
			            var results = regex.exec(location.search);
			            return results === null ? '' : decodeURIComponent(results[1].replace(/\\+/g, ' '));
			        }
			
			        function parseJwt(token) {
			            try {
			                const base64Url = token.split('.')[1];
			                const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
			                const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
			                    return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
			                }).join(''));
			                return JSON.parse(jsonPayload);
			            } catch (e) {
			                console.error("JWT 파싱 오류:", e);
			                return null;
			            }
			        }
			
			        function updateLoginStatus(email, type) {
			            const loginStatusDiv = document.getElementById('login-status');
			            if (email) {
			                loginStatusDiv.innerHTML = `<p>${email} 계정으로 <strong>${type}</strong> 로그인되었습니다.</p>`;
			                loginStatusDiv.classList.remove('logged-out');
			            } else {
			                loginStatusDiv.innerHTML = '<p>로그인되지 않았습니다.</p>';
			                loginStatusDiv.classList.add('logged-out');
			            }
			        }
			
			        function displayStatus(containerId, title, data) {
			            const statusContainer = document.getElementById(containerId);
			            statusContainer.innerHTML = `<h3>${title}</h3><pre>${JSON.stringify(data, null, 2)}</pre>`;
			        }
			
			        async function apiCall(endpoint, options, statusTitle) {
			            try {
			                const response = await fetch(`/api/auth/${endpoint}`, options);
			                const data = await response.json();
			                if (!response.ok) {
			                    throw data; // Throw error object from backend
			                }
			                displayStatus('api-status', statusTitle, data);
			                return data;
			            } catch (error) {
			                console.error(`${statusTitle} 오류:`, error);
			                displayStatus('api-status', `${statusTitle} 오류`, error);
			                return null;
			            }
			        }
			
			        async function signup(event) {
			            event.preventDefault();
			            const form = event.target;
			            const body = JSON.stringify({
			                email: form.email.value,
			                password: form.password.value,
			                username: form.username.value
			            });
			            await apiCall('signup', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body }, '회원가입 결과');
			        }
			
			        async function login(event) {
			            event.preventDefault();
			            const form = event.target;
			            const email = form.email.value; // Get email from form
			            const body = JSON.stringify({ email: email, password: form.password.value });
			            const result = await apiCall('login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body }, '로그인 결과');
			            if (result && result.data && result.data.accessToken) {
			                currentAccessToken = result.data.accessToken;
			                document.getElementById('token-display').innerHTML = `<h3>현재 토큰</h3><pre>${currentAccessToken}</pre>`;
			                updateLoginStatus(email, '일반'); // Update status with email
			            }
			        }
			
			        async function logout() {
			            if (!currentAccessToken) {
			                alert('먼저 로그인해주세요.');
			                return;
			            }
			            const result = await apiCall('logout', { method: 'POST', headers: { 'Authorization': `Bearer ${currentAccessToken}` } }, '로그아웃 결과');
			
			            if (result && result.success && result.data) {
			                console.log('Redirecting to social logout:', result.data);
			                updateLoginStatus(null); // Clear status before redirect
			                window.location.href = result.data;
			            } else if (result && result.success) {
			                console.log('App logout successful.');
			                currentAccessToken = '';
			                document.getElementById('token-display').innerHTML = '<h3>현재 토큰</h3><p>로그아웃되었습니다.</p>';
			                displayStatus('api-status', '로그아웃 결과', { success: true, message: '앱에서 로그아웃되었습니다.' });
			                updateLoginStatus(null); // Clear status after app logout
			            }
			        }
			
			        async function reissue() {
			            const result = await apiCall('reissue', { method: 'POST' }, '토큰 재발급 결과');
			            if (result && result.data && result.data.accessToken) {
			                currentAccessToken = result.data.accessToken;
			                document.getElementById('token-display').innerHTML = `<h3>새 토큰</h3><pre>${currentAccessToken}</pre>`;
			                const decodedToken = parseJwt(currentAccessToken);
			                if (decodedToken && decodedToken.sub) {
			                    updateLoginStatus(decodedToken.sub, '재발급'); // Update status with new token's email
			                }
			            }
			        }
			
			        window.onload = () => {
			            const socialAccessToken = getUrlParameter('accessToken');
			            if (socialAccessToken) {
			                currentAccessToken = socialAccessToken;
			                document.getElementById('token-display').innerHTML = `<h3>소셜 로그인 토큰</h3><pre>${currentAccessToken}</pre>`;
			                window.history.replaceState({}, document.title, window.location.pathname);
			
			                const decodedToken = parseJwt(socialAccessToken);
			                if (decodedToken && decodedToken.sub) {
			                    updateLoginStatus(decodedToken.sub, '소셜'); // Update status with social login email
			                }
			            } else {
			                updateLoginStatus(null); // Initialize as logged out
			            }
			        };
			    </script>
			</head>
			<body>
			    <div class="page-wrapper">
			        <h1 style="text-align: center;">인증 테스트 페이지</h1>
			        <div id="login-status"></div>
			        <div class="grid-container">
			            <div class="container">
			                <h1>일반 인증 테스트</h1>
			
			                <h2>회원가입</h2>
			                <form onsubmit="signup(event)">
			                    <div class="form-group"><label>이메일</label><input name="email" type="email" required></div>
			                    <div class="form-group"><label>비밀번호</label><input name="password" type="password" required></div>
			                    <div class="form-group"><label>사용자 이름</label><input name="username" type="text" required></div>
			                    <button type="submit">회원가입</button>
			                </form>
			
			                <h2>로그인</h2>
			                <form onsubmit="login(event)">
			                    <div class="form-group"><label>이메일</label><input name="email" type="email" required></div>
			                    <div class="form-group"><label>비밀번호</label><input name="password" type="password" required></div>
			                    <button type="submit">로그인</button>
			                </form>
			
			                <h2>기능</h2>
			                <div class="action-buttons">
			                    <button onclick="logout()">로그아웃</button>
			                    <button onclick="reissue()">토큰 재발급</button>
			                </div>
			
			                <div id="api-status">
			                    <h3>API 상태</h3>
			                    <p>API 호출 결과가 여기에 표시됩니다.</p>
			                </div>
			            </div>
			
			            <div class="container">
			                <h1>소셜 로그인 테스트</h1>
			                <p>아래 버튼을 클릭하여 각 소셜 로그인을 테스트합니다. 로그인 성공 시, 이 페이지로 리디렉션되며 발급된 Access Token이 화면에 표시됩니다.</p>
			                <div class="login-buttons">
			                    <a href="/oauth2/authorization/google" class="button google">Google 계정으로 로그인</a>
			                    <a href="/oauth2/authorization/kakao" class="button kakao">Kakao 계정으로 로그인</a>
			                    <a href="/oauth2/authorization/naver" class="button naver">Naver 계정으로 로그인</a>
			                </div>
			
			                <div id="token-display">
			                    <h3>현재 토큰</h3>
			                    <p>로그인하여 토큰을 확인하세요.</p>
			                </div>
			            </div>
			        </div>
			    </div>
			</body>
			</html>
			""";
	}
}