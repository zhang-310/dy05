-- 抖音后台行业分类对齐：美妆下一级类目（供与抖音电商后台一致）
-- 执行：docker cp sql/config/migration-douyin-industry-align.sql dy-postgres:/tmp/; docker exec dy-postgres psql -U postgres -d douyin_operations -f /tmp/migration-douyin-industry-align.sql

-- 确保美妆护肤(beauty)存在
INSERT INTO sys_industry (parent_id, industry_name, industry_code, sort_order, status, deleted)
SELECT 0, '美妆护肤', 'beauty', 1, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_industry WHERE industry_code = 'beauty' AND deleted = 0);

-- 抖音一级类目：彩妆/香水/美妆工具（若已有 makeup 彩妆 则跳过，否则新增更完整名称）
INSERT INTO sys_industry (parent_id, industry_name, industry_code, sort_order, status, deleted)
SELECT (SELECT id FROM sys_industry WHERE industry_code = 'beauty' AND deleted = 0 LIMIT 1),
       '彩妆/香水/美妆工具', 'makeup_perfume_tools', 1, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_industry WHERE industry_code = 'makeup_perfume_tools' AND deleted = 0);

-- 抖音一级类目：美容护肤
INSERT INTO sys_industry (parent_id, industry_name, industry_code, sort_order, status, deleted)
SELECT (SELECT id FROM sys_industry WHERE industry_code = 'beauty' AND deleted = 0 LIMIT 1),
       '美容护肤', 'skincare', 2, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_industry WHERE industry_code = 'skincare' AND deleted = 0);

-- 抖音一级类目：美容/个护仪器
INSERT INTO sys_industry (parent_id, industry_name, industry_code, sort_order, status, deleted)
SELECT (SELECT id FROM sys_industry WHERE industry_code = 'beauty' AND deleted = 0 LIMIT 1),
       '美容/个护仪器', 'beauty_instrument', 3, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_industry WHERE industry_code = 'beauty_instrument' AND deleted = 0);

-- 保留原有：彩妆(makeup)、护肤品套盒(skincare_set) 用于直播套盒甩货场景
