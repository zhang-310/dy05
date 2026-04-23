-- 新增 ai.query-rewrite.profile_override 配置
-- 用于覆盖进化/查询改写时的用户画像（解决 demo 账号「每日穿搭」与实际业务不符）
-- 执行：psql -U postgres -d dy01 -f sql/ai/migration-query-rewrite-profile-override.sql

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'ai.query-rewrite.profile_override', '', 'string', 0, 'ai', '查询改写/进化用户画像覆盖（留空则用抖音账号数据）。示例：粉丝数 50000，账号描述：护肤品套盒直播、直播间话术', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.query-rewrite.profile_override' AND deleted = 0);
