-- ============================================================
-- 580AI 代理配置 (https://cc.580ai.net)
-- OpenAI 兼容 API，模型广场多模型接入
-- 执行：在 config/schema 之后
-- ============================================================

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'ai.580ai.api_url', 'https://cc.580ai.net', 'string', 0, 'ai', '580AI 代理 API 地址', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.580ai.api_url' AND deleted = 0);

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'ai.580ai.api_key', '', 'password', 1, 'ai', '580AI 令牌 (sk-xxx)', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.580ai.api_key' AND deleted = 0);
