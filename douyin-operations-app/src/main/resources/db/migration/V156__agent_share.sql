-- V156: agent_share 表（对话分享）
-- 支持分享智能体对话给其他用户，支持导入查看

CREATE TABLE IF NOT EXISTS agent_share (
    id BIGSERIAL PRIMARY KEY,
    share_code VARCHAR(32) NOT NULL UNIQUE,
    conversation_id BIGINT NOT NULL,
    agent_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    title VARCHAR(256) NOT NULL,
    summary VARCHAR(512),
    message_count INTEGER NOT NULL DEFAULT 0,
    view_count INTEGER NOT NULL DEFAULT 0,
    is_public INTEGER NOT NULL DEFAULT 1,       -- 1=公开, 0=私密
    expires_at TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_agent_share_share_code ON agent_share(share_code);
CREATE INDEX IF NOT EXISTS idx_agent_share_conversation_id ON agent_share(conversation_id);
CREATE INDEX IF NOT EXISTS idx_agent_share_owner_id ON agent_share(owner_id);
CREATE INDEX IF NOT EXISTS idx_agent_share_deleted ON agent_share(deleted);

COMMENT ON TABLE agent_share IS '智能体对话分享表';
COMMENT ON COLUMN agent_share.share_code IS '分享码（8位随机字符串）';
COMMENT ON COLUMN agent_share.summary IS '对话摘要（AI生成或手动）';
