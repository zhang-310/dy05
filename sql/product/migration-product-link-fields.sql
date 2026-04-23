-- 产品链接 + AI 提炼卖点字段
-- 执行: Get-Content sql/product/migration-product-link-fields.sql | docker exec -i dy-postgres psql -U postgres -d douyin_operations -f -

ALTER TABLE dy_product ADD COLUMN IF NOT EXISTS product_link VARCHAR(512);
COMMENT ON COLUMN dy_product.product_link IS '商品链接（抖音/淘宝等），用于 AI 提取信息';

ALTER TABLE dy_product ADD COLUMN IF NOT EXISTS ai_selling_points TEXT;
COMMENT ON COLUMN dy_product.ai_selling_points IS 'AI 从链接提炼的卖点（多行文本）';
