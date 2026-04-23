-- ============================================================
-- Create dy_product_script_usage table
-- PostgreSQL compatible version
-- ============================================================

CREATE TABLE IF NOT EXISTS dy_product_script_usage (
    id                      BIGSERIAL           PRIMARY KEY,
    product_script_id       BIGINT              NOT NULL,
    live_script_id          BIGINT,
    session_id              BIGINT,
    applied_time            TIMESTAMP           NOT NULL,
    effectiveness_score     DECIMAL(5, 2),
    conversion_rate         DECIMAL(5, 2),
    sales_amount            DECIMAL(12, 2),
    deleted                 INTEGER             NOT NULL DEFAULT 0,
    create_time             TIMESTAMP           DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_usage_script ON dy_product_script_usage (product_script_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_usage_live_script ON dy_product_script_usage (live_script_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_usage_session ON dy_product_script_usage (session_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_usage_applied_time ON dy_product_script_usage (applied_time DESC) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_usage_effectiveness ON dy_product_script_usage (effectiveness_score DESC) WHERE deleted = 0;

COMMENT ON TABLE dy_product_script_usage IS 'Product script usage record table';
COMMENT ON COLUMN dy_product_script_usage.product_script_id IS 'Product script version ID';
COMMENT ON COLUMN dy_product_script_usage.live_script_id IS 'Applied live script ID';
COMMENT ON COLUMN dy_product_script_usage.session_id IS 'Live session ID';
COMMENT ON COLUMN dy_product_script_usage.applied_time IS 'Time when script was applied';
COMMENT ON COLUMN dy_product_script_usage.effectiveness_score IS 'Effectiveness score for this usage';
COMMENT ON COLUMN dy_product_script_usage.conversion_rate IS 'Conversion rate for this usage (percentage)';
COMMENT ON COLUMN dy_product_script_usage.sales_amount IS 'Sales amount for this usage';
COMMENT ON COLUMN dy_product_script_usage.deleted IS 'Logical deletion flag (0=normal 1=deleted)';
