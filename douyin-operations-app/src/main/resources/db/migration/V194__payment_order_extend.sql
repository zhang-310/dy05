-- V194: 支付订单扩展字段
ALTER TABLE payment_order ADD COLUMN IF NOT EXISTS session_id BIGINT;
ALTER TABLE payment_order ADD COLUMN IF NOT EXISTS product_code VARCHAR(64) DEFAULT 'douyin-ops';
ALTER TABLE payment_order ADD COLUMN IF NOT EXISTS feature_code VARCHAR(128);
ALTER TABLE payment_order ADD COLUMN IF NOT EXISTS coupon_code VARCHAR(64);
ALTER TABLE payment_order ADD COLUMN IF NOT EXISTS discount_amount BIGINT DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_payment_product ON payment_order(product_code) WHERE deleted = 0;
