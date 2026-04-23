-- ai_kb_document 来源类型（用于知识引用率统计）
ALTER TABLE ai_kb_document ADD COLUMN IF NOT EXISTS source_type VARCHAR(32) DEFAULT 'manual';
COMMENT ON COLUMN ai_kb_document.source_type IS '来源：manual=手动 evolved=进化 viral=爆款拆解 live_review=直播复盘';
