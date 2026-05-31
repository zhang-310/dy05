-- V169: Operation log soft-delete column
-- Owner: platform/log module. Written by request/login logging filters; read by monitoring/log pages.

ALTER TABLE sys_operation_log
    ADD COLUMN IF NOT EXISTS deleted INTEGER NOT NULL DEFAULT 0;

UPDATE sys_operation_log
SET deleted = 0
WHERE deleted IS NULL;

CREATE INDEX IF NOT EXISTS idx_sys_operation_log_deleted_time
    ON sys_operation_log(deleted, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_sys_operation_log_module_deleted_time
    ON sys_operation_log(module, deleted, create_time DESC);
