-- ai_kb_document 效果统计字段（阶段一：检索/引用打点）
-- 用于 viral/live_review 迭代闭环的数据基础

ALTER TABLE ai_kb_document ADD COLUMN IF NOT EXISTS retrieval_count BIGINT DEFAULT 0;
ALTER TABLE ai_kb_document ADD COLUMN IF NOT EXISTS citation_count BIGINT DEFAULT 0;
ALTER TABLE ai_kb_document ADD COLUMN IF NOT EXISTS last_retrieval_at TIMESTAMP;
ALTER TABLE ai_kb_document ADD COLUMN IF NOT EXISTS last_citation_at TIMESTAMP;

COMMENT ON COLUMN ai_kb_document.retrieval_count IS '检索命中次数（hybridSearch 返回时+1）';
COMMENT ON COLUMN ai_kb_document.citation_count IS '进化引用次数（作为进化上下文时+1）';
COMMENT ON COLUMN ai_kb_document.last_retrieval_at IS '最近一次检索命中时间';
COMMENT ON COLUMN ai_kb_document.last_citation_at IS '最近一次进化引用时间';

CREATE INDEX IF NOT EXISTS idx_kb_doc_retrieval_count ON ai_kb_document(retrieval_count DESC) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_kb_doc_citation_count ON ai_kb_document(citation_count DESC) WHERE deleted = 0;
