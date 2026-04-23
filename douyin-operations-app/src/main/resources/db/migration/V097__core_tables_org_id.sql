-- INFRA-01：核心表租户键 org_id（可空 + 回填，应用层继续以 user_id/owner_id 为主隔离）

ALTER TABLE live_session ADD COLUMN IF NOT EXISTS org_id BIGINT;
ALTER TABLE payment_order ADD COLUMN IF NOT EXISTS org_id BIGINT;
ALTER TABLE sv_project ADD COLUMN IF NOT EXISTS org_id BIGINT;
ALTER TABLE live_product ADD COLUMN IF NOT EXISTS org_id BIGINT;

UPDATE live_session ls
SET org_id = u.organization_id
FROM auth_user u
WHERE ls.user_id = u.id AND ls.deleted = 0 AND ls.org_id IS NULL AND u.organization_id IS NOT NULL;

UPDATE payment_order po
SET org_id = u.organization_id
FROM auth_user u
WHERE po.user_id = u.id AND po.deleted = 0 AND po.org_id IS NULL AND u.organization_id IS NOT NULL;

UPDATE sv_project sp
SET org_id = u.organization_id
FROM auth_user u
WHERE sp.owner_id = u.id AND sp.deleted = 0 AND sp.org_id IS NULL AND u.organization_id IS NOT NULL;

UPDATE live_product lp
SET org_id = u.organization_id
FROM auth_user u
WHERE lp.user_id = u.id AND lp.deleted = 0 AND lp.org_id IS NULL AND u.organization_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_live_session_org_deleted ON live_session (org_id, deleted);
CREATE INDEX IF NOT EXISTS idx_payment_order_org_deleted ON payment_order (org_id, deleted);
CREATE INDEX IF NOT EXISTS idx_sv_project_org_deleted ON sv_project (org_id, deleted);
CREATE INDEX IF NOT EXISTS idx_live_product_org_deleted ON live_product (org_id, deleted);
