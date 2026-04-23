-- V061: live_session 补齐 Entity 有但 DB 可能缺失的列（与 LiveSession Entity 对齐）
ALTER TABLE live_session ADD COLUMN IF NOT EXISTS persona_id BIGINT;
ALTER TABLE live_session ADD COLUMN IF NOT EXISTS session_cover VARCHAR(512);
ALTER TABLE live_session ADD COLUMN IF NOT EXISTS script_style VARCHAR(64) DEFAULT 'professional';
ALTER TABLE live_session ADD COLUMN IF NOT EXISTS readiness_check VARCHAR(512);
ALTER TABLE live_session ADD COLUMN IF NOT EXISTS scheduled_end_time TIMESTAMP;
