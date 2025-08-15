-- V2 마이그레이션 후 실행되는 초기 데이터
-- 감정별 단계별 코멘트 데이터

-- 기쁨 (JOY) 감정 코멘트
INSERT INTO melog.emotion_comments (emotion_type, step, comment, is_active, created_at, updated_at) VALUES
('JOY', 1, '조금 기분이 좋은 하루네요. 더 밝은 에너지를 느껴보세요.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('JOY', 2, '기분이 좋은 상태입니다. 이 기분을 유지해보세요.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('JOY', 3, '매우 기쁜 하루를 보내고 계시네요!', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('JOY', 4, '완벽한 기쁨의 순간입니다!', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('JOY', 5, '최고의 행복을 경험하고 계시네요!', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 설렘 (EXCITEMENT) 감정 코멘트
INSERT INTO melog.emotion_comments (emotion_type, step, comment, is_active, created_at, updated_at) VALUES
('EXCITEMENT', 1, '조금 설레는 기분이군요. 기대감을 느껴보세요.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('EXCITEMENT', 2, '설렘을 느끼고 계시네요. 긍정적인 에너지를 유지하세요.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('EXCITEMENT', 3, '매우 설레는 순간입니다!', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('EXCITEMENT', 4, '완벽한 설렘의 순간이네요!', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('EXCITEMENT', 5, '최고의 설렘을 경험하고 계시네요!', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 평온 (CALMNESS) 감정 코멘트
INSERT INTO melog.emotion_comments (emotion_type, step, comment, is_active, created_at, updated_at) VALUES
('CALMNESS', 1, '조금 평온한 기분이군요. 마음의 여유를 느껴보세요.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CALMNESS', 2, '평온한 상태입니다. 이 마음의 평화를 유지하세요.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CALMNESS', 3, '매우 평온한 하루를 보내고 계시네요!', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CALMNESS', 4, '완벽한 평온의 순간입니다!', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CALMNESS', 5, '최고의 평온을 경험하고 계시네요!', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 분노 (ANGER) 감정 코멘트
INSERT INTO melog.emotion_comments (emotion_type, step, comment, is_active, created_at, updated_at) VALUES
('ANGER', 1, '조금 화가 난 기분이군요. 심호흡을 해보세요.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ANGER', 2, '화가 난 상태입니다. 마음을 진정시켜보세요.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ANGER', 3, '매우 화가 난 상태네요. 잠시 휴식을 취해보세요.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ANGER', 4, '심한 분노를 느끼고 계시네요. 전문가와 상담을 고려해보세요.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ANGER', 5, '극도의 분노 상태입니다. 즉시 전문가의 도움을 받으세요.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 슬픔 (SADNESS) 감정 코멘트
INSERT INTO melog.emotion_comments (emotion_type, step, comment, is_active, created_at, updated_at) VALUES
('SADNESS', 1, '조금 슬픈 기분이군요. 마음을 다독여주세요.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SADNESS', 2, '슬픈 상태입니다. 좋아하는 것을 해보세요.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SADNESS', 3, '매우 슬픈 상태네요. 주변 사람들과 이야기해보세요.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SADNESS', 4, '심한 슬픔을 느끼고 계시네요. 전문가와 상담을 고려해보세요.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SADNESS', 5, '극도의 슬픔 상태입니다. 즉시 전문가의 도움을 받으세요.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 지침 (GUIDANCE) 감정 코멘트
INSERT INTO melog.emotion_comments (emotion_type, step, comment, is_active, created_at, updated_at) VALUES
('GUIDANCE', 1, '조금 지친 기분이군요. 휴식을 취해보세요.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('GUIDANCE', 2, '지친 상태입니다. 마음의 여유를 가져보세요.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('GUIDANCE', 3, '매우 지친 상태네요. 충분한 휴식이 필요합니다.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('GUIDANCE', 4, '심한 피로를 느끼고 계시네요. 즉시 휴식을 취하세요.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('GUIDANCE', 5, '극도의 피로 상태입니다. 전문의와 상담하세요.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
