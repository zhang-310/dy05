-- ============================================================
-- config 模块 - 数据库表结构
-- 模块：系统配置中心（Configuration Management）
-- 数据库：PostgreSQL
-- 版本：2.1
-- 更新日期：2026-02-24
-- ============================================================

-- ============================================================
-- 1. sys_config_group - 配置分组表
-- ============================================================
CREATE TABLE sys_config_group (
    id          BIGSERIAL       PRIMARY KEY,
    parent_id   BIGINT                   DEFAULT 0,                 -- 父分组 ID（0=一级分组）
    group_code  VARCHAR(64)     NOT NULL,                           -- 分组编码（唯一，如 ai_quota）
    group_name  VARCHAR(64)     NOT NULL,                           -- 分组名称
    icon        VARCHAR(64),                                        -- 图标 CSS 类名（如 fa fa-cog）
    description VARCHAR(256),                                       -- 分组说明
    sort_order  INTEGER                  DEFAULT 0,                 -- 排序
    is_system   INTEGER         NOT NULL DEFAULT 0,                 -- 是否系统预设（1=不可删除）
    status      INTEGER         NOT NULL DEFAULT 1,                 -- 1=启用 0=禁用
    deleted     INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除
    create_time TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP                DEFAULT CURRENT_TIMESTAMP
);

-- 分组编码唯一（排除已删除记录）
CREATE UNIQUE INDEX uk_sys_config_group_code ON sys_config_group (group_code) WHERE deleted = 0;

-- 按父分组+排序查询
CREATE INDEX idx_sys_config_group_parent_sort ON sys_config_group (parent_id, sort_order) WHERE deleted = 0;

COMMENT ON TABLE  sys_config_group              IS '配置分组表';
COMMENT ON COLUMN sys_config_group.parent_id    IS '父分组 ID（0=一级分组）';
COMMENT ON COLUMN sys_config_group.group_code   IS '分组编码（唯一，如 ai_quota）';
COMMENT ON COLUMN sys_config_group.group_name   IS '分组名称';
COMMENT ON COLUMN sys_config_group.icon         IS '图标 CSS 类名（如 fa fa-cog）';
COMMENT ON COLUMN sys_config_group.description  IS '分组说明';
COMMENT ON COLUMN sys_config_group.sort_order   IS '排序';
COMMENT ON COLUMN sys_config_group.is_system    IS '是否系统预设：1=不可删除 0=可删除';
COMMENT ON COLUMN sys_config_group.status       IS '状态：1=启用 0=禁用';
COMMENT ON COLUMN sys_config_group.deleted      IS '逻辑删除：0=正常 1=已删除';

-- ============================================================
-- 2. sys_config - 系统配置表（与 SysConfig Entity 对齐）
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_config (
    id           BIGSERIAL       PRIMARY KEY,
    config_key   VARCHAR(128)    NOT NULL,                           -- 配置键（唯一）
    config_value TEXT,                                               -- 配置值
    value_type   VARCHAR(16)     NOT NULL DEFAULT 'string',          -- 值类型：string / number / boolean / json / password
    is_sensitive INTEGER         NOT NULL DEFAULT 0,                 -- 是否敏感：1=是 0=否
    config_group VARCHAR(64),                                        -- 配置分组：ai / storage / system
    remark       VARCHAR(256),                                       -- 备注说明
    deleted      INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除
    create_time  TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP                DEFAULT CURRENT_TIMESTAMP
);

-- 配置键唯一（排除已删除记录）
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_config_key ON sys_config (config_key) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sys_config_group ON sys_config (config_group) WHERE deleted = 0;

COMMENT ON TABLE  sys_config               IS '系统配置表';
COMMENT ON COLUMN sys_config.config_key    IS '配置键（唯一）';
COMMENT ON COLUMN sys_config.config_value  IS '配置值';
COMMENT ON COLUMN sys_config.value_type    IS '值类型：string / number / boolean / json / password';
COMMENT ON COLUMN sys_config.is_sensitive  IS '是否敏感：1=是 0=否';
COMMENT ON COLUMN sys_config.config_group  IS '配置分组：ai / storage / system';
COMMENT ON COLUMN sys_config.remark        IS '备注说明';
COMMENT ON COLUMN sys_config.deleted       IS '逻辑删除：0=正常 1=已删除';

-- ============================================================
-- 3. sys_industry - 行业分类表
-- ============================================================
CREATE TABLE sys_industry (
    id             BIGSERIAL       PRIMARY KEY,
    parent_id      BIGINT                   DEFAULT 0,                 -- 父行业 ID（0=一级行业）
    industry_name  VARCHAR(64)     NOT NULL,                           -- 行业名称
    industry_code  VARCHAR(32)     NOT NULL,                           -- 行业编码（唯一）
    icon           VARCHAR(128),                                       -- 行业图标
    sort_order     INTEGER                  DEFAULT 0,                 -- 排序
    status         INTEGER         NOT NULL DEFAULT 1,                 -- 1=启用 0=禁用
    deleted        INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除
    create_time    TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,
    update_time    TIMESTAMP                DEFAULT CURRENT_TIMESTAMP
);

-- 行业编码唯一（排除已删除记录）
CREATE UNIQUE INDEX uk_sys_industry_code ON sys_industry (industry_code) WHERE deleted = 0;

-- 按父行业+排序查询
CREATE INDEX idx_sys_industry_parent_sort ON sys_industry (parent_id, sort_order) WHERE deleted = 0;

COMMENT ON TABLE  sys_industry                IS '行业分类表';
COMMENT ON COLUMN sys_industry.parent_id      IS '父行业 ID（0=一级行业）';
COMMENT ON COLUMN sys_industry.industry_name  IS '行业名称';
COMMENT ON COLUMN sys_industry.industry_code  IS '行业编码（唯一）';
COMMENT ON COLUMN sys_industry.icon           IS '行业图标';
COMMENT ON COLUMN sys_industry.sort_order     IS '排序';
COMMENT ON COLUMN sys_industry.status         IS '状态：1=启用 0=禁用';
COMMENT ON COLUMN sys_industry.deleted        IS '逻辑删除：0=正常 1=已删除';
