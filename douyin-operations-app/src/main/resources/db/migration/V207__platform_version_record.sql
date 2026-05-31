-- V207: 平台版本记录
CREATE TABLE IF NOT EXISTS sys_platform_version (
    id BIGSERIAL PRIMARY KEY,
    version VARCHAR(32) NOT NULL,
    release_date DATE NOT NULL DEFAULT CURRENT_DATE,
    description TEXT,
    migration_count INT DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO sys_platform_version (version, release_date, description, migration_count) VALUES
    ('1.0', '2025-06-01', 'dy02 初始版本', 80),
    ('2.0', '2026-05-30', 'dy05 基座升级: 多租户/产品目录/授权/GMV/治理/微服务拓扑', 207)
ON CONFLICT DO NOTHING;
