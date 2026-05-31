-- V198: 组织架构扩展
ALTER TABLE auth_organization ADD COLUMN IF NOT EXISTS parent_id BIGINT;
ALTER TABLE auth_organization ADD COLUMN IF NOT EXISTS org_type VARCHAR(32) DEFAULT 'team';
ALTER TABLE auth_organization ADD COLUMN IF NOT EXISTS contact_email VARCHAR(128);
ALTER TABLE auth_organization ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(32);

COMMENT ON COLUMN auth_organization.org_type IS 'team/company/agency';
