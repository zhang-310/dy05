-- V170: System log soft-delete column
-- Owner: platform/log module. Written by system logging; read by monitoring/log pages.

ALTER TABLE sys_system_log
    ADD COLUMN IF NOT EXISTS deleted INTEGER NOT NULL DEFAULT 0;

UPDATE sys_system_log
SET deleted = 0
WHERE deleted IS NULL;

CREATE INDEX IF NOT EXISTS idx_sys_system_log_deleted_time
    ON sys_system_log(deleted, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_sys_system_log_module_deleted_time
    ON sys_system_log(module, deleted, create_time DESC);
