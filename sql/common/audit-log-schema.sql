-- ============================================================
-- 审计日志表（与 AuditLog Entity 对齐）
-- Entity: cn.gaifan.douyinOperations.common.entity.AuditLog
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_log (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL DEFAULT 0,                  -- 操作用户 ID
    username    VARCHAR(100)    NOT NULL DEFAULT 'UNKNOWN',          -- 操作用户名
    action      VARCHAR(50)     NOT NULL,                            -- 操作类型：CREATE / UPDATE / DELETE
    entity      VARCHAR(100)    NOT NULL,                            -- 操作的实体类型
    entity_id   BIGINT          NOT NULL DEFAULT 0,                   -- 操作的实体 ID
    old_value   TEXT,                                                -- 修改前的值
    new_value   TEXT,                                                -- 修改后的值
    ip          VARCHAR(50)     NOT NULL DEFAULT 'UNKNOWN',          -- 客户端 IP
    user_agent  TEXT,                                                -- User-Agent
    status      INTEGER         NOT NULL DEFAULT 1,                   -- 1=成功 0=失败
    error_msg   TEXT,                                                -- 错误信息（失败时）
    create_time TIMESTAMP                DEFAULT CURRENT_TIMESTAMP   -- 创建时间
);

CREATE INDEX IF NOT EXISTS idx_audit_log_user_id ON audit_log (user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_action ON audit_log (action);
CREATE INDEX IF NOT EXISTS idx_audit_log_create_time ON audit_log (create_time);
CREATE INDEX IF NOT EXISTS idx_audit_log_username_created ON audit_log (username, create_time);
