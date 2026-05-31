-- V192: 支付退款记录 + 对账文件
CREATE TABLE IF NOT EXISTS sys_refund_record (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(128) NOT NULL DEFAULT 'default',
    order_no VARCHAR(64) NOT NULL,
    refund_no VARCHAR(64) NOT NULL UNIQUE,
    amount BIGINT NOT NULL,
    reason VARCHAR(512),
    status VARCHAR(16) NOT NULL DEFAULT 'pending',
    provider_refund_id VARCHAR(128),
    create_time TIMESTAMP NOT NULL DEFAULT now(),
    complete_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_refund_order ON sys_refund_record(order_no);

CREATE TABLE IF NOT EXISTS sys_reconciliation (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(128) NOT NULL DEFAULT 'default',
    batch_no VARCHAR(64) NOT NULL,
    total_orders INT DEFAULT 0,
    matched_orders INT DEFAULT 0,
    diff_amount BIGINT DEFAULT 0,
    status VARCHAR(16) DEFAULT 'pending',
    report_path VARCHAR(512),
    create_time TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_recon_tenant ON sys_reconciliation(tenant_id, create_time DESC);
