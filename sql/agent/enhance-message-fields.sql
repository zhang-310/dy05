-- 智能体消息增强：添加 tool_calls 和 rating 字段
-- 日期: 2026-04-22

-- 添加 tool_calls 字段（JSON 数组，存储工具调用记录）
ALTER TABLE agent_message ADD COLUMN IF NOT EXISTS tool_calls TEXT;

-- 添加 rating 字段（up=好评 down=差评）
ALTER TABLE agent_message ADD COLUMN IF NOT EXISTS rating VARCHAR(8);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_agent_message_rating ON agent_message(rating) WHERE rating IS NOT NULL;

-- 验证
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'agent_message' AND column_name IN ('tool_calls', 'rating');