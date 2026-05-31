ALTER TABLE ai_kb_document
    ADD COLUMN IF NOT EXISTS sync_retry_count INTEGER DEFAULT 0;

ALTER TABLE ai_kb_document
    ADD COLUMN IF NOT EXISTS last_sync_retry_at TIMESTAMP;

UPDATE ai_kb_document
SET sync_retry_count = 0
WHERE sync_retry_count IS NULL;

CREATE INDEX IF NOT EXISTS idx_ai_kb_doc_compensation
    ON ai_kb_document (status, sync_retry_count, create_time)
    WHERE deleted = 0 AND status = 0;

COMMENT ON COLUMN ai_kb_document.sync_retry_count IS 'Vector/ES dual-write compensation retry count';
COMMENT ON COLUMN ai_kb_document.last_sync_retry_at IS 'Last vector/ES dual-write compensation retry time';
