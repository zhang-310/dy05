-- ============================================================
-- abtest 模块 - 数据库表结构
-- 模块：A/B 测试管理
-- 数据库：PostgreSQL
-- 版本：3.0
-- 更新日期：2026-02-26
-- 说明：A/B 实验、变体、事件记录
-- ============================================================

-- ============================================================
-- 1. ab_experiment — A/B 实验表
-- Entity: cn.gaifan.douyinOperations.module.abtest.entity.AbExperiment
-- ============================================================
CREATE TABLE IF NOT EXISTS ab_experiment (
    id                 BIGSERIAL       PRIMARY KEY,
    owner_id           BIGINT          NOT NULL,                              -- 所属用户 ID
    name               VARCHAR(128)    NOT NULL,                              -- 实验名称
    description        TEXT,                                                   -- 实验描述
    experiment_type    VARCHAR(16)     NOT NULL,                              -- 实验类型：video / live / copy
    status             SMALLINT        NOT NULL DEFAULT 0,                    -- 状态：0=草稿 1=运行中 2=已完成 3=已暂停
    start_time         TIMESTAMP,                                              -- 实验启动时间
    end_time           TIMESTAMP,                                              -- 实验结束时间
    winner_variant_id  BIGINT,                                                 -- 获胜变体 ID（结束时设置）
    conclusion         TEXT,                                                    -- 实验结论（手动填写）
    deleted            INTEGER         NOT NULL DEFAULT 0,                    -- 逻辑删除：0=正常 1=已删除
    create_time        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 创建时间
    update_time        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 更新时间

    CONSTRAINT chk_experiment_type CHECK (experiment_type IN ('video', 'live', 'copy')),
    CONSTRAINT chk_experiment_status CHECK (status IN (0, 1, 2, 3))
);

CREATE INDEX IF NOT EXISTS idx_ab_experiment_owner_status ON ab_experiment (owner_id, status, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_ab_experiment_owner_type   ON ab_experiment (owner_id, experiment_type);
CREATE INDEX IF NOT EXISTS idx_ab_experiment_winner       ON ab_experiment (winner_variant_id);

COMMENT ON TABLE  ab_experiment                    IS 'A/B 测试实验表';
COMMENT ON COLUMN ab_experiment.owner_id           IS '所属用户 ID';
COMMENT ON COLUMN ab_experiment.name               IS '实验名称';
COMMENT ON COLUMN ab_experiment.experiment_type    IS '实验类型：video=短视频 live=直播 copy=商品文案';
COMMENT ON COLUMN ab_experiment.status             IS '状态：0=草稿 1=运行中 2=已完成 3=已暂停';
COMMENT ON COLUMN ab_experiment.winner_variant_id  IS '获胜变体 ID，结束时设置';
COMMENT ON COLUMN ab_experiment.conclusion         IS '实验结论，手动填写';

-- ============================================================
-- 2. ab_variant — 变体表
-- Entity: cn.gaifan.douyinOperations.module.abtest.entity.AbVariant
-- ============================================================
CREATE TABLE IF NOT EXISTS ab_variant (
    id               BIGSERIAL       PRIMARY KEY,
    experiment_id    BIGINT          NOT NULL,                              -- 所属实验 ID
    variant_name     VARCHAR(64)     NOT NULL,                              -- 变体名称（如"版本A"、"版本B"）
    variant_type     VARCHAR(2)      NOT NULL,                              -- 变体类型：A / B
    content          TEXT,                                                   -- 变体内容文本（冗余存储）
    entity_type      VARCHAR(32),                                           -- 关联实体类型：sv_plan / live_session_script / cp_library
    entity_id        BIGINT,                                                -- 关联实体 ID（跨模块，无 FK 约束）
    view_count       BIGINT          NOT NULL DEFAULT 0,                    -- 曝光次数
    click_count      BIGINT          NOT NULL DEFAULT 0,                    -- 点击次数
    conversion_count BIGINT          NOT NULL DEFAULT 0,                    -- 转化次数
    conversion_rate  DECIMAL(5,4)    NOT NULL DEFAULT 0.0000,               -- 转化率
    is_winner        SMALLINT        NOT NULL DEFAULT 0,                    -- 是否获胜：0=否 1=是
    deleted          INTEGER         NOT NULL DEFAULT 0,                    -- 逻辑删除：0=正常 1=已删除
    create_time      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 创建时间
    update_time      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 更新时间

    CONSTRAINT chk_variant_type CHECK (variant_type IN ('A', 'B'))
);

CREATE INDEX IF NOT EXISTS idx_ab_variant_experiment     ON ab_variant (experiment_id, variant_type);
CREATE INDEX IF NOT EXISTS idx_ab_variant_experiment_del ON ab_variant (experiment_id, deleted);
CREATE INDEX IF NOT EXISTS idx_ab_variant_entity         ON ab_variant (entity_type, entity_id);

COMMENT ON TABLE  ab_variant                    IS 'A/B 测试变体表';
COMMENT ON COLUMN ab_variant.experiment_id     IS '所属实验 ID';
COMMENT ON COLUMN ab_variant.variant_type      IS '变体类型：A 或 B';
COMMENT ON COLUMN ab_variant.content           IS '变体内容文本（冗余存储）';
COMMENT ON COLUMN ab_variant.entity_type       IS '关联实体类型：sv_plan / live_session_script / cp_library';
COMMENT ON COLUMN ab_variant.entity_id         IS '关联实体 ID（跨模块，无 FK 约束）';
COMMENT ON COLUMN ab_variant.conversion_rate   IS '转化率 = conversion_count / view_count';
COMMENT ON COLUMN ab_variant.is_winner         IS '是否获胜：0=否 1=是';

-- ============================================================
-- 3. ab_event — 事件记录表
-- Entity: cn.gaifan.douyinOperations.module.abtest.entity.AbEvent
-- 注意：无 deleted 字段，事件记录不可删除，仅支持按 create_time 归档
-- ============================================================
CREATE TABLE IF NOT EXISTS ab_event (
    id               BIGSERIAL       PRIMARY KEY,
    experiment_id    BIGINT          NOT NULL,                              -- 所属实验 ID
    variant_id       BIGINT          NOT NULL,                              -- 所属变体 ID
    event_type       VARCHAR(16)     NOT NULL,                              -- 事件类型：view / click / conversion
    user_fingerprint VARCHAR(64)     NOT NULL,                              -- 用户指纹（用于去重）
    session_id       VARCHAR(128),                                          -- 会话 ID（用于漏斗分析）
    create_time      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 事件发生时间

    CONSTRAINT chk_event_type CHECK (event_type IN ('view', 'click', 'conversion'))
);

CREATE INDEX IF NOT EXISTS idx_ab_event_variant_type ON ab_event (experiment_id, variant_id, event_type);
CREATE INDEX IF NOT EXISTS idx_ab_event_dedup        ON ab_event (variant_id, event_type, user_fingerprint);
CREATE INDEX IF NOT EXISTS idx_ab_event_time         ON ab_event (experiment_id, create_time);
CREATE INDEX IF NOT EXISTS idx_ab_event_create_time  ON ab_event (create_time);

COMMENT ON TABLE  ab_event                    IS 'A/B 测试事件记录表';
COMMENT ON COLUMN ab_event.experiment_id     IS '所属实验 ID';
COMMENT ON COLUMN ab_event.variant_id        IS '所属变体 ID';
COMMENT ON COLUMN ab_event.event_type        IS '事件类型：view=曝光 click=点击 conversion=转化';
COMMENT ON COLUMN ab_event.user_fingerprint  IS '用户指纹（浏览器/设备指纹，用于去重）';
COMMENT ON COLUMN ab_event.session_id        IS '会话 ID，用于漏斗分析';
