-- Q3-4: 直播结束自动数据同步 — 字段扩展
-- Auto-sync fields for live_session table

ALTER TABLE live_session ADD COLUMN IF NOT EXISTS auto_sync_enabled BOOLEAN DEFAULT true;
ALTER TABLE live_session ADD COLUMN IF NOT EXISTS planned_end_time TIMESTAMP;

-- Index for scheduler query: status=live AND planned_end_time < now AND auto_sync_enabled = true
CREATE INDEX IF NOT EXISTS idx_live_session_auto_sync
    ON live_session (status, planned_end_time, auto_sync_enabled)
    WHERE deleted = 0;

COMMENT ON COLUMN live_session.auto_sync_enabled IS '是否开启直播结束后自动数据同步';
COMMENT ON COLUMN live_session.planned_end_time IS '计划结束时间，超过此时间且仍在直播中将自动结束并同步数据';
