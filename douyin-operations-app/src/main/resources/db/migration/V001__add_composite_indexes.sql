-- V001: 添加复合索引优化常见查询模式
-- 所有查询都需要 deleted=0 过滤 + owner 隔离

-- live_session: 按 user + status 查询
CREATE INDEX IF NOT EXISTS idx_live_session_user_status_del ON live_session(user_id, status, deleted);
CREATE INDEX IF NOT EXISTS idx_live_session_account_status_del ON live_session(account_id, status, deleted);

-- live_script: 按 user + session 查询
CREATE INDEX IF NOT EXISTS idx_live_script_user_session_del ON live_script(user_id, session_id, deleted);
CREATE INDEX IF NOT EXISTS idx_live_script_session_sort_del ON live_script(session_id, sort_order, deleted);

-- live_monitor: 时间序列数据优化
CREATE INDEX IF NOT EXISTS idx_live_monitor_session_time ON live_monitor(session_id, timestamp DESC);

-- dy_product: 按 user + status + category 查询
CREATE INDEX IF NOT EXISTS idx_product_user_status_del ON dy_product(user_id, status, deleted);
CREATE INDEX IF NOT EXISTS idx_product_user_category_del ON dy_product(user_id, product_category, deleted);

-- ai_knowledge_base: 按 owner 查询
CREATE INDEX IF NOT EXISTS idx_ai_kb_owner_status_del ON ai_knowledge_base(owner_id, status, deleted);

-- ai_kb_document: 使用部分索引优化
DROP INDEX IF EXISTS idx_kb_document_deleted;
CREATE INDEX IF NOT EXISTS idx_ai_kb_doc_kb_status ON ai_kb_document(kb_id, status) WHERE deleted = 0;

-- auth_user: 登录查询优化
CREATE INDEX IF NOT EXISTS idx_auth_user_username_del ON auth_user(username, deleted);
CREATE INDEX IF NOT EXISTS idx_auth_user_mobile_del ON auth_user(mobile, deleted);

-- sv_video_data: 按 user + status 查询
CREATE INDEX IF NOT EXISTS idx_sv_video_user_status_del ON sv_video_data(user_id, status, deleted);

-- sc_script: 话术查询优化
CREATE INDEX IF NOT EXISTS idx_sc_script_user_type_del ON sc_script(user_id, script_type, deleted);
