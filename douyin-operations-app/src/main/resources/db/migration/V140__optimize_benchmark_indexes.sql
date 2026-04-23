-- =====================================================
-- Benchmark Module Index Optimization
-- 对标账号分析系统索引优化
-- =====================================================

-- 1. benchmark_account 表索引优化
-- 复合索引：owner_id + platform + deleted（常用查询组合）
CREATE INDEX IF NOT EXISTS idx_benchmark_account_owner_platform
    ON benchmark_account(owner_id, platform, deleted);

-- 复合索引：owner_id + category + deleted（分类查询）
CREATE INDEX IF NOT EXISTS idx_benchmark_account_owner_category
    ON benchmark_account(owner_id, category, deleted)
    WHERE category IS NOT NULL;

-- 复合索引：owner_id + is_active + deleted（激活状态查询）
CREATE INDEX IF NOT EXISTS idx_benchmark_account_owner_active
    ON benchmark_account(owner_id, is_active, deleted);

-- 粉丝数范围查询索引
CREATE INDEX IF NOT EXISTS idx_benchmark_account_fan_count
    ON benchmark_account(fan_count DESC)
    WHERE deleted = 0;

-- 最后采集时间索引（用于定时任务）
CREATE INDEX IF NOT EXISTS idx_benchmark_account_last_collect
    ON benchmark_account(last_collect_time DESC)
    WHERE deleted = 0 AND is_active = true;

-- 2. benchmark_video 表索引优化
-- 复合索引：account_id + analysis_status + deleted（常用查询）
CREATE INDEX IF NOT EXISTS idx_benchmark_video_account_status
    ON benchmark_video(benchmark_account_id, analysis_status, deleted);

-- 复合索引：account_id + is_qualified + deleted（符合条件的视频）
CREATE INDEX IF NOT EXISTS idx_benchmark_video_account_qualified
    ON benchmark_video(benchmark_account_id, is_qualified, deleted);

-- 复合索引：account_id + like_count（按点赞数排序）
CREATE INDEX IF NOT EXISTS idx_benchmark_video_account_like
    ON benchmark_video(benchmark_account_id, like_count DESC)
    WHERE deleted = 0;

-- 视频ID唯一性索引（防重复）
CREATE UNIQUE INDEX IF NOT EXISTS idx_benchmark_video_unique
    ON benchmark_video(video_id, benchmark_account_id)
    WHERE deleted = 0;

-- 发布时间索引（时间范围查询）
CREATE INDEX IF NOT EXISTS idx_benchmark_video_publish_time
    ON benchmark_video(publish_time DESC)
    WHERE deleted = 0;

-- 3. benchmark_analysis 表索引优化
-- 复合索引：video_id + deleted（一对一关系查询）
CREATE UNIQUE INDEX IF NOT EXISTS idx_benchmark_analysis_video_unique
    ON benchmark_analysis(benchmark_video_id)
    WHERE deleted = 0;

-- 创意类型索引（分类统计）
CREATE INDEX IF NOT EXISTS idx_benchmark_analysis_creative
    ON benchmark_analysis(creative_type)
    WHERE deleted = 0 AND creative_type IS NOT NULL;

-- AI模型使用统计索引
CREATE INDEX IF NOT EXISTS idx_benchmark_analysis_model
    ON benchmark_analysis(ai_model_used, create_time DESC)
    WHERE deleted = 0;

-- 4. douyin_cookie 表索引优化
-- 复合索引：owner_id + platform + is_valid + deleted（获取可用Cookie）
CREATE INDEX IF NOT EXISTS idx_douyin_cookie_owner_platform_valid
    ON douyin_cookie(owner_id, platform, is_valid, deleted);

-- 过期时间索引（定时清理）
CREATE INDEX IF NOT EXISTS idx_douyin_cookie_expire
    ON douyin_cookie(expire_time)
    WHERE deleted = 0 AND is_valid = true;

-- 最后使用时间索引（使用统计）
CREATE INDEX IF NOT EXISTS idx_douyin_cookie_last_used
    ON douyin_cookie(last_used_time DESC)
    WHERE deleted = 0;

-- 5. benchmark_task 表索引优化
-- 复合索引：owner_id + task_status（任务列表查询）
CREATE INDEX IF NOT EXISTS idx_benchmark_task_owner_status
    ON benchmark_task(owner_id, task_status);

-- 复合索引：task_status + started_at（运行中任务监控）
CREATE INDEX IF NOT EXISTS idx_benchmark_task_status_started
    ON benchmark_task(task_status, started_at DESC)
    WHERE task_status IN ('pending', 'running');

-- 完成时间索引（历史任务查询）
CREATE INDEX IF NOT EXISTS idx_benchmark_task_completed
    ON benchmark_task(completed_at DESC)
    WHERE task_status = 'completed';

-- 6. 添加表统计信息注释（用于查询优化器）
ANALYZE benchmark_account;
ANALYZE benchmark_video;
ANALYZE benchmark_analysis;
ANALYZE douyin_cookie;
ANALYZE benchmark_task;

-- 7. 创建物化视图：账号视频统计（可选，用于Dashboard）
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_benchmark_account_stats AS
SELECT
    ba.id AS account_id,
    ba.owner_id,
    ba.account_name,
    ba.platform,
    ba.fan_count,
    COUNT(bv.id) AS total_videos,
    COUNT(CASE WHEN bv.is_qualified = true THEN 1 END) AS qualified_videos,
    COUNT(CASE WHEN bv.analysis_status = 'completed' THEN 1 END) AS analyzed_videos,
    AVG(bv.like_count) AS avg_like_count,
    MAX(bv.like_count) AS max_like_count,
    MAX(bv.create_time) AS last_video_time
FROM benchmark_account ba
LEFT JOIN benchmark_video bv ON ba.id = bv.benchmark_account_id AND bv.deleted = 0
WHERE ba.deleted = 0
GROUP BY ba.id, ba.owner_id, ba.account_name, ba.platform, ba.fan_count;

-- 物化视图索引
CREATE INDEX IF NOT EXISTS idx_mv_account_stats_owner
    ON mv_benchmark_account_stats(owner_id);

-- 刷新物化视图的函数（可定时调用）
CREATE OR REPLACE FUNCTION refresh_benchmark_stats()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_benchmark_account_stats;
END;
$$ LANGUAGE plpgsql;

COMMENT ON MATERIALIZED VIEW mv_benchmark_account_stats IS '账号视频统计物化视图（需定期刷新）';
COMMENT ON FUNCTION refresh_benchmark_stats() IS '刷新账号统计物化视图';
