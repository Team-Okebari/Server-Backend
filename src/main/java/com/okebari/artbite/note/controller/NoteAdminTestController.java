package com.okebari.artbite.note.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class NoteAdminTestController {

	@GetMapping("/note-admin-test-page")
	@ResponseBody
	public String getNoteAdminTestPage() {
		return """
			<!DOCTYPE html>
			<html lang="ko">
			<head>
			    <meta charset="UTF-8">
			    <title>Note 관리자 기능 테스트</title>
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
			        .form-group input, .form-group textarea, .form-group select { width: 100%; padding: 10px; border: 1px solid #ced4da; border-radius: 4px; box-sizing: border-box; font-size: 14px; }
			        textarea { min-height: 80px; resize: vertical; }
			        button, a.button { display: inline-block; padding: 10px 15px; font-size: 14px; font-weight: bold; color: #fff; background-color: #007bff; border: none; border-radius: 5px; cursor: pointer; transition: background-color 0.2s; text-decoration: none; text-align: center; margin-right: 10px; margin-top: 10px; }
			        button:hover, a.button:hover { background-color: #0056b3; }
			        button.delete { background-color: #dc3545; }
			        button.delete:hover { background-color: #c82333; }
			        button.update { background-color: #28a745; }
			        button.update:hover { background-color: #218838; }
			        #api-status { margin-top: 20px; padding: 15px; background-color: #e9ecef; border-radius: 5px; word-wrap: break-word; white-space: pre-wrap; font-family: monospace; }
			        .note-form-section { border: 1px solid #e0e0e0; padding: 15px; margin-top: 15px; border-radius: 5px; }
			        .flex-nav { display: flex; gap: 10px; margin-bottom: 20px; }
			        .spinner { display: none; margin: 20px auto; width: 40px; height: 40px; border: 4px solid #f3f3f3; border-top: 4px solid #3498db; border-radius: 50%; animation: spin 1s linear infinite; }
			        @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
			    </style>
			</head>
			<body>
			    <div class="page-container">
			        <div class="flex-nav">
			            <a href="/login-test-page" class="button" style="background-color: #6c757d;">인증 테스트</a>
			            <a href="/note-user-test-page" class="button" style="background-color: #17a2b8;">Note 사용자 기능 테스트</a>
			        </div>
			        <h1>Note 관리자 기능 테스트</h1>
			
			        <div class="container">
			            <h2>공통 설정</h2>
			            <div class="form-group">
			                <label for="auth-token">ADMIN Access Token (Bearer)</label>
			                <textarea id="auth-token" rows="3" placeholder="ADMIN 권한의 Access Token을 여기에 붙여넣으세요."></textarea>
			            </div>
			        </div>
			
			        <div class="container">
			            <h2>노트 생성/수정</h2>
			            <form id="note-form">
			                <div class="form-group">
			                    <label for="note-id-update">수정할 Note ID (수정 시에만 입력)</label>
			                    <input type="text" id="note-id-update" placeholder="수정할 노트의 ID">
			                    <button type="button" onclick="loadNoteForUpdate()">불러오기</button>
			                </div>
			                <hr>
			                <div class="form-group">
			                    <label for="note-status">상태 (*필수)</label>
			                    <select id="note-status">
			                        <option value="IN_PROGRESS">IN_PROGRESS (임시저장)</option>
			                        <option value="COMPLETED">COMPLETED (작성완료)</option>
			                        <option value="ARCHIVED">ARCHIVED (아카이브)</option>
			                    </select>
			                </div>
			                <div class="form-group">
			                    <label for="note-tag">태그 (선택 사항, 최대 60자)</label>
			                    <input type="text" id="note-tag" maxlength="60" onkeyup="updateCharCount('note-tag', 'note-tag-count')">
			                    <span id="note-tag-count">0/60</span>
			                </div>
			                <div class="form-group">
			                    <label for="note-creator-id">제작자(Creator) ID (*필수)</label>
			                    <input type="number" id="note-creator-id" onblur="fetchCreatorInfo()">
			                </div>
			
			                <div class="note-form-section">
			                    <h4>커버 (Cover)</h4>
			                    <div class="form-group">
			                        <label for="cover-title">커버 제목 (*필수, 최대 50자)</label>
			                        <input type="text" id="cover-title" maxlength="50" onkeyup="updateCharCount('cover-title', 'cover-title-count')">
			                        <span id="cover-title-count">0/50</span>
			                    </div>
			                    <div class="form-group">
			                        <label for="cover-teaser">커버 티저 (*필수, 최대 100자)</label>
			                        <input type="text" id="cover-teaser" maxlength="100" onkeyup="updateCharCount('cover-teaser', 'cover-teaser-count')">
			                        <span id="cover-teaser-count">0/100</span>
			                    </div>
			                    <div class="form-group">
			                        <label for="cover-category">카테고리 (선택 사항, 기본값: NONE)</label>
			                        <select id="cover-category">
			                            <option value="NONE">카테고리 없음</option>
			                            <option value="MURAL">벽화</option>
			                            <option value="EMOTICON">이모티콘</option>
			                            <option value="GRAPHIC">그래픽</option>
			                            <option value="PRODUCT">제품</option>
			                            <option value="FASHION">패션</option>
			                            <option value="THREE_D">3D</option>
			                            <option value="BRANDING">브랜딩</option>
			                            <option value="ILLUSTRATION">일러스트</option>
			                            <option value="MEDIA_ART">미디어아트</option>
			                            <option value="FURNITURE">가구</option>
			                            <option value="THEATER_SIGN">극장 손간판</option>
			                            <option value="LANDSCAPE">조경</option>
			                            <option value="ALBUM_ARTWORK">음반 아트워크</option>
			                            <option value="VISUAL_DIRECTING">비주얼 디렉팅</option>
			                        </select>
			                    </div>
			                    <div class="form-group"><label for="creator-name">제작자 이름</label><input type="text" id="creator-name" readonly></div>
			                    <div class="form-group"><label for="creator-job-title">제작자 직무</label><input type="text" id="creator-job-title" readonly></div>
			                    <div class="form-group"><label>커버 이미지 파일 (수정 시에는 비워두면 기존 이미지 유지)</label><input type="file" id="cover-image-file" accept="image/*"></div>
			                </div>
			
			                <div class="note-form-section">
			                    <h4>개요 (Overview)</h4>
			                    <div class="form-group">
			                        <label for="overview-title">개요 섹션 제목 (*필수, 최대 30자)</label>
			                        <input type="text" id="overview-title" maxlength="30" onkeyup="updateCharCount('overview-title', 'overview-title-count')">
			                        <span id="overview-title-count">0/30</span>
			                    </div>
			                    <div class="form-group">
			                        <label for="overview-body">개요 본문 (*필수, 최대 200자)</label>
			                        <textarea id="overview-body" maxlength="200" onkeyup="updateCharCount('overview-body', 'overview-body-count')"></textarea>
			                        <span id="overview-body-count">0/200</span>
			                    </div>
			                    <div class="form-group"><label>개요 이미지 파일 (수정 시에는 비워두면 기존 이미지 유지)</label><input type="file" id="overview-image-file" accept="image/*"></div>
			                </div>
			
			                <div class="note-form-section">
			                    <h4>제작 과정 (Processes) - 2개 필수</h4>
			                    <p>Process 1</p>
			                    <div class="form-group">
			                        <label for="process1-title">과정1 제목 (*필수, 최대 30자)</label>
			                        <input type="text" id="process1-title" maxlength="30" onkeyup="updateCharCount('process1-title', 'process1-title-count')">
			                        <span id="process1-title-count">0/30</span>
			                    </div>
			                    <div class="form-group">
			                        <label for="process1-body">과정1 본문 (*필수, 최대 500자)</label>
			                        <textarea id="process1-body" maxlength="500" onkeyup="updateCharCount('process1-body', 'process1-body-count')"></textarea>
			                        <span id="process1-body-count">0/500</span>
			                    </div>
			                    <div class="form-group"><label>과정1 이미지 파일 (수정 시에는 비워두면 기존 이미지 유지)</label><input type="file" id="process1-image-file" accept="image/*"></div>
			                    <p style="margin-top: 10px;">Process 2</p>
			                    <div class="form-group">
			                        <label for="process2-title">과정2 제목 (*필수, 최대 30자)</label>
			                        <input type="text" id="process2-title" maxlength="30" onkeyup="updateCharCount('process2-title', 'process2-title-count')">
			                        <span id="process2-title-count">0/30</span>
			                    </div>
			                    <div class="form-group">
			                        <label for="process2-body">과정2 본문 (*필수, 최대 500자)</label>
			                        <textarea id="process2-body" maxlength="500" onkeyup="updateCharCount('process2-body', 'process2-body-count')"></textarea>
			                        <span id="process2-body-count">0/500</span>
			                    </div>
			                    <div class="form-group"><label>과정2 이미지 파일 (수정 시에는 비워두면 기존 이미지 유지)</label><input type="file" id="process2-image-file" accept="image/*"></div>
			                </div>
			
			                <div class="note-form-section">
			                    <h4>회고 (Retrospect)</h4>
			                    <div class="form-group">
			                        <label for="retrospect-title">회고 섹션 제목 (*필수, 최대 30자)</label>
			                        <input type="text" id="retrospect-title" maxlength="30" onkeyup="updateCharCount('retrospect-title', 'retrospect-title-count')">
			                        <span id="retrospect-title-count">0/30</span>
			                    </div>
			                    <div class="form-group">
			                        <label for="retrospect-body">회고 본문 (*필수, 최대 200자)</label>
			                        <textarea id="retrospect-body" maxlength="200" onkeyup="updateCharCount('retrospect-body', 'retrospect-body-count')"></textarea>
			                        <span id="retrospect-body-count">0/200</span>
			                    </div>
			                </div>
			
			                <div class="note-form-section">
			                    <h4>질문 (Question)</h4>
			                    <div class="form-group">
			                        <label for="question-text">질문 텍스트 (선택 사항, 최대 100자)</label>
			                        <input type="text" id="question-text" placeholder="이 작업에서 가장 어려웠던 점은 무엇인가요?" maxlength="100" onkeyup="updateCharCount('question-text', 'question-text-count')">
			                        <span id="question-text-count">0/100</span>
			                    </div>
			                </div>
			
			                <div class="note-form-section">
			                    <h4>외부 링크 (External Link)</h4>
			                    <div class="form-group">
			                        <label for="external-link">외부 링크 URL (선택 사항, 최대 500자)</label>
			                        <input type="text" id="external-link" placeholder="https://example.com" maxlength="500" onkeyup="updateCharCount('external-link', 'external-link-count')">
			                        <span id="external-link-count">0/500</span>
			                    </div>
			                </div>
			
			                <button type="button" onclick="createNote()">노트 등록</button>
			                <button type="button" class="update" onclick="updateNote()">노트 수정</button>
			                <div class="spinner" id="loading-spinner"></div>
			            </form>
			        </div>
			
			        <div class="container">
			            <h2>노트 조회/삭제</h2>
			            <div class="form-group">
			                <label for="note-id-get-delete">조회/삭제할 Note ID</label>
			                <input type="text" id="note-id-get-delete" placeholder="조회/삭제할 노트의 ID">
			            </div>
			            <button onclick="getNote()">노트 상세 조회</button>
			            <button class="delete" onclick="deleteNote()">노트 삭제</button>
			            <hr>
			            <button onclick="listNotes()">노트 목록 조회 (페이징)</button>
			        </div>
			
			        <div class="container">
			            <h2>API 호출 결과</h2>
			            <pre id="api-status">API 호출 결과가 여기에 표시됩니다.</pre>
			        </div>
			    </div>
			
			    <script>
			        const statusDiv = document.getElementById('api-status');
			        const spinner = document.getElementById('loading-spinner');
			        // 현재 로드된 노트의 이미지 URL을 저장하기 위한 전역 변수
			        let currentImageUrls = {};
			
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
			
			        async function apiCall(endpoint, options) {
			            try {
			                const response = await fetch(endpoint, options);
			                if (response.status === 204 || response.headers.get("content-length") === "0") {
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
			
			        // 개별 이미지 업로드 헬퍼 함수
			        async function uploadImage(fileInputId) {
			            const fileInput = document.getElementById(fileInputId);
			            if (fileInput.files.length === 0) {
			                // 수정 시 파일이 없으면 기존 이미지 URL 사용
			                return currentImageUrls[fileInputId] || "";
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
			                if (!response.ok) throw (data.error || new Error('Image upload failed'));
			                return data.imageUrl;
			            } catch (error) {
			                console.error(`Error uploading ${fileInputId}:`, error);
			                throw error;
			            }
			        }
			
			        async function fetchCreatorInfo() {
			            const creatorId = document.getElementById('note-creator-id').value.trim();
			            const creatorNameInput = document.getElementById('creator-name');
			            const creatorJobTitleInput = document.getElementById('creator-job-title');
			
			            if (!creatorId) {
			                creatorNameInput.value = '';
			                creatorJobTitleInput.value = '';
			                return;
			            }
			
			            try {
			                const result = await apiCall(`/api/admin/creators/${creatorId}`, { method: 'GET', headers: getAuthHeader() });
			                if (result.success && result.data) {
			                    creatorNameInput.value = result.data.name || '';
			                    creatorJobTitleInput.value = result.data.jobTitle || '';
			                } else {
			                    alert('제작자 정보를 찾을 수 없습니다: ' + (result.error ? result.error.message : '알 수 없는 오류'));
			                    creatorNameInput.value = '';
			                    creatorJobTitleInput.value = '';
			                }
			            } catch (error) {
			                console.error('Error fetching creator info:', error);
			                alert('제작자 정보 조회 중 오류 발생: ' + (error.message || '알 수 없는 오류'));
			                creatorNameInput.value = '';
			                creatorJobTitleInput.value = '';
			            }
			        }
			
			        function getNotePayloadFromForm() {
			            return {
			                status: document.getElementById('note-status').value,
			                tagText: document.getElementById('note-tag').value,
			                creatorId: parseInt(document.getElementById('note-creator-id').value),
			                cover: {
			                    title: document.getElementById('cover-title').value,
			                    teaser: document.getElementById('cover-teaser').value,
			                    creatorName: document.getElementById('creator-name').value,
			                    creatorJobTitle: document.getElementById('creator-job-title').value,
			                    category: document.getElementById('cover-category').value
			                },
			                overview: {
			                    sectionTitle: document.getElementById('overview-title').value,
			                    bodyText: document.getElementById('overview-body').value,
			                },
			                retrospect: {
			                    sectionTitle: document.getElementById('retrospect-title').value,
			                    bodyText: document.getElementById('retrospect-body').value
			                },
			                processes: [
			                    { position: 1, sectionTitle: document.getElementById('process1-title').value, bodyText: document.getElementById('process1-body').value },
			                    { position: 2, sectionTitle: document.getElementById('process2-title').value, bodyText: document.getElementById('process2-body').value }
			                ],
			                question: {
			                    questionText: document.getElementById('question-text').value
			                },
			                externalLink: {
			                    sourceUrl: document.getElementById('external-link').value
			                }
			            };
			        }
			
			        async function createNote() {
			            spinner.style.display = 'block';
			            displayStatus({ message: "이미지 업로드 및 노트 등록을 시작합니다..." });
			            currentImageUrls = {}; // 생성 시에는 기존 이미지 URL 초기화
			
			            try {
			                const imageUrls = await Promise.all([
			                    uploadImage('cover-image-file'),
			                    uploadImage('overview-image-file'),
			                    uploadImage('process1-image-file'),
			                    uploadImage('process2-image-file')
			                ]);
			
			                const [coverImageUrl, overviewImageUrl, process1ImageUrl, process2ImageUrl] = imageUrls;
			                displayStatus({ message: "이미지 업로드 완료. 노트 등록을 시작합니다.", uploadedUrls: imageUrls });
			
			                const body = getNotePayloadFromForm();
			                body.cover.mainImageUrl = coverImageUrl;
			                body.overview.imageUrl = overviewImageUrl;
			                body.processes[0].imageUrl = process1ImageUrl;
			                body.processes[1].imageUrl = process2ImageUrl;
			
			                const result = await apiCall('/api/admin/notes', {
			                    method: 'POST',
			                    headers: getAuthHeader(),
			                    body: JSON.stringify(body)
			                });
			
			                displayStatus(result);
			                if(result.success) {
			                    alert('노트가 성공적으로 등록되었습니다! ID: ' + result.data);
			                } else {
			                    alert('노트 등록에 실패했습니다.');
			                }
			
			            } catch (error) {
			                displayStatus({ success: false, message: "이미지 업로드 또는 노트 등록 중 오류 발생", error: error });
			                alert('작업 중 오류가 발생했습니다. 콘솔을 확인하세요.');
			            } finally {
			                spinner.style.display = 'none';
			            }
			        }
			
			        async function updateNote() {
			            const noteId = document.getElementById('note-id-update').value.trim();
			            if (!noteId) {
			                alert('수정할 Note ID를 입력해주세요.');
			                return;
			            }
			
			            spinner.style.display = 'block';
			            displayStatus({ message: "이미지 업로드 및 노트 수정을 시작합니다..." });
			
			            try {
			                const imageUrls = await Promise.all([
			                    uploadImage('cover-image-file'),
			                    uploadImage('overview-image-file'),
			                    uploadImage('process1-image-file'),
			                    uploadImage('process2-image-file')
			                ]);
			
			                const [coverImageUrl, overviewImageUrl, process1ImageUrl, process2ImageUrl] = imageUrls;
			                displayStatus({ message: "이미지 업로드 완료. 노트 수정을 시작합니다.", uploadedUrls: imageUrls });
			
			                const body = getNotePayloadFromForm();
			                body.cover.mainImageUrl = coverImageUrl;
			                body.overview.imageUrl = overviewImageUrl;
			                body.processes[0].imageUrl = process1ImageUrl;
			                body.processes[1].imageUrl = process2ImageUrl;
			
			                const result = await apiCall(`/api/admin/notes/${noteId}`, {
			                    method: 'PUT',
			                    headers: getAuthHeader(),
			                    body: JSON.stringify(body)
			                });
			
			                displayStatus(result);
			                if(result.success) {
			                    alert('노트가 성공적으로 수정되었습니다!');
			                } else {
			                    alert('노트 수정에 실패했습니다.');
			                }
			
			            } catch (error) {
			                displayStatus({ success: false, message: "이미지 업로드 또는 노트 수정 중 오류 발생", error: error });
			                alert('작업 중 오류가 발생했습니다. 콘솔을 확인하세요.');
			            } finally {
			                spinner.style.display = 'none';
			            }
			        }
			
			        async function getNote() {
			            const noteId = document.getElementById('note-id-get-delete').value.trim();
			            if (!noteId) {
			                alert('조회할 Note ID를 입력해주세요.');
			                return;
			            }
			            const result = await apiCall(`/api/admin/notes/${noteId}`, { method: 'GET', headers: getAuthHeader() });
			            displayStatus(result);
			        }
			
			        async function loadNoteForUpdate() {
			            const noteId = document.getElementById('note-id-update').value.trim();
			            if (!noteId) {
			                alert('불러올 Note ID를 입력해주세요.');
			                return;
			            }
			            const result = await apiCall(`/api/admin/notes/${noteId}`, { method: 'GET', headers: getAuthHeader() });
			            displayStatus(result);
			
			            if (result.success && result.data) {
			                const note = result.data;
			                document.getElementById('note-status').value = note.status || 'IN_PROGRESS';
			                document.getElementById('note-tag').value = note.tagText || '';
			                document.getElementById('note-creator-id').value = note.creatorId || '';
			                if (note.creatorId) {
			                    fetchCreatorInfo(); // 제작자 정보 자동 로드
			                }
			
			                document.getElementById('note-tag').value = note.tagText || '';
			                updateCharCount('note-tag', 'note-tag-count');
			
			                if (note.cover) {
			                    document.getElementById('cover-title').value = note.cover.title || '';
			                    document.getElementById('cover-teaser').value = note.cover.teaser || '';
			                    document.getElementById('cover-category').value = note.cover.category ? note.cover.category.type : 'NONE';
			                    currentImageUrls['cover-image-file'] = note.cover.mainImageUrl || '';
			                    updateCharCount('cover-title', 'cover-title-count');
			                    updateCharCount('cover-teaser', 'cover-teaser-count');
			                }
			
			                if (note.overview) {
			                    document.getElementById('overview-title').value = note.overview.sectionTitle || '';
			                    document.getElementById('overview-body').value = note.overview.bodyText || '';
			                    currentImageUrls['overview-image-file'] = note.overview.imageUrl || '';
			                    updateCharCount('overview-title', 'overview-title-count');
			                    updateCharCount('overview-body', 'overview-body-count');
			                }
			
			                if (note.retrospect) {
			                    document.getElementById('retrospect-title').value = note.retrospect.sectionTitle || '';
			                    document.getElementById('retrospect-body').value = note.retrospect.bodyText || '';
			                    updateCharCount('retrospect-title', 'retrospect-title-count');
			                    updateCharCount('retrospect-body', 'retrospect-body-count');
			                }
			
			                // Processes
			                if (note.processes && note.processes.length > 0) {
			                    note.processes.forEach((p, i) => {
			                        if (i < 2) { // 최대 2개까지만 폼에 반영
			                            document.getElementById(`process${i+1}-title`).value = p.sectionTitle || '';
			                            document.getElementById(`process${i+1}-body`).value = p.bodyText || '';
			                            currentImageUrls[`process${i+1}-image-file`] = p.imageUrl || '';
			                            updateCharCount(`process${i+1}-title`, `process${i+1}-title-count`);
			                            updateCharCount(`process${i+1}-body`, `process${i+1}-body-count`);
			                        }
			                    });
			                }
			
			                // Question
			                document.getElementById('question-text').value = note.question ? note.question.questionText : '';
			                updateCharCount('question-text', 'question-text-count');
			
			                // External Link
			                document.getElementById('external-link').value = note.externalLink ? note.externalLink.sourceUrl : '';
			                updateCharCount('external-link', 'external-link-count');
			
			                alert('노트 데이터를 폼에 로드했습니다.');
			            } else {
			                alert('노트 데이터를 불러오는 데 실패했습니다.');
			            }
			        }
			
			        async function deleteNote() {
			            const noteId = document.getElementById('note-id-get-delete').value.trim();
			            if (!noteId) {
			                alert('삭제할 Note ID를 입력해주세요.');
			                return;
			            }
			            const result = await apiCall(`/api/admin/notes/${noteId}`, { method: 'DELETE', headers: getAuthHeader() });
			            displayStatus(result);
			        }
			
			        async function listNotes() {
			            const result = await apiCall('/api/admin/notes?page=0&size=5', { method: 'GET', headers: getAuthHeader() });
			            displayStatus(result);
			        }
			
			        function updateCharCount(inputId, countSpanId) {
			            const inputElement = document.getElementById(inputId);
			            const countSpan = document.getElementById(countSpanId);
			            if (inputElement && countSpan) {
			                const currentLength = inputElement.value.length;
			                const maxLength = inputElement.maxLength;
			                countSpan.textContent = `${currentLength}/${maxLength}`;
			            }
			        }
			
			        // Initialize counters on page load
			        document.addEventListener('DOMContentLoaded', () => {
			            updateCharCount('note-tag', 'note-tag-count');
			            updateCharCount('cover-title', 'cover-title-count');
			            updateCharCount('cover-teaser', 'cover-teaser-count');
			            updateCharCount('overview-title', 'overview-title-count');
			            updateCharCount('overview-body', 'overview-body-count');
			            updateCharCount('process1-title', 'process1-title-count');
			            updateCharCount('process1-body', 'process1-body-count');
			            updateCharCount('process2-title', 'process2-title-count');
			            updateCharCount('process2-body', 'process2-body-count');
			            updateCharCount('retrospect-title', 'retrospect-title-count');
			            updateCharCount('retrospect-body', 'retrospect-body-count');
			            updateCharCount('question-text', 'question-text-count');
			            updateCharCount('external-link', 'external-link-count');
			        });
			    </script>
			</body>
			</html>
			""";
	}
}