-- Migration: 为 live_script / live_product 添加 user_id 列实现直接数据隔离
-- Phase 1.1: 修复安全漏洞 — 之前依赖 session 级联查询，可被越权访问
-- 执行前提: live_session 已有 user_id 列且 NOT NULL

-- ① live_script 添加 user_id
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS user_id BIGINT;

-- 回填: 从关联的 live_session 获取 user_id
UPDATE live_script s
SET user_id = (SELECT sess.user_id FROM live_session sess WHERE sess.id = s.session_id)
WHERE s.user_id IS NULL;

-- 设置 NOT NULL（回填完成后）
ALTER TABLE live_script ALTER COLUMN user_id SET NOT NULL;

-- 创建索引（配合 deleted 过滤）
CREATE INDEX IF NOT EXISTS idx_live_script_user
    ON live_script(user_id)
    WHERE deleted = 0;

-- ② live_product 添加 user_id
ALTER TABLE live_product ADD COLUMN IF NOT EXISTS user_id BIGINT;

-- 回填: 从关联的 live_session 获取 user_id
UPDATE live_product p
SET user_id = (SELECT sess.user_id FROM live_session sess WHERE sess.id = p.session_id)
WHERE p.user_id IS NULL;

-- 设置 NOT NULL（回填完成后）
ALTER TABLE live_product ALTER COLUMN user_id SET NOT NULL;

-- 创建索引（配合 deleted 过滤）
CREATE INDEX IF NOT EXISTS idx_live_product_user
    ON live_product(user_id)
    WHERE deleted = 0;
