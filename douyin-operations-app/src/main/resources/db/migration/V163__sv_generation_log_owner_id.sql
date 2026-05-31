-- Owner: shortvideo-center
-- Purpose: attach generated media history to the current owner so BGM/SFX history is not global or mocked.

ALTER TABLE sv_generation_log ADD COLUMN IF NOT EXISTS owner_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_sv_generation_log_owner_content_time
    ON sv_generation_log(owner_id, content_type, create_time DESC);

COMMENT ON COLUMN sv_generation_log.owner_id IS 'Owner/user id for generated media history isolation';
