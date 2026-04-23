-- ============================================================
-- P1/P2 升级迁移：dy_product 扩展字段 + auth 菜单命名
-- 执行: psql -h localhost -p 5433 -U postgres -d douyin_operations -f sql/product/migration-p1-p2-upgrade.sql
-- 或: Get-Content sql/product/migration-p1-p2-upgrade.sql | docker exec -i dy-postgres psql -U postgres -d douyin_operations -f -
-- ============================================================

-- 1. dy_product 扩展字段（若已存在则跳过）
ALTER TABLE dy_product ADD COLUMN IF NOT EXISTS profit_margin_pct DECIMAL(5,4);
ALTER TABLE dy_product ADD COLUMN IF NOT EXISTS loss_per_unit DECIMAL(10,2);
ALTER TABLE dy_product ADD COLUMN IF NOT EXISTS control_strategy VARCHAR(128);
ALTER TABLE dy_product ADD COLUMN IF NOT EXISTS product_link VARCHAR(512);
ALTER TABLE dy_product ADD COLUMN IF NOT EXISTS ai_selling_points TEXT;

-- 2. auth_resource 菜单命名：直播商品 → 直播选品
UPDATE auth_resource SET resource_name = '直播选品' WHERE resource_code = '/live/products' AND resource_name = '直播商品';
UPDATE auth_resource SET resource_name = '直播选品列表' WHERE resource_code = '/api/v1/live/product/list' AND resource_name = '直播商品列表';
