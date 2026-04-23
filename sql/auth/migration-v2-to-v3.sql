-- ============================================================
-- auth 模块 - v2.0 至 v3.0 迁移脚本
-- 执行条件：已存在 auth_login_log 表且为 v2.0 结构
-- 执行方式：psql -f migration-v2-to-v3.sql
-- ============================================================

-- 1. user_id 改为可空（失败登录时用户可能不存在）
ALTER TABLE auth_login_log ALTER COLUMN user_id DROP NOT NULL;

-- 2. 新增 username（冗余，方便查询）
ALTER TABLE auth_login_log ADD COLUMN IF NOT EXISTS username VARCHAR(64);

-- 3. 新增 status（1=成功 0=失败）
ALTER TABLE auth_login_log ADD COLUMN IF NOT EXISTS status INTEGER NOT NULL DEFAULT 1;

-- 4. 新增 fail_reason
ALTER TABLE auth_login_log ADD COLUMN IF NOT EXISTS fail_reason VARCHAR(256);

-- 5. 重建索引（user_id 可空后，原索引需调整）
DROP INDEX IF EXISTS idx_auth_login_log_user_time;
CREATE INDEX IF NOT EXISTS idx_auth_login_log_user_time ON auth_login_log (user_id, login_time DESC) WHERE user_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_auth_login_log_login_time ON auth_login_log (login_time DESC);

-- 6. 添加注释
COMMENT ON COLUMN auth_login_log.user_id   IS '用户 ID（失败时可为空）';
COMMENT ON COLUMN auth_login_log.username IS '用户名（冗余，失败时记录尝试的用户名）';
COMMENT ON COLUMN auth_login_log.status    IS '1=成功 0=失败';
COMMENT ON COLUMN auth_login_log.fail_reason IS '失败原因';
