-- ============================================================
-- 短视频模块 (shortvideo) - 表结构
-- 数据库：PostgreSQL
-- 说明：短视频、数据快照、评论、创作方案、爆款库、热点话题等
-- ============================================================

-- ============================================================
-- 1. sv_video — 短视频表
-- Entity: cn.gaifan.douyinOperations.module.shortvideo.entity.SvVideo
-- ============================================================
CREATE TABLE IF NOT EXISTS sv_video (
    id               BIGSERIAL    PRIMARY KEY,
    owner_id         BIGINT       NOT NULL,                              -- 所属用户 ID
    account_id       BIGINT       NOT NULL,                              -- 关联抖音账号 ID
    douyin_video_id  VARCHAR(128),                                       -- 抖音视频 ID（同步去重）
    title            VARCHAR(512),                                       -- 视频标题
    description      TEXT,                                                -- 视频描述
    cover_url        VARCHAR(512),                                       -- 封面图 URL
    video_url        VARCHAR(512),                                       -- 视频链接
    duration         INTEGER      DEFAULT 0,                             -- 视频时长（秒）
    tags             VARCHAR(512),                                       -- 标签（JSON 数组）
    category_id      BIGINT,                                             -- 分类 ID
    publish_time     TIMESTAMP,                                          -- 发布时间
    view_count       BIGINT       DEFAULT 0,                             -- 最新播放量
    like_count       INTEGER      DEFAULT 0,                             -- 最新点赞数
    comment_count    INTEGER      DEFAULT 0,                             -- 最新评论数
    share_count      INTEGER      DEFAULT 0,                             -- 最新分享数
    favorite_count   INTEGER      DEFAULT 0,                             -- 最新收藏数
    is_viral         BOOLEAN      DEFAULT false,                         -- 是否爆款
    ai_call_log_id   BIGINT,                                             -- 关联 AI 调用日志 ID
    ai_generated     BOOLEAN      DEFAULT false,                         -- 是否 AI 生成内容
    plan_id          BIGINT,                                             -- 关联创作方案 ID
    sync_status      VARCHAR(16)  DEFAULT 'synced',                      -- 同步状态：synced / pending / failed
    last_sync_time   TIMESTAMP,                                          -- 最后同步时间
    deleted          INTEGER      NOT NULL DEFAULT 0,                    -- 逻辑删除
    create_time      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sv_video_owner    ON sv_video (owner_id, publish_time DESC);
CREATE INDEX IF NOT EXISTS idx_sv_video_account  ON sv_video (account_id, publish_time DESC);
CREATE UNIQUE INDEX IF NOT EXISTS idx_sv_video_douyin ON sv_video (douyin_video_id);
CREATE INDEX IF NOT EXISTS idx_sv_video_viral    ON sv_video (is_viral, view_count DESC);
CREATE INDEX IF NOT EXISTS idx_sv_video_ai       ON sv_video (ai_call_log_id);
CREATE INDEX IF NOT EXISTS idx_sv_video_category ON sv_video (category_id);

COMMENT ON TABLE  sv_video                IS '短视频表';
COMMENT ON COLUMN sv_video.owner_id       IS '所属用户 ID';
COMMENT ON COLUMN sv_video.account_id     IS '关联抖音账号 ID';
COMMENT ON COLUMN sv_video.douyin_video_id IS '抖音视频 ID（同步去重）';
COMMENT ON COLUMN sv_video.is_viral       IS '是否爆款';
COMMENT ON COLUMN sv_video.sync_status    IS '同步状态：synced / pending / failed';

-- ============================================================
-- 2. sv_video_data — 视频数据每日快照表
-- Entity: cn.gaifan.douyinOperations.module.shortvideo.entity.SvVideoData
-- 注意：无 deleted 字段，快照数据按 snapshot_date 归档
-- ============================================================
CREATE TABLE IF NOT EXISTS sv_video_data (
    id             BIGSERIAL  PRIMARY KEY,
    video_id       BIGINT     NOT NULL,                              -- 关联视频 ID
    snapshot_date  DATE       NOT NULL,                              -- 快照日期
    view_count     BIGINT     DEFAULT 0,
    like_count     INTEGER    DEFAULT 0,
    comment_count  INTEGER    DEFAULT 0,
    share_count    INTEGER    DEFAULT 0,
    favorite_count INTEGER    DEFAULT 0,
    new_followers  INTEGER    DEFAULT 0,                             -- 该视频带来的新增粉丝
    view_delta     BIGINT     DEFAULT 0,                             -- 较前一天播放量增量
    like_delta     INTEGER    DEFAULT 0,                             -- 较前一天点赞增量
    create_time    TIMESTAMP  DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_sv_vd_video_date ON sv_video_data (video_id, snapshot_date);
CREATE INDEX IF NOT EXISTS idx_sv_vd_date ON sv_video_data (snapshot_date);

COMMENT ON TABLE  sv_video_data             IS '视频数据每日快照表';
COMMENT ON COLUMN sv_video_data.video_id    IS '关联视频 ID';
COMMENT ON COLUMN sv_video_data.snapshot_date IS '快照日期';
COMMENT ON COLUMN sv_video_data.view_delta  IS '较前一天播放量增量';

-- ============================================================
-- 3. sv_comment — 视频评论表
-- Entity: cn.gaifan.douyinOperations.module.shortvideo.entity.SvComment
-- ============================================================
CREATE TABLE IF NOT EXISTS sv_comment (
    id                 BIGSERIAL    PRIMARY KEY,
    video_id           BIGINT       NOT NULL,                        -- 关联视频 ID
    douyin_comment_id  VARCHAR(128),                                 -- 抖音评论 ID（同步去重）
    content            TEXT         NOT NULL,                        -- 评论内容
    author_name        VARCHAR(128),                                 -- 评论者昵称
    author_avatar      VARCHAR(512),                                 -- 评论者头像
    like_count         INTEGER      DEFAULT 0,
    reply_count        INTEGER      DEFAULT 0,
    sentiment          VARCHAR(16),                                  -- 情感标签：positive / neutral / negative
    sentiment_score    DECIMAL(3,2),                                 -- 情感分数（0-1）
    comment_time       TIMESTAMP,                                    -- 评论时间
    deleted            INTEGER      NOT NULL DEFAULT 0,
    create_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sv_comment_video     ON sv_comment (video_id, comment_time DESC);
CREATE UNIQUE INDEX IF NOT EXISTS idx_sv_comment_douyin ON sv_comment (douyin_comment_id);
CREATE INDEX IF NOT EXISTS idx_sv_comment_sentiment ON sv_comment (video_id, sentiment);

COMMENT ON TABLE  sv_comment                   IS '视频评论表';
COMMENT ON COLUMN sv_comment.sentiment         IS '情感标签：positive / neutral / negative';
COMMENT ON COLUMN sv_comment.sentiment_score   IS '情感分数（0-1，1=最正面）';

-- ============================================================
-- 4. sv_plan — 创作方案表
-- Entity: cn.gaifan.douyinOperations.module.shortvideo.entity.SvPlan
-- ============================================================
CREATE TABLE IF NOT EXISTS sv_plan (
    id                   BIGSERIAL    PRIMARY KEY,
    owner_id             BIGINT       NOT NULL,                        -- 所属用户 ID
    account_id           BIGINT       NOT NULL,                        -- 关联抖音账号 ID
    persona_id           BIGINT,                                       -- 关联人设 ID
    plan_type            VARCHAR(32)  NOT NULL,                        -- 方案类型
    title                VARCHAR(256) NOT NULL,                        -- 方案标题
    description          TEXT,                                          -- 方案描述
    content              TEXT,                                          -- 方案内容
    script_content       TEXT,                                          -- 脚本内容
    source_video_id      BIGINT,                                       -- 来源视频 ID
    source_topic_id      BIGINT,                                       -- 来源话题 ID
    ai_call_log_id       BIGINT,                                       -- 关联 AI 调用日志 ID
    linked_video_id      BIGINT,                                       -- 关联已发布视频 ID
    content_effect       VARCHAR(32),                                   -- 内容效果标签
    effect_score         DECIMAL(8,2),                                  -- 效果评分
    status               VARCHAR(16)  NOT NULL DEFAULT 'draft',        -- 状态：draft / ready / published / archived
    planned_publish_time TIMESTAMP,                                     -- 计划发布时间
    actual_publish_time  TIMESTAMP,                                     -- 实际发布时间
    deleted              INTEGER      NOT NULL DEFAULT 0,
    create_time          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sv_plan_owner   ON sv_plan (owner_id, status, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_sv_plan_account ON sv_plan (account_id, plan_type);
CREATE INDEX IF NOT EXISTS idx_sv_plan_ai      ON sv_plan (ai_call_log_id);
CREATE INDEX IF NOT EXISTS idx_sv_plan_video   ON sv_plan (linked_video_id);
CREATE INDEX IF NOT EXISTS idx_sv_plan_publish ON sv_plan (planned_publish_time);

COMMENT ON TABLE  sv_plan              IS '创作方案表';
COMMENT ON COLUMN sv_plan.plan_type    IS '方案类型';
COMMENT ON COLUMN sv_plan.status       IS '状态：draft / ready / published / archived';

-- ============================================================
-- 5. sv_plan_asset — 方案素材/版本表
-- Entity: cn.gaifan.douyinOperations.module.shortvideo.entity.SvPlanAsset
-- ============================================================
CREATE TABLE IF NOT EXISTS sv_plan_asset (
    id                 BIGSERIAL    PRIMARY KEY,
    plan_id            BIGINT       NOT NULL,                          -- 关联方案 ID
    asset_type         VARCHAR(32)  NOT NULL,                          -- 素材类型
    content            TEXT,                                            -- 文本内容
    file_url           VARCHAR(512),                                    -- 文件 URL
    file_size          BIGINT       DEFAULT 0,                         -- 文件大小（字节）
    duration           INTEGER      DEFAULT 0,                         -- 时长（秒）
    version            INTEGER      NOT NULL DEFAULT 1,                -- 版本号
    generation_task_id BIGINT,                                         -- 关联生成任务 ID
    metadata           TEXT,                                            -- 元数据（JSON）
    deleted            INTEGER      NOT NULL DEFAULT 0,
    create_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sv_pa_plan ON sv_plan_asset (plan_id, asset_type, version DESC);
CREATE INDEX IF NOT EXISTS idx_sv_pa_task ON sv_plan_asset (generation_task_id);

COMMENT ON TABLE  sv_plan_asset            IS '方案素材/版本表';
COMMENT ON COLUMN sv_plan_asset.asset_type IS '素材类型';
COMMENT ON COLUMN sv_plan_asset.version    IS '版本号';

-- ============================================================
-- 6. sv_viral_video — 爆款库表
-- Entity: cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo
-- ============================================================
CREATE TABLE IF NOT EXISTS sv_viral_video (
    id                 BIGSERIAL    PRIMARY KEY,
    owner_id           BIGINT,                                         -- 所属用户 ID
    account_id         BIGINT,                                         -- 关联抖音账号 ID
    video_id           BIGINT,                                         -- 关联视频 ID
    source             VARCHAR(32)  NOT NULL DEFAULT 'manual',         -- 来源：manual / auto / crawl
    title              VARCHAR(512) NOT NULL,                          -- 视频标题
    description        TEXT,                                            -- 视频描述
    cover_url          VARCHAR(512),                                    -- 封面图 URL
    video_url          VARCHAR(512),                                    -- 视频链接
    author_name        VARCHAR(128),                                    -- 作者昵称
    industry           VARCHAR(64),                                     -- 行业分类
    view_count         BIGINT       DEFAULT 0,                         -- 播放量
    like_count         INTEGER      DEFAULT 0,                         -- 点赞数
    viral_score        DECIMAL(5,2) DEFAULT 0,                         -- 爆款评分
    success_factors    TEXT,                                            -- 成功因素分析
    replicable_methods TEXT,                                            -- 可复制方法
    analysis_report_id BIGINT,                                         -- 关联分析报告 ID
    detected_time      TIMESTAMP,                                      -- 检测时间
    tags               VARCHAR(512),                                    -- 标签
    deleted            INTEGER      NOT NULL DEFAULT 0,
    create_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sv_vv_industry ON sv_viral_video (industry, view_count DESC);
CREATE UNIQUE INDEX IF NOT EXISTS idx_sv_vv_video ON sv_viral_video (video_id);
CREATE INDEX IF NOT EXISTS idx_sv_vv_source ON sv_viral_video (source, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_sv_vv_score  ON sv_viral_video (viral_score DESC);

COMMENT ON TABLE  sv_viral_video              IS '爆款库表';
COMMENT ON COLUMN sv_viral_video.source       IS '来源：manual / auto / crawl';
COMMENT ON COLUMN sv_viral_video.viral_score  IS '爆款评分';

-- ============================================================
-- 7. sv_hot_topic — 热点话题表
-- Entity: cn.gaifan.douyinOperations.module.shortvideo.entity.SvHotTopic
-- 注意：无 deleted 字段，过期话题通过 status 管理
-- ============================================================
CREATE TABLE IF NOT EXISTS sv_hot_topic (
    id             BIGSERIAL    PRIMARY KEY,
    source         VARCHAR(32)  NOT NULL DEFAULT 'manual',             -- 来源：manual / douyin / weibo
    douyin_hot_id  VARCHAR(128),                                       -- 抖音热点 ID（去重）
    title          VARCHAR(256) NOT NULL,                              -- 话题标题
    description    TEXT,                                                -- 话题描述
    heat_score     DECIMAL(10,2) DEFAULT 0,                            -- 热度分数
    category       VARCHAR(64),                                        -- 话题分类
    related_tags   VARCHAR(512),                                       -- 相关标签
    status         VARCHAR(16)  NOT NULL DEFAULT 'active',             -- 状态：active / expired
    expiry_time    TIMESTAMP,                                          -- 过期时间
    create_time    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sv_ht_status   ON sv_hot_topic (status, heat_score DESC);
CREATE UNIQUE INDEX IF NOT EXISTS idx_sv_ht_douyin ON sv_hot_topic (douyin_hot_id);
CREATE INDEX IF NOT EXISTS idx_sv_ht_expiry   ON sv_hot_topic (expiry_time);
CREATE INDEX IF NOT EXISTS idx_sv_ht_category ON sv_hot_topic (category, status);

COMMENT ON TABLE  sv_hot_topic              IS '热点话题表';
COMMENT ON COLUMN sv_hot_topic.source       IS '来源：manual / douyin / weibo';
COMMENT ON COLUMN sv_hot_topic.heat_score   IS '热度分数';
COMMENT ON COLUMN sv_hot_topic.status       IS '状态：active / expired';

-- ============================================================
-- 8. sv_category — 视频分类表
-- Entity: cn.gaifan.douyinOperations.module.shortvideo.entity.SvCategory
-- ============================================================
CREATE TABLE IF NOT EXISTS sv_category (
    id           BIGSERIAL    PRIMARY KEY,
    owner_id     BIGINT       NOT NULL,                                -- 所属用户 ID
    name         VARCHAR(64)  NOT NULL,                                -- 分类名称
    description  VARCHAR(256),                                         -- 分类描述
    sort_order   INTEGER      DEFAULT 0,                               -- 排序
    deleted      INTEGER      NOT NULL DEFAULT 0,
    create_time  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_sv_cat_owner_name ON sv_category (owner_id, name);
CREATE INDEX IF NOT EXISTS idx_sv_cat_sort ON sv_category (owner_id, sort_order);

COMMENT ON TABLE  sv_category           IS '视频分类表';
COMMENT ON COLUMN sv_category.owner_id  IS '所属用户 ID';
COMMENT ON COLUMN sv_category.name      IS '分类名称';

-- ============================================================
-- 9. sv_video_generation — 视频生成任务表
-- Entity: cn.gaifan.douyinOperations.module.shortvideo.entity.SvVideoGeneration
-- 注意：无 deleted 字段，任务记录不可删除
-- ============================================================
CREATE TABLE IF NOT EXISTS sv_video_generation (
    id                BIGSERIAL    PRIMARY KEY,
    owner_id          BIGINT       NOT NULL,                           -- 所属用户 ID
    plan_id           BIGINT,                                          -- 关联方案 ID
    task_type         VARCHAR(32)  NOT NULL,                           -- 任务类型
    input_data        TEXT,                                             -- 输入数据（JSON）
    output_url        VARCHAR(512),                                     -- 输出文件 URL
    output_metadata   TEXT,                                             -- 输出元数据（JSON）
    generation_status VARCHAR(16)  NOT NULL DEFAULT 'pending',         -- 状态：pending / running / completed / failed
    progress          INTEGER      DEFAULT 0,                          -- 进度（0-100）
    error_message     TEXT,                                             -- 错误信息
    started_at        TIMESTAMP,                                        -- 开始时间
    completed_at      TIMESTAMP,                                        -- 完成时间
    create_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sv_vg_owner  ON sv_video_generation (owner_id, generation_status, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_sv_vg_plan   ON sv_video_generation (plan_id);
CREATE INDEX IF NOT EXISTS idx_sv_vg_status ON sv_video_generation (generation_status);

COMMENT ON TABLE  sv_video_generation                   IS '视频生成任务表';
COMMENT ON COLUMN sv_video_generation.generation_status IS '状态：pending / running / completed / failed';
COMMENT ON COLUMN sv_video_generation.progress          IS '进度（0-100）';

-- ============================================================
-- 10. sv_publish_time_analysis — 发布时间分析表
-- Entity: cn.gaifan.douyinOperations.module.shortvideo.entity.SvPublishTimeAnalysis
-- 注意：无 deleted 字段，分析数据通过 update_time 覆盖更新
-- ============================================================
CREATE TABLE IF NOT EXISTS sv_publish_time_analysis (
    id             BIGSERIAL PRIMARY KEY,
    account_id     BIGINT    NOT NULL,                                 -- 关联抖音账号 ID
    day_of_week    INTEGER   NOT NULL,                                 -- 星期几（1=周一 7=周日）
    hour_of_day    INTEGER   NOT NULL,                                 -- 小时（0-23）
    avg_view_count BIGINT    DEFAULT 0,                                -- 平均播放量
    video_count    INTEGER   DEFAULT 0,                                -- 视频数量
    recommended    BOOLEAN   DEFAULT false,                            -- 是否推荐时段
    update_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_sv_pta_unique    ON sv_publish_time_analysis (account_id, day_of_week, hour_of_day);
CREATE INDEX IF NOT EXISTS idx_sv_pta_recommend ON sv_publish_time_analysis (account_id, recommended);

COMMENT ON TABLE  sv_publish_time_analysis               IS '发布时间分析表';
COMMENT ON COLUMN sv_publish_time_analysis.day_of_week   IS '星期几（1=周一 7=周日）';
COMMENT ON COLUMN sv_publish_time_analysis.hour_of_day   IS '小时（0-23）';
COMMENT ON COLUMN sv_publish_time_analysis.recommended   IS '是否推荐时段';
