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
			            <h2>노트 생성</h2>
			            <form id="note-form">
			                <div class="form-group">
			                    <label for="note-status">상태</label>
			                    <select id="note-status">
			                        <option value="IN_PROGRESS">IN_PROGRESS (임시저장)</option>
			                        <option value="COMPLETED">COMPLETED (작성완료)</option>
			                    </select>
			                </div>
			                <div class="form-group">
			                    <label for="note-tag">태그</label>
			                    <input type="text" id="note-tag" value="테스트, 샘플">
			                </div>
			                <div class="form-group">
			                    <label for="note-creator-id">제작자(Creator) ID</label>
			                    <input type="number" id="note-creator-id" value="1">
			                </div>
			
			                <div class="note-form-section">
			                    <h4>커버 (Cover)</h4>
			                    <div class="form-group"><input type="text" id="cover-title" placeholder="커버 제목" value="테스트 커버 제목"></div>
			                    <div class="form-group"><input type="text" id="cover-teaser" placeholder="커버 티저" value="테스트 커버 티저입니다."></div>
			                    <div class="form-group"><input type="text" id="creator-name" placeholder="제작자 이름 (선택 사항)" value="김작가"></div>
			                    <div class="form-group"><input type="text" id="creator-job-title" placeholder="제작자 직무 (선택 사항)" value="디지털 아티스트"></div>
			                    <div class="form-group"><label>커버 이미지 파일</label><input type="file" id="cover-image-file" accept="image/*"></div>
			                </div>
			
			                <div class="note-form-section">
			                    <h4>개요 (Overview)</h4>
			                    <div class="form-group"><input type="text" id="overview-title" placeholder="개요 섹션 제목" value="작업 개요"></div>
			                    <div class="form-group"><textarea id="overview-body" placeholder="개요 본문">이 작업은 테스트를 위해 생성되었습니다.</textarea></div>
			                    <div class="form-group"><label>개요 이미지 파일</label><input type="file" id="overview-image-file" accept="image/*"></div>
			                </div>
			
			                <div class="note-form-section">
			                    <h4>회고 (Retrospect)</h4>
			                    <div class="form-group"><input type="text" id="retrospect-title" placeholder="회고 섹션 제목" value="작업 회고"></div>
			                    <div class="form-group"><textarea id="retrospect-body" placeholder="회고 본문">이번 작업을 통해 많은 것을 배웠습니다.</textarea></div>
			                </div>
			
			                <div class="note-form-section">
			                    <h4>제작 과정 (Processes) - 2개 필수</h4>
			                    <p>Process 1</p>
			                    <div class="form-group"><input type="text" id="process1-title" placeholder="과정1 제목" value="1단계: 기획"></div>
			                    <div class="form-group"><textarea id="process1-body" placeholder="과정1 본문">요구사항을 분석했습니다.</textarea></div>
			                    <div class="form-group"><label>과정1 이미지 파일</label><input type="file" id="process1-image-file" accept="image/*"></div>
			                    <p style="margin-top: 10px;">Process 2</p>
			                    <div class="form-group"><input type="text" id="process2-title" placeholder="과정2 제목" value="2단계: 디자인"></div>
			                    <div class="form-group"><textarea id="process2-body" placeholder="과정2 본문">디자인 시안을 제작했습니다.</textarea></div>
			                    <div class="form-group"><label>과정2 이미지 파일</label><input type="file" id="process2-image-file" accept="image/*"></div>
			                </div>
			
			                <div class="note-form-section">
			                    <h4>질문 (Question)</h4>
			                    <div class="form-group"><input type="text" id="question-text" placeholder="질문 텍스트" value="이 작업에서 가장 어려웠던 점은 무엇인가요?"></div>
			                </div>
			
			                <div class="note-form-section">
			                    <h4>외부 링크 (External Link)</h4>
			                    <div class="form-group"><input type="text" id="external-link" placeholder="https://example.com"></div>
			                </div>
			
			                <button type="button" onclick="createNote()">노트 등록</button>
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
			
			        // 개별 이미지 업로드 헬퍼 함수
			        async function uploadImage(fileInputId) {
			            const fileInput = document.getElementById(fileInputId);
			            if (fileInput.files.length === 0) {
			                return ""; // 파일이 없으면 빈 문자열 반환
			            }
			            const file = fileInput.files[0];
			            const formData = new FormData();
			            formData.append('image', file);
			
			            try {
			                const headers = getAuthHeader(true); // multipart/form-data
			                const response = await fetch('/api/admin/images', {
			                    method: 'POST',
			                    headers: headers,
			                    body: formData
			                });
			                const data = await response.json();
			                if (!response.ok) throw (data.error || new Error('Image upload failed'));
			                return data.imageUrl; // 성공 시 URL 반환
			            } catch (error) {
			                console.error(`Error uploading ${fileInputId}:`, error);
			                throw error; // 에러를 상위로 전파하여 Promise.all이 실패하게 만듦
			            }
			        }
			
			        async function createNote() {
			            spinner.style.display = 'block';
			            displayStatus({ message: "이미지 업로드 및 노트 등록을 시작합니다..." });
			
			            try {
			                // 1. 이미지들을 병렬로 업로드
			                const imageUrls = await Promise.all([
			                    uploadImage('cover-image-file'),
			                    uploadImage('overview-image-file'),
			                    uploadImage('process1-image-file'),
			                    uploadImage('process2-image-file')
			                ]);
			
			                const [coverImageUrl, overviewImageUrl, process1ImageUrl, process2ImageUrl] = imageUrls;
			                displayStatus({ message: "이미지 업로드 완료. 노트 등록을 시작합니다.", uploadedUrls: imageUrls });
			
			                // 2. 이미지 URL을 포함하여 노트 생성 payload 구성
			                const body = {
			                    status: document.getElementById('note-status').value,
			                    tagText: document.getElementById('note-tag').value,
			                    creatorId: parseInt(document.getElementById('note-creator-id').value),
			                    cover: {
			                        title: document.getElementById('cover-title').value,
			                        teaser: document.getElementById('cover-teaser').value,
			                        mainImageUrl: coverImageUrl,
			                        creatorName: document.getElementById('creator-name').value,
			                        creatorJobTitle: document.getElementById('creator-job-title').value
			                    },
			                    overview: {
			                        sectionTitle: document.getElementById('overview-title').value,
			                        bodyText: document.getElementById('overview-body').value,
			                        imageUrl: overviewImageUrl
			                    },
			                    retrospect: {
			                        sectionTitle: document.getElementById('retrospect-title').value,
			                        bodyText: document.getElementById('retrospect-body').value
			                    },
			                    processes: [
			                        { position: 1, sectionTitle: document.getElementById('process1-title').value, bodyText: document.getElementById('process1-body').value, imageUrl: process1ImageUrl },
			                        { position: 2, sectionTitle: document.getElementById('process2-title').value, bodyText: document.getElementById('process2-body').value, imageUrl: process2ImageUrl }
			                    ],
			                    question: {
			                        questionText: document.getElementById('question-text').value
			                    },
			                    externalLink: {
			                        sourceUrl: document.getElementById('external-link').value
			                    }
			                };
			
			                // 3. 노트 생성 API 호출
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
			
			        async function getNote() {
			            const noteId = document.getElementById('note-id-get-delete').value.trim();
			            if (!noteId) {
			                alert('조회할 Note ID를 입력해주세요.');
			                return;
			            }
			            const result = await apiCall(`/api/admin/notes/${noteId}`, { method: 'GET', headers: getAuthHeader() });
			            displayStatus(result);
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
			    </script>
			</body>
			</html>
			""";
	}
}
