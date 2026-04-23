-- Add autoSyncEnabled flag to live_session
-- Q3-4: Auto Data Sync After Live End
ALTER TABLE live_session ADD COLUMN IF NOT EXISTS auto_sync_enabled INTEGER DEFAULT 1;

COMMENT ON COLUMN live_session.auto_sync_enabled IS '是否启用自动同步 0=关闭 1=开启';
