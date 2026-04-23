-- ================================================
-- JSONB 多维度风格标签 #25（与 style 并存，渐进迁移）
-- ================================================
ALTER TABLE dy_product_script ADD COLUMN IF NOT EXISTS style_tags JSONB;
COMMENT ON COLUMN dy_product_script.style_tags IS '多维度风格标签 JSON，如 {"tone":"professional","length":"medium"}';
