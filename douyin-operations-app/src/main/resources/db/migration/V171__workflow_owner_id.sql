-- V171: Workflow owner columns
-- Owner: content/workflow module. Backfills old single-tenant workflow data to owner_id=1.

ALTER TABLE workflow_definition
    ADD COLUMN IF NOT EXISTS owner_id BIGINT;

UPDATE workflow_definition
SET owner_id = 1
WHERE owner_id IS NULL;

ALTER TABLE workflow_definition
    ALTER COLUMN owner_id SET DEFAULT 1;

ALTER TABLE workflow_definition
    ALTER COLUMN owner_id SET NOT NULL;

ALTER TABLE workflow_step
    ADD COLUMN IF NOT EXISTS owner_id BIGINT;

UPDATE workflow_step ws
SET owner_id = COALESCE(wd.owner_id, 1)
FROM workflow_definition wd
WHERE ws.definition_id = wd.id
  AND ws.owner_id IS NULL;

UPDATE workflow_step
SET owner_id = 1
WHERE owner_id IS NULL;

ALTER TABLE workflow_step
    ALTER COLUMN owner_id SET DEFAULT 1;

ALTER TABLE workflow_step
    ALTER COLUMN owner_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_workflow_def_owner
    ON workflow_definition(owner_id) WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_workflow_step_owner
    ON workflow_step(owner_id) WHERE deleted = 0;
