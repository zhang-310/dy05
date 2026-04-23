-- ============================================================
-- live 模块 - 增量迁移：场次数据汇总 + 商品数据汇总
-- ============================================================

-- 1. live_session_data - 直播场次数据汇总表
CREATE TABLE IF NOT EXISTS live_session_data (
    id              BIGSERIAL       PRIMARY KEY,
    session_id      BIGINT          NOT NULL,                           -- 场次ID
    total_viewers   INTEGER         NOT NULL DEFAULT 0,                 -- 累计观看人数
    peak_viewers    INTEGER         NOT NULL DEFAULT 0,                 -- 峰值在线人数
    total_likes     BIGINT          NOT NULL DEFAULT 0,                 -- 累计点赞数
    total_comments  INTEGER         NOT NULL DEFAULT 0,                 -- 累计评论数
    total_shares    INTEGER         NOT NULL DEFAULT 0,                 -- 累计分享数
    total_revenue   NUMERIC(14,2)   NOT NULL DEFAULT 0,                 -- 总销售额
    total_orders    INTEGER         NOT NULL DEFAULT 0,                 -- 总订单数
    avg_stay_time   INTEGER         NOT NULL DEFAULT 0,                 -- 平均停留时长(秒)
    new_followers   INTEGER         NOT NULL DEFAULT 0,                 -- 新增粉丝数
    sync_time       TIMESTAMP,                                          -- 最后同步时间
    create_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_lsd_session_id ON live_session_data (session_id);
CREATE INDEX IF NOT EXISTS idx_lsd_sync_time ON live_session_data (sync_time);

COMMENT ON TABLE  live_session_data                 IS '直播场次数据汇总表';
COMMENT ON COLUMN live_session_data.session_id      IS '场次ID';
COMMENT ON COLUMN live_session_data.total_viewers   IS '累计观看人数';
COMMENT ON COLUMN live_session_data.peak_viewers    IS '峰值在线人数';
COMMENT ON COLUMN live_session_data.total_likes     IS '累计点赞数';
COMMENT ON COLUMN live_session_data.total_comments  IS '累计评论数';
COMMENT ON COLUMN live_session_data.total_shares    IS '累计分享数';
COMMENT ON COLUMN live_session_data.total_revenue   IS '总销售额';
COMMENT ON COLUMN live_session_data.total_orders    IS '总订单数';
COMMENT ON COLUMN live_session_data.avg_stay_time   IS '平均停留时长(秒)';
COMMENT ON COLUMN live_session_data.new_followers   IS '新增粉丝数';
COMMENT ON COLUMN live_session_data.sync_time       IS '最后同步时间';

-- 2. live_product_data - 直播商品数据汇总表
CREATE TABLE IF NOT EXISTS live_product_data (
    id              BIGSERIAL       PRIMARY KEY,
    session_id      BIGINT          NOT NULL,                           -- 场次ID
    product_id      BIGINT          NOT NULL,                           -- 商品ID
    impressions     INTEGER         NOT NULL DEFAULT 0,                 -- 曝光次数
    clicks          INTEGER         NOT NULL DEFAULT 0,                 -- 点击次数
    orders          INTEGER         NOT NULL DEFAULT 0,                 -- 下单数
    sale_quantity   INTEGER         NOT NULL DEFAULT 0,                 -- 销售件数
    revenue         NUMERIC(12,2)   NOT NULL DEFAULT 0,                 -- 销售额
    refund_quantity INTEGER         NOT NULL DEFAULT 0,                 -- 退款件数
    conversion_rate NUMERIC(5,4)    NOT NULL DEFAULT 0,                 -- 转化率
    sync_time       TIMESTAMP,                                          -- 最后同步时间
    create_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_lpd_session_product ON live_product_data (session_id, product_id);
CREATE INDEX IF NOT EXISTS idx_lpd_session_id ON live_product_data (session_id);
CREATE INDEX IF NOT EXISTS idx_lpd_product_id ON live_product_data (product_id);

COMMENT ON TABLE  live_product_data                  IS '直播商品数据汇总表';
COMMENT ON COLUMN live_product_data.session_id       IS '场次ID';
COMMENT ON COLUMN live_product_data.product_id       IS '商品ID';
COMMENT ON COLUMN live_product_data.impressions      IS '曝光次数';
COMMENT ON COLUMN live_product_data.clicks           IS '点击次数';
COMMENT ON COLUMN live_product_data.orders           IS '下单数';
COMMENT ON COLUMN live_product_data.sale_quantity    IS '销售件数';
COMMENT ON COLUMN live_product_data.revenue          IS '销售额';
COMMENT ON COLUMN live_product_data.refund_quantity  IS '退款件数';
COMMENT ON COLUMN live_product_data.conversion_rate  IS '转化率';
COMMENT ON COLUMN live_product_data.sync_time        IS '最后同步时间';
