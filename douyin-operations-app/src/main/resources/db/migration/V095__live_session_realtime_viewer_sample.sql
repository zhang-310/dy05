-- GMV-02：实时面板/抖音 ingest 在线人数时序样本（≥2 条用于峰值-末次留存推算）
CREATE TABLE IF NOT EXISTS live_session_realtime_viewer_sample (
    id                BIGSERIAL PRIMARY KEY,
    live_session_id    BIGINT      NOT NULL,
    viewer_count       INTEGER     NOT NULL DEFAULT 0,
    sampled_at         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source             VARCHAR(32),
    deleted            INTEGER     NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_lsrvs_session_sampled
    ON live_session_realtime_viewer_sample(live_session_id, sampled_at)
    WHERE deleted = 0;
