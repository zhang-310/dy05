-- T-5：图生视频任务 Webhook 投递最终失败记录（脱敏：仅存 URL 的 SHA-256）
CREATE TABLE IF NOT EXISTS sv_webhook_dlq (
    id              BIGSERIAL PRIMARY KEY,
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    owner_id        BIGINT,
    task_id         BIGINT NOT NULL,
    webhook_url_sha256 VARCHAR(64) NOT NULL,
    last_http_status INTEGER,
    attempt_count   INTEGER NOT NULL DEFAULT 0,
    error_preview   VARCHAR(512),
    event_code      VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_sv_webhook_dlq_task ON sv_webhook_dlq (task_id);
CREATE INDEX IF NOT EXISTS idx_sv_webhook_dlq_owner ON sv_webhook_dlq (owner_id);
