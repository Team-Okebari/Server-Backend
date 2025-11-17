package com.okebari.artbite.note.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class NoteUserTestController {

	@GetMapping("/note-user-test-page")
	@ResponseBody
	public String getNoteUserTestPage() {
		return """
			<!DOCTYPE html>
			<html lang="ko">
			<head>
			    <meta charset="UTF-8">
			    <title>Note 사용자 기능 테스트</title>
			    <style>
			        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; padding: 20px; background-color: #f8f9fa; line-height: 1.6; }
			        .page-container { max-width: 1200px; margin: 20px auto; }
			        .grid-container { display: grid; grid-template-columns: repeat(auto-fit, minmax(350px, 1fr)); gap: 20px; }
			        .container { background: #fff; padding: 25px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.08); margin-bottom: 20px; }
			        h1, h2, h3 { color: #343a40; }
			        h1 { text-align: center; margin-bottom: 30px; }
			        h2 { border-bottom: 2px solid #dee2e6; padding-bottom: 10px; margin-top: 20px; }
			        h3 { margin-top: 25px; color: #495057; }
			        .form-group { margin-bottom: 15px; }
			        .form-group label { display: block; margin-bottom: 5px; font-weight: 500; }
			        .form-group input, .form-group textarea { width: 100%; padding: 10px; border: 1px solid #ced4da; border-radius: 4px; box-sizing: border-box; font-size: 14px; }
			        button, a.button { display: inline-block; padding: 10px 15px; font-size: 14px; font-weight: bold; color: #fff; background-color: #007bff; border: none; border-radius: 5px; cursor: pointer; transition: background-color 0.2s; text-decoration: none; text-align: center; margin-right: 10px; margin-top: 10px; }
			        button:hover, a.button:hover { background-color: #0056b3; }
			        button.delete { background-color: #dc3545; }
			        button.delete:hover { background-color: #c82333; }
			        #api-status { margin-top: 20px; padding: 15px; background-color: #e9ecef; border-radius: 5px; word-wrap: break-word; white-space: pre-wrap; font-family: monospace; }
			        .flex-nav { display: flex; gap: 10px; margin-bottom: 20px; }
			        .user-type-section { border: 1px solid #ccc; border-radius: 8px; padding: 15px; margin-top: 20px; background-color: #f0f8ff; }
			        .user-type-section h3 { margin-top: 0; color: #0056b3; }
			        .user-type-section.logged-in { background-color: #e6ffe6; border-color: #4CAF50; }
			        .user-type-section.membership { background-color: #fff0e6; border-color: #ff9800; }
			    </style>
			</head> 
			<body>
			    <div class="page-container">
			        <div class="flex-nav">
			            <a href="/login-test-page" class="button" style="background-color: #6c757d;">인증 테스트</a>
			            <a href="/note-admin-test-page" class="button" style="background-color: #fd7e14;">Note 관리자 기능 테스트</a>
			        </div>
			        <h1>Note 사용자 기능 테스트</h1>
			
			        <div class="container">
			            <h2>공통 설정</h2>
			            <div class="form-group">
			                <label for="auth-token">Access Token (Bearer)</label>
			                <textarea id="auth-token" rows="3" placeholder="USER 권한의 Access Token을 여기에 붙여넣으세요. (공개 API는 불필요)"></textarea>
			            </div>
			        </div>
			
			        <div class="grid-container">
			            <div class="container user-type-section">
			                <h3>비로그인 사용자 (Non-Logged-in User)</h3>
			                <p>Access Token 없이 호출 가능</p>
			                <button onclick="getTodayCover()">금일 커버 조회 (공개)</button>
			            </div>
			
			            <div class="container user-type-section logged-in">
			                <h3>로그인 사용자 (Logged-in User)</h3>
			                <p>Access Token 필요</p>
			                <h4>노트 조회 (Query)</h4>
			                <button onclick="getTodayPreview()">금일 노트 미리보기 (USER)</button>
			                <div class="form-group">
			                    <label for="archived-keyword">지난 노트 검색 키워드 (선택 사항)</label>
			                    <input type="text" id="archived-keyword" placeholder="검색어 (선택 사항)">
			                </div>
			                <button onclick="getArchivedList()">지난 노트 목록 조회 (USER)</button>
			
			                <h4>북마크 (Bookmark)</h4>
			                <div class="form-group">
			                    <label for="bookmark-note-id">북마크할 Note ID (*필수)</label>
			                    <input type="text" id="bookmark-note-id" placeholder="북마크 토글할 노트 ID">
			                </div>
			                <button onclick="toggleBookmark()">북마크 토글</button>
			                <button onclick="listBookmarks()">내 북마크 목록 조회</button>
			
			                <h4>질문 답변 (Answer)</h4>
			                <div class="form-group">
			                    <label for="question-id">답변할 Question ID (*필수)</label>
			                    <input type="text" id="question-id" placeholder="답변할 질문의 ID">
			                </div>
			                <div class="form-group">
			                    <label for="answer-text">답변 내용 (*필수, 최대 200자)</label>
			                    <textarea id="answer-text" placeholder="답변을 입력하세요..."></textarea>
			                </div>
			                <button onclick="createAnswer()">답변 생성</button>
			                <button onclick="updateAnswer()">답변 수정</button>
			                <button onclick="getAnswer()">내 답변 조회</button>
			                <button class="delete" onclick="deleteAnswer()">답변 삭제</button>
			
			                <h4>리마인더 (Reminder)</h4>
			                <button onclick="getTodayReminder()">오늘의 리마인더 조회</button>
			                <button onclick="dismissReminder()">오늘 리마인더 그만보기</button>
			            </div>
			
			            <div class="container user-type-section membership">
			                <h3>멤버십 사용자 (Membership User)</h3>
			                <p>Access Token 및 활성 멤버십 필요</p>
			                <h4>노트 조회 (Query)</h4>
			                <button onclick="getTodayDetail()">금일 노트 상세 (멤버십 구독자)</button>
			                <div class="form-group">
			                    <label for="archived-note-id">지난 노트 ID (*필수)</label>
			                    <input type="text" id="archived-note-id" placeholder="조회할 지난 노트 ID">
			                </div>
			                <button onclick="getArchivedDetail()">지난 노트 상세/미리보기 (멤버십)</button>
			            </div>
			        </div>
			
			        <div class="container" style="margin-top: 20px;">
			            <h2>API 호출 결과</h2>
			            <pre id="api-status">API 호출 결과가 여기에 표시됩니다.</pre>
			        </div>
			    </div>
			
			    <script>
			        const statusDiv = document.getElementById('api-status');
			
			        function displayStatus(data) {
			            statusDiv.textContent = JSON.stringify(data, null, 2);
			        }
			
			        function getAuthHeader(isAuthRequired = true) {
			            const token = document.getElementById('auth-token').value.trim();
			            if (isAuthRequired && !token) {
			                alert('Access Token을 입력해주세요.');
			                throw new Error('Token not found');
			            }
			            return token ? { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json' } : { 'Content-Type': 'application/json' };
			        }
			
			        async function apiCall(endpoint, options) {
			            try {
			                const response = await fetch(endpoint, options);
			                if (response.status === 204) {
			                    return { success: true, message: '요청이 성공적으로 처리되었습니다 (No Content).' };
			                }
			                const data = await response.json();
			                if (!response.ok) throw data;
			                return data;
			            } catch (error) {
			                console.error('API Error:', error);
			                return { success: false, error: error };
			            }
			        }
			
			        // --- Query Functions ---
			        async function getTodayCover() {
			            const result = await apiCall('/api/notes/published/today-cover', { method: 'GET', headers: getAuthHeader(false) });
			            displayStatus(result);
			        }
			
			        async function getTodayPreview() {
			            const result = await apiCall('/api/notes/published/today-preview', { method: 'GET', headers: getAuthHeader() });
			            displayStatus(result);
			        }
			
			        async function getTodayDetail() {
			            const result = await apiCall('/api/notes/published/today-detail', { method: 'GET', headers: getAuthHeader() });
			            displayStatus(result);
			        }
			
			        async function getArchivedDetail() {
			            const noteId = document.getElementById('archived-note-id').value.trim();
			            if (!noteId) { alert('조회할 지난 노트 ID를 입력해주세요.'); return; }
			            const result = await apiCall(`/api/notes/archived/${noteId}`, { method: 'GET', headers: getAuthHeader() });
			            displayStatus(result);
			        }
			
			        async function getArchivedList() {
			            const keyword = document.getElementById('archived-keyword').value.trim();
			            const endpoint = keyword ? `/api/notes/archived?keyword=${keyword}` : '/api/notes/archived';
			            const result = await apiCall(endpoint, { method: 'GET', headers: getAuthHeader() });
			            displayStatus(result);
			        }
			
			        // --- Bookmark Functions ---
			        async function toggleBookmark() {
			            const noteId = document.getElementById('bookmark-note-id').value.trim();
			            if (!noteId) { alert('북마크할 Note ID를 입력해주세요.'); return; }
			            const result = await apiCall(`/api/notes/${noteId}/bookmark`, { method: 'POST', headers: getAuthHeader() });
			            displayStatus(result);
			        }
			
			        async function listBookmarks() {
			            const result = await apiCall('/api/notes/bookmarks', { method: 'GET', headers: getAuthHeader() });
			            displayStatus(result);
			        }
			
			        // --- Answer Functions ---
			        async function createAnswer() {
			            const questionId = document.getElementById('question-id').value.trim();
			            if (!questionId) { alert('답변할 Question ID를 입력해주세요.'); return; }
			            const body = JSON.stringify({ answerText: document.getElementById('answer-text').value });
			            const result = await apiCall(`/api/notes/questions/${questionId}/answer`, { method: 'POST', headers: getAuthHeader(), body: body });
			            displayStatus(result);
			        }
			
			        async function updateAnswer() {
			            const questionId = document.getElementById('question-id').value.trim();
			            if (!questionId) { alert('수정할 Question ID를 입력해주세요.'); return; }
			            const body = JSON.stringify({ answerText: document.getElementById('answer-text').value });
			            const result = await apiCall(`/api/notes/questions/${questionId}/answer`, { method: 'PUT', headers: getAuthHeader(), body: body });
			            displayStatus(result);
			        }
			
			        async function getAnswer() {
			            const questionId = document.getElementById('question-id').value.trim();
			            if (!questionId) { alert('조회할 Question ID를 입력해주세요.'); return; }
			            const result = await apiCall(`/api/notes/questions/${questionId}/answer`, { method: 'GET', headers: getAuthHeader() });
			            displayStatus(result);
			            if (result.success && result.data && result.data.answerText) {
			                document.getElementById('answer-text').value = result.data.answerText;
			            } else if (result.success && result.message === '요청이 성공적으로 처리되었습니다 (No Content).') {
			                document.getElementById('answer-text').value = ''; // Clear if no answer
			            }
			        }
			
			        async function deleteAnswer() {
			            const questionId = document.getElementById('question-id').value.trim();
			            if (!questionId) { alert('삭제할 답변의 Question ID를 입력해주세요.'); return; }
			            const result = await apiCall(`/api/notes/questions/${questionId}/answer`, { method: 'DELETE', headers: getAuthHeader() });
			            displayStatus(result);
			        }
			
			        // --- Reminder Functions ---
			        async function getTodayReminder() {
			            const result = await apiCall('/api/notes/reminder/today', { method: 'GET', headers: getAuthHeader() });
			            displayStatus(result);
			        }
			
			        async function dismissReminder() {
			            const result = await apiCall('/api/notes/reminder/dismiss', { method: 'POST', headers: getAuthHeader() });
			            displayStatus(result);
			        }
			    </script>
			</body>
			</html>
			""";
	}
}
