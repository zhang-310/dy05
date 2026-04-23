-- V024: external_api_call_log 外部API调用日志表（与 Entity 对齐）
CREATE TABLE IF NOT EXISTS external_api_call_log (
    id               BIGSERIAL     PRIMARY KEY,
    provider_code    VARCHAR(64)   NOT NULL,
    endpoint         VARCHAR(512),
    method           VARCHAR(16),
    request_summary  VARCHAR(512),
    response_status  INTEGER,
    latency_ms       INTEGER,
    error_message    VARCHAR(1024),
    caller_module    VARCHAR(64),
    caller_user_id   BIGINT,
    estimated_cost   NUMERIC(12,6),
    create_time      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ext_api_log_provider_date
    ON external_api_call_log(provider_code, create_time);
