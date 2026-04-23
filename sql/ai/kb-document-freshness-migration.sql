-- ============================================================
-- ai_kb_document 时效性检测字段（时效性检测 Agent）
-- 版本：1.3 | 更新日期：2026-02-28
-- ============================================================

ALTER TABLE ai_kb_document ADD COLUMN IF NOT EXISTS expiry_status INTEGER DEFAULT 0;
ALTER TABLE ai_kb_document ADD COLUMN IF NOT EXISTS last_expiry_check TIMESTAMP;

COMMENT ON COLUMN ai_kb_document.expiry_status IS '时效性：0=未检测 1=有效 2=过期待更新';
COMMENT ON COLUMN ai_kb_document.last_expiry_check IS '上次时效性检测时间';
