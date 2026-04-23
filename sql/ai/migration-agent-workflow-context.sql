-- Agent 工作流执行上下文 — 支持断点续跑
-- 对应 Flyway: V128__ai_agent_workflow_context.sql
CREATE TABLE IF NOT EXISTS ai_agent_workflow_context (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT,
    workflow_id     VARCHAR(64)  NOT NULL,
    node_role       VARCHAR(50)  NOT NULL,
    node_status     VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    input_json      TEXT,
    output_json     TEXT,
    error_message   VARCHAR(500),
    retry_count     INTEGER      NOT NULL DEFAULT 0,
    started_at      TIMESTAMP,
    completed_at    TIMESTAMP,
    create_time     TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted         INTEGER      NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_ai_awc_workflow ON ai_agent_workflow_context(workflow_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_ai_awc_session ON ai_agent_workflow_context(session_id) WHERE deleted = 0;
