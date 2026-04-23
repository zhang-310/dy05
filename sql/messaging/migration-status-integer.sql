-- ============================================================
-- 迁移：msg_platform_config.status 从 SMALLINT 改为 INTEGER
-- 修复 Hibernate schema-validation: Entity 使用 Integer，与 DB SMALLINT 不匹配
-- 用法：psql -U postgres -d douyin_operations -f sql/messaging/migration-status-integer.sql
-- ============================================================

ALTER TABLE msg_platform_config ALTER COLUMN status TYPE INTEGER USING status::integer;
