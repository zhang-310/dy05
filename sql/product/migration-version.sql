-- ================================================
-- DyProduct 乐观锁：version 字段，防止并发库存更新超卖
-- 执行前请确认 dy_product 表已存在
-- ================================================

ALTER TABLE dy_product ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
