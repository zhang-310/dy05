-- ========================================
-- 账号采集优化 - 数据库迁移脚本
-- 日期: 2026-04-21
-- 说明: 新增 sv_account 账号主表，优化账号管理
-- ========================================

-- ─── 1. 创建账号主表 ──────────────────────────────────────

CREATE TABLE IF NOT EXISTS sv_account (
    id                      BIGSERIAL    PRIMARY KEY,
    owner_id                BIGINT       NOT NULL,

    -- 账号基本信息
    sec_uid                 VARCHAR(256) NOT NULL,              -- 抖音唯一标识
    douyin_id               VARCHAR(128),                       -- 抖音号
    nickname                VARCHAR(128),                       -- 昵称
    avatar_url              VARCHAR(512),                       -- 头像 URL
    signature               TEXT,                               -- 个人简介

    -- 账号数据
    follower_count          BIGINT       DEFAULT 0,             -- 粉丝数
    following_count         BIGINT       DEFAULT 0,             -- 关注数
    total_favorited         BIGINT       DEFAULT 0,             -- 获赞总数
    video_count             INTEGER      DEFAULT 0,             -- 作品数

    -- 认证信息
    is_verified             BOOLEAN      DEFAULT FALSE,         -- 是否认证
    verification_type       VARCHAR(32),                        -- 认证类型：personal/enterprise/government

    -- 采集统计
    collect_count           INTEGER      DEFAULT 0,             -- 采集次数
    last_collect_time       TIMESTAMP,                          -- 最后采集时间
    total_collected_videos  INTEGER      DEFAULT 0,             -- 累计采集视频数

    -- 分析统计
    avg_view_count          BIGINT       DEFAULT 0,             -- 平均播放量
    avg_like_count          INTEGER      DEFAULT 0,             -- 平均点赞数
    avg_share_count         INTEGER      DEFAULT 0,             -- 平均分享数
    avg_comment_count       INTEGER      DEFAULT 0,             -- 平均评论数
    avg_viral_score         DECIMAL(5,2) DEFAULT 0,             -- 平均爆款评分
    top_viral_score         DECIMAL(5,2) DEFAULT 0,             -- 最高爆款评分

    -- 标签与分类
    industry_tags           VARCHAR(512),                       -- 行业标签（JSON 数组）
    content_tags            VARCHAR(512),                       -- 内容标签（JSON 数组）
    account_category        VARCHAR(64),                        -- 账号分类

    -- 来源信息
    source_type             VARCHAR(32)  DEFAULT 'manual',      -- manual/keyword_search/recommend/import
    source_keyword          VARCHAR(256),                       -- 来源关键词（关键词采集时记录）
    source_task_id          BIGINT,                             -- 来源采集任务 ID

    -- 备注与状态
    notes                   TEXT,                               -- 备注
    status                  VARCHAR(32)  DEFAULT 'active',      -- active/archived/blocked

    -- 系统字段
    deleted                 INTEGER      NOT NULL DEFAULT 0,
    create_time             TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time             TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE UNIQUE INDEX idx_sv_acc_owner_sec_uid ON sv_account(owner_id, sec_uid) WHERE deleted = 0;
CREATE INDEX idx_sv_acc_owner ON sv_account(owner_id, deleted);
CREATE INDEX idx_sv_acc_category ON sv_account(account_category) WHERE deleted = 0;
CREATE INDEX idx_sv_acc_source ON sv_account(source_type, source_keyword) WHERE deleted = 0 AND source_keyword IS NOT NULL;
CREATE INDEX idx_sv_acc_follower ON sv_account(follower_count DESC) WHERE deleted = 0;
CREATE INDEX idx_sv_acc_viral_score ON sv_account(avg_viral_score DESC) WHERE deleted = 0;

-- 注释
COMMENT ON TABLE  sv_account                        IS '短视频账号主表';
COMMENT ON COLUMN sv_account.sec_uid                IS '抖音唯一标识（全局唯一）';
COMMENT ON COLUMN sv_account.source_type            IS '来源类型：manual=手动添加, keyword_search=关键词采集, recommend=推荐, import=导入';
COMMENT ON COLUMN sv_account.source_keyword         IS '来源关键词（关键词采集时记录，用于分组管理）';
COMMENT ON COLUMN sv_account.avg_viral_score        IS '平均爆款评分（基于该账号所有采集视频计算）';
COMMENT ON COLUMN sv_account.status                 IS '账号状态：active=活跃, archived=归档, blocked=屏蔽';

-- ─── 2. 修改采集任务表 ──────────────────────────────────────

-- 添加账号主表关联
ALTER TABLE sv_account_collect_task
ADD COLUMN IF NOT EXISTS sv_account_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_sv_act_sv_account ON sv_account_collect_task(sv_account_id) WHERE deleted = 0;

COMMENT ON COLUMN sv_account_collect_task.sv_account_id IS '关联 sv_account.id（账号主表）';

-- ─── 3. 修改爆款视频表 ──────────────────────────────────────

-- 添加账号主表关联
ALTER TABLE sv_viral_video
ADD COLUMN IF NOT EXISTS sv_account_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_sv_vv_sv_account ON sv_viral_video(sv_account_id) WHERE deleted = 0;

COMMENT ON COLUMN sv_viral_video.sv_account_id IS '关联 sv_account.id（账号主表）';

-- ─── 4. 数据迁移（可选）──────────────────────────────────────

-- 从现有采集任务中提取账号信息，创建 sv_account 记录
-- 注意：此脚本需要根据实际数据情况调整

-- 4.1 迁移已有账号（基于 sec_uid 去重）
INSERT INTO sv_account (
    owner_id,
    sec_uid,
    nickname,
    source_type,
    collect_count,
    last_collect_time,
    create_time,
    update_time
)
SELECT DISTINCT ON (owner_id, sec_uid)
    owner_id,
    sec_uid,
    account_name AS nickname,
    CASE
        WHEN input_type = 'search_video' THEN 'keyword_search'
        ELSE 'manual'
    END AS source_type,
    1 AS collect_count,
    create_time AS last_collect_time,
    create_time,
    update_time
FROM sv_account_collect_task
WHERE sec_uid IS NOT NULL
  AND sec_uid != ''
  AND deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sv_account
      WHERE sv_account.owner_id = sv_account_collect_task.owner_id
        AND sv_account.sec_uid = sv_account_collect_task.sec_uid
        AND sv_account.deleted = 0
  )
ORDER BY owner_id, sec_uid, create_time DESC;

-- 4.2 更新采集任务的 sv_account_id
UPDATE sv_account_collect_task task
SET sv_account_id = acc.id
FROM sv_account acc
WHERE task.owner_id = acc.owner_id
  AND task.sec_uid = acc.sec_uid
  AND task.deleted = 0
  AND acc.deleted = 0
  AND task.sv_account_id IS NULL;

-- 4.3 更新爆款视频的 sv_account_id（基于 author_id 或 sec_uid）
-- 注意：需要根据实际字段调整
UPDATE sv_viral_video video
SET sv_account_id = acc.id
FROM sv_account acc
WHERE video.owner_id = acc.owner_id
  AND video.deleted = 0
  AND acc.deleted = 0
  AND video.sv_account_id IS NULL
  AND (
      -- 通过采集任务关联
      video.collect_task_id IN (
          SELECT id FROM sv_account_collect_task
          WHERE sv_account_id = acc.id AND deleted = 0
      )
  );

-- ─── 5. 验证数据 ──────────────────────────────────────

-- 查看账号统计
SELECT
    COUNT(*) AS total_accounts,
    COUNT(CASE WHEN source_type = 'manual' THEN 1 END) AS manual_accounts,
    COUNT(CASE WHEN source_type = 'keyword_search' THEN 1 END) AS keyword_accounts,
    COUNT(CASE WHEN follower_count > 0 THEN 1 END) AS has_follower_data
FROM sv_account
WHERE deleted = 0;

-- 查看关联情况
SELECT
    COUNT(*) AS total_tasks,
    COUNT(sv_account_id) AS linked_tasks,
    COUNT(*) - COUNT(sv_account_id) AS unlinked_tasks
FROM sv_account_collect_task
WHERE deleted = 0;

SELECT
    COUNT(*) AS total_videos,
    COUNT(sv_account_id) AS linked_videos,
    COUNT(*) - COUNT(sv_account_id) AS unlinked_videos
FROM sv_viral_video
WHERE deleted = 0;
