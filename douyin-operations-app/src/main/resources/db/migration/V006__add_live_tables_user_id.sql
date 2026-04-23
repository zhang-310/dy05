-- V006: live 三表补充 user_id 字段，支持直接数据隔离
-- 从 live_session 级联填充已有记录

ALTER TABLE live_script ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE live_monitor ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE live_product ADD COLUMN IF NOT EXISTS user_id BIGINT;

-- 级联填充已有记录
UPDATE live_script ls SET user_id = (SELECT s.user_id FROM live_session s WHERE s.id = ls.session_id) WHERE ls.user_id IS NULL;
UPDATE live_monitor lm SET user_id = (SELECT s.user_id FROM live_session s WHERE s.id = lm.session_id) WHERE lm.user_id IS NULL;
UPDATE live_product lp SET user_id = (SELECT s.user_id FROM live_session s WHERE s.id = lp.session_id) WHERE lp.user_id IS NULL;

-- 索引
CREATE INDEX IF NOT EXISTS idx_live_script_user_session ON live_script(user_id, session_id);
CREATE INDEX IF NOT EXISTS idx_live_monitor_user_session ON live_monitor(user_id, session_id);
CREATE INDEX IF NOT EXISTS idx_live_product_user_session ON live_product(user_id, session_id);
