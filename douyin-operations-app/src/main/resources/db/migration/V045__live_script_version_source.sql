-- V045: live_script_version 增加 source 列（Phase 1 任务 1.4）
-- source: manual=手动, auto_improve=AI自动优化, ab_winner=AB测试胜出

ALTER TABLE live_script_version ADD COLUMN IF NOT EXISTS source VARCHAR(32) DEFAULT 'manual';
COMMENT ON COLUMN live_script_version.source IS '版本来源：manual=手动, auto_improve=AI自动优化, ab_winner=AB测试胜出';
