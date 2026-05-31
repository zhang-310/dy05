-- V189: 配额管理 + 功能开关表
CREATE TABLE IF NOT EXISTS sys_quota (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(128) NOT NULL DEFAULT 'default',
    feature_code VARCHAR(128) NOT NULL,
    monthly_limit BIGINT NOT NULL DEFAULT 1000,
    monthly_used BIGINT NOT NULL DEFAULT 0,
    reset_date DATE NOT NULL DEFAULT CURRENT_DATE,
    create_time TIMESTAMP NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_quota_tenant_feature ON sys_quota(tenant_id, feature_code);

CREATE TABLE IF NOT EXISTS sys_feature_flag (
    id BIGSERIAL PRIMARY KEY,
    flag_key VARCHAR(128) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT false,
    description VARCHAR(512),
    tenant_override TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO sys_feature_flag (flag_key, enabled, description) VALUES
    ('ai.chat.enabled', true, 'AI 对话功能'),
    ('ai.generation.enabled', true, 'AI 生成功能'),
    ('douyin.oauth.enabled', false, '抖音 OAuth 真实对接'),
    ('payment.real.enabled', false, '真实支付通道'),
    ('notification.wecom.enabled', false, '企业微信通知')
ON CONFLICT (flag_key) DO NOTHING;
