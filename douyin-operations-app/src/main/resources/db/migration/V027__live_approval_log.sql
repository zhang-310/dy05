-- V027: live_approval_log 直播话术审批日志表（与 Entity 对齐）
CREATE TABLE IF NOT EXISTS live_approval_log (
    id               BIGSERIAL     PRIMARY KEY,
    session_id       BIGINT        NOT NULL,
    script_id        BIGINT,
    action           VARCHAR(16)   NOT NULL,
    operator_id      BIGINT        NOT NULL,
    comment          TEXT,
    deleted          INTEGER       NOT NULL DEFAULT 0,
    create_time      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_approval_log_session ON live_approval_log(session_id, create_time);
