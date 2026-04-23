-- 火山方舟：可选模板行（默认停用）。启用前请在控制台创建 Endpoint，将 model_version 改为 ep-xxxx，并配置 ARK_API_KEY 或 ai_model.api_key。
INSERT INTO ai_model (model_name, model_provider, model_version, api_key, max_tokens, temperature, status, cost_per_1k_tokens, quota_limit, quota_used, deleted, create_time, update_time)
SELECT '火山方舟（请改为 Endpoint ID）',
       'volcengine',
       'ep-replace-in-console',
       NULL,
       8192,
       0.70,
       0,
       0,
       0,
       0,
       0,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM ai_model WHERE deleted = 0 AND LOWER(model_provider) IN ('volcengine', 'ark', 'volcano-ark', 'volcengine-ark')
);
