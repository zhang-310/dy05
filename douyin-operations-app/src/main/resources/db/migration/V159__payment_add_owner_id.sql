-- V159: Payment 模块添加 owner_id 字段（数据隔离）
-- 来源: docs/modules/payment/fix-plan.md P0-6

-- 1. payment_order 添加 owner_id
ALTER TABLE payment_order ADD COLUMN IF NOT EXISTS owner_id BIGINT;

-- 初始化 owner_id（假设单租户场景，所有数据属于租户 1）
UPDATE payment_order SET owner_id = 1 WHERE owner_id IS NULL;

-- 设置 NOT NULL 约束
ALTER TABLE payment_order ALTER COLUMN owner_id SET NOT NULL;

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_payment_order_owner_id ON payment_order(owner_id) WHERE deleted = 0;

-- 2. payment_refund 添加 owner_id
ALTER TABLE payment_refund ADD COLUMN IF NOT EXISTS owner_id BIGINT;

-- 初始化 owner_id（从关联的订单获取）
UPDATE payment_refund pr
SET owner_id = po.owner_id
FROM payment_order po
WHERE pr.order_id = po.id AND pr.owner_id IS NULL;

-- 设置 NOT NULL 约束
ALTER TABLE payment_refund ALTER COLUMN owner_id SET NOT NULL;

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_payment_refund_owner_id ON payment_refund(owner_id) WHERE deleted = 0;

-- 3. payment_subscription 添加 owner_id
ALTER TABLE payment_subscription ADD COLUMN IF NOT EXISTS owner_id BIGINT;

-- 初始化 owner_id（假设单租户场景，所有数据属于租户 1）
UPDATE payment_subscription SET owner_id = 1 WHERE owner_id IS NULL;

-- 设置 NOT NULL 约束
ALTER TABLE payment_subscription ALTER COLUMN owner_id SET NOT NULL;

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_payment_subscription_owner_id ON payment_subscription(owner_id) WHERE deleted = 0;
