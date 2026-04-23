-- msg_platform_config.status: SMALLINT -> INTEGER (Entity 使用 Integer，与 Hibernate schema-validation 一致)
ALTER TABLE msg_platform_config ALTER COLUMN status TYPE INTEGER USING status::integer;
