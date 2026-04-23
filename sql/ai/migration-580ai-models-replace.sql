-- 替换 580ai 为更强模型（Claude Sonnet 4.x / GGG 5.x）
-- 1. 逻辑删除旧 580ai 模型
UPDATE ai_model SET deleted = 1, update_time = CURRENT_TIMESTAMP
WHERE model_provider = '580ai' AND deleted = 0;

-- 2. 插入新模型
INSERT INTO ai_model (model_name, model_provider, model_version, api_key, max_tokens, temperature, status, cost_per_1k_tokens, quota_limit, quota_used, deleted, create_time, update_time)
VALUES
  ('Claude-Sonnet-4.6-Thinking', '580ai', 'claude-sonnet-4-6-thinking', NULL, 32768, 0.70, 1, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('Claude-Sonnet-4.6', '580ai', 'claude-sonnet-4-6', NULL, 32768, 0.70, 1, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('Claude-Sonnet-4.5', '580ai', 'claude-sonnet-4-5-20250929', NULL, 32768, 0.70, 1, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('Claude-Sonnet-4-Thinking', '580ai', 'claude-sonnet-4-20250514-thinking', NULL, 32768, 0.70, 1, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('GGG-5.2', '580ai', 'ggg-5.2', NULL, 32768, 0.70, 1, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('GGG-5.1-Codex-Max-High', '580ai', 'ggg-5.1-codex-max-high', NULL, 32768, 0.70, 1, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
