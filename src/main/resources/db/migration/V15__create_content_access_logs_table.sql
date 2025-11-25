-- 유료 콘텐츠 접근 기록을 저장하는 테이블
CREATE TABLE content_access_logs
(
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT    NOT NULL,
    note_id     BIGINT    NOT NULL,
    accessed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_content_access_logs_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_content_access_logs_note FOREIGN KEY (note_id) REFERENCES notes_head (id)
);

-- 인덱스 추가: 특정 사용자가 특정 노트에 접근했는지 빠르게 확인하기 위함
CREATE INDEX idx_content_access_logs_user_note ON content_access_logs (user_id, note_id);

