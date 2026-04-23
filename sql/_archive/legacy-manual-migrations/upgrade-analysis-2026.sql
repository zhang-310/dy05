-- ============================================================
-- 升级脚本：依据 docs/analysis 各模块深度分析（2026-03-05）
-- 执行前请备份数据库。幂等：使用 IF NOT EXISTS / ADD COLUMN IF NOT EXISTS
-- ============================================================

\echo '=== upgrade-analysis-2026: 开始 ==='

-- ============================================================
-- 1. auth 模块：索引优化（01-auth 分析）
-- ============================================================
\echo '1. auth 索引'

CREATE INDEX IF NOT EXISTS idx_auth_resource_type_module ON auth_resource(resource_type, module);
CREATE INDEX IF NOT EXISTS idx_auth_role_resource_res_id ON auth_role_resource(resource_id);

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'auth_verify_code') THEN
    CREATE INDEX IF NOT EXISTS idx_auth_verify_code_target ON auth_verify_code(target, type);
  END IF;
END $$;

-- ============================================================
-- 2. ai 模块：检索与日志索引（02-ai 分析）
-- ============================================================
\echo '2. ai 索引'

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'ai_kb_document') THEN
    CREATE INDEX IF NOT EXISTS idx_kb_document_kb_status ON ai_kb_document(kb_id, status, create_time DESC);
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'ai_call_log') THEN
    CREATE INDEX IF NOT EXISTS idx_call_log_user_time ON ai_call_log(user_id, create_time DESC);
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'ai_evolve_task') THEN
    CREATE INDEX IF NOT EXISTS idx_evolve_task_status ON ai_evolve_task(status, create_time DESC);
  END IF;
END $$;

-- ============================================================
-- 2b. ai 模块：进化调度器 DB 分布式锁表（Redis 不可用时备用）
-- ============================================================
\echo '2b. ai_evolve_scheduler_lock'

CREATE TABLE IF NOT EXISTS ai_evolve_scheduler_lock (
  lock_key   VARCHAR(64) PRIMARY KEY,
  lock_value VARCHAR(128) NOT NULL,
  expire_at  TIMESTAMP NOT NULL
);
COMMENT ON TABLE ai_evolve_scheduler_lock IS '进化引擎调度器分布式锁（Redis 不可用时 DB 备用）';
COMMENT ON COLUMN ai_evolve_scheduler_lock.expire_at IS '锁过期时间，超时后可被其他实例抢占';

-- ============================================================
-- 3. live 模块：逻辑删除字段（03-live 分析）
-- ============================================================
\echo '3. live deleted 列'

ALTER TABLE live_monitor ADD COLUMN IF NOT EXISTS deleted INTEGER NOT NULL DEFAULT 0;
COMMENT ON COLUMN live_monitor.deleted IS '逻辑删除：0=正常 1=已删除';

ALTER TABLE live_session_data ADD COLUMN IF NOT EXISTS deleted INTEGER NOT NULL DEFAULT 0;
COMMENT ON COLUMN live_session_data.deleted IS '逻辑删除：0=正常 1=已删除';

-- ============================================================
-- 4. shortvideo 模块：Dashboard 趋势查询索引（04-shortvideo 分析）
-- ============================================================
\echo '4. shortvideo 索引'

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'sv_video_data') THEN
    CREATE INDEX IF NOT EXISTS idx_sv_vd_view_delta ON sv_video_data(snapshot_date DESC, view_delta DESC);
  END IF;
END $$;

-- ============================================================
-- 5a. shortvideo 模块：sv_project 乐观锁 version 列（04 分析）
-- ============================================================
\echo '5a. sv_project version'

ALTER TABLE sv_project ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 1;
COMMENT ON COLUMN sv_project.version IS '乐观锁版本号，更新时自增';

-- ============================================================
-- 5. log 模块：request_body / response_body（06 基础设施分析，可选）
-- ============================================================
\echo '5. log 操作日志扩展字段'

ALTER TABLE sys_operation_log ADD COLUMN IF NOT EXISTS request_body VARCHAR(2000);
ALTER TABLE sys_operation_log ADD COLUMN IF NOT EXISTS response_body VARCHAR(2000);
COMMENT ON COLUMN sys_operation_log.request_body IS '请求体摘要（调试用，限制 2000 字符）';
COMMENT ON COLUMN sys_operation_log.response_body IS '响应体摘要（调试用，限制 2000 字符）';

-- ============================================================
-- 6. config 模块：配置版本历史表（06 基础设施分析）
-- ============================================================
\echo '6. sys_config_version_history'

CREATE TABLE IF NOT EXISTS sys_config_version_history (
  id           BIGSERIAL    PRIMARY KEY,
  config_id    BIGINT       NOT NULL,
  config_key   VARCHAR(128) NOT NULL,
  old_value    TEXT,
  new_value    TEXT,
  operator_id  BIGINT,
  create_time  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_config_version_config_id ON sys_config_version_history(config_id);
CREATE INDEX IF NOT EXISTS idx_config_version_create_time ON sys_config_version_history(create_time DESC);
COMMENT ON TABLE sys_config_version_history IS '配置变更历史（每次更新配置时写入一条）';
COMMENT ON COLUMN sys_config_version_history.operator_id IS '操作人用户 ID，可为空';

\echo '=== upgrade-analysis-2026: 完成 ==='
