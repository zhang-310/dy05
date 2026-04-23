-- 用量记录表
CREATE TABLE IF NOT EXISTS payment_usage_record (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    org_id BIGINT,
    metric VARCHAR(50) NOT NULL,
    delta INTEGER NOT NULL DEFAULT 1,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_pay_usage_user_metric ON payment_usage_record(user_id, metric) WHERE deleted = 0;

-- 月度用量汇总表
CREATE TABLE IF NOT EXISTS payment_usage_monthly (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    org_id BIGINT,
    metric VARCHAR(50) NOT NULL,
    month VARCHAR(7) NOT NULL,
    total INTEGER DEFAULT 0,
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, metric, month)
);
CREATE INDEX IF NOT EXISTS idx_pay_usage_monthly_user ON payment_usage_monthly(user_id, month) WHERE deleted = 0;
