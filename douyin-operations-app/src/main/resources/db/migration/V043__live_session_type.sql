-- V043: live_session 增加 session_type（standard / chat_2h）
ALTER TABLE live_session ADD COLUMN IF NOT EXISTS session_type VARCHAR(32) DEFAULT 'standard';
