-- ============================================================
-- AI 模型配置 - ai_model 表初始数据
-- 进化引擎需 Ollama 模型；云端模型需在系统配置中填写 API Key
-- 执行：在 ai/schema 之后，demo-all 或 init 时执行
-- ============================================================

-- Ollama 本地模型（进化引擎、知识生成）
INSERT INTO ai_model (model_name, model_provider, model_version, api_key, max_tokens, temperature, status, cost_per_1k_tokens, quota_limit, quota_used, deleted, create_time, update_time)
SELECT 'Qwen2.5-7B', 'ollama', 'qwen2.5:7b', NULL, 4096, 0.70, 1, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_model WHERE model_provider = 'ollama' AND model_version = 'qwen2.5:7b' AND deleted = 0);

INSERT INTO ai_model (model_name, model_provider, model_version, api_key, max_tokens, temperature, status, cost_per_1k_tokens, quota_limit, quota_used, deleted, create_time, update_time)
SELECT 'DeepSeek-R1-7B', 'ollama', 'deepseek-r1:7b', NULL, 4096, 0.70, 1, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_model WHERE model_provider = 'ollama' AND model_version = 'deepseek-r1:7b' AND deleted = 0);

INSERT INTO ai_model (model_name, model_provider, model_version, api_key, max_tokens, temperature, status, cost_per_1k_tokens, quota_limit, quota_used, deleted, create_time, update_time)
SELECT 'Qwen2.5-14B', 'ollama', 'qwen2.5:14b', NULL, 4096, 0.70, 1, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_model WHERE model_provider = 'ollama' AND model_version = 'qwen2.5:14b' AND deleted = 0);

-- 注：Ollama 无 deepseek-v3:7b，使用 deepseek-llm:7b（7B 参数，Ollama 官方支持）
INSERT INTO ai_model (model_name, model_provider, model_version, api_key, max_tokens, temperature, status, cost_per_1k_tokens, quota_limit, quota_used, deleted, create_time, update_time)
SELECT 'DeepSeek-LLM-7B', 'ollama', 'deepseek-llm:7b', NULL, 4096, 0.70, 1, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_model WHERE model_provider = 'ollama' AND model_version = 'deepseek-llm:7b' AND deleted = 0);

-- DeepSeek 云端（聊天模型 + 深度思考模型，共用 ai.deepseek.api_key）
INSERT INTO ai_model (model_name, model_provider, model_version, api_key, max_tokens, temperature, status, cost_per_1k_tokens, quota_limit, quota_used, deleted, create_time, update_time)
SELECT 'DeepSeek-Chat', 'deepseek', 'deepseek-chat', NULL, 8192, 0.70, 1, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_model WHERE model_provider = 'deepseek' AND model_version = 'deepseek-chat' AND deleted = 0);

INSERT INTO ai_model (model_name, model_provider, model_version, api_key, max_tokens, temperature, status, cost_per_1k_tokens, quota_limit, quota_used, deleted, create_time, update_time)
SELECT 'DeepSeek-Reasoner', 'deepseek', 'deepseek-reasoner', NULL, 32768, 0.70, 1, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_model WHERE model_provider = 'deepseek' AND model_version = 'deepseek-reasoner' AND deleted = 0);

-- 智谱 OpenClaw（共用 ai.glm.api_key）
INSERT INTO ai_model (model_name, model_provider, model_version, api_key, max_tokens, temperature, status, cost_per_1k_tokens, quota_limit, quota_used, deleted, create_time, update_time)
SELECT 'OPEN_CLAW GLM-4-Plus', 'glm', 'glm-4-plus', NULL, 8192, 0.70, 1, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_model WHERE model_provider = 'glm' AND model_version = 'glm-4-plus' AND deleted = 0);

INSERT INTO ai_model (model_name, model_provider, model_version, api_key, max_tokens, temperature, status, cost_per_1k_tokens, quota_limit, quota_used, deleted, create_time, update_time)
SELECT 'OPEN_CLAW GLM-5', 'glm', 'glm-5', NULL, 8192, 0.70, 1, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_model WHERE model_provider = 'glm' AND model_version = 'glm-5' AND deleted = 0);

INSERT INTO ai_model (model_name, model_provider, model_version, api_key, max_tokens, temperature, status, cost_per_1k_tokens, quota_limit, quota_used, deleted, create_time, update_time)
SELECT 'OPEN_CLAW GLM-4-Flash', 'glm', 'glm-4-flash', NULL, 8192, 0.70, 1, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_model WHERE model_provider = 'glm' AND model_version = 'glm-4-flash' AND deleted = 0);

-- Minimax（共用 ai.minimax.api_key）
INSERT INTO ai_model (model_name, model_provider, model_version, api_key, max_tokens, temperature, status, cost_per_1k_tokens, quota_limit, quota_used, deleted, create_time, update_time)
SELECT 'Minimax abab6.5s-chat', 'minimax', 'abab6.5s-chat', NULL, 8192, 0.70, 1, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_model WHERE model_provider = 'minimax' AND model_version = 'abab6.5s-chat' AND deleted = 0);

INSERT INTO ai_model (model_name, model_provider, model_version, api_key, max_tokens, temperature, status, cost_per_1k_tokens, quota_limit, quota_used, deleted, create_time, update_time)
SELECT 'Minimax abab6.5-chat', 'minimax', 'abab6.5-chat', NULL, 8192, 0.70, 1, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_model WHERE model_provider = 'minimax' AND model_version = 'abab6.5-chat' AND deleted = 0);
