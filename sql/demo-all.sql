-- ============================================================
-- 全量 Demo 数据导入脚本
-- 执行顺序：在 init-all.sql（表结构+资源树）之后执行
-- 用途：开发/演示环境快速填充示例数据
-- 默认账号：admin / admin123
-- ============================================================

\set QUIET on
\echo '========== 开始导入 Demo 数据 =========='

-- ============================================================
-- 0. 管理员用户（必须最先插入，其他表依赖 user_id=1）
-- ============================================================
INSERT INTO auth_user (username, password_hash, nickname, role_code, status, deleted, create_time, update_time)
SELECT 'admin', '$2a$10$tEp/V/6Ij98O6LgLsCjDXetlMR./FCXgfMcIRB49BcqI/jBnM/5yS', '管理员', 'admin', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM auth_user WHERE username = 'admin' AND deleted = 0);

-- 演示用户（copy 模块部分数据依赖 user_id=2）
INSERT INTO auth_user (username, password_hash, nickname, role_code, status, deleted, create_time, update_time)
SELECT 'demo', '$2a$10$tEp/V/6Ij98O6LgLsCjDXetlMR./FCXgfMcIRB49BcqI/jBnM/5yS', '演示用户', 'user', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM auth_user WHERE username = 'demo' AND deleted = 0);

-- ============================================================
-- 1. Config - AI 第三方配置 + 配置分组 + 配置项 + 行业分类
-- ============================================================
\i sql/config/ai-providers-data.sql
\i sql/config/bos-storage-data.sql
\i sql/ai/ai-model-data.sql
INSERT INTO sys_config_group (parent_id, group_code, group_name, sort_order, is_system, status, deleted)
SELECT v.parent_id, v.group_code, v.group_name, v.sort_order, v.is_system, v.status, v.deleted
FROM (VALUES (0::bigint, 'basic', '基础设置', 1, 1, 1, 0),
       (0::bigint, 'storage', '存储配置', 2, 1, 1, 0),
       (0::bigint, 'security', '安全配置', 3, 1, 1, 0)) AS v(parent_id, group_code, group_name, sort_order, is_system, status, deleted)
WHERE NOT EXISTS (SELECT 1 FROM sys_config_group g WHERE g.group_code = v.group_code AND g.deleted = 0);

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted)
SELECT 'site.name', '抖音运营平台', 'string', 0, 'system', '平台显示名称', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'site.name' AND deleted = 0);

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted)
SELECT 'upload.max_size', '10485760', 'number', 0, 'storage', '上传大小限制（字节）', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'upload.max_size' AND deleted = 0);

INSERT INTO sys_industry (parent_id, industry_name, industry_code, sort_order, status, deleted)
SELECT v.parent_id, v.industry_name, v.industry_code, v.sort_order, v.status, v.deleted
FROM (VALUES (0::bigint, '美食', 'food', 1, 1, 0),
       (0::bigint, '穿搭', 'fashion', 2, 1, 0),
       (0::bigint, '美妆', 'beauty', 3, 1, 0),
       (0::bigint, '数码', 'digital', 4, 1, 0)) AS v(parent_id, industry_name, industry_code, sort_order, status, deleted)
WHERE NOT EXISTS (SELECT 1 FROM sys_industry si WHERE si.industry_code = v.industry_code AND si.deleted = 0);

\echo 'Config demo done.'

-- ============================================================
-- 2. Douyin - 抖音账号、视频、人设
-- ============================================================
INSERT INTO douyin_account (user_id, account_name, account_id, follow_count, fan_count, video_count, total_likes, description, status, bind_time, deleted, create_time, update_time)
SELECT id, '美食探店达人', 'dy_food_001', 120, 58000, 86, 320000, '专注美食探店', 1, CURRENT_TIMESTAMP - INTERVAL '30 days', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM auth_user
WHERE username = 'admin' AND deleted = 0 AND NOT EXISTS (SELECT 1 FROM douyin_account WHERE account_id = 'dy_food_001' AND deleted = 0)
LIMIT 1;

INSERT INTO douyin_account (user_id, account_name, account_id, follow_count, fan_count, video_count, total_likes, description, status, bind_time, deleted, create_time, update_time)
SELECT id, '穿搭种草官', 'dy_fashion_002', 85, 32000, 52, 180000, '每日穿搭分享', 1, CURRENT_TIMESTAMP - INTERVAL '20 days', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM auth_user
WHERE username = 'admin' AND deleted = 0 AND NOT EXISTS (SELECT 1 FROM douyin_account WHERE account_id = 'dy_fashion_002' AND deleted = 0)
LIMIT 1;

INSERT INTO douyin_video (account_id, video_id, title, description, view_count, like_count, share_count, comment_count, download_count, video_type, publish_time, deleted, create_time, update_time)
SELECT a.id, 'v_001', '探店｜人均50的宝藏日料', '隐藏的日料小店', 125000, 8600, 1200, 560, 320, 'normal', CURRENT_TIMESTAMP - INTERVAL '5 days', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM douyin_account a
WHERE a.account_id = 'dy_food_001' AND a.deleted = 0 AND NOT EXISTS (SELECT 1 FROM douyin_video WHERE video_id = 'v_001' AND deleted = 0)
LIMIT 1;

INSERT INTO douyin_video (account_id, video_id, title, description, view_count, like_count, share_count, comment_count, download_count, video_type, publish_time, deleted, create_time, update_time)
SELECT a.id, 'v_002', '这家火锅真的绝了', '牛油锅底配鲜切毛肚', 89000, 5200, 800, 380, 210, 'normal', CURRENT_TIMESTAMP - INTERVAL '3 days', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM douyin_account a
WHERE a.account_id = 'dy_food_001' AND a.deleted = 0 AND NOT EXISTS (SELECT 1 FROM douyin_video WHERE video_id = 'v_002' AND deleted = 0)
LIMIT 1;

INSERT INTO douyin_video (account_id, video_id, title, description, view_count, like_count, share_count, comment_count, download_count, video_type, publish_time, deleted, create_time, update_time)
SELECT a.id, 'v_003', '秋冬穿搭｜小个子显高秘诀', '155穿出170', 67000, 4100, 650, 290, 180, 'normal', CURRENT_TIMESTAMP - INTERVAL '2 days', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM douyin_account a
WHERE a.account_id = 'dy_fashion_002' AND a.deleted = 0 AND NOT EXISTS (SELECT 1 FROM douyin_video WHERE video_id = 'v_003' AND deleted = 0)
LIMIT 1;

INSERT INTO dy_persona (owner_id, account_id, persona_name, persona_type, description, tone, target_audience, content_style, keywords, is_default, status, deleted, create_time, update_time)
SELECT 1, (SELECT id FROM douyin_account WHERE account_id = 'dy_food_001' AND deleted = 0 LIMIT 1), '吃货小姐姐', 'lifestyle', '热爱美食的90后', 'casual', '18-35岁女性', '探店vlog', '美食,探店', 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE EXISTS (SELECT 1 FROM auth_user WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM dy_persona WHERE persona_name = '吃货小姐姐' AND owner_id = 1 AND deleted = 0);

\echo 'Douyin demo done.'

-- ============================================================
-- 3. Product - 商品（live 依赖 product_id 1,2,3）
-- ============================================================
\i sql/product/sample-data.sql

\echo 'Product demo done.'

-- ============================================================
-- 4. Copy - 文案库
-- ============================================================
\i sql/copy/sample-data.sql

-- ============================================================
-- 5. Script - 话术库
-- ============================================================
\i sql/script/sample-data.sql

-- ============================================================
-- 6. Live - 直播场次（依赖 douyin_account、dy_product）
-- ============================================================
\i sql/live/sample-data.sql

-- ============================================================
-- 7. Shortvideo - 短视频
-- ============================================================
\i sql/shortvideo/sample-data.sql

-- ============================================================
-- 8. ABTest
-- ============================================================
\i sql/abtest/sample-data.sql

-- ============================================================
-- 9. Agent（需 user_id=1）
-- ============================================================
\i sql/agent/sample-data.sql

-- ============================================================
-- 10. Wecom（需 owner_id=1）
-- ============================================================
\i sql/wecom/sample-data.sql

\echo '========== Demo 数据导入完成 =========='
\echo '登录账号: admin / admin123'
