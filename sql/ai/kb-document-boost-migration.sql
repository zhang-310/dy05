-- ============================================================
-- ai_kb_document 知识权重字段迁移（BR-31）
-- 版本：1.2 | 更新日期：2026-02-28
-- ============================================================

ALTER TABLE ai_kb_document ADD COLUMN IF NOT EXISTS boost_factor DECIMAL(5,2) DEFAULT 1.00;

COMMENT ON COLUMN ai_kb_document.boost_factor IS '检索提权因子（1.0 默认，高效内容+0.15，低效-0.05）';
