-- LLM 工具调用执行日志，用于监控和排障
CREATE TABLE IF NOT EXISTS ai_tool_execution_log (
    id              BIGSERIAL    PRIMARY KEY,
    tool_name       VARCHAR(128) NOT NULL,
    arguments_hash  VARCHAR(64),
    result_text     TEXT,
    success         BOOLEAN      NOT NULL DEFAULT TRUE,
    latency_ms      BIGINT,
    error_msg       TEXT,
    retry_count     INTEGER      NOT NULL DEFAULT 0,
    owner_id        BIGINT,
    deleted         INTEGER      NOT NULL DEFAULT 0,
    create_time     TIMESTAMP    DEFAULT NOW(),
    update_time     TIMESTAMP    DEFAULT NOW()
);

COMMENT ON TABLE ai_tool_execution_log IS 'LLM 工具调用执行日志';

CREATE INDEX IF NOT EXISTS idx_ai_tool_exec_log_name_time ON ai_tool_execution_log(tool_name, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_ai_tool_exec_log_owner ON ai_tool_execution_log(owner_id, deleted);
CREATE INDEX IF NOT EXISTS idx_ai_tool_exec_log_success ON ai_tool_execution_log(success, create_time DESC);
