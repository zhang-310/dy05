-- V158: 添加 agent_message 表索引以优化查询性能
-- 问题: agent_message 表缺少 conversation_id、create_time 等索引，导致对话历史查询全表扫描
-- 预期收益: 查询时间 300ms → 30ms (90% 提升)

-- 单列索引
CREATE INDEX IF NOT EXISTS idx_agent_message_conversation_id ON agent_message(conversation_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agent_message_create_time ON agent_message(create_time DESC) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agent_message_sender_type ON agent_message(sender_type) WHERE deleted = 0;

-- 复合索引（常用查询组合）
CREATE INDEX IF NOT EXISTS idx_agent_message_conv_time ON agent_message(conversation_id, create_time ASC) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agent_message_conv_sender ON agent_message(conversation_id, sender_type) WHERE deleted = 0;
