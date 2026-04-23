-- ============================================================
-- 排品表扩展字段：利润、控单策略
-- 用于拍品千次成交/密度成交等关键战略策略
-- ============================================================

-- 利润百分比（如 0.45=45%），正数表示利润
ALTER TABLE dy_product ADD COLUMN IF NOT EXISTS profit_margin_pct DECIMAL(5,4);
COMMENT ON COLUMN dy_product.profit_margin_pct IS '利润百分比，如 0.45=45%；正数=利润，与 loss_per_unit 二选一';

-- 每单亏损金额（元），正数表示亏多少钱
ALTER TABLE dy_product ADD COLUMN IF NOT EXISTS loss_per_unit DECIMAL(10,2);
COMMENT ON COLUMN dy_product.loss_per_unit IS '每单亏损金额（元），正数表示亏多少；与 profit_margin_pct 二选一';

-- 控单策略：控3单、不控单1分钟下、控单、控单憋单 等，针对拍品千次/密度成交
ALTER TABLE dy_product ADD COLUMN IF NOT EXISTS control_strategy VARCHAR(128);
COMMENT ON COLUMN dy_product.control_strategy IS '控单策略：控3单、不控单1分钟下、控单憋单 等，拍品千次成交/密度成交关键策略';
