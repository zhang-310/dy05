-- V030: live_generation_task 话术生成任务表（与 Entity 对齐）
CREATE TABLE IF NOT EXISTS live_generation_task (
    id              BIGSERIAL       PRIMARY KEY,
    session_id      BIGINT          NOT NULL,
    status          VARCHAR(20)     DEFAULT 'running',
    total_slots     INTEGER         DEFAULT 0,
    completed_slots INTEGER         DEFAULT 0,
    failed_slots    INTEGER         DEFAULT 0,
    style           VARCHAR(50),
    model_id        BIGINT,
    use_kb_ref      BOOLEAN         DEFAULT false,
    hot_keywords    TEXT,
    error_message   TEXT,
    owner_id        BIGINT,
    deleted         INTEGER         NOT NULL DEFAULT 0,
    create_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_live_gen_task_session ON live_generation_task(session_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_gen_task_owner ON live_generation_task(owner_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_gen_task_status ON live_generation_task(status) WHERE deleted = 0;
