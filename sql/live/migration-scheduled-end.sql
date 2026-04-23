-- scheduled_end_time: planned end time for create/edit session
ALTER TABLE live_session ADD COLUMN IF NOT EXISTS scheduled_end_time TIMESTAMP;
COMMENT ON COLUMN live_session.scheduled_end_time IS 'Planned end time';
