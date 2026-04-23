-- 数据库索引优化脚本
CREATE INDEX IF NOT EXISTS idx_live_session_user_status ON live_session(user_id, status, deleted);
CREATE INDEX IF NOT EXISTS idx_live_session_owner_status ON live_session(owner_id, status, deleted);
CREATE INDEX IF NOT EXISTS idx_live_session_scheduled_time ON live_session(scheduled_time) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_douyin_video_account_status ON douyin_video(account_id, status, deleted);
CREATE INDEX IF NOT EXISTS idx_douyin_video_owner_status ON douyin_video(owner_id, status, deleted);
CREATE INDEX IF NOT EXISTS idx_douyin_video_publish_time ON douyin_video(publish_time) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_script_session_executed ON live_script(session_id, executed, deleted);
CREATE INDEX IF NOT EXISTS idx_live_product_session_order ON live_product(session_id, display_order, deleted);
CREATE INDEX IF NOT EXISTS idx_live_monitor_session_time ON live_monitor(session_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_auth_user_role_status ON auth_user(role_code, status, deleted);
CREATE INDEX IF NOT EXISTS idx_auth_login_log_user_time ON auth_login_log(user_id, login_time DESC);
CREATE INDEX IF NOT EXISTS idx_ai_viral_analysis_video_status ON ai_viral_analysis(video_id, status, deleted);
CREATE INDEX IF NOT EXISTS idx_attribution_session_type ON attribution(session_id, attribution_type, deleted);
CREATE INDEX IF NOT EXISTS idx_oauth_token_expires ON oauth_token(expires_at) WHERE deleted = 0;
SELECT '索引优化完成' AS status;
