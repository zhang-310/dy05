-- V187: 操作日志归档 + API 调用日志表
CREATE TABLE IF NOT EXISTS sys_operation_log (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(128) NOT NULL DEFAULT 'default',
    user_id BIGINT NOT NULL DEFAULT 0,
    module VARCHAR(64) NOT NULL,
    operation VARCHAR(64) NOT NULL,
    target_id VARCHAR(128),
    target_type VARCHAR(64),
    detail TEXT,
    ip_address VARCHAR(64),
    user_agent VARCHAR(512),
    duration_ms BIGINT DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_oplog_tenant ON sys_operation_log(tenant_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_oplog_module ON sys_operation_log(module, operation);

CREATE TABLE IF NOT EXISTS sys_api_call_log (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(128) NOT NULL DEFAULT 'default',
    user_id BIGINT NOT NULL DEFAULT 0,
    method VARCHAR(8) NOT NULL,
    path VARCHAR(256) NOT NULL,
    query_string TEXT,
    status_code INT NOT NULL DEFAULT 200,
    duration_ms BIGINT DEFAULT 0,
    request_body TEXT,
    response_size BIGINT DEFAULT 0,
    ip_address VARCHAR(64),
    create_time TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_apilog_tenant ON sys_api_call_log(tenant_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_apilog_path ON sys_api_call_log(path, status_code);
