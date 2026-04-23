-- ============================================================
-- log 模块 - 数据库表结构
-- 模块：操作日志（Operation Log）
-- 数据库：PostgreSQL
-- 版本：1.0
-- 更新日期：2026-02-24
-- ============================================================

-- ============================================================
-- 1. sys_operation_log - 操作日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_operation_log (
    id              BIGSERIAL       PRIMARY KEY,
    trace_id        VARCHAR(64),                                        -- 请求追踪 ID
    user_id         BIGINT,                                             -- 操作用户 ID
    username        VARCHAR(64),                                        -- 用户名（冗余）
    module          VARCHAR(64),                                        -- 模块：auth / douyin / live ...
    action          VARCHAR(128),                                        -- 操作标识（如 login-logs / ban）
    request_uri     VARCHAR(256),                                       -- 请求 URI
    request_method  VARCHAR(16),                                        -- 请求方法 GET/POST
    ip              VARCHAR(64),                                        -- 操作 IP
    user_agent      VARCHAR(512),                                       -- 浏览器 UA
    duration_ms     INTEGER,                                            -- 耗时（毫秒）
    status          INTEGER          DEFAULT 1,                         -- 结果 0失败 1成功
    error_msg       TEXT,                                               -- 异常信息（如有）
    create_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP  -- 操作时间
);

-- 按用户+时间查询
CREATE INDEX IF NOT EXISTS idx_sys_operation_log_user_time ON sys_operation_log (user_id, create_time DESC);

-- 按模块+时间查询
CREATE INDEX IF NOT EXISTS idx_sys_operation_log_module_time ON sys_operation_log (module, create_time DESC);

COMMENT ON TABLE  sys_operation_log                  IS '操作日志表';
COMMENT ON COLUMN sys_operation_log.trace_id         IS '请求追踪 ID';
COMMENT ON COLUMN sys_operation_log.user_id          IS '操作用户 ID';
COMMENT ON COLUMN sys_operation_log.username         IS '用户名（冗余，方便查询）';
COMMENT ON COLUMN sys_operation_log.module           IS '模块：auth / douyin / live ...';
COMMENT ON COLUMN sys_operation_log.action           IS '操作标识（如 login-logs / ban）';
COMMENT ON COLUMN sys_operation_log.request_uri      IS '请求 URI';
COMMENT ON COLUMN sys_operation_log.request_method   IS '请求方法 GET/POST';
COMMENT ON COLUMN sys_operation_log.ip               IS '操作 IP';
COMMENT ON COLUMN sys_operation_log.user_agent       IS '浏览器 UA';
COMMENT ON COLUMN sys_operation_log.duration_ms      IS '耗时（毫秒）';
COMMENT ON COLUMN sys_operation_log.status           IS '结果 0失败 1成功';
COMMENT ON COLUMN sys_operation_log.error_msg        IS '异常信息（如有）';
COMMENT ON COLUMN sys_operation_log.create_time      IS '操作时间';

-- ============================================================
-- 2. sys_system_log - 系统日志表
-- Entity: cn.gaifan.douyinOperations.module.log.entity.SystemLog
-- 注意：无 deleted 字段，系统日志不支持逻辑删除
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_system_log (
    id          BIGSERIAL       PRIMARY KEY,
    module      VARCHAR(64),                                        -- 模块名称
    event_type  VARCHAR(32)     NOT NULL,                           -- 事件类型：startup / shutdown / error / warn / info
    summary     VARCHAR(256),                                       -- 事件摘要
    detail      TEXT,                                               -- 详细信息
    status      INTEGER         NOT NULL DEFAULT 1,                 -- 状态：0=失败 1=成功
    create_time TIMESTAMP                DEFAULT CURRENT_TIMESTAMP  -- 事件时间
);

CREATE INDEX IF NOT EXISTS idx_sys_system_log_module_time ON sys_system_log (module, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_sys_system_log_event_time  ON sys_system_log (event_type, create_time DESC);

COMMENT ON TABLE  sys_system_log             IS '系统日志表';
COMMENT ON COLUMN sys_system_log.module      IS '模块名称';
COMMENT ON COLUMN sys_system_log.event_type  IS '事件类型：startup / shutdown / error / warn / info';
COMMENT ON COLUMN sys_system_log.summary     IS '事件摘要';
COMMENT ON COLUMN sys_system_log.detail      IS '详细信息';
COMMENT ON COLUMN sys_system_log.status      IS '状态：0=失败 1=成功';
