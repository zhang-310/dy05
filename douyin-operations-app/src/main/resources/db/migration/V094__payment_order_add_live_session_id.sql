-- GMV-01: 支付订单关联直播场次，支持单场 GMV 汇总与对账
ALTER TABLE payment_order ADD COLUMN IF NOT EXISTS live_session_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_payment_order_live_session_id ON payment_order(live_session_id);
