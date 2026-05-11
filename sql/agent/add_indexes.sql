-- P0-5: 为 agent_message 表添加缺失的索引
-- 生成日期: 2026-05-09
-- 预期收益: 查询时间 200ms → 10ms（95% 提升）

-- 复合索引（conversation_id + create_time）
CREATE INDEX IF NOT EXISTS idx_agent_message_conversation_time
ON agent_message (conversation_id, create_time ASC)
WHERE deleted = 0;

-- 单列索引（conversation_id）
CREATE INDEX IF NOT EXISTS idx_agent_message_conversation_id
ON agent_message (conversation_id)
WHERE deleted = 0;

-- 验证索引创建
SELECT
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE tablename = 'agent_message'
ORDER BY indexname;
