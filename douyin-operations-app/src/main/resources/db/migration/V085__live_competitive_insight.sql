-- P2 C-2/C-3/C-5：竞品定价·份额·GMV（手工录入 + 对比，无第三方则 MVP）
CREATE TABLE IF NOT EXISTS live_competitive_insight (
    id                    BIGSERIAL PRIMARY KEY,
    owner_id              BIGINT         NOT NULL,
    session_id            BIGINT,
    competitor_label      VARCHAR(128)   NOT NULL,
    product_price         NUMERIC(12, 2),
    market_share_percent  NUMERIC(6, 2),
    gmv_estimate          NUMERIC(14, 2),
    win_loss_notes        TEXT,
    deleted               INTEGER        NOT NULL DEFAULT 0,
    create_time           TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    update_time           TIMESTAMP      DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_live_comp_in_owner ON live_competitive_insight(owner_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_comp_in_session ON live_competitive_insight(session_id) WHERE deleted = 0;

COMMENT ON TABLE live_competitive_insight IS '直播竞品商业对比快照（手工/导入）';
