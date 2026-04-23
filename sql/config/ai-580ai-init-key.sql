-- 580AI 令牌初始化（请勿提交到版本库，或使用后旋转密钥）
-- 执行：psql ... -f sql/config/ai-580ai-init-key.sql

UPDATE sys_config SET config_value = 'https://cc.580ai.net', update_time = CURRENT_TIMESTAMP
WHERE config_key = 'ai.580ai.api_url' AND deleted = 0;

UPDATE sys_config SET config_value = 'sk-wOk0CBGD5J391XxWZiXythjQx6v8BVa7lbpbE1K5QmVcsWJj', update_time = CURRENT_TIMESTAMP
WHERE config_key = 'ai.580ai.api_key' AND deleted = 0;
