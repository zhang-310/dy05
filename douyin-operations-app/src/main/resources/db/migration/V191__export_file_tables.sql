-- V191: 数据导出任务 + 文件上传记录
CREATE TABLE IF NOT EXISTS sys_export_task (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(128) NOT NULL DEFAULT 'default',
    user_id BIGINT NOT NULL DEFAULT 0,
    type VARCHAR(32) NOT NULL,
    params TEXT,
    status VARCHAR(16) NOT NULL DEFAULT 'pending',
    file_path VARCHAR(512),
    file_size BIGINT DEFAULT 0,
    error_msg TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT now(),
    complete_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_export_tenant ON sys_export_task(tenant_id, status);

CREATE TABLE IF NOT EXISTS sys_file_record (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(128) NOT NULL DEFAULT 'default',
    user_id BIGINT NOT NULL DEFAULT 0,
    original_name VARCHAR(512) NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    storage_type VARCHAR(32) DEFAULT 'minio',
    file_size BIGINT DEFAULT 0,
    mime_type VARCHAR(128),
    upload_status VARCHAR(16) DEFAULT 'completed',
    create_time TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_file_tenant ON sys_file_record(tenant_id, create_time DESC);
