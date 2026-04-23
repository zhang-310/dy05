-- V009: 升级计划剩余表结构变更

-- 人设一致性检查记录
CREATE TABLE IF NOT EXISTS sv_persona_check_log (
    id               BIGSERIAL     PRIMARY KEY,
    owner_id         BIGINT        NOT NULL,
    persona_id       BIGINT        NOT NULL,
    content_type     VARCHAR(32),
    content_id       BIGINT,
    overall_score    INTEGER,
    dimension_scores JSONB,
    suggestions      TEXT,
    deleted          INTEGER       DEFAULT 0,
    create_time      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

-- 跨模块内容转化记录
CREATE TABLE IF NOT EXISTS sv_cross_module_log (
    id               BIGSERIAL     PRIMARY KEY,
    owner_id         BIGINT        NOT NULL,
    source_type      VARCHAR(32),
    source_id        BIGINT,
    target_type      VARCHAR(32),
    target_id        BIGINT,
    deleted          INTEGER       DEFAULT 0,
    create_time      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

-- ai_model_benchmark 表结构对齐（如果表已存在但字段不同则添加缺失列）
ALTER TABLE ai_model_benchmark ADD COLUMN IF NOT EXISTS call_date DATE;
ALTER TABLE ai_model_benchmark ADD COLUMN IF NOT EXISTS total_calls INTEGER DEFAULT 0;
ALTER TABLE ai_model_benchmark ADD COLUMN IF NOT EXISTS success_calls INTEGER DEFAULT 0;
ALTER TABLE ai_model_benchmark ADD COLUMN IF NOT EXISTS avg_latency_ms INTEGER;
ALTER TABLE ai_model_benchmark ADD COLUMN IF NOT EXISTS p95_latency_ms INTEGER;
ALTER TABLE ai_model_benchmark ADD COLUMN IF NOT EXISTS avg_input_tokens INTEGER;
ALTER TABLE ai_model_benchmark ADD COLUMN IF NOT EXISTS avg_output_tokens INTEGER;
ALTER TABLE ai_model_benchmark ADD COLUMN IF NOT EXISTS estimated_cost NUMERIC(10,4);
ALTER TABLE ai_model_benchmark ADD COLUMN IF NOT EXISTS avg_quality_score FLOAT;
