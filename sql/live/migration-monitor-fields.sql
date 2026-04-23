-- live_monitor 扩展字段（文档 02 可选字段）
-- 执行日期：2026-03-03

-- total_viewers: 累计观看人数
ALTER TABLE live_monitor ADD COLUMN IF NOT EXISTS total_viewers INTEGER DEFAULT 0;
COMMENT ON COLUMN live_monitor.total_viewers IS '累计观看人数';

-- new_followers: 累计新增粉丝
ALTER TABLE live_monitor ADD COLUMN IF NOT EXISTS new_followers INTEGER DEFAULT 0;
COMMENT ON COLUMN live_monitor.new_followers IS '累计新增粉丝';

-- online_count: 在线人数
ALTER TABLE live_monitor ADD COLUMN IF NOT EXISTS online_count INTEGER DEFAULT 0;
COMMENT ON COLUMN live_monitor.online_count IS '在线人数';

-- gmv: 累计 GMV
ALTER TABLE live_monitor ADD COLUMN IF NOT EXISTS gmv DECIMAL(12,2) DEFAULT 0;
COMMENT ON COLUMN live_monitor.gmv IS '累计 GMV';

-- orders: 累计订单数
ALTER TABLE live_monitor ADD COLUMN IF NOT EXISTS orders INTEGER DEFAULT 0;
COMMENT ON COLUMN live_monitor.orders IS '累计订单数';
