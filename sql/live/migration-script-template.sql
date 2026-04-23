-- ============================================================
-- live 模块 - live_script_template 直播话术模板表
-- 高效话术（effectiveness_score > 80）自动入库，供下次直播复用
-- 执行：psql -U postgres -d douyin_operations -f sql/live/migration-script-template.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS live_script_template (
    id                      BIGSERIAL       PRIMARY KEY,
    template_name           VARCHAR(128)    NOT NULL,                       -- 模板名称
    script_type             VARCHAR(32)    NOT NULL,                       -- 话术类型：opening/product/transition/closing/custom
    category                VARCHAR(64),                                   -- 适用品类（美妆/食品/服装/数码等）
    content                 TEXT            NOT NULL,                       -- 话术内容（含变量占位符）
    variables               VARCHAR(512),                                  -- 支持的变量列表（JSON数组）
    effectiveness_score     DECIMAL(5,2)            DEFAULT 0,             -- 效果评分（基于历史数据）
    usage_count             INTEGER                 DEFAULT 0,            -- 使用次数
    avg_conversion_rate     DECIMAL(5,2)            DEFAULT 0,            -- 平均转化率（%）
    source_script_id        BIGINT,                                        -- 来源 live_script.id
    source_session_id       BIGINT,                                        -- 来源 live_session.id
    status                  INTEGER         NOT NULL DEFAULT 1,            -- 1=启用 0=禁用
    deleted                 INTEGER         NOT NULL DEFAULT 0,            -- 逻辑删除
    create_time             TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,
    update_time             TIMESTAMP                DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_lst_script_type ON live_script_template(script_type);

CREATE INDEX IF NOT EXISTS idx_lst_effectiveness ON live_script_template(effectiveness_score DESC) WHERE deleted = 0;

-- 复合索引：按类型+品类+效果评分
CREATE INDEX IF NOT EXISTS idx_lst_type_category_score ON live_script_template(script_type, category, effectiveness_score DESC) WHERE deleted = 0;

COMMENT ON TABLE live_script_template IS '直播话术模板表（高效话术自动入库）';
COMMENT ON COLUMN live_script_template.template_name IS '模板名称';
COMMENT ON COLUMN live_script_template.script_type IS '话术类型：opening/product/transition/closing/custom';
COMMENT ON COLUMN live_script_template.effectiveness_score IS '效果评分（基于历史数据）';
COMMENT ON COLUMN live_script_template.source_script_id IS '来源 live_script.id';
