-- =====================================================
-- Melog 초기 데이터베이스 스키마
-- Version: 1.0
-- Description: 사용자, 감정 기록, 감정 점수, 키워드 테이블 생성
-- =====================================================

-- 감정 타입 enum 생성
CREATE TYPE emotion_type AS ENUM (
    'JOY',           -- 기쁨
    'EXCITEMENT',    -- 설렘
    'CALMNESS',      -- 평온
    'ANGER',         -- 분노
    'SADNESS',       -- 슬픔
    'GUIDANCE'       -- 지침
);

-- 사용자 테이블
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    nickname VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 사용자 테이블 인덱스
CREATE INDEX idx_users_nickname ON users (nickname);
CREATE INDEX idx_users_created_at ON users (created_at);

-- 감정 기록 테이블
CREATE TABLE emotion_record (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    text TEXT,
    summary TEXT,
    date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_emotion_record_user 
        FOREIGN KEY (user_id) REFERENCES users (id) 
        ON DELETE CASCADE
);

-- 감정 기록 테이블 인덱스
CREATE INDEX idx_emotion_record_user_id ON emotion_record (user_id);
CREATE INDEX idx_emotion_record_date ON emotion_record (date);
CREATE INDEX idx_emotion_record_created_at ON emotion_record (created_at);

-- 감정 점수 테이블
CREATE TABLE emotion_score (
    id BIGSERIAL PRIMARY KEY,
    record_id BIGINT NOT NULL,
    emotion_type emotion_type NOT NULL,
    percentage INTEGER NOT NULL CHECK (percentage >= 0 AND percentage <= 100),
    step INTEGER NOT NULL CHECK (step >= 1 AND step <= 5),
    
    CONSTRAINT fk_emotion_score_record 
        FOREIGN KEY (record_id) REFERENCES emotion_record (id) 
        ON DELETE CASCADE
);

-- 감정 점수 테이블 인덱스
CREATE INDEX idx_emotion_score_record_id ON emotion_score (record_id);
CREATE INDEX idx_emotion_score_emotion_type ON emotion_score (emotion_type);

-- 사용자 선택 감정 테이블
CREATE TABLE user_selected_emotion (
    id BIGSERIAL PRIMARY KEY,
    record_id BIGINT NOT NULL UNIQUE,
    emotion_type emotion_type NOT NULL,
    percentage INTEGER NOT NULL CHECK (percentage >= 0 AND percentage <= 100),
    step INTEGER NOT NULL CHECK (step >= 1 AND step <= 5),
    
    CONSTRAINT fk_user_selected_emotion_record 
        FOREIGN KEY (record_id) REFERENCES emotion_record (id) 
        ON DELETE CASCADE
);

-- 사용자 선택 감정 테이블 인덱스
CREATE INDEX idx_user_selected_emotion_record_id ON user_selected_emotion (record_id);
CREATE INDEX idx_user_selected_emotion_emotion_type ON user_selected_emotion (emotion_type);

-- 감정 키워드 테이블
CREATE TABLE emotion_keyword (
    id BIGSERIAL PRIMARY KEY,
    record_id BIGINT NOT NULL,
    keyword VARCHAR(255) NOT NULL,
    weight INTEGER NOT NULL CHECK (weight >= 1 AND weight <= 100),
    
    CONSTRAINT fk_emotion_keyword_record 
        FOREIGN KEY (record_id) REFERENCES emotion_record (id) 
        ON DELETE CASCADE
);

-- 감정 키워드 테이블 인덱스
CREATE INDEX idx_emotion_keyword_record_id ON emotion_keyword (record_id);
CREATE INDEX idx_emotion_keyword_keyword ON emotion_keyword (keyword);
CREATE INDEX idx_emotion_keyword_weight ON emotion_keyword (weight);

-- 샘플 테이블 (기존 코드와의 호환성)
CREATE TABLE sample (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255),
    content TEXT
);

-- 테이블 생성 완료 로그
COMMENT ON TABLE users IS '사용자 정보 테이블';
COMMENT ON TABLE emotion_record IS '감정 기록 테이블';
COMMENT ON TABLE emotion_score IS '감정 분석 점수 테이블';
COMMENT ON TABLE user_selected_emotion IS '사용자가 선택한 감정 테이블';
COMMENT ON TABLE emotion_keyword IS '감정 키워드 테이블';
COMMENT ON TABLE sample IS '샘플 테이블';

-- 시퀀스 정보
COMMENT ON SEQUENCE users_id_seq IS '사용자 ID 시퀀스';
COMMENT ON SEQUENCE emotion_record_id_seq IS '감정 기록 ID 시퀀스';
COMMENT ON SEQUENCE emotion_score_id_seq IS '감정 점수 ID 시퀀스';
COMMENT ON SEQUENCE user_selected_emotion_id_seq IS '사용자 선택 감정 ID 시퀀스';
COMMENT ON SEQUENCE emotion_keyword_id_seq IS '감정 키워드 ID 시퀀스';
COMMENT ON SEQUENCE sample_id_seq IS '샘플 ID 시퀀스';
