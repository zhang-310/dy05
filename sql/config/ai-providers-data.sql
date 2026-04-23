-- ============================================================
-- AI 第三方服务配置 - 后台「配置管理」中维护
-- 除 DB/ES/Milvus/Redis 外，所有 AI 相关配置均在此
-- 执行：在 config/schema 之后，demo-all 或 init 时执行
-- ============================================================

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'ai.glm.api_url', 'https://open.bigmodel.cn/api/paas/v4', 'string', 0, 'ai', '智谱 GLM / OpenClaw API 地址', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.glm.api_url' AND deleted = 0);
INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'ai.glm.api_key', '', 'password', 1, 'ai', '智谱 GLM / OpenClaw API Key', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.glm.api_key' AND deleted = 0);
INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'ai.minimax.api_url', 'https://api.minimaxi.com/v1', 'string', 0, 'ai', 'Minimax API 地址', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.minimax.api_url' AND deleted = 0);
INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'ai.minimax.api_key', '', 'password', 1, 'ai', 'Minimax API Key', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.minimax.api_key' AND deleted = 0);
INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'ai.deepseek.api_url', 'https://api.deepseek.com', 'string', 0, 'ai', 'DeepSeek API 地址', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.deepseek.api_url' AND deleted = 0);
INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'ai.deepseek.api_key', '', 'password', 1, 'ai', 'DeepSeek API Key', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.deepseek.api_key' AND deleted = 0);
INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'ai.ollama.url', 'http://localhost:11434', 'string', 0, 'ai', 'Ollama 本地服务地址', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.ollama.url' AND deleted = 0);
INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'ai.embedding.model', 'qwen3-embedding:0.6b', 'string', 0, 'ai', 'Embedding 模型（Ollama，1024 维）', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.embedding.model' AND deleted = 0);
-- 将已有 4b 配置改为 0.6b（与 app.ai.embedding.dimension:1024 匹配）
UPDATE sys_config SET config_value = 'qwen3-embedding:0.6b', remark = 'Embedding 模型（Ollama，1024 维）', update_time = CURRENT_TIMESTAMP
WHERE config_key = 'ai.embedding.model' AND deleted = 0 AND config_value = 'qwen3-embedding:4b';
