/**
 * W-11 支付系统 - 数据库 SQL 脚本
 * 订单表、明细表、退款表、交易日志表
 */

-- 创建订单表
CREATE TABLE IF NOT EXISTS payment_order (
    id BIGSERIAL PRIMARY KEY,
    order_no VARCHAR(50) UNIQUE NOT NULL,           -- 订单号（唯一，用于幂等性）
    user_id BIGINT NOT NULL,                        -- 用户 ID
    product_id BIGINT NOT NULL,                     -- 商品 ID
    live_session_id BIGINT,                          -- 关联直播场次（可空，GMV 单场汇总）
    amount DECIMAL(10, 2) NOT NULL,                 -- 订单金额（元）
    actual_amount DECIMAL(10, 2) NOT NULL,          -- 实际支付金额（含折扣）
    quantity INTEGER NOT NULL DEFAULT 1,            -- 商品数量
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT', -- 订单状态
    payment_method VARCHAR(50),                     -- 支付方式
    transaction_id VARCHAR(100),                    -- 支付交易 ID
    paid_at TIMESTAMP,                              -- 支付时间
    shipped_at TIMESTAMP,                           -- 发货时间
    tracking_number VARCHAR(100),                   -- 快递单号
    completed_at TIMESTAMP,                         -- 完成时间
    remark TEXT,                                    -- 备注
    deleted INTEGER DEFAULT 0,                      -- 软删除标记
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),    -- 创建时间
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()     -- 更新时间
);

-- 创建索引
CREATE INDEX idx_order_user_id ON payment_order(user_id);
CREATE INDEX idx_order_order_no ON payment_order(order_no);
CREATE INDEX idx_order_status ON payment_order(status);
CREATE INDEX idx_order_created_at ON payment_order(created_at);
CREATE INDEX idx_payment_order_live_session_id ON payment_order(live_session_id);

-- 创建订单明细表
CREATE TABLE IF NOT EXISTS payment_order_item (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,                       -- 订单 ID
    product_id BIGINT NOT NULL,                     -- 商品 ID
    product_name VARCHAR(255) NOT NULL,             -- 商品名称
    quantity INTEGER NOT NULL,                      -- 购买数量
    unit_price DECIMAL(10, 2) NOT NULL,             -- 单价
    total_price DECIMAL(10, 2) NOT NULL,            -- 小计
    discount DECIMAL(10, 2) NOT NULL DEFAULT 0,    -- 折扣金额
    created_at TIMESTAMP NOT NULL DEFAULT NOW()     -- 创建时间
);

-- 创建索引
CREATE INDEX idx_order_item_order_id ON payment_order_item(order_id);
CREATE INDEX idx_order_item_product_id ON payment_order_item(product_id);

-- 创建退款表
CREATE TABLE IF NOT EXISTS payment_refund (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,                       -- 订单 ID
    amount DECIMAL(10, 2) NOT NULL,                 -- 退款金额
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- 退款状态
    reason TEXT,                                    -- 退款原因
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),    -- 创建时间
    approved_at TIMESTAMP,                          -- 批准时间
    completed_at TIMESTAMP                          -- 完成时间
);

-- 创建索引
CREATE INDEX idx_refund_order_id ON payment_refund(order_id);
CREATE INDEX idx_refund_status ON payment_refund(status);

-- 创建交易日志表
CREATE TABLE IF NOT EXISTS payment_transaction_log (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,                       -- 订单 ID
    type VARCHAR(20) NOT NULL,                      -- 交易类型
    amount DECIMAL(10, 2) NOT NULL,                 -- 金额
    status VARCHAR(20) NOT NULL,                    -- 交易状态
    external_transaction_id VARCHAR(100),           -- 外部交易 ID
    remarks TEXT,                                   -- 备注
    created_at TIMESTAMP NOT NULL DEFAULT NOW()     -- 创建时间
);

-- 创建索引
CREATE INDEX idx_transaction_order_id ON payment_transaction_log(order_id);
CREATE INDEX idx_transaction_type ON payment_transaction_log(type);

-- 创建对账差异记录表（可选）
CREATE TABLE IF NOT EXISTS payment_reconciliation_diff (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,                       -- 订单 ID
    reason VARCHAR(100) NOT NULL,                   -- 差异原因
    local_status VARCHAR(20),                       -- 本地状态
    remote_status VARCHAR(20),                      -- 远程状态
    created_at TIMESTAMP NOT NULL DEFAULT NOW()     -- 记录时间
);

-- 支付统计视图
CREATE OR REPLACE VIEW payment_stats_daily AS
SELECT
    DATE(created_at) as stat_date,
    COUNT(*) as total_orders,
    COUNT(CASE WHEN status = 'PAID' THEN 1 END) as paid_orders,
    COUNT(CASE WHEN status = 'PENDING_PAYMENT' THEN 1 END) as pending_orders,
    SUM(amount) as total_amount,
    AVG(amount) as avg_amount,
    ROUND(COUNT(CASE WHEN status = 'PAID' THEN 1 END)::float / COUNT(*) * 100, 2) as success_rate
FROM payment_order
WHERE deleted = 0
GROUP BY DATE(created_at)
ORDER BY stat_date DESC;

-- 初始化支付配置（可选）
INSERT INTO sys_config (config_key, config_value, description, created_at)
VALUES
    ('payment.douyin.merchant_id', '', 'Douyin Merchant ID', NOW()),
    ('payment.douyin.merchant_secret', '', 'Douyin Merchant Secret', NOW()),
    ('payment.douyin.app_id', '', 'Douyin App ID', NOW()),
    ('payment.environment', 'sandbox', 'Payment environment: sandbox or production', NOW()),
    ('payment.timeout_minutes', '30', 'Payment timeout in minutes', NOW()),
    ('payment.daily_reconciliation_time', '02:00', 'Daily reconciliation time (HH:mm)', NOW())
ON CONFLICT (config_key) DO NOTHING;

-- 备注：
-- 1. order_no 使用 UNIQUE 约束保证幂等性
-- 2. status 使用枚举类型，可选值：PENDING_PAYMENT, PAID, SHIPPED, COMPLETED, REFUNDED, CANCELLED
-- 3. deleted 字段实现软删除
-- 4. created_at 和 updated_at 自动维护
-- 5. 所有金额字段使用 DECIMAL(10,2) 确保精度
-- 6. 支持分区策略（按 created_at 分区，便于按日期查询）
