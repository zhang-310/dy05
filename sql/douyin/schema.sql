-- ============================================================
-- douyin 模块 - 数据库表结构
-- 模块：抖音账号管理（Douyin Account Management）
-- 数据库：PostgreSQL
-- 版本：1.0
-- 更新日期：2026-02-25
-- 说明：字段名、类型、默认值已与 Entity 层完全对齐
-- ============================================================

-- ============================================================
-- 1. douyin_account - 抖音账号表
-- Entity: cn.gaifan.douyinOperations.module.douyin.entity.DouyinAccount
-- ============================================================
CREATE TABLE IF NOT EXISTS douyin_account (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,                              -- 用户 ID，关联 auth_user.id
    account_name    VARCHAR(128)    NOT NULL,                              -- 账号名称
    account_id      VARCHAR(128)    NOT NULL,                              -- 账号 ID（唯一标识）
    follow_count    BIGINT          NOT NULL DEFAULT 0,                    -- 关注数
    fan_count       BIGINT          NOT NULL DEFAULT 0,                    -- 粉丝数
    video_count     BIGINT          NOT NULL DEFAULT 0,                    -- 视频数
    total_likes     BIGINT          NOT NULL DEFAULT 0,                    -- 总获赞数
    description     VARCHAR(512),                                           -- 账号描述
    status          INTEGER         NOT NULL DEFAULT 0,                    -- 状态：0=正常 1=禁用
    bind_time       TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,    -- 绑定时间
    deleted         INTEGER         NOT NULL DEFAULT 0,                    -- 逻辑删除：0=正常 1=已删除
    create_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,    -- 创建时间
    update_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP     -- 更新时间
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_douyin_account_id ON douyin_account (account_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_douyin_account_user_id ON douyin_account (user_id) WHERE deleted = 0;

COMMENT ON TABLE  douyin_account              IS '抖音账号表';
COMMENT ON COLUMN douyin_account.user_id      IS '用户 ID，关联 auth_user.id';
COMMENT ON COLUMN douyin_account.account_name IS '账号名称';
COMMENT ON COLUMN douyin_account.account_id   IS '账号 ID（唯一标识）';
COMMENT ON COLUMN douyin_account.follow_count IS '关注数';
COMMENT ON COLUMN douyin_account.fan_count    IS '粉丝数';
COMMENT ON COLUMN douyin_account.video_count  IS '视频数';
COMMENT ON COLUMN douyin_account.total_likes  IS '总获赞数';
COMMENT ON COLUMN douyin_account.description  IS '账号描述';
COMMENT ON COLUMN douyin_account.status       IS '状态：0=正常 1=禁用';
COMMENT ON COLUMN douyin_account.bind_time    IS '绑定时间';
COMMENT ON COLUMN douyin_account.deleted      IS '逻辑删除：0=正常 1=已删除';

-- ============================================================
-- 2. douyin_video - 抖音视频表
-- Entity: cn.gaifan.douyinOperations.module.douyin.entity.DouyinVideo
-- ============================================================
CREATE TABLE IF NOT EXISTS douyin_video (
    id              BIGSERIAL       PRIMARY KEY,
    account_id      BIGINT          NOT NULL,                              -- 账号 ID，关联 douyin_account.id
    video_id        VARCHAR(128)    NOT NULL,                              -- 视频 ID（唯一标识）
    title           VARCHAR(256)    NOT NULL,                              -- 视频标题
    description     VARCHAR(1024),                                          -- 视频描述
    view_count      BIGINT          NOT NULL DEFAULT 0,                    -- 观看数
    like_count      BIGINT          NOT NULL DEFAULT 0,                    -- 点赞数
    share_count     BIGINT          NOT NULL DEFAULT 0,                    -- 分享数
    comment_count   BIGINT          NOT NULL DEFAULT 0,                    -- 评论数
    download_count  BIGINT          NOT NULL DEFAULT 0,                    -- 下载数
    video_type      VARCHAR(32),                                            -- 视频类型
    publish_time    TIMESTAMP,                                              -- 发布时间
    deleted         INTEGER         NOT NULL DEFAULT 0,                    -- 逻辑删除：0=正常 1=已删除
    create_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,    -- 创建时间
    update_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP     -- 更新时间
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_douyin_video_id ON douyin_video (video_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_douyin_video_account_id ON douyin_video (account_id) WHERE deleted = 0;

COMMENT ON TABLE  douyin_video              IS '抖音视频表';
COMMENT ON COLUMN douyin_video.account_id   IS '账号 ID，关联 douyin_account.id';
COMMENT ON COLUMN douyin_video.video_id     IS '视频 ID（唯一标识）';
COMMENT ON COLUMN douyin_video.title        IS '视频标题';
COMMENT ON COLUMN douyin_video.description  IS '视频描述';
COMMENT ON COLUMN douyin_video.view_count   IS '观看数';
COMMENT ON COLUMN douyin_video.like_count   IS '点赞数';
COMMENT ON COLUMN douyin_video.share_count  IS '分享数';
COMMENT ON COLUMN douyin_video.comment_count IS '评论数';
COMMENT ON COLUMN douyin_video.download_count IS '下载数';
COMMENT ON COLUMN douyin_video.video_type   IS '视频类型';
COMMENT ON COLUMN douyin_video.publish_time IS '发布时间';
COMMENT ON COLUMN douyin_video.deleted      IS '逻辑删除：0=正常 1=已删除';

-- ============================================================
-- 3. dy_persona - 抖音人设表
-- Entity: cn.gaifan.douyinOperations.module.douyin.entity.DyPersona
-- ============================================================
CREATE TABLE IF NOT EXISTS dy_persona (
    id               BIGSERIAL       PRIMARY KEY,
    owner_id         BIGINT          NOT NULL,                              -- 所属用户 ID
    account_id       BIGINT,                                                -- 关联账号 ID（可空，人设可跨账号复用）
    persona_name     VARCHAR(128)    NOT NULL,                              -- 人设名称
    persona_type     VARCHAR(32),                                           -- 人设类型：knowledge/entertainment/lifestyle/commerce
    description      TEXT,                                                  -- 人设描述
    tone             VARCHAR(64),                                           -- 语气风格：professional/casual/humorous/warm
    target_audience  VARCHAR(256),                                          -- 目标受众描述
    content_style    TEXT,                                                  -- 内容风格说明
    keywords         VARCHAR(512),                                          -- 关键词（逗号分隔）
    is_default       INTEGER         NOT NULL DEFAULT 0,                    -- 是否默认人设：0=否 1=是
    status           INTEGER         NOT NULL DEFAULT 1,                    -- 状态：0=禁用 1=启用
    deleted          INTEGER         NOT NULL DEFAULT 0,                    -- 逻辑删除：0=正常 1=已删除
    create_time      TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,    -- 创建时间
    update_time      TIMESTAMP                DEFAULT CURRENT_TIMESTAMP     -- 更新时间
);

CREATE INDEX IF NOT EXISTS idx_dy_persona_owner    ON dy_persona (owner_id, deleted);
CREATE INDEX IF NOT EXISTS idx_dy_persona_account  ON dy_persona (account_id, deleted);

COMMENT ON TABLE  dy_persona                  IS '抖音人设表';
COMMENT ON COLUMN dy_persona.owner_id         IS '所属用户 ID';
COMMENT ON COLUMN dy_persona.account_id       IS '关联账号 ID（可空，人设可跨账号复用）';
COMMENT ON COLUMN dy_persona.persona_name     IS '人设名称';
COMMENT ON COLUMN dy_persona.persona_type     IS '人设类型：knowledge=知识 entertainment=娱乐 lifestyle=生活 commerce=带货';
COMMENT ON COLUMN dy_persona.tone             IS '语气风格：professional=专业 casual=随意 humorous=幽默 warm=温暖';
COMMENT ON COLUMN dy_persona.target_audience  IS '目标受众描述';
COMMENT ON COLUMN dy_persona.content_style    IS '内容风格说明';
COMMENT ON COLUMN dy_persona.keywords         IS '关键词（逗号分隔）';
COMMENT ON COLUMN dy_persona.is_default       IS '是否默认人设：0=否 1=是';
