-- V025: external_api_config 外部API配置表（与 Entity 对齐）
CREATE TABLE IF NOT EXISTS external_api_config (
    id                   BIGSERIAL     PRIMARY KEY,
    provider_code        VARCHAR(64)   NOT NULL,
    provider_name        VARCHAR(128)  NOT NULL,
    category             VARCHAR(32)   NOT NULL,
    base_url             VARCHAR(512)  NOT NULL,
    api_key_encrypted    VARCHAR(512),
    api_secret_encrypted VARCHAR(512),
    is_enabled           BOOLEAN       DEFAULT TRUE,
    priority             INTEGER       DEFAULT 100,
    rate_limit_per_min   INTEGER,
    daily_quota          INTEGER,
    monthly_quota        INTEGER,
    last_health_check    TIMESTAMP,
    health_status        VARCHAR(32)   DEFAULT 'unknown',
    avg_latency_ms       INTEGER,
    success_rate_pct     FLOAT,
    extra_config         JSONB,
    deleted              INTEGER       DEFAULT 0,
    create_time          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ext_api_cfg_code
    ON external_api_config(provider_code) WHERE deleted = 0;
