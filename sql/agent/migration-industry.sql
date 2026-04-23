-- ============================================================
-- agent 模块 - 行业关联迁移
-- 业务范围：彩妆 + 护肤品套盒
-- 执行：psql -h localhost -p 5433 -U postgres -d douyin_operations -f sql/agent/migration-industry.sql
-- ============================================================

-- 1. 添加 industry_id 列
ALTER TABLE agent ADD COLUMN IF NOT EXISTS industry_id BIGINT;
COMMENT ON COLUMN agent.industry_id IS '所属行业ID，关联 sys_industry（彩妆/护肤品套盒）';

-- 2. 索引（便于按行业筛选）
CREATE INDEX IF NOT EXISTS idx_agent_industry_id ON agent (industry_id) WHERE deleted = 0;

-- 3. 确保行业数据存在（若 demo-all 未执行）
INSERT INTO sys_industry (parent_id, industry_name, industry_code, sort_order, status, deleted)
SELECT 0, '美妆护肤', 'beauty', 1, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_industry WHERE industry_code = 'beauty' AND deleted = 0);

INSERT INTO sys_industry (parent_id, industry_name, industry_code, sort_order, status, deleted)
SELECT (SELECT id FROM sys_industry WHERE industry_code = 'beauty' AND deleted = 0 LIMIT 1), '彩妆', 'makeup', 1, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_industry WHERE industry_code = 'makeup' AND deleted = 0);

INSERT INTO sys_industry (parent_id, industry_name, industry_code, sort_order, status, deleted)
SELECT (SELECT id FROM sys_industry WHERE industry_code = 'beauty' AND deleted = 0 LIMIT 1), '护肤品套盒', 'skincare_set', 2, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_industry WHERE industry_code = 'skincare_set' AND deleted = 0);

-- 4. 将已有智能体默认关联到彩妆
UPDATE agent
SET industry_id = (SELECT id FROM sys_industry WHERE industry_code = 'makeup' AND deleted = 0 LIMIT 1)
WHERE industry_id IS NULL AND deleted = 0;
