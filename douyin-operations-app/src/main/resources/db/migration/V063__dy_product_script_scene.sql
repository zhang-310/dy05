-- 产品话术应用场景：短视频带货、过品带货、仓播带货、单品直播间带货、娱播穿插带货
-- scene 为可选，NULL 表示通用/不限场景
ALTER TABLE dy_product_script ADD COLUMN IF NOT EXISTS scene VARCHAR(32) DEFAULT NULL;
COMMENT ON COLUMN dy_product_script.scene IS '应用场景：short_video=短视频带货,guopin=过品带货,cangbo=仓播带货,danpin=单品直播间带货,yubo=娱播穿插带货，NULL=通用';

ALTER TABLE script_version_history ADD COLUMN IF NOT EXISTS scene VARCHAR(32) DEFAULT NULL;
COMMENT ON COLUMN script_version_history.scene IS '话术应用场景（与 dy_product_script.scene 一致）';
