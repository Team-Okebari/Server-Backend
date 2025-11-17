package com.okebari.artbite.creator.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class CreatorTestController {

	@GetMapping("/creator-test-page")
	@ResponseBody
	public String getCreatorTestPage() {
		return """
			<!DOCTYPE html>
			<html lang="ko">
			<head>
			    <meta charset="UTF-8">
			    <title>Creator 관리 기능 테스트</title>
			    <style>
			        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; padding: 20px; background-color: #f8f9fa; line-height: 1.6; }
			        .page-container { max-width: 800px; margin: 20px auto; }
			        .container { background: #fff; padding: 25px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.08); margin-bottom: 20px; }
			        h1, h2 { color: #343a40; }
			        h1 { text-align: center; margin-bottom: 30px; }
			        h2 { border-bottom: 2px solid #dee2e6; padding-bottom: 10px; margin-top: 20px; }
			        .form-group { margin-bottom: 15px; }
			        .form-group label { display: block; margin-bottom: 5px; font-weight: 500; }
			        .form-group input { width: 100%; padding: 10px; border: 1px solid #ced4da; border-radius: 4px; box-sizing: border-box; font-size: 14px; }
			        button, a.button { display: inline-block; padding: 10px 15px; font-size: 14px; font-weight: bold; color: #fff; background-color: #007bff; border: none; border-radius: 5px; cursor: pointer; transition: background-color 0.2s; text-decoration: none; text-align: center; margin-right: 10px; margin-top: 10px; }
			        button:hover, a.button:hover { background-color: #0056b3; }
			        #api-status { margin-top: 20px; padding: 15px; background-color: #e9ecef; border-radius: 5px; word-wrap: break-word; white-space: pre-wrap; font-family: monospace; }
			        .flex-nav { display: flex; gap: 10px; margin-bottom: 20px; }
			        .spinner { display: none; margin: 0 10px; width: 20px; height: 20px; border: 3px solid #f3f3f3; border-top: 3px solid #3498db; border-radius: 50%; animation: spin 1s linear infinite; vertical-align: middle; }
			        @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
			    </style>
			</head>
			<body>
			    <div class="page-container">
			        <div class="flex-nav">
			            <a href="/login-test-page" class="button" style="background-color: #6c757d;">인증 테스트</a>
			            <a href="/note-admin-test-page" class="button" style="background-color: #fd7e14;">Note 관리자 테스트</a>
			        </div>
			        <h1>Creator(작가) 관리 기능 테스트</h1>
			
			        <div class="container">
			            <h2>공통 설정</h2>
			            <div class="form-group">
			                <label for="auth-token">ADMIN Access Token (Bearer)</label>
			                <textarea id="auth-token" rows="3" placeholder="ADMIN 권한의 Access Token을 여기에 붙여넣으세요."></textarea>
			            </div>
			        </div>
			
			        <div class="container">
			            <h2>작가 등록</h2>
			            <form id="creator-form">
			                <div class="form-group"><label for="name">이름 (*필수)</label><input type="text" id="name" value="새로운 작가"></div>
			                <div class="form-group"><label for="bio">소개</label><input type="text" id="bio" value="안녕하세요. 새로운 작가입니다."></div>
			                <div class="form-group"><label for="jobTitle">직무</label><input type="text" id="jobTitle" value="일러스트레이터"></div>
			                <div class="form-group"><label for="profile-image-file">프로필 이미지 파일</label><input type="file" id="profile-image-file" accept="image/*"></div>
			                <div class="form-group"><label for="instagramUrl">인스타그램 URL</label><input type="text" id="instagramUrl"></div>
			                <div class="form-group"><label for="youtubeUrl">유튜브 URL</label><input type="text" id="youtubeUrl"></div>
			                <div class="form-group"><label for="behanceUrl">Behance URL</label><input type="text" id="behanceUrl"></div>
			                <div class="form-group"><label for="xUrl">X (트위터) URL</label><input type="text" id="xUrl"></div>
			                <div class="form-group"><label for="blogUrl">블로그 URL</label><input type="text" id="blogUrl"></div>
			                <div class="form-group"><label for="newsUrl">뉴스 URL</label><input type="text" id="newsUrl"></div>
			                <button type="button" onclick="createCreator()">작가 등록</button>
			                <div class="spinner" id="loading-spinner"></div>
			            </form>
			        </div>
			
			        <div class="container">
			            <h2>API 호출 결과</h2>
			            <pre id="api-status">API 호출 결과가 여기에 표시됩니다.</pre>
			        </div>
			    </div>
			
			    <script>
			        const statusDiv = document.getElementById('api-status');
			        const spinner = document.getElementById('loading-spinner');

			        function displayStatus(data) {
			            statusDiv.textContent = JSON.stringify(data, null, 2);
			        }
			
			        function getAuthHeader(isMultipart = false) {
			            const token = document.getElementById('auth-token').value.trim();
			            if (!token) {
			                alert('ADMIN Access Token을 입력해주세요.');
			                throw new Error('Token not found');
			            }
			            const headers = { 'Authorization': 'Bearer ' + token };
			            if (!isMultipart) {
			                headers['Content-Type'] = 'application/json';
			            }
			            return headers;
			        }

			        async function uploadProfileImage() {
			            const fileInput = document.getElementById('profile-image-file');
			            if (fileInput.files.length === 0) {
			                return ""; // 파일 없으면 빈 URL 반환
			            }
			            const file = fileInput.files[0];
			            const formData = new FormData();
			            formData.append('image', file);

			            try {
			                const headers = getAuthHeader(true);
			                const response = await fetch('/api/admin/images', {
			                    method: 'POST',
			                    headers: headers,
			                    body: formData
			                });
			                const data = await response.json();
			                if (!response.ok) throw (data.error || new Error('Profile image upload failed'));
			                return data.imageUrl;
			            } catch (error) {
			                console.error('Profile image upload error:', error);
			                throw error;
			            }
			        }
			
			        async function createCreator() {
			            spinner.style.display = 'inline-block';
			            displayStatus({ message: "작가 등록을 시작합니다..." });

			            try {
			                // 1. 프로필 이미지 업로드
			                displayStatus({ message: "프로필 이미지를 업로드하는 중..." });
			                const profileImageUrl = await uploadProfileImage();
			                
			                displayStatus({ message: "이미지 업로드 완료. 작가 정보를 등록합니다.", uploadedUrl: profileImageUrl });

			                // 2. 작가 정보 페이로드 구성
			                const body = JSON.stringify({
			                    name: document.getElementById('name').value,
			                    bio: document.getElementById('bio').value,
			                    jobTitle: document.getElementById('jobTitle').value,
			                    profileImageUrl: profileImageUrl, // 업로드된 URL 사용
			                    instagramUrl: document.getElementById('instagramUrl').value,
			                    youtubeUrl: document.getElementById('youtubeUrl').value,
			                    behanceUrl: document.getElementById('behanceUrl').value,
			                    xUrl: document.getElementById('xUrl').value,
			                    blogUrl: document.getElementById('blogUrl').value,
			                    newsUrl: document.getElementById('newsUrl').value
			                });
			
			                // 3. 작가 등록 API 호출
			                const response = await fetch('/api/admin/creators', {
			                    method: 'POST',
			                    headers: getAuthHeader(),
			                    body: body
			                });
			
			                const result = await response.json();
			                displayStatus(result);
			
			                if (result.success) {
			                    alert('작가 등록 성공! 반환된 ID: ' + result.data);
			                } else {
			                    alert('작가 등록 실패: ' + (result.error.message || '알 수 없는 오류'));
			                }
			            } catch (error) {
			                console.error('Error:', error);
			                displayStatus({ success: false, error: error.message || '클라이언트 측 오류 발생' });
			                alert('작업 중 오류가 발생했습니다. 콘솔을 확인하세요.');
			            } finally {
			                spinner.style.display = 'none';
			            }
			        }
			    </script>
			</body>
			</html>
			""";
	}
}
