-- V186: 任务调度 + 通知记录表
CREATE TABLE IF NOT EXISTS sys_scheduled_task (
    id BIGSERIAL PRIMARY KEY,
    task_name VARCHAR(128) NOT NULL,
    cron_expression VARCHAR(64),
    last_run TIMESTAMP,
    next_run TIMESTAMP,
    status VARCHAR(16) NOT NULL DEFAULT 'active',
    create_time TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS sys_notification (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(128) NOT NULL DEFAULT 'default',
    user_id BIGINT NOT NULL DEFAULT 0,
    type VARCHAR(32) NOT NULL DEFAULT 'system',
    title VARCHAR(256) NOT NULL,
    content TEXT,
    channel VARCHAR(16) NOT NULL DEFAULT 'web',
    read_status BOOLEAN NOT NULL DEFAULT false,
    create_time TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_notification_tenant ON sys_notification(tenant_id, user_id, read_status);

-- 种子：调度任务注册
INSERT INTO sys_scheduled_task (task_name, cron_expression) VALUES
    ('morning_briefing', '0 30 7 * * ?'),
    ('session_health_check', '0 0 8 * * ?'),
    ('gmv_snapshot', '0 */30 * * * ?'),
    ('hourly_audit', '0 0 * * * ?')
ON CONFLICT DO NOTHING;
