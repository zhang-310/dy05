-- ============================================================
-- script 模块 - 增量迁移：话术模板 + 用户自定义违规词
-- ============================================================

-- 1. sc_script_template - 话术模板表
CREATE TABLE IF NOT EXISTS sc_script_template (
    id              BIGSERIAL       PRIMARY KEY,
    template_name   VARCHAR(256)    NOT NULL,                           -- 模板名称
    template_type   VARCHAR(32)     NOT NULL DEFAULT 'system',          -- 模板类型：system/user
    scene           VARCHAR(64),                                        -- 适用场景：opening/product/transition/closing/general
    content         TEXT            NOT NULL,                            -- 模板内容
    description     VARCHAR(512),                                       -- 模板描述
    user_id         BIGINT,                                             -- 创建用户ID（system类型为空）
    use_count       BIGINT          NOT NULL DEFAULT 0,                 -- 使用次数
    status          INTEGER         NOT NULL DEFAULT 1,                 -- 状态：1=有效 0=禁用
    deleted         INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除
    create_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sst_type ON sc_script_template (template_type) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sst_scene ON sc_script_template (scene) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sst_user_id ON sc_script_template (user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sst_status ON sc_script_template (status) WHERE deleted = 0;

COMMENT ON TABLE  sc_script_template                IS '话术模板表';
COMMENT ON COLUMN sc_script_template.template_name  IS '模板名称';
COMMENT ON COLUMN sc_script_template.template_type  IS '模板类型：system=系统预设 user=用户自建';
COMMENT ON COLUMN sc_script_template.scene          IS '适用场景：opening/product/transition/closing/general';
COMMENT ON COLUMN sc_script_template.content        IS '模板内容';
COMMENT ON COLUMN sc_script_template.description    IS '模板描述';
COMMENT ON COLUMN sc_script_template.user_id        IS '创建用户ID';
COMMENT ON COLUMN sc_script_template.use_count      IS '使用次数';

-- 2. sc_user_violation_word - 用户自定义违规词表
CREATE TABLE IF NOT EXISTS sc_user_violation_word (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,                           -- 用户ID
    word            VARCHAR(256)    NOT NULL,                           -- 违规词
    level           INTEGER         NOT NULL DEFAULT 2,                 -- 严重程度：1=低 2=中 3=高
    reason          VARCHAR(128),                                       -- 违规原因
    replacement     VARCHAR(256),                                       -- 推荐替代词
    status          INTEGER         NOT NULL DEFAULT 1,                 -- 状态：1=有效 0=禁用
    deleted         INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除
    create_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_uvw_user_word ON sc_user_violation_word (user_id, word) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_uvw_user_id ON sc_user_violation_word (user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_uvw_level ON sc_user_violation_word (level) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_uvw_status ON sc_user_violation_word (status) WHERE deleted = 0;

COMMENT ON TABLE  sc_user_violation_word            IS '用户自定义违规词表';
COMMENT ON COLUMN sc_user_violation_word.user_id    IS '用户ID';
COMMENT ON COLUMN sc_user_violation_word.word       IS '违规词';
COMMENT ON COLUMN sc_user_violation_word.level      IS '严重程度：1=低 2=中 3=高';
COMMENT ON COLUMN sc_user_violation_word.reason     IS '违规原因';
COMMENT ON COLUMN sc_user_violation_word.replacement IS '推荐替代词';
