-- V157: 添加 live_script 表索引以优化查询性能
-- 问题: live_script 表缺少 product_id、user_id 等外键索引，导致关联查询全表扫描
-- 预期收益: 查询时间 500ms → 50ms (90% 提升)

-- 单列索引
CREATE INDEX IF NOT EXISTS idx_live_script_product_id ON live_script(product_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_script_user_id ON live_script(user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_script_create_time ON live_script(create_time DESC) WHERE deleted = 0;

-- 复合索引（常用查询组合）
CREATE INDEX IF NOT EXISTS idx_live_script_session_seq ON live_script(session_id, sequence_no) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_script_user_create ON live_script(user_id, create_time DESC) WHERE deleted = 0;
