package com.okebari.artbite.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class LogoutTestController {

	@GetMapping("/logout-test-page")
	@ResponseBody
	public String getLogoutTestPage() {
		return """
			<!DOCTYPE html>
			<html lang="en">
			<head>
			    <meta charset="UTF-8">
			    <meta name="viewport" content="width=device-width, initial-scale=1.0">
			    <title>Logout Test</title>
			    <script>
			        function logout() {
			            const accessToken = document.getElementById('accessToken').value;
			            const refreshToken = document.getElementById('refreshToken').value;
			
			            fetch('http://localhost:8080/api/auth/logout', {
			                method: 'POST',
			                headers: {
			                    'Authorization': 'Bearer ' + accessToken,
			                    'Content-Type': 'application/json'
			                }
			            })
			                        .then(response => {
			                            console.log('Backend Logout Response Status:', response.status);
			                            if (!response.ok) {
			                                return response.json().then(error => Promise.reject(error));
			                            }
			                            return response.json(); // Always parse JSON
			                        })
			                        .then(data => {
			                            console.log('Backend Logout Success:', data);
			                            if (data.success && data.data) { // Check if data contains a redirect URL
			                                console.log('Redirecting to social logout:', data.data);
			                                window.location.href = data.data; // Redirect the browser
			                            } else {
			                                document.getElementById('status').innerText = 'Logout successful from app. Check console for details.';
			                            }
			                        })
			                        .catch(error => {
			                console.error('Backend Logout Error:', error);
			                document.getElementById('status').innerText = 'Logout failed from app. Check console for errors.';
			            });
			        }
			
			        function getUrlParameter(name) {
			            name = name.replace(/[\\[]/, '\\\\[').replace(/[\\]]/, '\\\\]');
			            var regex = new RegExp('[\\\\?&]' + name + '=([^&#]*)');
			            var results = regex.exec(location.search);
			            return results === null ? '' : decodeURIComponent(results[1].replace(/\\+/g, ' '));
			        };
			
			        window.onload = function() {
			            const urlAccessToken = getUrlParameter('accessToken');
			            if (urlAccessToken) {
			                document.getElementById('accessToken').value = urlAccessToken;
			                window.history.replaceState({}, document.title, window.location.pathname);
			            }
			        };
			    </script>
			    <style>
			        body { font-family: sans-serif; margin: 20px; }
			        input[type="text"], button { padding: 10px; margin-bottom: 10px; width: 300px; display: block; }
			        button { cursor: pointer; background-color: #4CAF50; color: white; border: none; }
			        button:hover { background-color: #45a049; }
			        #status { margin-top: 20px; font-weight: bold; }
			    </style>
			</head>
			<body>
			    <h1>Logout Test Page</h1>
			    <p>1. 카카오 로그인 후 리다이렉트된 URL에서 Access Token을 복사하여 아래 입력란에 붙여넣으세요.</p>
			    <p>2. (선택 사항) 브라우저 개발자 도구에서 `refreshToken` 쿠키 값을 확인하여 `refreshToken` 입력란에 붙여넣으세요.</p>
			    <p>3. "Logout" 버튼을 클릭하여 카카오 로그아웃 흐름을 테스트합니다.</p>
			
			    <label for="accessToken">Access Token:</label>
			    <input type="text" id="accessToken" placeholder="Enter Access Token here">
			
			    <label for="refreshToken">Refresh Token (for reference, browser sends cookie):</label>
			    <input type="text" id="refreshToken" placeholder="Enter Refresh Token here (optional, for info)">
			
			    <button onclick="logout()">Logout</button>
			
			    <div id="status"></div>
			</body>
			</html>
			""";
	}
}
