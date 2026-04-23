-- P0 dual-write compensation: sync_retry_count, last_sync_retry_at
ALTER TABLE ai_kb_document ADD COLUMN IF NOT EXISTS sync_retry_count INTEGER DEFAULT 0;
ALTER TABLE ai_kb_document ADD COLUMN IF NOT EXISTS last_sync_retry_at TIMESTAMP;
