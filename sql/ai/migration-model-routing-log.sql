-- 模型路由日志表
-- 记录每次路由决策的结果，用于历史学习调整权重
CREATE TABLE IF NOT EXISTS ai_model_routing_log (
    id              BIGSERIAL PRIMARY KEY,
    content_type    VARCHAR(30),
    selected_model  VARCHAR(100),
    latency_ms      INTEGER,
    quality_score   INTEGER,
    success         BOOLEAN,
    cost_tokens     INTEGER,
    deleted         INTEGER      NOT NULL DEFAULT 0,
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_model_routing_log_type ON ai_model_routing_log(content_type);
CREATE INDEX IF NOT EXISTS idx_model_routing_log_model ON ai_model_routing_log(selected_model);
CREATE INDEX IF NOT EXISTS idx_model_routing_log_time ON ai_model_routing_log(create_time);
