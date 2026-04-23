-- ============================================================
-- 580AI 模型广场 - ai_model 表
-- 需在 sys_config 配置 ai.580ai.api_url 和 ai.580ai.api_key
-- 模型名称以 580ai 定价页为准，可自行增删
-- ============================================================

INSERT INTO ai_model (model_name, model_provider, model_version, api_key, max_tokens, temperature, status, cost_per_1k_tokens, quota_limit, quota_used, deleted, create_time, update_time)
SELECT 'Claude-Sonnet-4.6-Thinking', '580ai', 'claude-sonnet-4-6-thinking', NULL, 32768, 0.70, 1, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_model WHERE model_provider = '580ai' AND model_version = 'claude-sonnet-4-6-thinking' AND deleted = 0);

INSERT INTO ai_model (model_name, model_provider, model_version, api_key, max_tokens, temperature, status, cost_per_1k_tokens, quota_limit, quota_used, deleted, create_time, update_time)
SELECT 'Claude-Sonnet-4.6', '580ai', 'claude-sonnet-4-6', NULL, 32768, 0.70, 1, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_model WHERE model_provider = '580ai' AND model_version = 'claude-sonnet-4-6' AND deleted = 0);

INSERT INTO ai_model (model_name, model_provider, model_version, api_key, max_tokens, temperature, status, cost_per_1k_tokens, quota_limit, quota_used, deleted, create_time, update_time)
SELECT 'Claude-Opus-4.6', '580ai', 'claude-opus-4-6', NULL, 32768, 0.70, 1, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_model WHERE model_provider = '580ai' AND model_version = 'claude-opus-4-6' AND deleted = 0);

INSERT INTO ai_model (model_name, model_provider, model_version, api_key, max_tokens, temperature, status, cost_per_1k_tokens, quota_limit, quota_used, deleted, create_time, update_time)
SELECT 'Claude-Opus-4.6-Thinking', '580ai', 'claude-opus-4-6-thinking', NULL, 32768, 0.70, 1, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_model WHERE model_provider = '580ai' AND model_version = 'claude-opus-4-6-thinking' AND deleted = 0);

INSERT INTO ai_model (model_name, model_provider, model_version, api_key, max_tokens, temperature, status, cost_per_1k_tokens, quota_limit, quota_used, deleted, create_time, update_time)
SELECT 'Claude-Sonnet-4.5', '580ai', 'claude-sonnet-4-5-20250929', NULL, 32768, 0.70, 1, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_model WHERE model_provider = '580ai' AND model_version = 'claude-sonnet-4-5-20250929' AND deleted = 0);

INSERT INTO ai_model (model_name, model_provider, model_version, api_key, max_tokens, temperature, status, cost_per_1k_tokens, quota_limit, quota_used, deleted, create_time, update_time)
SELECT 'Claude-Sonnet-4-Thinking', '580ai', 'claude-sonnet-4-20250514-thinking', NULL, 32768, 0.70, 1, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_model WHERE model_provider = '580ai' AND model_version = 'claude-sonnet-4-20250514-thinking' AND deleted = 0);

INSERT INTO ai_model (model_name, model_provider, model_version, api_key, max_tokens, temperature, status, cost_per_1k_tokens, quota_limit, quota_used, deleted, create_time, update_time)
SELECT 'GGG-5.2', '580ai', 'ggg-5.2', NULL, 32768, 0.70, 1, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_model WHERE model_provider = '580ai' AND model_version = 'ggg-5.2' AND deleted = 0);

INSERT INTO ai_model (model_name, model_provider, model_version, api_key, max_tokens, temperature, status, cost_per_1k_tokens, quota_limit, quota_used, deleted, create_time, update_time)
SELECT 'GGG-5.1-Codex-Max-High', '580ai', 'ggg-5.1-codex-max-high', NULL, 32768, 0.70, 1, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_model WHERE model_provider = '580ai' AND model_version = 'ggg-5.1-codex-max-high' AND deleted = 0);

INSERT INTO ai_model (model_name, model_provider, model_version, api_key, max_tokens, temperature, status, cost_per_1k_tokens, quota_limit, quota_used, deleted, create_time, update_time)
SELECT 'Claude-Haiku-4.5', '580ai', 'claude-haiku-4-5-20251001', NULL, 32768, 0.70, 1, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_model WHERE model_provider = '580ai' AND model_version = 'claude-haiku-4-5-20251001' AND deleted = 0);

INSERT INTO ai_model (model_name, model_provider, model_version, api_key, max_tokens, temperature, status, cost_per_1k_tokens, quota_limit, quota_used, deleted, create_time, update_time)
SELECT 'Claude-Haiku-4.5-Thinking', '580ai', 'claude-haiku-4-5-20251001-thinking', NULL, 32768, 0.70, 1, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_model WHERE model_provider = '580ai' AND model_version = 'claude-haiku-4-5-20251001-thinking' AND deleted = 0);
