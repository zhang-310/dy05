-- ============================================================
-- script 模块 - 数据库表结构
-- 模块：话术库与违规词管理（Script Library & Violation Word Management）
-- 数据库：PostgreSQL
-- 版本：1.0
-- 更新日期：2026-02-25
-- 说明：包含话术库、违规词库、检测记录三个表
-- ============================================================

-- ============================================================
-- 1. script_library - 话术库表
-- Entity: cn.gaifan.douyinOperations.module.script.entity.Script
-- ============================================================
CREATE TABLE IF NOT EXISTS script_library (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,                           -- 用户ID
    title           VARCHAR(256)    NOT NULL,                           -- 话术标题
    content         TEXT,                                               -- 话术内容
    category        VARCHAR(32),                                        -- 分类：售前/售后/促销/其他
    tags            TEXT,                                               -- 标签（JSON格式）
    use_count       BIGINT          NOT NULL DEFAULT 0,                 -- 使用次数
    status          INTEGER         NOT NULL DEFAULT 1,                 -- 状态：1=有效 0=禁用
    deleted         INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除：0=正常 1=已删除
    create_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    update_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP  -- 更新时间
);

CREATE INDEX IF NOT EXISTS idx_script_user_id ON script_library (user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_script_category ON script_library (category) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_script_status ON script_library (status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_script_create_time ON script_library (create_time) WHERE deleted = 0;

COMMENT ON TABLE  script_library                 IS '话术库表';
COMMENT ON COLUMN script_library.user_id         IS '用户ID';
COMMENT ON COLUMN script_library.title           IS '话术标题';
COMMENT ON COLUMN script_library.content         IS '话术内容';
COMMENT ON COLUMN script_library.category        IS '分类：售前/售后/促销/其他';
COMMENT ON COLUMN script_library.tags            IS '标签（JSON格式）';
COMMENT ON COLUMN script_library.use_count       IS '使用次数';
COMMENT ON COLUMN script_library.status          IS '状态：1=有效 0=禁用';
COMMENT ON COLUMN script_library.deleted         IS '逻辑删除：0=正常 1=已删除';

-- ============================================================
-- 2. violation_word - 违规词库表
-- Entity: cn.gaifan.douyinOperations.module.script.entity.ViolationWord
-- ============================================================
CREATE TABLE IF NOT EXISTS violation_word (
    id              BIGSERIAL       PRIMARY KEY,
    word            VARCHAR(256)    NOT NULL,                           -- 违规词
    level           INTEGER         NOT NULL,                           -- 严重程度：1=低 2=中 3=高
    reason          VARCHAR(128),                                       -- 违规原因：虚假宣传/敏感词/广告法
    replacement     VARCHAR(256),                                       -- 推荐替代词
    status          INTEGER         NOT NULL DEFAULT 1,                 -- 状态：1=有效 0=禁用
    deleted         INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除：0=正常 1=已删除
    create_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    update_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP  -- 更新时间
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_violation_word ON violation_word (word) WHERE deleted = 0 AND status = 1;
CREATE INDEX IF NOT EXISTS idx_violation_level ON violation_word (level) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_violation_status ON violation_word (status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_violation_reason ON violation_word (reason) WHERE deleted = 0;

COMMENT ON TABLE  violation_word                 IS '违规词库表';
COMMENT ON COLUMN violation_word.word            IS '违规词';
COMMENT ON COLUMN violation_word.level           IS '严重程度：1=低 2=中 3=高';
COMMENT ON COLUMN violation_word.reason          IS '违规原因：虚假宣传/敏感词/广告法';
COMMENT ON COLUMN violation_word.replacement     IS '推荐替代词';
COMMENT ON COLUMN violation_word.status          IS '状态：1=有效 0=禁用';
COMMENT ON COLUMN violation_word.deleted         IS '逻辑删除：0=正常 1=已删除';

-- ============================================================
-- 3. script_check - 话术检测记录表
-- Entity: cn.gaifan.douyinOperations.module.script.entity.ScriptCheck
-- ============================================================
CREATE TABLE IF NOT EXISTS script_check (
    id              BIGSERIAL       PRIMARY KEY,
    script_id       BIGINT          NOT NULL,                           -- 话术ID
    check_time      TIMESTAMP       NOT NULL,                           -- 检测时间
    violation_count INTEGER         NOT NULL DEFAULT 0,                 -- 违规词数量
    violations      TEXT,                                               -- 违规词列表（JSON格式）
    status          INTEGER         NOT NULL DEFAULT 1,                 -- 检测状态：1=通过 0=不通过
    deleted         INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除：0=正常 1=已删除
    create_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    update_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP  -- 更新时间
);

CREATE INDEX IF NOT EXISTS idx_check_script_id ON script_check (script_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_check_time ON script_check (check_time) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_check_status ON script_check (status) WHERE deleted = 0;

COMMENT ON TABLE  script_check                   IS '话术检测记录表';
COMMENT ON COLUMN script_check.script_id         IS '话术ID';
COMMENT ON COLUMN script_check.check_time        IS '检测时间';
COMMENT ON COLUMN script_check.violation_count   IS '违规词数量';
COMMENT ON COLUMN script_check.violations        IS '违规词列表（JSON格式）';
COMMENT ON COLUMN script_check.status            IS '检测状态：1=通过 0=不通过';
COMMENT ON COLUMN script_check.deleted           IS '逻辑删除：0=正常 1=已删除';

-- ============================================================
-- 4. sc_compliance_word - 合规词库表
-- Entity: cn.gaifan.douyinOperations.module.script.entity.ComplianceWord
-- ============================================================
CREATE TABLE IF NOT EXISTS sc_compliance_word (
    id              BIGSERIAL       PRIMARY KEY,
    word_type       VARCHAR(16)     NOT NULL,                           -- absolute=绝对化用语(可自动修复) medical=医疗功效(不可修复)
    word_value      VARCHAR(128)    NOT NULL,                            -- 待检测词
    replacement     VARCHAR(256),                                        -- 替换建议（absolute 有值，medical 为空）
    is_enabled      INTEGER         NOT NULL DEFAULT 1,                  -- 1=启用 0=禁用
    deleted         INTEGER         NOT NULL DEFAULT 0,                  -- 逻辑删除
    create_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sc_compliance_word_type ON sc_compliance_word (word_type) WHERE deleted = 0 AND is_enabled = 1;
CREATE UNIQUE INDEX IF NOT EXISTS uk_sc_compliance_word ON sc_compliance_word (word_type, word_value) WHERE deleted = 0;

COMMENT ON TABLE  sc_compliance_word           IS '合规词库：绝对化用语、医疗功效词';
COMMENT ON COLUMN sc_compliance_word.word_type IS 'absolute=可自动修复 medical=不可修复';
COMMENT ON COLUMN sc_compliance_word.word_value IS '待检测词';
COMMENT ON COLUMN sc_compliance_word.replacement IS '替换建议（仅 absolute 类型）';
COMMENT ON COLUMN sc_compliance_word.is_enabled IS '1=启用 0=禁用';
