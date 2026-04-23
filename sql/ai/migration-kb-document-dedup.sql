-- ============================================================
-- Phase 2 去重：ai_kb_document 内容指纹、SimHash、metadata
-- ============================================================

ALTER TABLE ai_kb_document ADD COLUMN IF NOT EXISTS content_fingerprint VARCHAR(64);
ALTER TABLE ai_kb_document ADD COLUMN IF NOT EXISTS simhash BIGINT;
ALTER TABLE ai_kb_document ADD COLUMN IF NOT EXISTS metadata JSONB;

COMMENT ON COLUMN ai_kb_document.content_fingerprint IS 'MD5(归一化正文)，用于文档级完全一致去重';
COMMENT ON COLUMN ai_kb_document.simhash IS '64位 SimHash，用于近似重复检测（海明距离<3 疑似重复）';
COMMENT ON COLUMN ai_kb_document.metadata IS '扩展元数据（如 parseWithMetadata 的 title/author/keywords 等）';

CREATE INDEX IF NOT EXISTS idx_kb_doc_fingerprint ON ai_kb_document(kb_id, content_fingerprint) WHERE content_fingerprint IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_kb_doc_simhash ON ai_kb_document(kb_id, simhash) WHERE simhash IS NOT NULL;
