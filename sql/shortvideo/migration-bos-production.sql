-- ============================================================
-- 短视频模块 BOS 集成升级 - 迁移脚本
-- 版本: v1.0
-- 日期: 2026-03-01
-- 说明: 新增短视频生产流程表 + sv_plan_asset 添加 bos_key
-- 依赖: schema.sql, BAIDU-BOS-STORAGE-INTEGRATION.md
-- ============================================================

-- 1. sv_plan_asset 添加 bos_key（用于 BOS 删除）
ALTER TABLE sv_plan_asset ADD COLUMN IF NOT EXISTS bos_key VARCHAR(500);
COMMENT ON COLUMN sv_plan_asset.bos_key IS 'BOS 对象 Key（用于删除）';

-- 2. sv_project — 短视频项目表（对应 short_video_project）
CREATE TABLE IF NOT EXISTS sv_project (
    id                 BIGSERIAL    PRIMARY KEY,
    owner_id           BIGINT       NOT NULL,                        -- 所属用户 ID（数据隔离）
    account_id         BIGINT,                                       -- 关联抖音账号 ID
    title              VARCHAR(255) NOT NULL,                        -- 项目标题
    project_type       VARCHAR(50)  NOT NULL,                        -- 类型：viral_clone/daily/soft_ad
    status             VARCHAR(50)  NOT NULL DEFAULT 'draft',       -- 状态：draft/processing/completed/failed

    script_id          BIGINT,                                       -- 脚本 ID
    shot_list_id       BIGINT,                                       -- 分镜 ID

    -- 成果（BOS CDN URL）
    final_video_url    VARCHAR(500),                                 -- 成片 URL（BOS）
    thumbnail_url      VARCHAR(500),                                 -- 封面 URL（BOS）
    duration           INTEGER,                                      -- 时长（秒）

    -- 发布信息
    publish_title      VARCHAR(255),                                 -- 发布标题
    publish_platforms  TEXT,                                         -- 发布平台（JSON）
    publish_time       TIMESTAMP,                                    -- 发布时间

    -- 审核信息
    review_status      VARCHAR(50),                                  -- 审核状态：pending/passed/rejected
    reviewer_id         BIGINT,                                       -- 审核人 ID
    review_time         TIMESTAMP,                                    -- 审核时间
    review_comment      TEXT,                                         -- 审核意见

    deleted            INTEGER      NOT NULL DEFAULT 0,
    create_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sv_project_owner ON sv_project (owner_id, status, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_sv_project_type ON sv_project (project_type);
CREATE INDEX IF NOT EXISTS idx_sv_project_script ON sv_project (script_id);
CREATE INDEX IF NOT EXISTS idx_sv_project_shot_list ON sv_project (shot_list_id);

COMMENT ON TABLE  sv_project              IS '短视频项目表';
COMMENT ON COLUMN sv_project.owner_id    IS '所属用户 ID';
COMMENT ON COLUMN sv_project.project_type IS '类型：viral_clone/daily/soft_ad';
COMMENT ON COLUMN sv_project.status       IS '状态：draft/processing/completed/failed';
COMMENT ON COLUMN sv_project.final_video_url IS '成片 URL（BOS CDN）';
COMMENT ON COLUMN sv_project.thumbnail_url   IS '封面 URL（BOS CDN）';

-- 3. sv_script — 脚本表（对应 short_video_script）
CREATE TABLE IF NOT EXISTS sv_script (
    id                 BIGSERIAL    PRIMARY KEY,
    owner_id           BIGINT       NOT NULL,                        -- 所属用户 ID
    title              VARCHAR(255) NOT NULL,                        -- 脚本标题
    content            TEXT         NOT NULL,                          -- 脚本内容
    script_type        VARCHAR(50)  NOT NULL,                        -- 类型：viral_clone/daily/soft_ad

    generation_type    VARCHAR(50),                                   -- 生成方式：ai/manual
    reference_viral_id BIGINT,                                        -- 参考爆款视频 ID
    theme              VARCHAR(255),                                  -- 主题
    style              VARCHAR(50),                                   -- 风格：funny/emotional/educational

    duration           INTEGER,                                       -- 预估时长（秒）
    word_count         INTEGER,                                       -- 字数
    tags               TEXT,                                          -- 标签（JSON）

    ai_prompt          TEXT,                                          -- AI 提示词
    ai_model           VARCHAR(100),                                  -- AI 模型

    deleted            INTEGER      NOT NULL DEFAULT 0,
    create_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sv_script_owner ON sv_script (owner_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_sv_script_type ON sv_script (script_type);
CREATE INDEX IF NOT EXISTS idx_sv_script_style ON sv_script (style);

COMMENT ON TABLE  sv_script              IS '脚本表';
COMMENT ON COLUMN sv_script.script_type  IS '类型：viral_clone/daily/soft_ad';

-- 4. sv_shot_list — 分镜列表表（对应 short_video_shot_list）
CREATE TABLE IF NOT EXISTS sv_shot_list (
    id                 BIGSERIAL    PRIMARY KEY,
    owner_id           BIGINT       NOT NULL,                        -- 所属用户 ID
    script_id          BIGINT       NOT NULL,                        -- 脚本 ID
    shot_count         INTEGER      NOT NULL,                        -- 分镜数量

    deleted            INTEGER      NOT NULL DEFAULT 0,
    create_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sv_shot_list_owner ON sv_shot_list (owner_id);
CREATE INDEX IF NOT EXISTS idx_sv_shot_list_script ON sv_shot_list (script_id);

COMMENT ON TABLE  sv_shot_list           IS '分镜列表表';

-- 5. sv_shot — 分镜详情表（对应 short_video_shot）
CREATE TABLE IF NOT EXISTS sv_shot (
    id                 BIGSERIAL    PRIMARY KEY,
    shot_list_id       BIGINT       NOT NULL,                        -- 分镜列表 ID
    shot_number        INTEGER      NOT NULL,                        -- 分镜序号

    time_range         VARCHAR(50),                                   -- 时间范围：0-3s
    scene_description  TEXT,                                          -- 场景描述
    camera_angle       VARCHAR(100),                                  -- 机位：俯拍/平拍/仰拍
    action             VARCHAR(255),                                  -- 动作描述
    dialogue           TEXT,                                           -- 台词
    mood               VARCHAR(100),                                  -- 情绪：神秘/搞笑/感动

    -- 素材（BOS CDN URL）
    keyframe_url       VARCHAR(500),                                  -- 关键帧图片 URL（BOS）
    keyframe_bos_key   VARCHAR(500),                                  -- 关键帧 BOS Key（用于删除）
    video_url          VARCHAR(500),                                  -- 视频素材 URL（BOS）
    video_bos_key      VARCHAR(500),                                  -- 视频 BOS Key（用于删除）
    audio_url          VARCHAR(500),                                  -- 音频 URL（BOS）
    audio_bos_key      VARCHAR(500),                                  -- 音频 BOS Key（用于删除）

    duration           INTEGER,                                       -- 时长（秒）

    deleted            INTEGER      NOT NULL DEFAULT 0,
    create_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sv_shot_list_id ON sv_shot (shot_list_id);
CREATE INDEX IF NOT EXISTS idx_sv_shot_number ON sv_shot (shot_number);

COMMENT ON TABLE  sv_shot               IS '分镜详情表';
COMMENT ON COLUMN sv_shot.keyframe_url  IS '关键帧图片 URL（BOS CDN）';
COMMENT ON COLUMN sv_shot.video_url     IS '视频素材 URL（BOS CDN）';
COMMENT ON COLUMN sv_shot.audio_url     IS '音频 URL（BOS CDN）';

-- 6. sv_material — 素材库表（对应 short_video_material）
CREATE TABLE IF NOT EXISTS sv_material (
    id                 BIGSERIAL    PRIMARY KEY,
    owner_id           BIGINT       NOT NULL,                        -- 所属用户 ID
    material_type      VARCHAR(50)  NOT NULL,                        -- 类型：image/video/audio
    url                VARCHAR(500) NOT NULL,                        -- 素材 URL（BOS CDN URL）
    bos_key            VARCHAR(500),                                  -- BOS 对象 Key（用于删除）

    shot_id            BIGINT,                                        -- 分镜 ID
    project_id         BIGINT,                                        -- 项目 ID

    file_size          BIGINT,                                        -- 文件大小（字节）
    duration           INTEGER,                                        -- 时长（秒，视频/音频）
    width              INTEGER,                                        -- 宽度（图片/视频）
    height             INTEGER,                                        -- 高度（图片/视频）
    format_type        VARCHAR(50),                                   -- 格式：mp4/png/mp3

    generation_type    VARCHAR(50),                                   -- 生成方式：ai/upload
    ai_prompt          TEXT,                                           -- AI 提示词
    ai_model           VARCHAR(100),                                   -- AI 模型

    deleted            INTEGER      NOT NULL DEFAULT 0,
    create_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sv_material_owner ON sv_material (owner_id);
CREATE INDEX IF NOT EXISTS idx_sv_material_type ON sv_material (material_type);
CREATE INDEX IF NOT EXISTS idx_sv_material_shot ON sv_material (shot_id);
CREATE INDEX IF NOT EXISTS idx_sv_material_project ON sv_material (project_id);

COMMENT ON TABLE  sv_material             IS '素材库表';
COMMENT ON COLUMN sv_material.url          IS '素材 URL（BOS CDN）';
COMMENT ON COLUMN sv_material.bos_key      IS 'BOS 对象 Key（用于删除）';
