-- V173: benchmark 视频/分析 owner_id 回填与隔离索引
-- 历史 V142 给 benchmark_video / benchmark_analysis 增加 owner_id 时默认 0，
-- 应用层修复后需要先按账号归属回填，避免老 Docker 数据在登录用户视角下消失。

ALTER TABLE benchmark_video ADD COLUMN IF NOT EXISTS owner_id BIGINT NOT NULL DEFAULT 0;
ALTER TABLE benchmark_analysis ADD COLUMN IF NOT EXISTS owner_id BIGINT NOT NULL DEFAULT 0;

UPDATE benchmark_video bv
SET owner_id = ba.owner_id
FROM benchmark_account ba
WHERE bv.benchmark_account_id = ba.id
  AND (bv.owner_id IS NULL OR bv.owner_id = 0);

UPDATE benchmark_analysis ban
SET owner_id = bv.owner_id
FROM benchmark_video bv
WHERE ban.benchmark_video_id = bv.id
  AND bv.owner_id IS NOT NULL
  AND bv.owner_id <> 0
  AND (ban.owner_id IS NULL OR ban.owner_id = 0);

CREATE INDEX IF NOT EXISTS idx_benchmark_video_owner_deleted
    ON benchmark_video(owner_id, deleted, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_benchmark_video_owner_status
    ON benchmark_video(owner_id, analysis_status, deleted);

CREATE INDEX IF NOT EXISTS idx_benchmark_analysis_owner_video
    ON benchmark_analysis(owner_id, benchmark_video_id)
    WHERE deleted = 0;
