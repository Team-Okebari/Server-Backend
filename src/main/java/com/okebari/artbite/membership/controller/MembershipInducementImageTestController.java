package com.okebari.artbite.membership.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class MembershipInducementImageTestController {

    @GetMapping("/membership-inducement-image-test-page")
    @ResponseBody
    public String getMembershipInducementImageTestPage() {
        return """
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <title>멤버십 유도 이미지 테스트</title>
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; padding: 20px; background-color: #f8f9fa; line-height: 1.6; }
                    .page-container { max-width: 800px; margin: 20px auto; }
                    .container { background: #fff; padding: 25px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.08); margin-bottom: 20px; }
                    h1, h2, h3, h4 { color: #343a40; }
                    h1 { text-align: center; margin-bottom: 30px; }
                    h2 { border-bottom: 2px solid #dee2e6; padding-bottom: 10px; margin-top: 20px; }
                    h3 { margin-top: 25px; color: #495057; }
                    .form-group { margin-bottom: 15px; }
                    .form-group label { display: block; margin-bottom: 5px; font-weight: 500; }
                    .form-group input, .form-group textarea { width: 100%; padding: 10px; border: 1px solid #ced4da; border-radius: 4px; box-sizing: border-box; font-size: 14px; }
                    button { display: inline-block; padding: 10px 15px; font-size: 14px; font-weight: bold; color: #fff; background-color: #007bff; border: none; border-radius: 5px; cursor: pointer; transition: background-color 0.2s; text-decoration: none; text-align: center; margin-right: 10px; margin-top: 10px; }
                    button:hover { background-color: #0056b3; }
                    button.update { background-color: #28a745; }
                    button.update:hover { background-color: #218838; }
                    #api-status { margin-top: 20px; padding: 15px; background-color: #e9ecef; border-radius: 5px; word-wrap: break-word; white-space: pre-wrap; font-family: monospace; }
                    #current-image-display { margin-top: 20px; text-align: center; }
                    #current-image-display img { max-width: 100%; height: auto; border: 1px solid #ced4da; border-radius: 4px; }
                    .flex-nav { display: flex; gap: 10px; margin-bottom: 20px; }
                </style>
            </head>
            <body>
                <div class="page-container">
                    <div class="flex-nav">
                        <a href="/login-test-page" class="button" style="background-color: #6c757d;">인증 테스트</a>
                        <a href="/note-admin-test-page" class="button" style="background-color: #17a2b8;">Note 관리자 기능 테스트</a>
                    </div>
                    <h1>멤버십 유도 이미지 테스트</h1>
            
                    <div class="container">
                        <h2>공개 API 테스트 (GET /api/membership-inducement-image)</h2>
                        <p>로그인 없이 누구나 접근 가능한 유도 이미지 URL을 조회합니다.</p>
                        <button onclick="getInducementImage()">현재 이미지 URL 조회</button>
                        <div id="current-image-display">
                            <p>이미지 URL: <span id="image-url-text">불러오는 중...</span></p>
                            <img id="inducement-image" src="" alt="멤버십 유도 이미지" style="display: none;">
                        </div>
                    </div>
            
                    <div class="container">
                        <h2>관리자 API 테스트 (PUT /api/admin/membership-inducement-image)</h2>
                        <p>ADMIN 권한이 있는 사용자만 유도 이미지 URL을 업데이트할 수 있습니다.</p>
                        <div class="form-group">
                            <label for="admin-auth-token">ADMIN Access Token (Bearer)</label>
                            <textarea id="admin-auth-token" rows="3" placeholder="ADMIN 권한의 Access Token을 여기에 붙여넣으세요."></textarea>
                        </div>
                        <div class="form-group">
                            <label for="new-image-url">새 이미지 URL</label>
                            <input type="text" id="new-image-url" placeholder="새로운 S3 이미지 URL을 입력하세요.">
                        </div>
                        <button class="update" onclick="updateInducementImage()">이미지 URL 업데이트</button>
                    </div>
            
                    <div class="container">
                        <h2>API 호출 결과</h2>
                        <pre id="api-status">API 호출 결과가 여기에 표시됩니다.</pre>
                    </div>
                </div>
            
                <script>
                    const statusDiv = document.getElementById('api-status');
            
                    function displayStatus(data) {
                        statusDiv.textContent = JSON.stringify(data, null, 2);
                    }
            
                    function getAdminAuthHeader() {
                        const token = document.getElementById('admin-auth-token').value.trim();
                        if (!token) {
                            alert('ADMIN Access Token을 입력해주세요.');
                            throw new Error('ADMIN Token not found');
                        }
                        return {
                            'Authorization': 'Bearer ' + token,
                            'Content-Type': 'application/json'
                        };
                    }
            
                    async function apiCall(endpoint, options) {
                        try {
                            const response = await fetch(endpoint, options);
                            if (response.status === 204 || response.headers.get("content-length") === "0") {
                                return { success: true, message: '요청이 성공적으로 처리되었습니다 (No Content).' };
                            }
                            const data = await response.json();
                            if (!response.ok) throw (data.error || new Error('API call failed'));
                            return data;
                        } catch (error) {
                            console.error('API Error:', error);
                            displayStatus({ success: false, error: error });
                            return null;
                        }
                    }
            
                    async function getInducementImage() {
                        try {
                            // Public API call, no auth header needed
                            const result = await apiCall('/api/membership-inducement-image', { method: 'GET' });
                            displayStatus(result);
                            if (result && result.success && result.data && result.data.imageUrl) {
                                document.getElementById('image-url-text').textContent = result.data.imageUrl;
                                const imgElement = document.getElementById('inducement-image');
                                imgElement.src = result.data.imageUrl;
                                imgElement.style.display = 'block';
                            } else {
                                document.getElementById('image-url-text').textContent = '이미지 URL을 찾을 수 없습니다.';
                                document.getElementById('inducement-image').style.display = 'none';
                            }
                        } catch (error) {
                            console.error('Error fetching inducement image:', error);
                            document.getElementById('image-url-text').textContent = '이미지 로드 중 오류 발생.';
                            document.getElementById('inducement-image').style.display = 'none';
                        }
                    }
            
                    async function updateInducementImage() {
                        const newImageUrl = document.getElementById('new-image-url').value.trim();
                        if (!newImageUrl) {
                            alert('새 이미지 URL을 입력해주세요.');
                            return;
                        }
            
                        try {
                            const headers = getAdminAuthHeader(); // Admin API call, requires auth header
                            const body = JSON.stringify({ imageUrl: newImageUrl });
                            const result = await apiCall('/api/admin/membership-inducement-image', {
                                method: 'PUT',
                                headers: headers,
                                body: body
                            });
                            displayStatus(result);
                            if (result && result.success) {
                                alert('이미지 URL이 성공적으로 업데이트되었습니다!');
                                // 업데이트 후 현재 이미지 다시 로드
                                getInducementImage();
                            } else {
                                alert('이미지 URL 업데이트에 실패했습니다.');
                            }
                        } catch (error) {
                            console.error('Error updating inducement image:', error);
                            alert('이미지 URL 업데이트 중 오류 발생.');
                        }
                    }
            
                    window.onload = () => {
                        getInducementImage(); // 페이지 로드 시 현재 이미지 조회
                    };
                </script>
            </body>
            </html>
            """;
    }
}

