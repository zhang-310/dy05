-- ============================================================
-- Create live_session_realtime_data table
-- PostgreSQL compatible version
-- ============================================================

CREATE TABLE IF NOT EXISTS live_session_realtime_data (
  id BIGSERIAL PRIMARY KEY,
  live_session_id BIGINT NOT NULL,
  watched_count INTEGER DEFAULT 0,
  viewer_count INTEGER DEFAULT 0,
  like_count INTEGER DEFAULT 0,
  comment_count INTEGER DEFAULT 0,
  share_count INTEGER DEFAULT 0,
  follow_count INTEGER DEFAULT 0,
  gift_amount DECIMAL(10,2) DEFAULT 0.00,
  product_click_count INTEGER DEFAULT 0,
  product_purchase_count INTEGER DEFAULT 0,
  product_purchase_amount DECIMAL(10,2) DEFAULT 0.00,
  current_slot_index INTEGER,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_live_session_realtime_data_session_id
  ON live_session_realtime_data(live_session_id) WHERE deleted = 0;

COMMENT ON TABLE live_session_realtime_data IS 'Live session realtime data table - Stores real-time statistics for live sessions';
