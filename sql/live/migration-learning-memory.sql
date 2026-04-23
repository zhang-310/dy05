-- 跨场次学习记忆表
-- 持久化每场直播的学习成果，跨场次沉淀
CREATE TABLE IF NOT EXISTS live_learning_memory (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    org_id          BIGINT,
    category        VARCHAR(50),
    insight_type    VARCHAR(30)  NOT NULL,
    content         TEXT         NOT NULL,
    confidence      DECIMAL(3,2) DEFAULT 1.00,
    source_session_id BIGINT,
    usage_count     INTEGER      DEFAULT 0,
    last_used_at    TIMESTAMP,
    deleted         INTEGER      NOT NULL DEFAULT 0,
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_learning_memory_user_cat ON live_learning_memory(user_id, category);
CREATE INDEX IF NOT EXISTS idx_learning_memory_type ON live_learning_memory(insight_type);
CREATE INDEX IF NOT EXISTS idx_learning_memory_confidence ON live_learning_memory(confidence);
