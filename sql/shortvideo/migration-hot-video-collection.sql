-- ============================================================
-- 短视频模块 - 热门视频采集表
-- 版本: v1.0
-- 日期: 2026-03-01
-- 说明: 用于数据分析模块的热门视频采集
-- ============================================================

CREATE TABLE IF NOT EXISTS hot_video_collection (
    id                 BIGSERIAL    PRIMARY KEY,
    owner_id           BIGINT       NOT NULL,
    platform           VARCHAR(50)  NOT NULL DEFAULT 'douyin',
    video_id           VARCHAR(100),
    video_url          VARCHAR(500),
    title              VARCHAR(500),
    cover_url          VARCHAR(500),
    author_name        VARCHAR(255),
    view_count         BIGINT       DEFAULT 0,
    like_count         BIGINT       DEFAULT 0,
    comment_count      BIGINT       DEFAULT 0,
    share_count        BIGINT       DEFAULT 0,
    collect_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    analysis_status    VARCHAR(50)  DEFAULT 'pending',  -- pending/analyzing/done/failed
    analysis_result    TEXT,
    deleted            INTEGER      NOT NULL DEFAULT 0,
    create_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_hot_video_owner ON hot_video_collection (owner_id, collect_time DESC);
CREATE INDEX IF NOT EXISTS idx_hot_video_platform ON hot_video_collection (platform);
COMMENT ON TABLE hot_video_collection IS '热门视频采集表（数据分析用）';
