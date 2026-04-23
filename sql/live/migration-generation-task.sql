-- 话术生成任务持久化表
-- 用于替代 in-memory GENERATING_SESSION_IDS，支持浏览器刷新/后端重启后恢复生成进度
CREATE TABLE IF NOT EXISTS live_generation_task (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'pending',  -- pending/running/completed/failed/cancelled
    total_slots     INTEGER NOT NULL DEFAULT 0,
    completed_slots INTEGER NOT NULL DEFAULT 0,
    failed_slots    INTEGER NOT NULL DEFAULT 0,
    style           VARCHAR(64),
    model_id        BIGINT,
    use_kb_ref      INTEGER DEFAULT 1,
    hot_keywords    TEXT,           -- JSON array
    error_message   TEXT,
    started_at      TIMESTAMP,
    completed_at    TIMESTAMP,
    deleted         INTEGER DEFAULT 0,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_live_gen_task_session ON live_generation_task(session_id);
CREATE INDEX IF NOT EXISTS idx_live_gen_task_user ON live_generation_task(user_id);
CREATE INDEX IF NOT EXISTS idx_live_gen_task_status ON live_generation_task(status);
