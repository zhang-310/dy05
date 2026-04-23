-- 方案 A：将 live_script 中无效 product_id 置空
UPDATE live_script s
SET product_id = NULL, update_time = CURRENT_TIMESTAMP
WHERE s.deleted = 0
  AND s.script_type = 'product'
  AND s.product_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM dy_product p WHERE p.id = s.product_id AND p.deleted = 0);
