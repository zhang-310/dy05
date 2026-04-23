-- ============================================================
-- dy_persona 人设模板与示例数据修正
-- 1. 补充系统模板 (owner_id=0)
-- 2. 修正 sample-data 中的不规范类型
-- ============================================================

-- 系统模板：owner_id=0，供「从模板创建」使用（仅当不存在时插入）
INSERT INTO dy_persona (owner_id, account_id, persona_name, persona_type, description, tone, target_audience, content_style, keywords, is_default, status, deleted, create_time, update_time)
SELECT 0, NULL, '美妆博主', 'commerce', '平价美妆分享，真实测评，像闺蜜一样推荐好物', 'warm', '18-30岁女性', '亲切种草、测评分享', '美妆,护肤,平价,测评', 0, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM dy_persona WHERE owner_id = 0 AND persona_name = '美妆博主' AND deleted = 0);

INSERT INTO dy_persona (owner_id, account_id, persona_name, persona_type, description, tone, target_audience, content_style, keywords, is_default, status, deleted, create_time, update_time)
SELECT 0, NULL, '健身达人', 'lifestyle', '家庭健身教练，居家减脂塑形', 'professional', '20-40岁男女', '专业讲解、动作示范', '健身,减脂,居家运动', 0, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM dy_persona WHERE owner_id = 0 AND persona_name = '健身达人' AND deleted = 0);

INSERT INTO dy_persona (owner_id, account_id, persona_name, persona_type, description, tone, target_audience, content_style, keywords, is_default, status, deleted, create_time, update_time)
SELECT 0, NULL, '美食探店', 'lifestyle', '本地美食探店，分享真实体验', 'casual', '18-35岁', '探店vlog、美食分享', '美食,探店,种草', 0, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM dy_persona WHERE owner_id = 0 AND persona_name = '美食探店' AND deleted = 0);

INSERT INTO dy_persona (owner_id, account_id, persona_name, persona_type, description, tone, target_audience, content_style, keywords, is_default, status, deleted, create_time, update_time)
SELECT 0, NULL, '知识科普', 'knowledge', '科普类内容创作者，通俗易懂', 'professional', '全年龄段', '知识讲解、科普', '知识,科普,教育', 0, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM dy_persona WHERE owner_id = 0 AND persona_name = '知识科普' AND deleted = 0);

INSERT INTO dy_persona (owner_id, account_id, persona_name, persona_type, description, tone, target_audience, content_style, keywords, is_default, status, deleted, create_time, update_time)
SELECT 0, NULL, '护肤品套盒过品', 'commerce', '护肤品套盒/套装讲解，成分分析、使用顺序与场景，适合直播过品、种草', 'professional', '22-40岁女性', '成分讲解、套盒搭配、使用教程', '护肤,套盒,精华,面霜,乳液,成分', 0, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM dy_persona WHERE owner_id = 0 AND persona_name = '护肤品套盒过品' AND deleted = 0);

INSERT INTO dy_persona (owner_id, account_id, persona_name, persona_type, description, tone, target_audience, content_style, keywords, is_default, status, deleted, create_time, update_time)
SELECT 0, NULL, '彩妆达人', 'commerce', '彩妆教程、妆容分享、产品测评，口红眼影等单品种草', 'warm', '18-35岁女性', '妆容教程、试色测评、日常妆分享', '彩妆,口红,眼影,妆容,教程', 0, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM dy_persona WHERE owner_id = 0 AND persona_name = '彩妆达人' AND deleted = 0);

-- 修正 sample-data 中 persona_type：food->lifestyle, fashion->commerce
-- 若已存在旧数据则更新（可选，按需执行）
-- UPDATE dy_persona SET persona_type = 'lifestyle' WHERE persona_type = 'food' AND deleted = 0;
-- UPDATE dy_persona SET persona_type = 'commerce' WHERE persona_type = 'fashion' AND deleted = 0;
