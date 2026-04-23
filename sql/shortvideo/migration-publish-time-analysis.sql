-- ============================================================
-- 短视频模块 - 发布时间分析表（增量迁移）
-- 版本: v1.0
-- 日期: 2026-03-02
-- 说明: 若 schema.sql 未执行，此迁移确保表存在（幂等）
-- ============================================================

CREATE TABLE IF NOT EXISTS sv_publish_time_analysis (
    id             BIGSERIAL PRIMARY KEY,
    account_id     BIGINT    NOT NULL,
    day_of_week    INTEGER   NOT NULL,
    hour_of_day    INTEGER   NOT NULL,
    avg_view_count BIGINT    DEFAULT 0,
    video_count    INTEGER   DEFAULT 0,
    recommended    BOOLEAN   DEFAULT false,
    update_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_sv_pta_unique ON sv_publish_time_analysis (account_id, day_of_week, hour_of_day);
CREATE INDEX IF NOT EXISTS idx_sv_pta_recommend ON sv_publish_time_analysis (account_id, recommended);

COMMENT ON TABLE sv_publish_time_analysis IS '发布时间分析表';
