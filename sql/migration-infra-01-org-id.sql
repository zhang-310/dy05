-- 与 Flyway V097 对齐：核心表 org_id（见 src/main/resources/db/migration/V097__core_tables_org_id.sql）

ALTER TABLE live_session ADD COLUMN IF NOT EXISTS org_id BIGINT;
ALTER TABLE payment_order ADD COLUMN IF NOT EXISTS org_id BIGINT;
ALTER TABLE sv_project ADD COLUMN IF NOT EXISTS org_id BIGINT;
ALTER TABLE live_product ADD COLUMN IF NOT EXISTS org_id BIGINT;

COMMENT ON COLUMN live_session.org_id IS '租户组织 auth_organization.id，冗余自 auth_user.organization_id';
COMMENT ON COLUMN payment_order.org_id IS '租户组织，冗余自下单用户';
COMMENT ON COLUMN sv_project.org_id IS '租户组织，冗余自 owner';
COMMENT ON COLUMN live_product.org_id IS '租户组织，冗余自 user_id';
