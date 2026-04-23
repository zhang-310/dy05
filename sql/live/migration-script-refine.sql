-- live_script 话术精细化：时长限制、需求、风格
-- duration_limit_sec: 用户设定的时长上限（秒），0 表示不限制
-- requirement: 需求/意图，如 开场白/产品介绍/促单/转场/收尾/互动引导
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS duration_limit_sec INTEGER DEFAULT 0;
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS requirement VARCHAR(128);
COMMENT ON COLUMN live_script.duration_limit_sec IS 'duration limit seconds, 0=no limit';
COMMENT ON COLUMN live_script.requirement IS 'requirement: opening/product/promotion/transition/closing/interaction';
