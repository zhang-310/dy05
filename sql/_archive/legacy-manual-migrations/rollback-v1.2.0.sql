-- ============================================================
-- 回滚脚本 - v1.2.0 → v1.1.0
-- ============================================================
-- 回滚直播模块增强功能
-- ============================================================

\echo '回滚 v1.2.0: 直播模块增强'

-- 删除直播产品数据表
DROP TABLE IF EXISTS live_product_data CASCADE;

-- 删除直播会话数据表
DROP TABLE IF EXISTS live_session_data CASCADE;

-- 删除 persona_id 字段
ALTER TABLE live_session DROP COLUMN IF EXISTS persona_id;

\echo 'v1.2.0 回滚完成'
