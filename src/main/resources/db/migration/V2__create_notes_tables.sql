-- 노트 도메인 테이블 생성
CREATE TABLE note_creator (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(60) NOT NULL,
    bio VARCHAR(200),
    instagram_url VARCHAR(255),
    youtube_url VARCHAR(255),
    behance_url VARCHAR(255),
    x_url VARCHAR(255),
    blog_url VARCHAR(255),
    news_url VARCHAR(255)
);

CREATE TABLE notes_head (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    tag_text VARCHAR(60),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP WITH TIME ZONE,
    archived_at TIMESTAMP WITH TIME ZONE,
    source_url VARCHAR(255),
    creator_id BIGINT,
    CONSTRAINT fk_notes_head_creator FOREIGN KEY (creator_id) REFERENCES note_creator (id)
);

CREATE TABLE note_cover (
    note_id BIGINT PRIMARY KEY,
    title VARCHAR(30) NOT NULL,
    teaser VARCHAR(100) NOT NULL,
    main_image_url VARCHAR(255) NOT NULL,
    CONSTRAINT fk_note_cover_note FOREIGN KEY (note_id) REFERENCES notes_head (id) ON DELETE CASCADE
);

CREATE TABLE note_overview (
    note_id BIGINT PRIMARY KEY,
    section_title VARCHAR(30) NOT NULL,
    body_text VARCHAR(200) NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    CONSTRAINT fk_note_overview_note FOREIGN KEY (note_id) REFERENCES notes_head (id) ON DELETE CASCADE
);

CREATE TABLE note_retrospect (
    note_id BIGINT PRIMARY KEY,
    section_title VARCHAR(30) NOT NULL,
    body_text VARCHAR(200) NOT NULL,
    CONSTRAINT fk_note_retrospect_note FOREIGN KEY (note_id) REFERENCES notes_head (id) ON DELETE CASCADE
);

CREATE TABLE note_process (
    note_id BIGINT NOT NULL,
    position SMALLINT NOT NULL,
    section_title VARCHAR(30) NOT NULL,
    body_text VARCHAR(500) NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    PRIMARY KEY (note_id, position),
    CONSTRAINT fk_note_process_note FOREIGN KEY (note_id) REFERENCES notes_head (id) ON DELETE CASCADE,
    CONSTRAINT ck_note_process_position CHECK (position BETWEEN 1 AND 2)
);

CREATE TABLE note_question (
    id BIGSERIAL PRIMARY KEY,
    note_id BIGINT NOT NULL UNIQUE,
    question_txt VARCHAR(100) NOT NULL,
    CONSTRAINT fk_note_question_note FOREIGN KEY (note_id) REFERENCES notes_head (id) ON DELETE CASCADE
);

CREATE TABLE note_answer (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT,
    answer_txt VARCHAR(200),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_note_answer_question FOREIGN KEY (question_id) REFERENCES note_question (id) ON DELETE CASCADE,
    CONSTRAINT fk_note_answer_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE note_bookmark (
    id BIGSERIAL PRIMARY KEY,
    note_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_note_bookmark_note FOREIGN KEY (note_id) REFERENCES notes_head (id) ON DELETE CASCADE,
    CONSTRAINT fk_note_bookmark_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_note_bookmark UNIQUE (note_id, user_id)
);

-- 조회 성능 향상을 위한 인덱스
CREATE INDEX idx_note_bookmark_user ON note_bookmark (user_id);
CREATE INDEX idx_note_bookmark_note ON note_bookmark (note_id);

CREATE INDEX idx_note_cover_title_lower ON note_cover (lower(title));
CREATE INDEX idx_notes_head_tag_lower ON notes_head (lower(tag_text));
CREATE INDEX idx_note_creator_name_lower ON note_creator (lower(name));
