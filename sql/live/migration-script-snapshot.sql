-- ============================================================
-- live_script 产品话术引用快照
-- 用途：引用产品话术时保存源话术 ID 及内容快照，可追溯
-- ============================================================

ALTER TABLE live_script ADD COLUMN IF NOT EXISTS referenced_script_id BIGINT;
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS referenced_script_snapshot JSONB;

COMMENT ON COLUMN live_script.referenced_script_id IS '引用的产品话术 ID（dy_product_script.id）';
COMMENT ON COLUMN live_script.referenced_script_snapshot IS '引用时的话术快照（id/version/content/style/scriptType）';

CREATE INDEX IF NOT EXISTS idx_live_script_referenced ON live_script(referenced_script_id) WHERE referenced_script_id IS NOT NULL;
