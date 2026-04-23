-- live_monitor_archive 归档表（结构同 live_monitor）
-- 超过 90 天的 live_monitor 数据迁移到此表后删除
-- 执行日期：2026-03-03

CREATE TABLE IF NOT EXISTS live_monitor_archive (
    id                      BIGSERIAL       PRIMARY KEY,
    session_id              BIGINT          NOT NULL,
    timestamp               TIMESTAMP       NOT NULL,
    viewers                 INTEGER         DEFAULT 0,
    likes                   BIGINT          DEFAULT 0,
    comments                INTEGER         DEFAULT 0,
    shares                  INTEGER         DEFAULT 0,
    product_impressions     INTEGER         DEFAULT 0,
    total_viewers           INTEGER         DEFAULT 0,
    new_followers           INTEGER         DEFAULT 0,
    online_count            INTEGER         DEFAULT 0,
    gmv                     NUMERIC(12,2)   DEFAULT 0,
    orders                  INTEGER         DEFAULT 0,
    create_time             TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_live_monitor_archive_session_id ON live_monitor_archive(session_id);
CREATE INDEX IF NOT EXISTS idx_live_monitor_archive_timestamp ON live_monitor_archive(timestamp);

COMMENT ON TABLE live_monitor_archive IS '直播监控数据归档表（90天前数据）';
