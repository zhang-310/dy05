-- live_product 产品类型：利润品/亏品/平价品/爆品/控单产品（可多选，逗号分隔）
ALTER TABLE live_product ADD COLUMN IF NOT EXISTS product_type VARCHAR(128);
COMMENT ON COLUMN live_product.product_type IS '产品类型：profit=利润品,loss=亏品,flat=平价品,hot=爆品,control=控单产品，可多选逗号分隔';
