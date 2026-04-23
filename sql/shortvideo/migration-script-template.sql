-- ============================================================
-- shortvideo 模块 - 增量迁移：短视频脚本模板
-- ============================================================

CREATE TABLE IF NOT EXISTS sv_script_template (
    id              BIGSERIAL       PRIMARY KEY,
    owner_id        BIGINT,                                             -- 创建者ID（系统模板为空）
    template_name   VARCHAR(256)    NOT NULL,                           -- 模板名称
    template_type   VARCHAR(32)     NOT NULL DEFAULT 'system',          -- 模板类型：system/user
    scene           VARCHAR(64),                                        -- 适用场景：hook/body/cta/full
    category_id     BIGINT,                                             -- 关联分类
    content         TEXT            NOT NULL,                            -- 模板内容
    description     VARCHAR(512),                                       -- 模板描述
    duration_hint   INTEGER,                                            -- 建议时长(秒)
    use_count       BIGINT          NOT NULL DEFAULT 0,                 -- 使用次数
    status          INTEGER         NOT NULL DEFAULT 1,                 -- 状态：1=有效 0=禁用
    deleted         INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除
    create_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_svst_owner ON sv_script_template (owner_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_svst_type ON sv_script_template (template_type) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_svst_scene ON sv_script_template (scene) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_svst_category ON sv_script_template (category_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_svst_status ON sv_script_template (status) WHERE deleted = 0;

COMMENT ON TABLE  sv_script_template                IS '短视频脚本模板表';
COMMENT ON COLUMN sv_script_template.owner_id       IS '创建者ID（系统模板为空）';
COMMENT ON COLUMN sv_script_template.template_name  IS '模板名称';
COMMENT ON COLUMN sv_script_template.template_type  IS '模板类型：system=系统预设 user=用户自建';
COMMENT ON COLUMN sv_script_template.scene          IS '适用场景：hook=开头 body=正文 cta=行动号召 full=完整脚本';
COMMENT ON COLUMN sv_script_template.category_id    IS '关联分类ID';
COMMENT ON COLUMN sv_script_template.content        IS '模板内容';
COMMENT ON COLUMN sv_script_template.description    IS '模板描述';
COMMENT ON COLUMN sv_script_template.duration_hint  IS '建议时长(秒)';
COMMENT ON COLUMN sv_script_template.use_count      IS '使用次数';
