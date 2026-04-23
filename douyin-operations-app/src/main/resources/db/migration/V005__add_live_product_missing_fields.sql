-- V005: 补充 live_product 缺失字段（Entity-Schema 对齐）

ALTER TABLE live_product ADD COLUMN IF NOT EXISTS script_source VARCHAR(32) DEFAULT 'session';
ALTER TABLE live_product ADD COLUMN IF NOT EXISTS product_script_id BIGINT;
ALTER TABLE live_product ADD COLUMN IF NOT EXISTS product_type VARCHAR(128);
ALTER TABLE live_product ADD COLUMN IF NOT EXISTS deleted INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE live_product ADD COLUMN IF NOT EXISTS update_time TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_live_product_session_position ON live_product(session_id, position);
