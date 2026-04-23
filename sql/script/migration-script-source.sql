-- ============================================================
-- script 模块 - 增量迁移：话术库增加 source/source_id 字段
-- source: manual=手动创建 / live=从直播保存 / ai=AI生成
-- ============================================================

ALTER TABLE script_library ADD COLUMN IF NOT EXISTS source VARCHAR(16) NOT NULL DEFAULT 'manual';
ALTER TABLE script_library ADD COLUMN IF NOT EXISTS source_id BIGINT;

COMMENT ON COLUMN script_library.source IS '来源：manual=手动创建 live=从直播保存 ai=AI生成';
COMMENT ON COLUMN script_library.source_id IS 'source ID e.g. live session id';
