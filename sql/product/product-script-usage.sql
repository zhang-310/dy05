-- ============================================================
-- 商品话术使用记录表 (product_script_usage)
-- 用于追踪产品话术模板的应用和效果
-- ============================================================

-- dy_product_script — 商品话术表（版本管理）
-- 说明：记录每个商品的所有版本，支持版本对比、回滚、推荐
CREATE TABLE IF NOT EXISTS dy_product_script (
    id                      BIGSERIAL           PRIMARY KEY,
    product_id              BIGINT              NOT NULL,                          -- 所属商品 ID
    version_number          INTEGER             NOT NULL DEFAULT 1,               -- 版本号（递增）
    content                 TEXT                NOT NULL,                         -- 话术内容
    style                   VARCHAR(64),                                          -- 话术风格（高端、亲切、幽默等）
    effectiveness_score     DECIMAL(5, 2),                                        -- 效果评分（0-100）
    usage_count             INTEGER             NOT NULL DEFAULT 0,              -- 使用次数统计
    is_active               INTEGER             NOT NULL DEFAULT 1,              -- 是否为当前版本：0=历史 1=当前
    change_reason           VARCHAR(256),                                         -- 变更原因
    editor_id               BIGINT,                                               -- 编辑者 ID
    editor_name             VARCHAR(128),                                         -- 编辑者名称
    deleted                 INTEGER             NOT NULL DEFAULT 0,              -- 逻辑删除：0=正常 1=已删除
    create_time             TIMESTAMP           DEFAULT CURRENT_TIMESTAMP,        -- 创建时间
    update_time             TIMESTAMP           DEFAULT CURRENT_TIMESTAMP,        -- 更新时间
    CONSTRAINT uk_product_script_version UNIQUE (product_id, version_number)
);

CREATE INDEX IF NOT EXISTS idx_product_script_product     ON dy_product_script (product_id, is_active);
CREATE INDEX IF NOT EXISTS idx_product_script_version     ON dy_product_script (product_id, version_number DESC);
CREATE INDEX IF NOT EXISTS idx_product_script_score       ON dy_product_script (product_id, effectiveness_score DESC);
CREATE INDEX IF NOT EXISTS idx_product_script_usage       ON dy_product_script (product_id, usage_count DESC);
CREATE INDEX IF NOT EXISTS idx_product_script_time        ON dy_product_script (create_time DESC);

COMMENT ON TABLE  dy_product_script                       IS '商品话术表（版本管理）';
COMMENT ON COLUMN dy_product_script.product_id            IS '所属商品 ID';
COMMENT ON COLUMN dy_product_script.version_number        IS '版本号（从 1 开始递增）';
COMMENT ON COLUMN dy_product_script.content               IS '话术内容文本';
COMMENT ON COLUMN dy_product_script.style                 IS '话术风格（高端、亲切、幽默、技术型等）';
COMMENT ON COLUMN dy_product_script.effectiveness_score   IS '效果评分（0-100，基于直播转化率）';
COMMENT ON COLUMN dy_product_script.usage_count           IS '该版本被使用的次数';
COMMENT ON COLUMN dy_product_script.is_active             IS '是否为当前活跃版本';
COMMENT ON COLUMN dy_product_script.change_reason         IS '版本变更原因（优化开场、增加特性等）';
COMMENT ON COLUMN dy_product_script.editor_id             IS '编辑者用户 ID';
COMMENT ON COLUMN dy_product_script.editor_name           IS '编辑者名称（快速显示用）';
COMMENT ON COLUMN dy_product_script.deleted               IS '逻辑删除标记';


-- dy_product_script_usage — 话术使用记录表
-- 说明：记录某个产品话术被应用到哪个直播脚本及其效果
CREATE TABLE IF NOT EXISTS dy_product_script_usage (
    id                      BIGSERIAL           PRIMARY KEY,
    product_script_id       BIGINT              NOT NULL,                        -- 商品话术 ID（dy_product_script.id）
    live_script_id          BIGINT,                                             -- 应用到的直播话术 ID（live_script.id）
    session_id              BIGINT,                                             -- 直播场次 ID（live_session.id）
    applied_time            TIMESTAMP           NOT NULL,                       -- 应用时间
    effectiveness_score     DECIMAL(5, 2),                                      -- 本次使用的效果评分
    conversion_rate         DECIMAL(5, 2),                                      -- 本次使用的转化率（百分比）
    sales_amount            DECIMAL(12, 2),                                     -- 销售金额
    deleted                 INTEGER             NOT NULL DEFAULT 0,            -- 逻辑删除：0=正常 1=已删除
    create_time             TIMESTAMP           DEFAULT CURRENT_TIMESTAMP      -- 创建时间
);

CREATE INDEX IF NOT EXISTS idx_usage_script              ON dy_product_script_usage (product_script_id);
CREATE INDEX IF NOT EXISTS idx_usage_live_script         ON dy_product_script_usage (live_script_id);
CREATE INDEX IF NOT EXISTS idx_usage_session             ON dy_product_script_usage (session_id);
CREATE INDEX IF NOT EXISTS idx_usage_applied_time        ON dy_product_script_usage (applied_time DESC);
CREATE INDEX IF NOT EXISTS idx_usage_effectiveness       ON dy_product_script_usage (effectiveness_score DESC);

COMMENT ON TABLE  dy_product_script_usage                IS '商品话术使用记录表';
COMMENT ON COLUMN dy_product_script_usage.product_script_id IS '商品话术版本 ID';
COMMENT ON COLUMN dy_product_script_usage.live_script_id    IS '应用到的直播话术 ID';
COMMENT ON COLUMN dy_product_script_usage.session_id        IS '直播场次 ID';
COMMENT ON COLUMN dy_product_script_usage.applied_time      IS '该话术被应用的时间';
COMMENT ON COLUMN dy_product_script_usage.effectiveness_score IS '该版本在本次直播的效果评分';
COMMENT ON COLUMN dy_product_script_usage.conversion_rate    IS '本次直播的转化率';
COMMENT ON COLUMN dy_product_script_usage.sales_amount       IS '本次直播该商品的销售额';
COMMENT ON COLUMN dy_product_script_usage.deleted            IS '逻辑删除标记';
