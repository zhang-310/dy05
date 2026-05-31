-- V185: 信用积分表 + 订阅表
CREATE TABLE IF NOT EXISTS sys_credit_account (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(128) NOT NULL DEFAULT 'default',
    user_id BIGINT NOT NULL DEFAULT 0,
    balance BIGINT NOT NULL DEFAULT 0,
    frozen BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT now(),
    update_time TIMESTAMP NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_credit_account ON sys_credit_account(tenant_id, user_id);

CREATE TABLE IF NOT EXISTS sys_credit_ledger (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(128) NOT NULL DEFAULT 'default',
    user_id BIGINT NOT NULL DEFAULT 0,
    amount BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    reference_id VARCHAR(128),
    balance_after BIGINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_credit_ledger_tenant ON sys_credit_ledger(tenant_id, create_time DESC);

CREATE TABLE IF NOT EXISTS sys_subscription (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(128) NOT NULL DEFAULT 'default',
    user_id BIGINT NOT NULL DEFAULT 0,
    product_code VARCHAR(64) NOT NULL,
    plan_code VARCHAR(64) DEFAULT 'basic',
    status VARCHAR(16) NOT NULL DEFAULT 'active',
    valid_from TIMESTAMP,
    valid_to TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_subscription_tenant ON sys_subscription(tenant_id, status);
