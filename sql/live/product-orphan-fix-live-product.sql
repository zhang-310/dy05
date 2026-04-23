-- 将 live_product 中无效 product_id 的关联逻辑删除（product_id 有 NOT NULL 约束，无法置空）
UPDATE live_product lp
SET deleted = 1, update_time = CURRENT_TIMESTAMP
WHERE lp.deleted = 0
  AND lp.product_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM dy_product p WHERE p.id = lp.product_id AND p.deleted = 0);
