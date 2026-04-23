-- V035: live_script 增加 ai_suggestion、duration_limit_sec、requirement 等列（与 Entity 对齐）
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS ai_suggestion TEXT;
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS duration_limit_sec INTEGER;
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS requirement VARCHAR(128);
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS referenced_script_id BIGINT;
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS referenced_script_snapshot TEXT;
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS approval_status INTEGER DEFAULT 0;
