-- E-3：与 Flyway V077 一致，供手工执行环境对齐
ALTER TABLE ai_kb_document ADD COLUMN IF NOT EXISTS quality_tier INTEGER NOT NULL DEFAULT 0;
ALTER TABLE ai_kb_document ADD COLUMN IF NOT EXISTS quality_heuristic_score INTEGER;
ALTER TABLE ai_kb_document ADD COLUMN IF NOT EXISTS last_quality_eval_at TIMESTAMP;

COMMENT ON COLUMN ai_kb_document.quality_tier IS '质量分层：0=未评估/中性 1=健康 2=疑似低质';
COMMENT ON COLUMN ai_kb_document.quality_heuristic_score IS '启发式质量分 0-100（最近一次扫描）';
COMMENT ON COLUMN ai_kb_document.last_quality_eval_at IS '上次质量评估时间（反馈或扫描）';

CREATE INDEX IF NOT EXISTS idx_ai_kb_doc_quality_tier ON ai_kb_document (kb_id, quality_tier) WHERE deleted = 0 AND quality_tier > 0;
