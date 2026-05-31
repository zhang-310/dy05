-- V188: 用户偏好 + 会话上下文表
CREATE TABLE IF NOT EXISTS sys_user_preference (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(128) NOT NULL DEFAULT 'default',
    user_id BIGINT NOT NULL DEFAULT 0,
    pref_key VARCHAR(128) NOT NULL,
    pref_value TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT now(),
    update_time TIMESTAMP NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_pref ON sys_user_preference(tenant_id, user_id, pref_key);

CREATE TABLE IF NOT EXISTS sys_session_context (
    id BIGSERIAL PRIMARY KEY,
    session_token VARCHAR(256) NOT NULL UNIQUE,
    tenant_id VARCHAR(128) NOT NULL DEFAULT 'default',
    user_id BIGINT NOT NULL DEFAULT 0,
    role_code VARCHAR(32),
    org_id BIGINT,
    ip_address VARCHAR(64),
    expires_at TIMESTAMP NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_session_token ON sys_session_context(session_token);
CREATE INDEX IF NOT EXISTS idx_session_expires ON sys_session_context(expires_at);
