-- V202: 性能优化 — 复合索引 + 数据清理策略
CREATE INDEX IF NOT EXISTS idx_live_session_user_status ON live_session(user_id, status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_payment_order_user_status ON payment_order(user_id, status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_douyin_video_account_time ON douyin_video(account_id, publish_time DESC) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_auth_user_org ON auth_user(organization_id) WHERE deleted = 0;

-- 自动清理: sys_api_call_log 保留 90 天
COMMENT ON TABLE sys_api_call_log IS 'API 调用日志 — 自动清理策略: 90 天';
