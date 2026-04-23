-- 与 Flyway V066 一致：AI 推理审计表（可选 app.ai.inference-audit.enabled=true）
-- 手工执行或纳入 sql/_archive/legacy-manual-migrations 汇总时使用本文件

CREATE TABLE IF NOT EXISTS ai_inference_audit (
    id              BIGSERIAL PRIMARY KEY,
    trace_id        VARCHAR(64),
    provider        VARCHAR(32),
    capability      VARCHAR(32),
    model_ref       VARCHAR(256),
    input_chars     INTEGER,
    ok              INTEGER DEFAULT 0,
    latency_ms      INTEGER,
    error_message   VARCHAR(512),
    meta_json       TEXT,
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_inference_audit_create_time ON ai_inference_audit (create_time DESC);
CREATE INDEX IF NOT EXISTS idx_ai_inference_audit_capability ON ai_inference_audit (capability);
CREATE INDEX IF NOT EXISTS idx_ai_inference_audit_trace ON ai_inference_audit (trace_id);
