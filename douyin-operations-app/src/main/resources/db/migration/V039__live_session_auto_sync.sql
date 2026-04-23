-- V039: live_session 增加 auto_sync_enabled 列（与 Entity 对齐）
ALTER TABLE live_session ADD COLUMN IF NOT EXISTS auto_sync_enabled INTEGER DEFAULT 1;
