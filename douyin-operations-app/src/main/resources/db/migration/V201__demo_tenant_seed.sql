-- V201: 数据初始化 — 演示租户种子数据
INSERT INTO sys_credit_account (tenant_id, user_id, balance) VALUES
    ('default', 1, 100000),
    ('default', 2, 50000)
ON CONFLICT DO NOTHING;

INSERT INTO sys_subscription (tenant_id, user_id, product_code, plan_code, status, valid_from, valid_to)
VALUES ('default', 1, 'douyin-ops', 'pro', 'active', now(), now() + interval '365 days')
ON CONFLICT DO NOTHING;

INSERT INTO sys_quota (tenant_id, feature_code, monthly_limit, monthly_used) VALUES
    ('default', 'ai.chat', 5000, 0),
    ('default', 'ai.generation', 1000, 0),
    ('default', 'douyin-ops.live-script', 200, 0)
ON CONFLICT DO NOTHING;
