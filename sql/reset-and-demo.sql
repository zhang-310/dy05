-- ============================================================
-- 数据库重置并导入 Demo 数据
-- ============================================================
-- 执行前提：
-- 1. 关闭正在连接该库的应用（如 Spring Boot）
-- 2. 在项目根目录执行：psql -h localhost -p 5433 -U postgres -d douyin_operations -f sql/reset-and-demo.sql
-- 3. 或进入 psql 后：\c douyin_operations
--    \i sql/reset-and-demo.sql
-- ============================================================

\set QUIET off
\echo '========== 开始重置数据库 =========='

-- 删除 public 下所有对象（表、视图、序列、函数等）
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO public;

-- 恢复默认注释
COMMENT ON SCHEMA public IS 'standard public schema';

\echo '========== 执行完整初始化 =========='
\i sql/init-all.sql

\echo '========== 重置完成，登录账号: admin / admin123 =========='
