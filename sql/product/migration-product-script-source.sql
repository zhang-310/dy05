-- ============================================================
-- dy_product_script 话术来源标记
-- 用途：追溯 AI 生成 vs 人工编写 vs 导入
-- ============================================================

ALTER TABLE dy_product_script ADD COLUMN IF NOT EXISTS source VARCHAR(16) DEFAULT 'ai';
COMMENT ON COLUMN dy_product_script.source IS '来源：ai=AI生成 manual=人工编写 import=导入';
