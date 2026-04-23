-- workflow_definition.status: SMALLINT -> INTEGER (Entity 使用 Integer，与 Hibernate schema-validation 一致)
ALTER TABLE workflow_definition ALTER COLUMN status TYPE INTEGER USING status::integer;
