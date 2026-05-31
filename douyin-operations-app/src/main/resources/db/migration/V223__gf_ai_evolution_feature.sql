-- AI 基座：Evolve 商业化 featureCode（gf_feature；sys_feature 可能未迁移时仅写 gf_*）
INSERT INTO gf_feature (feature_code, product_code, feature_name, quota_unit, monthly_limit, enabled)
VALUES ('ai.evolution.run', 'douyin-ops', 'AI 进化任务', 'call', 500, true)
ON CONFLICT (feature_code) DO NOTHING;

DO $$
BEGIN
    IF to_regclass('public.sys_feature') IS NOT NULL THEN
        INSERT INTO sys_feature (code, product_code, name, quota_unit, monthly_limit) VALUES
            ('ai.evolution.run', 'douyin-ops', 'AI 进化任务', 'call', 500)
        ON CONFLICT (code) DO NOTHING;
    END IF;
END $$;
