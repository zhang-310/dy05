-- V004: 补充 live_monitor 缺失字段（Entity-Schema 对齐）
-- 这些字段已在 Entity 中定义但 SQL 表中缺失

ALTER TABLE live_monitor ADD COLUMN IF NOT EXISTS total_viewers INTEGER DEFAULT 0;
ALTER TABLE live_monitor ADD COLUMN IF NOT EXISTS new_followers INTEGER DEFAULT 0;
ALTER TABLE live_monitor ADD COLUMN IF NOT EXISTS online_count INTEGER DEFAULT 0;
ALTER TABLE live_monitor ADD COLUMN IF NOT EXISTS gmv NUMERIC(12,2) DEFAULT 0;
ALTER TABLE live_monitor ADD COLUMN IF NOT EXISTS orders INTEGER DEFAULT 0;
ALTER TABLE live_monitor ADD COLUMN IF NOT EXISTS deleted INTEGER DEFAULT 0 NOT NULL;

CREATE INDEX IF NOT EXISTS idx_live_monitor_session_time ON live_monitor(session_id, "timestamp" DESC);
