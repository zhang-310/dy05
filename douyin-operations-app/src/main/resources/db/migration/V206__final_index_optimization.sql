-- V206: 最终索引优化 — 针对高频查询路径
CREATE INDEX IF NOT EXISTS idx_session_account_status ON live_session(account_id, status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_script_session_type ON live_script(session_id, script_type) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_video_account_publish ON douyin_video(account_id, publish_time DESC) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_order_user_time ON payment_order(user_id, create_time DESC) WHERE deleted = 0;
