-- ============================================================
-- system 模块 - 数据库表结构
-- 模块：系统监控（API调用日志、数据同步日志）
-- 数据库：PostgreSQL
-- 版本：1.0
-- 更新日期：2026-02-27
-- 说明：无 deleted 字段，日志数据通过定时任务物理清理
-- ============================================================

-- ============================================================
-- 1. sys_api_call_log — API 调用日志表
-- Entity: cn.gaifan.douyinOperations.module.system.entity.SysApiCallLog
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_api_call_log (
    id               BIGSERIAL       PRIMARY KEY,
    module           VARCHAR(64)     NOT NULL,                              -- 模块：douyin / ai / live
    api_name         VARCHAR(256)    NOT NULL,                              -- API 名称/路径
    request_url      VARCHAR(512),                                          -- 请求 URL
    request_method   VARCHAR(16),                                           -- 请求方法：GET / POST
    request_params   TEXT,                                                  -- 请求参数（脱敏后）
    response_status  INTEGER,                                               -- HTTP 响应状态码
    response_body    TEXT,                                                  -- 响应内容（截取前 2000 字符）
    status           INTEGER         NOT NULL DEFAULT 1,                    -- 调用状态：1=成功 0=失败
    error_message    VARCHAR(512),                                          -- 错误信息（失败时记录）
    duration_ms      BIGINT,                                                -- 调用耗时（毫秒）
    user_id          BIGINT,                                                -- 触发用户 ID
    create_time      TIMESTAMP                DEFAULT CURRENT_TIMESTAMP     -- 记录时间
);

CREATE INDEX IF NOT EXISTS idx_api_log_module_time ON sys_api_call_log (module, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_api_log_status_time ON sys_api_call_log (status, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_api_log_create_time ON sys_api_call_log (create_time);

COMMENT ON TABLE  sys_api_call_log                  IS 'API 调用日志表';
COMMENT ON COLUMN sys_api_call_log.module           IS '模块：douyin / ai / live';
COMMENT ON COLUMN sys_api_call_log.api_name         IS 'API 名称/路径';
COMMENT ON COLUMN sys_api_call_log.request_url      IS '请求 URL';
COMMENT ON COLUMN sys_api_call_log.request_method   IS '请求方法：GET / POST';
COMMENT ON COLUMN sys_api_call_log.request_params   IS '请求参数（脱敏后，不含密钥）';
COMMENT ON COLUMN sys_api_call_log.response_status  IS 'HTTP 响应状态码';
COMMENT ON COLUMN sys_api_call_log.response_body    IS '响应内容（截取前 2000 字符）';
COMMENT ON COLUMN sys_api_call_log.status           IS '调用状态：1=成功 0=失败';
COMMENT ON COLUMN sys_api_call_log.error_message    IS '错误信息（失败时记录）';
COMMENT ON COLUMN sys_api_call_log.duration_ms      IS '调用耗时（毫秒）';
COMMENT ON COLUMN sys_api_call_log.user_id          IS '触发用户 ID';

-- ============================================================
-- 2. sys_sync_log — 数据同步日志表
-- Entity: cn.gaifan.douyinOperations.module.system.entity.SysSyncLog
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_sync_log (
    id             BIGSERIAL       PRIMARY KEY,
    sync_type      VARCHAR(64)     NOT NULL,                              -- 同步类型：video_sync / live_data_sync / account_data_sync
    user_id        BIGINT,                                                -- 关联用户 ID
    account_id     BIGINT,                                                -- 关联抖音账号 ID
    status         VARCHAR(16)     NOT NULL DEFAULT 'running',            -- 状态：running / success / failed
    total_count    INTEGER                  DEFAULT 0,                    -- 总数据量
    success_count  INTEGER                  DEFAULT 0,                    -- 成功数量
    fail_count     INTEGER                  DEFAULT 0,                    -- 失败数量
    error_message  TEXT,                                                  -- 错误信息（失败时记录）
    start_time     TIMESTAMP,                                             -- 同步开始时间
    end_time       TIMESTAMP,                                             -- 同步结束时间
    create_time    TIMESTAMP                DEFAULT CURRENT_TIMESTAMP     -- 记录创建时间
);

CREATE INDEX IF NOT EXISTS idx_sync_log_type_time ON sys_sync_log (sync_type, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_sync_log_user_time ON sys_sync_log (user_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_sync_log_status    ON sys_sync_log (status, create_time DESC);

COMMENT ON TABLE  sys_sync_log                IS '数据同步日志表';
COMMENT ON COLUMN sys_sync_log.sync_type      IS '同步类型：video_sync / live_data_sync / account_data_sync';
COMMENT ON COLUMN sys_sync_log.user_id        IS '关联用户 ID';
COMMENT ON COLUMN sys_sync_log.account_id     IS '关联抖音账号 ID';
COMMENT ON COLUMN sys_sync_log.status         IS '状态：running=同步中 success=成功 failed=失败';
COMMENT ON COLUMN sys_sync_log.total_count    IS '总数据量';
COMMENT ON COLUMN sys_sync_log.success_count  IS '成功数量';
COMMENT ON COLUMN sys_sync_log.fail_count     IS '失败数量';
COMMENT ON COLUMN sys_sync_log.error_message  IS '错误信息（失败时记录）';
COMMENT ON COLUMN sys_sync_log.start_time     IS '同步开始时间';
COMMENT ON COLUMN sys_sync_log.end_time       IS '同步结束时间';
