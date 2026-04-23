-- Hibernate 校验要求 deleted / is_system 为 INTEGER，与 V058 中 SMALLINT 对齐
ALTER TABLE sv_workflow_template
    ALTER COLUMN deleted TYPE INTEGER USING deleted::integer;
ALTER TABLE sv_workflow_template
    ALTER COLUMN is_system TYPE INTEGER USING is_system::integer;
