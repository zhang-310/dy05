-- sys_operation_log 表结构对齐 OperationLog Entity
-- 用于已存在旧表结构的数据库，新库由 schema.sql 直接创建正确结构

ALTER TABLE sys_operation_log ADD COLUMN IF NOT EXISTS trace_id VARCHAR(64);
ALTER TABLE sys_operation_log ADD COLUMN IF NOT EXISTS action VARCHAR(128);
ALTER TABLE sys_operation_log ADD COLUMN IF NOT EXISTS request_uri VARCHAR(256);
ALTER TABLE sys_operation_log ADD COLUMN IF NOT EXISTS request_method VARCHAR(16);
ALTER TABLE sys_operation_log ADD COLUMN IF NOT EXISTS status INTEGER DEFAULT 1;
ALTER TABLE sys_operation_log ADD COLUMN IF NOT EXISTS error_msg TEXT;

-- 旧列 operation 改为可空，避免 Entity 插入时因缺少 operation 列报错（Entity 使用 action）
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='sys_operation_log' AND column_name='operation') THEN
    ALTER TABLE sys_operation_log ALTER COLUMN operation DROP NOT NULL;
  END IF;
END $$;
