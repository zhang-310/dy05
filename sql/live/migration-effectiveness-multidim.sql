-- ================================================
-- 效果评分多维度（#19 停留×W1 + 互动×W2 + 转化×W3）
-- ================================================
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS conversion_delta INTEGER;
COMMENT ON COLUMN live_script.conversion_delta IS '执行后 30s 转化变化（下单/加购等）';
