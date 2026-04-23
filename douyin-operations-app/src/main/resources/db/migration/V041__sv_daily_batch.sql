-- V041: sv_daily_batch 一键日更批次表（与 Entity 对齐）
CREATE TABLE IF NOT EXISTS sv_daily_batch (
    id               BIGSERIAL     PRIMARY KEY,
    owner_id         BIGINT        NOT NULL,
    persona_id       BIGINT,
    source_type      VARCHAR(32)   NOT NULL DEFAULT 'manual',
    batch_size       INTEGER       NOT NULL DEFAULT 3,
    status           VARCHAR(16)   NOT NULL DEFAULT 'pending',
    result_summary   JSONB,
    deleted          INTEGER       NOT NULL DEFAULT 0,
    create_time      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sv_daily_batch_owner ON sv_daily_batch(owner_id) WHERE deleted = 0;
