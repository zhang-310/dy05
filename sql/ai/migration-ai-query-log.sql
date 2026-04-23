-- ai_query_log: 检索查询日志（P2 监控埋点）
CREATE TABLE IF NOT EXISTS ai_query_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    kb_id BIGINT NOT NULL,
    query_text VARCHAR(512) NOT NULL,
    top_k INTEGER DEFAULT 10,
    hit_count INTEGER DEFAULT 0,
    latency_ms INTEGER,
    cache_hit INTEGER DEFAULT 0,
    source VARCHAR(32) DEFAULT 'user',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_query_log_user ON ai_query_log(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_query_log_kb ON ai_query_log(kb_id);
CREATE INDEX IF NOT EXISTS idx_ai_query_log_time ON ai_query_log(create_time);

COMMENT ON TABLE ai_query_log IS 'Knowledge search query log for monitoring';
