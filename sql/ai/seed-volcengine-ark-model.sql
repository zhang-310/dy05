-- 与 Flyway V065 一致：手动执行或参考用。正式环境以 db/migration/V065__ai_model_seed_volcengine_ark.sql 为准。
-- 详见 docs/ai/00-火山方舟对接.md

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
