-- 工具调用限流配置表
CREATE TABLE IF NOT EXISTS ai_tool_rate_limit_config (
    id                    BIGSERIAL    PRIMARY KEY,
    tool_name             VARCHAR(128) NOT NULL UNIQUE,
    max_calls_per_minute  INTEGER      NOT NULL DEFAULT 60,
    max_calls_per_hour    INTEGER      NOT NULL DEFAULT 1000,
    cooldown_seconds      INTEGER      NOT NULL DEFAULT 5,
    owner_id              BIGINT,
    deleted               INTEGER      NOT NULL DEFAULT 0,
    create_time           TIMESTAMP    DEFAULT NOW(),
    update_time           TIMESTAMP    DEFAULT NOW()
);

COMMENT ON TABLE ai_tool_rate_limit_config IS 'LLM 工具调用限流配置';

-- 种子数据：默认限流配置
INSERT INTO ai_tool_rate_limit_config (tool_name, max_calls_per_minute, max_calls_per_hour, cooldown_seconds)
VALUES
    ('kb_rag_search', 30, 500, 2),
    ('web_search', 10, 200, 5),
    ('code_interpreter', 20, 300, 3)
ON CONFLICT (tool_name) DO NOTHING;
