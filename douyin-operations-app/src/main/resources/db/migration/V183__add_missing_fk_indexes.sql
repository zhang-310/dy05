-- ============================================
-- Sprint 1 P0-4: 缺失外键索引
-- ============================================

-- live_script 表高频查询字段索引
CREATE INDEX IF NOT EXISTS idx_live_script_session_id ON live_script(session_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_script_user_id ON live_script(user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_script_product_id ON live_script(product_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_script_approval_status ON live_script(approval_status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_script_effectiveness ON live_script(effectiveness_score DESC) WHERE deleted = 0 AND effectiveness_score IS NOT NULL;

-- live_session 表高频查询字段索引
CREATE INDEX IF NOT EXISTS idx_live_session_user_id ON live_session(user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_session_account_id ON live_session(account_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_session_status ON live_session(status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_session_scheduled_time ON live_session(scheduled_time) WHERE deleted = 0;

-- payment_order 表高频查询索引
CREATE INDEX IF NOT EXISTS idx_payment_order_session_id ON payment_order(session_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_payment_order_user_id ON payment_order(user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_payment_order_status ON payment_order(status) WHERE deleted = 0;

-- douyin_video 表高频查询索引
CREATE INDEX IF NOT EXISTS idx_douyin_video_account_id ON douyin_video(account_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_douyin_video_publish_time ON douyin_video(publish_time DESC) WHERE deleted = 0;
