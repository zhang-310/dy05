-- ============================================================
-- 商品管理模块 (product) - 表结构
-- 数据库：PostgreSQL
-- 说明：复用 douyin 模块的 dy_product 表前缀
-- ============================================================

-- ============================================================
-- 1. dy_product — 商品库表
-- Entity: cn.gaifan.douyinOperations.module.product.entity.DyProduct
-- ============================================================
CREATE TABLE IF NOT EXISTS dy_product (
    id               BIGSERIAL       PRIMARY KEY,
    user_id          BIGINT          NOT NULL,                              -- 所属用户 ID
    product_name     VARCHAR(256)    NOT NULL,                              -- 商品名称
    product_category VARCHAR(128),                                           -- 商品分类
    description      TEXT,                                                   -- 商品描述
    image_url        VARCHAR(256),                                           -- 商品图片 URL
    price            DECIMAL(12,2)   NOT NULL,                              -- 售价
    cost_price       DECIMAL(12,2),                                          -- 成本价
    inventory        BIGINT                   DEFAULT 0,                    -- 库存数量
    sku              VARCHAR(64),                                            -- SKU 编码
    barcode          VARCHAR(128),                                           -- 条形码
    manufacturer     VARCHAR(256),                                           -- 生产厂商
    tags             VARCHAR(512),                                           -- 标签（逗号分隔或 JSON）
    profit_margin_pct DECIMAL(5,4),                                          -- 利润百分比，如 0.45=45%
    loss_per_unit     DECIMAL(10,2),                                         -- 每单亏损金额（元）
    control_strategy  VARCHAR(128),                                           -- 控单策略：控3单、不控单1分钟下 等，拍品千次/密度成交策略
    status           INTEGER         NOT NULL DEFAULT 1,                    -- 状态：0=下架 1=上架
    featured         INTEGER         NOT NULL DEFAULT 0,                    -- 推荐：0=普通 1=推荐
    deleted          INTEGER         NOT NULL DEFAULT 0,                    -- 逻辑删除：0=正常 1=已删除
    version          INTEGER         NOT NULL DEFAULT 0,                    -- 乐观锁版本号，防并发库存超卖
    create_time      TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,    -- 创建时间
    update_time      TIMESTAMP                DEFAULT CURRENT_TIMESTAMP     -- 更新时间
);

CREATE INDEX IF NOT EXISTS idx_product_user_status   ON dy_product (user_id, status, featured);
CREATE INDEX IF NOT EXISTS idx_product_user_category ON dy_product (user_id, product_category);
CREATE INDEX IF NOT EXISTS idx_product_sku           ON dy_product (sku);
CREATE INDEX IF NOT EXISTS idx_product_barcode       ON dy_product (barcode);
CREATE INDEX IF NOT EXISTS idx_product_user_name     ON dy_product (user_id, product_name);

COMMENT ON TABLE  dy_product                IS '商品库表';
COMMENT ON COLUMN dy_product.user_id        IS '所属用户 ID';
COMMENT ON COLUMN dy_product.product_name   IS '商品名称';
COMMENT ON COLUMN dy_product.product_category IS '商品分类';
COMMENT ON COLUMN dy_product.description    IS '商品描述';
COMMENT ON COLUMN dy_product.image_url      IS '商品图片 URL';
COMMENT ON COLUMN dy_product.price          IS '售价';
COMMENT ON COLUMN dy_product.cost_price     IS '成本价';
COMMENT ON COLUMN dy_product.inventory      IS '库存数量';
COMMENT ON COLUMN dy_product.sku            IS 'SKU 编码';
COMMENT ON COLUMN dy_product.barcode        IS '条形码';
COMMENT ON COLUMN dy_product.manufacturer   IS '生产厂商';
COMMENT ON COLUMN dy_product.tags           IS '标签（逗号分隔或 JSON）';
COMMENT ON COLUMN dy_product.status         IS '状态：0=下架 1=上架';
COMMENT ON COLUMN dy_product.featured       IS '推荐：0=普通 1=推荐';
COMMENT ON COLUMN dy_product.deleted        IS '逻辑删除：0=正常 1=已删除';
COMMENT ON COLUMN dy_product.version        IS '乐观锁版本号，防止并发库存更新超卖';

-- ============================================================
-- 2. dy_product_sales_history — 销售历史表
-- Entity: cn.gaifan.douyinOperations.module.product.entity.DyProductSalesHistory
-- ============================================================
CREATE TABLE IF NOT EXISTS dy_product_sales_history (
    id             BIGSERIAL       PRIMARY KEY,
    product_id     BIGINT          NOT NULL,                              -- 关联商品 ID
    sale_quantity  BIGINT                   DEFAULT 0,                    -- 销售数量
    sale_amount    DECIMAL(12,2)   NOT NULL,                              -- 销售金额（GMV）
    sale_time      TIMESTAMP       NOT NULL,                              -- 销售时间
    channel_source VARCHAR(64),                                            -- 渠道来源：douyin_live / store / manual
    session_id     VARCHAR(128),                                           -- 直播场次 ID
    deleted        INTEGER         NOT NULL DEFAULT 0,                    -- 逻辑删除：0=正常 1=已删除
    create_time    TIMESTAMP                DEFAULT CURRENT_TIMESTAMP     -- 创建时间
);

CREATE INDEX IF NOT EXISTS idx_sales_product_time    ON dy_product_sales_history (product_id, sale_time DESC);
CREATE INDEX IF NOT EXISTS idx_sales_product_channel ON dy_product_sales_history (product_id, channel_source);
CREATE INDEX IF NOT EXISTS idx_sales_session         ON dy_product_sales_history (session_id);
CREATE INDEX IF NOT EXISTS idx_sales_time            ON dy_product_sales_history (sale_time);

COMMENT ON TABLE  dy_product_sales_history              IS '商品销售历史表';
COMMENT ON COLUMN dy_product_sales_history.product_id   IS '关联商品 ID';
COMMENT ON COLUMN dy_product_sales_history.sale_quantity IS '销售数量';
COMMENT ON COLUMN dy_product_sales_history.sale_amount  IS '销售金额（GMV）';
COMMENT ON COLUMN dy_product_sales_history.sale_time    IS '销售时间';
COMMENT ON COLUMN dy_product_sales_history.channel_source IS '渠道来源：douyin_live=抖音直播 store=门店 manual=手动录入';
COMMENT ON COLUMN dy_product_sales_history.session_id   IS '直播场次 ID';
COMMENT ON COLUMN dy_product_sales_history.deleted      IS '逻辑删除：0=正常 1=已删除';

-- ============================================================
-- 3. style_preset — 风格预设模板表
-- Entity: cn.gaifan.douyinOperations.module.product.entity.StylePreset
-- ============================================================
CREATE TABLE IF NOT EXISTS style_preset (
    id               BIGSERIAL       PRIMARY KEY,
    preset_name      VARCHAR(64)     NOT NULL,
    preset_code      VARCHAR(64)     NOT NULL,
    style_value      VARCHAR(128)     NOT NULL,
    style_tags_json  JSONB,
    category         VARCHAR(32),
    description      TEXT,
    prompt_template  TEXT,
    word_count_min   INTEGER         DEFAULT 150,
    word_count_max   INTEGER         DEFAULT 300,
    sort_order       INTEGER         DEFAULT 0,
    is_enabled       BOOLEAN         DEFAULT TRUE,
    created_by       BIGINT          DEFAULT 0,
    deleted          INTEGER         NOT NULL DEFAULT 0,
    create_time      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_style_preset_code UNIQUE (preset_code)
);

CREATE INDEX IF NOT EXISTS idx_style_preset_enabled_sort ON style_preset (is_enabled, sort_order) WHERE deleted = 0;

COMMENT ON TABLE  style_preset             IS '风格预设模板表（产品话术生成用）';
COMMENT ON COLUMN style_preset.preset_name IS '预设名称';
COMMENT ON COLUMN style_preset.preset_code IS '预设编码（唯一）';
COMMENT ON COLUMN style_preset.style_value IS '风格值';
COMMENT ON COLUMN style_preset.is_enabled  IS '是否启用';
COMMENT ON COLUMN style_preset.deleted     IS '逻辑删除：0=正常 1=已删除';
