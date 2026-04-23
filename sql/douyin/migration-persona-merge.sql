-- ============================================================
-- 人设合并迁移：彩妆达人 + 护肤品套盒过品 → 彩妆护肤直播账号
-- 执行：docker exec -i dy-postgres psql -U postgres -d douyin_operations -f - < sql/douyin/migration-persona-merge.sql
-- ============================================================

-- 1. 逻辑删除原系统模板「彩妆达人」「护肤品套盒过品」
UPDATE dy_persona
SET deleted = 1, update_time = CURRENT_TIMESTAMP
WHERE owner_id = 0 AND deleted = 0
  AND persona_name IN ('彩妆达人', '护肤品套盒过品');

-- 2. 插入新人设「彩妆护肤直播账号」（若不存在）
INSERT INTO dy_persona (owner_id, account_id, persona_name, persona_type, description, tone, target_audience, content_style, keywords, is_default, status, deleted, create_time, update_time)
SELECT 0, NULL, '彩妆护肤直播账号', 'commerce', '彩妆类直播（粉底、眉笔等，每场1-5品类）与护肤品仓库甩货（每场50-120款套盒）通用人设。彩妆侧重试色、妆容教程；护肤侧重成分讲解、套盒搭配与甩货节奏', 'professional', '18-40岁女性', '彩妆试色测评、护肤成分讲解、套盒甩货过品', '彩妆,护肤,粉底,眉笔,套盒,甩货,过品', 0, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM dy_persona WHERE owner_id = 0 AND persona_name = '彩妆护肤直播账号' AND deleted = 0);

-- 3. 将引用原人设的 live_session 指向新人设
UPDATE live_session
SET persona_id = (SELECT id FROM dy_persona WHERE owner_id = 0 AND persona_name = '彩妆护肤直播账号' AND deleted = 0 LIMIT 1),
    update_time = CURRENT_TIMESTAMP
WHERE persona_id IN (SELECT id FROM dy_persona WHERE owner_id = 0 AND persona_name IN ('彩妆达人', '护肤品套盒过品'));
