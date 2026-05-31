-- V165: Content copy approval tenant owner backfill
-- Aligns older Docker databases with CopyApproval.ownerId JPA validation.

ALTER TABLE copy_approval ADD COLUMN IF NOT EXISTS owner_id BIGINT;

UPDATE copy_approval
SET owner_id = COALESCE(user_id, 0)
WHERE owner_id IS NULL;

ALTER TABLE copy_approval ALTER COLUMN owner_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_copy_approval_owner_id
    ON copy_approval(owner_id) WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_copy_approval_owner_status
    ON copy_approval(owner_id, approval_status) WHERE deleted = 0;
