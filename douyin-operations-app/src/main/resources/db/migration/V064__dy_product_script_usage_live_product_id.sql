-- 排品绑定 usage 幂等：按 live_product + product_script 去重
ALTER TABLE dy_product_script_usage ADD COLUMN IF NOT EXISTS live_product_id BIGINT;
COMMENT ON COLUMN dy_product_script_usage.live_product_id IS '直播排品行 live_product.id，与 product_script_id 组合幂等';

CREATE INDEX IF NOT EXISTS idx_dpsu_live_product_script
    ON dy_product_script_usage (live_product_id, product_script_id)
    WHERE deleted = 0 AND live_product_id IS NOT NULL;
