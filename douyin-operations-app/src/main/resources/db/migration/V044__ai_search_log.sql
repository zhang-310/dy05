-- V044: ai_search_log 表（支撑质量评分与进化规则）
-- Phase 1 任务 1.7：记录检索命中，用于 getUsageCount、evaluateArchivalRule 等

CREATE TABLE IF NOT EXISTS ai_search_log (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    query_text TEXT NOT NULL,
    query_rewritten TEXT,
    kb_id BIGINT,
    hit_doc_ids TEXT,
    hit_count INTEGER DEFAULT 0,
    top1_score DOUBLE PRECISION,
    search_type VARCHAR(32),
    latency_ms INTEGER,
    user_feedback VARCHAR(16),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_search_log_owner ON ai_search_log(owner_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_search_log_kb ON ai_search_log(kb_id, create_time DESC) WHERE deleted = 0;

COMMENT ON TABLE ai_search_log IS '知识库检索日志，用于质量评分 usageCount、进化规则 90 天无命中判断';
