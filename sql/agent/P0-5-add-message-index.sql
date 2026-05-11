-- P0-5: 为 agent_message 表添加索引
-- 优化对话历史查询性能（200ms → 10ms）

-- 复合索引：conversation_id + create_time（支持按时间排序查询）
CREATE INDEX IF NOT EXISTS idx_agent_message_conversation_time
ON agent_message (conversation_id, create_time ASC)
WHERE deleted = 0;

-- 说明：
-- 1. conversation_id: 过滤条件
-- 2. create_time ASC: 排序字段
-- 3. WHERE deleted = 0: 部分索引，只索引未删除的记录
-- 4. 预期收益：查询时间从 200ms → 10ms（95% 提升）
