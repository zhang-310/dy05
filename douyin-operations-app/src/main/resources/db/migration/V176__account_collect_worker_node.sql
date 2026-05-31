-- Remote account collection worker node registry.
-- The master server uses this table to observe idle/collecting/offline collector servers.
CREATE TABLE IF NOT EXISTS sv_account_collect_worker_node (
    worker_id        VARCHAR(128) PRIMARY KEY,
    worker_region    VARCHAR(64),
    status           VARCHAR(32)  NOT NULL DEFAULT 'idle',
    current_task_id  BIGINT,
    lease_until      TIMESTAMP,
    last_seen_at     TIMESTAMP,
    last_claim_at    TIMESTAMP,
    last_submit_at   TIMESTAMP,
    last_fail_at     TIMESTAMP,
    success_count    INTEGER      NOT NULL DEFAULT 0,
    fail_count       INTEGER      NOT NULL DEFAULT 0,
    last_error       TEXT,
    deleted          INTEGER      NOT NULL DEFAULT 0,
    create_time      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sv_acwn_last_seen
    ON sv_account_collect_worker_node(last_seen_at DESC)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_sv_acwn_status
    ON sv_account_collect_worker_node(status, lease_until)
    WHERE deleted = 0;
