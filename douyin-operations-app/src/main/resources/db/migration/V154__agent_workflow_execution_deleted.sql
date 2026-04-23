-- V154: agent_workflow_execution 表补充 deleted 字段
-- Hibernate schema-validation 要求所有逻辑删除表有 deleted 字段

ALTER TABLE agent_workflow_execution ADD COLUMN IF NOT EXISTS deleted INTEGER NOT NULL DEFAULT 0;
CREATE INDEX IF NOT EXISTS idx_execution_deleted ON agent_workflow_execution(deleted) WHERE deleted = 0;
