-- LF-01：直播形式 live_format（与 script_style 解耦），与 11-方向基线 §2.1 对齐
ALTER TABLE live_session ADD COLUMN IF NOT EXISTS live_format VARCHAR(32) DEFAULT 'heavy_paid_category';

COMMENT ON COLUMN live_session.live_format IS
  '直播形式：content_led=娱乐聊天, organic_micro_paid=自然流微付费, heavy_paid_category=重付费过品, warehouse=仓播, single_sku=单品';

-- 历史：chat_2h 场次 → content_led（与迁移策略一致）
UPDATE live_session
SET live_format = 'content_led'
WHERE session_type = 'chat_2h'
  AND (live_format IS NULL OR live_format = 'heavy_paid_category');

CREATE INDEX IF NOT EXISTS idx_live_session_format ON live_session(live_format) WHERE deleted = 0;
