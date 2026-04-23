-- ============================================================
-- 账号迁移：美食/穿搭 Demo 账号 → 彩妆/护肤品
-- 执行：docker cp sql/douyin/migration-account-to-makeup-skincare.sql dy-postgres:/tmp/
--       docker exec dy-postgres psql -U postgres -d douyin_operations -f /tmp/migration-account-to-makeup-skincare.sql
-- ============================================================

-- 1. 彩妆达人：原 dy_food_001 / 美食探店达人
UPDATE douyin_account
SET account_name = '彩妆达人', account_id = 'dy_makeup_001', description = '彩妆试色、妆容教程', update_time = CURRENT_TIMESTAMP
WHERE account_id = 'dy_food_001' AND deleted = 0;

-- 2. 护肤品套盒：原 dy_fashion_002 / 穿搭种草官
UPDATE douyin_account
SET account_name = '护肤品套盒', account_id = 'dy_skincare_002', description = '护肤品套盒甩货、成分讲解', update_time = CURRENT_TIMESTAMP
WHERE account_id = 'dy_fashion_002' AND deleted = 0;
