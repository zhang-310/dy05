-- P0-4: 为 live_script 表添加缺失的索引
-- 生成日期: 2026-05-09
-- 预期收益: 查询时间 500ms → 50ms（90% 提升）

-- 单列索引
CREATE INDEX IF NOT EXISTS idx_live_script_product_id ON live_script(product_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_script_user_id ON live_script(user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_script_session_id ON live_script(session_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_script_create_time ON live_script(create_time DESC) WHERE deleted = 0;

-- 复合索引（常用查询组合）
CREATE INDEX IF NOT EXISTS idx_live_script_session_seq ON live_script(session_id, sequence_no) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_script_user_create ON live_script(user_id, create_time DESC) WHERE deleted = 0;

-- 验证索引创建
SELECT
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE tablename = 'live_script'
ORDER BY indexname;
