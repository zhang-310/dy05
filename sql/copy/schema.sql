-- ============================================================
-- copy 模块 - 数据库表结构
-- 模块：文案库管理（Copy Management）
-- 数据库：PostgreSQL
-- 版本：1.0
-- 更新日期：2026-02-25
-- 说明：字段名、类型、默认值已与 Entity 层完全对齐
-- ============================================================

-- ============================================================
-- 1. copy_library - 文案库表
-- Entity: cn.gaifan.douyinOperations.module.copy.entity.CopyLibrary
-- ============================================================
CREATE TABLE IF NOT EXISTS copy_library (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,                           -- 用户 ID
    title           VARCHAR(256)    NOT NULL,                           -- 文案标题
    content         TEXT            NOT NULL,                           -- 文案内容
    category        VARCHAR(64),                                        -- 分类：商品描述/推广/营销/其他
    tags            VARCHAR(512),                                       -- 标签（逗号分隔）
    word_count      INTEGER,                                            -- 字数
    use_count       INTEGER         NOT NULL DEFAULT 0,                 -- 使用次数
    rating          INTEGER,                                            -- 评分 1-5
    status          INTEGER         NOT NULL DEFAULT 0,                 -- 状态：1=已审核 0=待审核
    deleted         INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除：0=正常 1=已删除
    create_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    update_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP  -- 更新时间
);

CREATE INDEX IF NOT EXISTS idx_copy_library_user_id ON copy_library (user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_copy_library_category ON copy_library (category) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_copy_library_status ON copy_library (status) WHERE deleted = 0;

COMMENT ON TABLE  copy_library                  IS '文案库表';
COMMENT ON COLUMN copy_library.user_id          IS '用户 ID';
COMMENT ON COLUMN copy_library.title            IS '文案标题';
COMMENT ON COLUMN copy_library.content          IS '文案内容';
COMMENT ON COLUMN copy_library.category         IS '分类：商品描述/推广/营销/其他';
COMMENT ON COLUMN copy_library.tags             IS '标签（逗号分隔）';
COMMENT ON COLUMN copy_library.word_count       IS '字数';
COMMENT ON COLUMN copy_library.use_count        IS '使用次数';
COMMENT ON COLUMN copy_library.rating           IS '评分 1-5';
COMMENT ON COLUMN copy_library.status           IS '状态：1=已审核 0=待审核';
COMMENT ON COLUMN copy_library.deleted          IS '逻辑删除：0=正常 1=已删除';

-- ============================================================
-- 2. copy_approval - 文案审核表
-- Entity: cn.gaifan.douyinOperations.module.copy.entity.CopyApproval
-- ============================================================
CREATE TABLE IF NOT EXISTS copy_approval (
    id              BIGSERIAL       PRIMARY KEY,
    copy_id         BIGINT          NOT NULL,                           -- 文案 ID
    user_id         BIGINT,                                             -- 审核员 ID
    approval_status INTEGER         NOT NULL DEFAULT 2,                 -- 审核状态：1=通过 0=拒绝 2=待审核
    comments        TEXT,                                               -- 审核评论
    approval_time   TIMESTAMP,                                          -- 审核时间
    deleted         INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除：0=正常 1=已删除
    create_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    update_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP  -- 更新时间
);

CREATE INDEX IF NOT EXISTS idx_copy_approval_copy_id ON copy_approval (copy_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_copy_approval_user_id ON copy_approval (user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_copy_approval_status ON copy_approval (approval_status) WHERE deleted = 0;

COMMENT ON TABLE  copy_approval                 IS '文案审核表';
COMMENT ON COLUMN copy_approval.copy_id         IS '文案 ID';
COMMENT ON COLUMN copy_approval.user_id         IS '审核员 ID';
COMMENT ON COLUMN copy_approval.approval_status IS '审核状态：1=通过 0=拒绝 2=待审核';
COMMENT ON COLUMN copy_approval.comments        IS '审核评论';
COMMENT ON COLUMN copy_approval.approval_time   IS '审核时间';
COMMENT ON COLUMN copy_approval.deleted         IS '逻辑删除：0=正常 1=已删除';

-- ============================================================
-- 3. copy_template - 文案模板表
-- Entity: cn.gaifan.douyinOperations.module.copy.entity.CopyTemplate
-- ============================================================
CREATE TABLE IF NOT EXISTS copy_template (
    id                  BIGSERIAL       PRIMARY KEY,
    user_id             BIGINT          NOT NULL,                           -- 用户 ID
    template_name       VARCHAR(256)    NOT NULL,                           -- 模板名称
    template_content    TEXT            NOT NULL,                           -- 模板内容（包含 {变量} 占位符）
    category            VARCHAR(64),                                        -- 分类
    description         TEXT,                                               -- 描述
    status              INTEGER         NOT NULL DEFAULT 1,                 -- 状态：1=有效 0=禁用
    deleted             INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除：0=正常 1=已删除
    create_time         TIMESTAMP                DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    update_time         TIMESTAMP                DEFAULT CURRENT_TIMESTAMP  -- 更新时间
);

CREATE INDEX IF NOT EXISTS idx_copy_template_user_id ON copy_template (user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_copy_template_category ON copy_template (category) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_copy_template_status ON copy_template (status) WHERE deleted = 0;

COMMENT ON TABLE  copy_template                 IS '文案模板表';
COMMENT ON COLUMN copy_template.user_id         IS '用户 ID';
COMMENT ON COLUMN copy_template.template_name   IS '模板名称';
COMMENT ON COLUMN copy_template.template_content IS '模板内容（包含 {变量} 占位符）';
COMMENT ON COLUMN copy_template.category        IS '分类';
COMMENT ON COLUMN copy_template.description     IS '描述';
COMMENT ON COLUMN copy_template.status          IS '状态：1=有效 0=禁用';
COMMENT ON COLUMN copy_template.deleted         IS '逻辑删除：0=正常 1=已删除';
