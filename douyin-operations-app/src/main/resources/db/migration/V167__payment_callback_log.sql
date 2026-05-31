-- V167: Payment callback log table
-- Owner: payment module. Written by payment callback handling; read by payment operations/audit flows.

CREATE TABLE IF NOT EXISTS pay_callback_log (
    id BIGSERIAL PRIMARY KEY,
    callback_id VARCHAR(100) NOT NULL,
    order_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    request_body TEXT,
    client_ip VARCHAR(50),
    process_status VARCHAR(20),
    error_message TEXT,
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);

ALTER TABLE pay_callback_log
    ADD COLUMN IF NOT EXISTS callback_id VARCHAR(100);

ALTER TABLE pay_callback_log
    ADD COLUMN IF NOT EXISTS order_id VARCHAR(50);

ALTER TABLE pay_callback_log
    ADD COLUMN IF NOT EXISTS status VARCHAR(20);

ALTER TABLE pay_callback_log
    ADD COLUMN IF NOT EXISTS request_body TEXT;

ALTER TABLE pay_callback_log
    ADD COLUMN IF NOT EXISTS client_ip VARCHAR(50);

ALTER TABLE pay_callback_log
    ADD COLUMN IF NOT EXISTS process_status VARCHAR(20);

ALTER TABLE pay_callback_log
    ADD COLUMN IF NOT EXISTS error_message TEXT;

ALTER TABLE pay_callback_log
    ADD COLUMN IF NOT EXISTS received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE pay_callback_log
    ADD COLUMN IF NOT EXISTS processed_at TIMESTAMP;

CREATE UNIQUE INDEX IF NOT EXISTS ux_pay_callback_log_callback_id
    ON pay_callback_log(callback_id);

CREATE INDEX IF NOT EXISTS idx_pay_callback_log_order_received
    ON pay_callback_log(order_id, received_at DESC);

CREATE INDEX IF NOT EXISTS idx_pay_callback_log_process_status
    ON pay_callback_log(process_status, received_at DESC);
