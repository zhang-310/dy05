-- 订阅表
CREATE TABLE IF NOT EXISTS payment_subscription (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    org_id BIGINT,
    plan VARCHAR(20) NOT NULL DEFAULT 'free',
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    auto_renew BOOLEAN DEFAULT false,
    max_live_sessions INTEGER DEFAULT 5,
    max_sv_projects INTEGER DEFAULT 10,
    max_ai_generations INTEGER DEFAULT 50,
    max_storage_mb INTEGER DEFAULT 500,
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_payment_sub_user ON payment_subscription(user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_payment_sub_org ON payment_subscription(org_id) WHERE deleted = 0;
