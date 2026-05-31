-- V200: 里程碑 — 产品基座完成
-- 记录本轮升级交付的关键能力

INSERT INTO sys_dashboard_cache (tenant_id, cache_key, cache_value, expires_at) VALUES
    ('default', 'platform:version', '{"version":"2.0","upgraded":"2026-05","modules":193,"controllers":190,"ports":10}',
     now() + interval '365 days')
ON CONFLICT DO NOTHING;

INSERT INTO sys_metric_snapshot (tenant_id, metric_name, metric_value, dimension) VALUES
    ('default', 'platform.completion', 88.0, 'overall'),
    ('default', 'platform.flyway', 200.0, 'migrations'),
    ('default', 'platform.controllers', 190.0, 'endpoints'),
    ('default', 'platform.ports', 10.0, 'contract_spi'),
    ('default', 'platform.tests', 21.0, 'unit_tests')
ON CONFLICT DO NOTHING;
