CREATE TABLE IF NOT EXISTS sys_alert_rule (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    metric_name VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL,
    threshold DOUBLE PRECISION NOT NULL,
    operator VARCHAR(8) NOT NULL,
    duration INTEGER NOT NULL,
    severity VARCHAR(32) NOT NULL,
    description VARCHAR(512),
    enabled BOOLEAN NOT NULL DEFAULT true,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sys_alert_rule_enabled_deleted
    ON sys_alert_rule(enabled, deleted);

CREATE TABLE IF NOT EXISTS sys_alert_record (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT,
    rule_name VARCHAR(128),
    metric_name VARCHAR(128) NOT NULL,
    message VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    metric_value DOUBLE PRECISION,
    threshold DOUBLE PRECISION,
    triggered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_sys_alert_record_status_time
    ON sys_alert_record(status, triggered_at DESC)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_sys_alert_record_rule_time
    ON sys_alert_record(rule_id, triggered_at DESC)
    WHERE deleted = 0;

