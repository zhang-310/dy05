-- V190: 数据看板缓存 + 指标快照
CREATE TABLE IF NOT EXISTS sys_dashboard_cache (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(128) NOT NULL DEFAULT 'default',
    cache_key VARCHAR(256) NOT NULL,
    cache_value TEXT,
    expires_at TIMESTAMP NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_dash_cache ON sys_dashboard_cache(tenant_id, cache_key);

CREATE TABLE IF NOT EXISTS sys_metric_snapshot (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(128) NOT NULL DEFAULT 'default',
    metric_name VARCHAR(128) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL DEFAULT 0,
    dimension VARCHAR(256),
    snapshot_time TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_metric_snapshot ON sys_metric_snapshot(tenant_id, metric_name, snapshot_time DESC);
