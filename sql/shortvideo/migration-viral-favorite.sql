-- ============================================================
-- 爆款收藏表 sv_viral_favorite（设计文档对齐）
-- 用途：用户收藏平台爆款库中的视频，多对多关系
-- 用法：psql -U postgres -d douyin_operations -f sql/shortvideo/migration-viral-favorite.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS sv_viral_favorite (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    viral_video_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sv_viral_favorite_user_video ON sv_viral_favorite (user_id, viral_video_id);

COMMENT ON TABLE sv_viral_favorite IS '用户收藏爆款（平台爆款库 + 个人收藏）';
COMMENT ON COLUMN sv_viral_favorite.user_id IS '用户 ID';
COMMENT ON COLUMN sv_viral_favorite.viral_video_id IS '爆款视频 ID（sv_viral_video.id）';
