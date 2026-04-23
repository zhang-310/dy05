ALTER TABLE live_session ADD COLUMN IF NOT EXISTS audience_profile TEXT;
COMMENT ON COLUMN live_session.audience_profile IS 'JSON格式的目标受众画像';
