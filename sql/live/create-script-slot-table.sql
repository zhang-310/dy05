-- ============================================================
-- Create live_session_script_slot table
-- PostgreSQL compatible version
-- ============================================================

CREATE TABLE IF NOT EXISTS live_session_script_slot (
  id BIGSERIAL PRIMARY KEY,
  live_session_id BIGINT NOT NULL,
  slot_index INTEGER NOT NULL,
  script_version_id BIGINT,
  content TEXT NOT NULL,
  duration_seconds INTEGER DEFAULT 120,
  script_type VARCHAR(32),
  style VARCHAR(64),
  is_current BOOLEAN DEFAULT false,
  is_completed BOOLEAN DEFAULT false,
  started_at TIMESTAMP,
  completed_at TIMESTAMP,
  owner_id BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_live_session_script_slot_session_id
  ON live_session_script_slot(live_session_id) WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_live_session_script_slot_session_slot
  ON live_session_script_slot(live_session_id, slot_index) WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_live_session_script_slot_is_current
  ON live_session_script_slot(live_session_id, is_current) WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_live_session_script_slot_owner_id
  ON live_session_script_slot(owner_id) WHERE deleted = 0;

COMMENT ON TABLE live_session_script_slot IS 'Live session script slot table - Records script slots for live sessions, supports navigation and countdown';
