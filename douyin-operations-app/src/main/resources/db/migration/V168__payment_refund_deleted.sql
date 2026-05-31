-- V168: Payment refund soft-delete column
-- Owner: payment module. Required by PaymentRefund SQL restriction.

ALTER TABLE payment_refund
    ADD COLUMN IF NOT EXISTS deleted INTEGER NOT NULL DEFAULT 0;

UPDATE payment_refund
SET deleted = 0
WHERE deleted IS NULL;

CREATE INDEX IF NOT EXISTS idx_payment_refund_owner_deleted
    ON payment_refund(owner_id, deleted);

CREATE INDEX IF NOT EXISTS idx_payment_refund_order_deleted
    ON payment_refund(order_id, deleted);
