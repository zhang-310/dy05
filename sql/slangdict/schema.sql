-- ============================================================
-- 话术梗库模块 (SlangDict) — 表结构
-- 前缀: sd_
-- ============================================================

-- 梗条目主表
CREATE TABLE IF NOT EXISTS sd_entry (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    phrase      VARCHAR(256) NOT NULL,              -- 梗/暗语
    meaning     VARCHAR(512),                        -- 真实含义
    category    VARCHAR(64)  DEFAULT 'general',      -- product_alias/catchphrase/slang/humor/general
    usage_scene VARCHAR(128),                        -- 适用场景：带货/暖场/互动/转场
    example     TEXT,                                -- 使用示例/完整话术片段
    source      VARCHAR(128),                        -- 来源：自创/同行学习/AI生成
    use_count   INTEGER      DEFAULT 0,
    status      INTEGER      DEFAULT 1,              -- 1=启用 0=禁用
    deleted     INTEGER      DEFAULT 0,
    create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sd_entry_user ON sd_entry(user_id);
CREATE INDEX IF NOT EXISTS idx_sd_entry_category ON sd_entry(category);
CREATE INDEX IF NOT EXISTS idx_sd_entry_status ON sd_entry(status);

COMMENT ON TABLE  sd_entry IS '话术梗库-梗条目';
COMMENT ON COLUMN sd_entry.phrase IS '梗/暗语，如"给小弟准备一套别墅"';
COMMENT ON COLUMN sd_entry.meaning IS '真实含义，如"推荐高端男士内裤"';
COMMENT ON COLUMN sd_entry.category IS '分类：product_alias/catchphrase/slang/humor/general';
COMMENT ON COLUMN sd_entry.usage_scene IS '适用场景：带货/暖场/互动/转场';

-- 梗与产品关联表
CREATE TABLE IF NOT EXISTS sd_product_mapping (
    id          BIGSERIAL PRIMARY KEY,
    entry_id    BIGINT  NOT NULL,                    -- → sd_entry.id
    product_id  BIGINT  NOT NULL,                    -- → dy_product.id
    user_id     BIGINT  NOT NULL,
    deleted     INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sd_pm_entry ON sd_product_mapping(entry_id);
CREATE INDEX IF NOT EXISTS idx_sd_pm_product ON sd_product_mapping(product_id);
CREATE INDEX IF NOT EXISTS idx_sd_pm_user ON sd_product_mapping(user_id);

COMMENT ON TABLE  sd_product_mapping IS '话术梗库-梗与产品关联';
