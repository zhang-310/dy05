-- V161: Payment 租户归属回填
-- owner_id 在 payment 域按组织租户使用；清理早期单租户默认 1 的回填。
-- 兼容老 Docker 数据库：历史表里 V159 可能被旧脚本占用，导致 owner_id DDL 未真正落库。

ALTER TABLE payment_order ADD COLUMN IF NOT EXISTS owner_id BIGINT;
ALTER TABLE payment_refund ADD COLUMN IF NOT EXISTS owner_id BIGINT;
ALTER TABLE payment_subscription ADD COLUMN IF NOT EXISTS owner_id BIGINT;

UPDATE payment_order
SET owner_id = COALESCE(org_id, 1)
WHERE owner_id IS NULL;

UPDATE payment_subscription
SET owner_id = COALESCE(org_id, 1)
WHERE owner_id IS NULL;

UPDATE payment_refund pr
SET owner_id = po.owner_id
FROM payment_order po
WHERE pr.order_id = po.id
  AND pr.owner_id IS NULL
  AND po.owner_id IS NOT NULL;

UPDATE payment_order po
SET owner_id = u.organization_id,
    org_id = COALESCE(po.org_id, u.organization_id)
FROM auth_user u
WHERE po.user_id = u.id
  AND po.deleted = 0
  AND u.organization_id IS NOT NULL
  AND (po.owner_id = 1 OR po.owner_id IS NULL OR po.org_id IS NULL);

UPDATE payment_subscription ps
SET owner_id = COALESCE(ps.org_id, u.organization_id),
    org_id = COALESCE(ps.org_id, u.organization_id)
FROM auth_user u
WHERE ps.user_id = u.id
  AND ps.deleted = 0
  AND u.organization_id IS NOT NULL
  AND (ps.owner_id = 1 OR ps.owner_id IS NULL OR ps.org_id IS NULL);

UPDATE payment_refund pr
SET owner_id = po.owner_id
FROM payment_order po
WHERE pr.order_id = po.id
  AND po.owner_id IS NOT NULL
  AND (pr.owner_id = 1 OR pr.owner_id IS NULL);

CREATE INDEX IF NOT EXISTS idx_payment_order_owner_user_deleted
    ON payment_order(owner_id, user_id, deleted);

CREATE INDEX IF NOT EXISTS idx_payment_subscription_owner_status_deleted
    ON payment_subscription(owner_id, status, deleted);

CREATE INDEX IF NOT EXISTS idx_payment_order_owner_id
    ON payment_order(owner_id) WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_payment_refund_owner_id
    ON payment_refund(owner_id);

CREATE INDEX IF NOT EXISTS idx_payment_subscription_owner_id
    ON payment_subscription(owner_id) WHERE deleted = 0;
