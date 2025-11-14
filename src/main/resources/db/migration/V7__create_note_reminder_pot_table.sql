-- Daily reminder snapshot storage
CREATE TABLE note_reminder_pot (
    id BIGSERIAL PRIMARY KEY,                         -- surrogate PK
    user_id BIGINT NOT NULL,                          -- 대상 사용자
    note_id BIGINT NOT NULL,                          -- 리마인드용 노트 PK
    reminder_date DATE NOT NULL,                      -- 이 레코드가 노출될 날짜(YYYY-MM-DD)
    source_type VARCHAR(30) NOT NULL,                 -- BOOKMARK or ANSWER

    -- payload snapshot (배너에서는 고정 멘트 + 노트 제목/썸네일만 사용)
    payload_note_id BIGINT,                           -- 캐시/Redis에서 꺼낼 때 사용되는 note_id
    payload_title VARCHAR(60),                        -- 배너에 노출할 노트 제목
    payload_main_image_url VARCHAR(255),              -- 썸네일(대표 이미지)

    first_visit_at TIMESTAMP WITH TIME ZONE,          -- 당일 첫 접속 기록
    banner_seen_at TIMESTAMP WITH TIME ZONE,          -- 배너가 실제 노출된 시각(두 번째 접속)
    modal_closed_at TIMESTAMP WITH TIME ZONE,         -- X 버튼 모달에서 “취소”를 선택한 시각
    dismissed BOOLEAN DEFAULT FALSE,                  -- “오늘은 그만 보기” 여부
    dismissed_at TIMESTAMP WITH TIME ZONE,            -- dismiss 처리 시각

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_note_reminder_user_date UNIQUE (user_id, reminder_date),
    CONSTRAINT fk_note_reminder_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_note_reminder_note FOREIGN KEY (note_id) REFERENCES notes_head (id) ON DELETE CASCADE
);

CREATE INDEX idx_note_reminder_date ON note_reminder_pot (reminder_date); -- 자정 배치/조회 최적화
CREATE INDEX idx_note_reminder_user ON note_reminder_pot (user_id);       -- 사용자별 상태 갱신
