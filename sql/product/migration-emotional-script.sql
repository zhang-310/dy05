-- 情绪价值话术支持：复用 dy_product_script 表
-- 当 is_emotional=true 时：product_id 为 NULL，script_type 为 quote/proverb/emotional_healing/female_perspective

ALTER TABLE dy_product_script ADD COLUMN IF NOT EXISTS is_emotional BOOLEAN DEFAULT FALSE;
COMMENT ON COLUMN dy_product_script.is_emotional IS '是否为情绪价值话术（true=情绪价值，false=产品话术）';

-- 允许 product_id 为 NULL（情绪话术不关联产品）
ALTER TABLE dy_product_script ALTER COLUMN product_id DROP NOT NULL;
