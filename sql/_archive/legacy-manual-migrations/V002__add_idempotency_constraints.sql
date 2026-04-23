-- V002: 添加幂等性唯一约束

-- 防止重复监控数据
CREATE UNIQUE INDEX IF NOT EXISTS uk_live_monitor_session_ts
    ON live_monitor(session_id, timestamp) WHERE deleted = 0;

-- 防止重复直播产品关联
CREATE UNIQUE INDEX IF NOT EXISTS uk_live_product_session_product
    ON live_product(session_id, product_id) WHERE deleted = 0;
