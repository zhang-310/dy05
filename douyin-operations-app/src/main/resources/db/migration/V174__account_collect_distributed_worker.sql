-- Distributed account short-video collection worker fields.
-- These columns let multiple app instances safely claim and resume collection tasks.
ALTER TABLE sv_account_collect_task ADD COLUMN IF NOT EXISTS max_count INTEGER;
ALTER TABLE sv_account_collect_task ADD COLUMN IF NOT EXISTS worker_id VARCHAR(128);
ALTER TABLE sv_account_collect_task ADD COLUMN IF NOT EXISTS worker_region VARCHAR(64);
ALTER TABLE sv_account_collect_task ADD COLUMN IF NOT EXISTS lease_until TIMESTAMP;
ALTER TABLE sv_account_collect_task ADD COLUMN IF NOT EXISTS claimed_at TIMESTAMP;
ALTER TABLE sv_account_collect_task ADD COLUMN IF NOT EXISTS started_at TIMESTAMP;
ALTER TABLE sv_account_collect_task ADD COLUMN IF NOT EXISTS finished_at TIMESTAMP;
ALTER TABLE sv_account_collect_task ADD COLUMN IF NOT EXISTS last_heartbeat_at TIMESTAMP;
ALTER TABLE sv_account_collect_task ADD COLUMN IF NOT EXISTS next_run_at TIMESTAMP;
ALTER TABLE sv_account_collect_task ADD COLUMN IF NOT EXISTS retry_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE sv_account_collect_task ADD COLUMN IF NOT EXISTS max_retry_count INTEGER NOT NULL DEFAULT 3;

UPDATE sv_account_collect_task
SET next_run_at = COALESCE(next_run_at, create_time, CURRENT_TIMESTAMP),
    retry_count = COALESCE(retry_count, 0),
    max_retry_count = COALESCE(max_retry_count, 3)
WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_sv_act_claim_pending
    ON sv_account_collect_task(status, next_run_at, create_time, id)
    WHERE deleted = 0 AND status = 'pending';

CREATE INDEX IF NOT EXISTS idx_sv_act_claim_expired
    ON sv_account_collect_task(status, lease_until, id)
    WHERE deleted = 0 AND status = 'collecting';

CREATE INDEX IF NOT EXISTS idx_sv_act_worker
    ON sv_account_collect_task(worker_id, status, lease_until)
    WHERE deleted = 0 AND worker_id IS NOT NULL;
