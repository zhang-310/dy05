-- ============================================================
-- AI 进化引擎 - 补充表结构
-- 在 sql/ai/schema.sql 基础上追加
-- 版本：2.0
-- 更新日期：2026-02-27
-- ============================================================

-- ============================================================
-- 5. ai_index_queue — 知识索引队列表
-- Entity: cn.gaifan.douyinOperations.module.ai.entity.AiIndexQueue
-- 注意：无 deleted 字段，处理完成后物理删除或归档
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_index_queue (
    id            BIGSERIAL       PRIMARY KEY,
    source_type   VARCHAR(32)     NOT NULL,                              -- 来源类型：viral_video / live_review / manual / evolved
    source_id     BIGINT          NOT NULL,                              -- 来源记录 ID
    target_kb_id  BIGINT,                                                -- evolved 类型时：目标知识库ID
    content      TEXT            NOT NULL,                              -- 待索引内容
    priority     INTEGER         NOT NULL DEFAULT 5,                    -- 优先级：1=高 5=普通 10=低
    status       VARCHAR(16)     NOT NULL DEFAULT 'pending',            -- 状态：pending / processing / done / failed
    retry_count  INTEGER         NOT NULL DEFAULT 0,                    -- 重试次数（最大 3 次）
    error_msg    VARCHAR(512),                                          -- 失败原因
    create_time  TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,    -- 入队时间
    update_time  TIMESTAMP                DEFAULT CURRENT_TIMESTAMP     -- 更新时间
);

CREATE INDEX IF NOT EXISTS idx_ai_index_queue_status_priority ON ai_index_queue (status, priority, create_time);
CREATE INDEX IF NOT EXISTS idx_ai_index_queue_source          ON ai_index_queue (source_type, source_id);

COMMENT ON TABLE  ai_index_queue              IS '知识索引队列表';
COMMENT ON COLUMN ai_index_queue.source_type  IS '来源类型：viral_video / live_review / manual';
COMMENT ON COLUMN ai_index_queue.source_id    IS '来源记录 ID';
COMMENT ON COLUMN ai_index_queue.content      IS '待索引内容';
COMMENT ON COLUMN ai_index_queue.priority     IS '优先级：1=高 5=普通 10=低';
COMMENT ON COLUMN ai_index_queue.status       IS '状态：pending=待处理 processing=处理中 done=完成 failed=失败';
COMMENT ON COLUMN ai_index_queue.retry_count  IS '重试次数（最大 3 次）';

-- ============================================================
-- 6. ai_viral_analysis — 爆款拆解分析表
-- Entity: cn.gaifan.douyinOperations.module.ai.entity.AiViralAnalysis
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_viral_analysis (
    id                  BIGSERIAL       PRIMARY KEY,
    video_id            BIGINT          NOT NULL,                              -- 关联短视频 ID（sv_video.id）
    account_id          BIGINT,                                                -- 关联账号 ID
    owner_id            BIGINT          NOT NULL,                              -- 所属用户 ID
    viral_score         INTEGER         NOT NULL DEFAULT 0,                    -- 爆款评分（0-100）
    view_count          BIGINT          NOT NULL DEFAULT 0,                    -- 视频播放量（快照）
    avg_view_count      BIGINT          NOT NULL DEFAULT 0,                    -- 账号近 30 天均值（快照）
    success_factors     TEXT,                                                  -- 成功因素（JSON 数组）
    replicable_methods  TEXT,                                                  -- 可复制方法（JSON 数组）
    report_content      TEXT,                                                  -- AI 生成的完整分析报告
    quality_score       INTEGER         NOT NULL DEFAULT 0,                    -- 报告质量评分（0-100）
    model_used          VARCHAR(64),                                           -- 使用的 AI 模型
    tokens_used         BIGINT          NOT NULL DEFAULT 0,                    -- 消耗 Token 数
    status              INTEGER         NOT NULL DEFAULT 0,                    -- 状态：0=分析中 1=完成 2=失败
    deleted             INTEGER         NOT NULL DEFAULT 0,                    -- 逻辑删除
    create_time         TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,    -- 创建时间
    update_time         TIMESTAMP                DEFAULT CURRENT_TIMESTAMP     -- 更新时间
);

CREATE INDEX IF NOT EXISTS idx_ai_viral_owner_time   ON ai_viral_analysis (owner_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_ai_viral_video        ON ai_viral_analysis (video_id);
CREATE INDEX IF NOT EXISTS idx_ai_viral_score        ON ai_viral_analysis (viral_score DESC) WHERE deleted = 0;

COMMENT ON TABLE  ai_viral_analysis                    IS '爆款拆解分析表';
COMMENT ON COLUMN ai_viral_analysis.video_id           IS '关联短视频 ID（sv_video.id）';
COMMENT ON COLUMN ai_viral_analysis.viral_score        IS '爆款评分（0-100）';
COMMENT ON COLUMN ai_viral_analysis.success_factors    IS '成功因素（JSON 数组）';
COMMENT ON COLUMN ai_viral_analysis.replicable_methods IS '可复制方法（JSON 数组）';
COMMENT ON COLUMN ai_viral_analysis.report_content     IS 'AI 生成的完整分析报告';
COMMENT ON COLUMN ai_viral_analysis.quality_score      IS '报告质量评分（0-100）';
COMMENT ON COLUMN ai_viral_analysis.status             IS '状态：0=分析中 1=完成 2=失败';

-- ============================================================
-- 7. ai_live_review — 直播复盘表
-- Entity: cn.gaifan.douyinOperations.module.ai.entity.AiLiveReview
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_live_review (
    id               BIGSERIAL       PRIMARY KEY,
    session_id       BIGINT          NOT NULL,                              -- 关联直播场次 ID（live_session.id）
    account_id       BIGINT,                                                -- 关联账号 ID
    owner_id         BIGINT          NOT NULL,                              -- 所属用户 ID
    total_viewers    BIGINT          NOT NULL DEFAULT 0,                    -- 总观看人数（快照）
    total_gmv        DECIMAL(12,2)   NOT NULL DEFAULT 0,                    -- 总 GMV（快照）
    conversion_rate  DECIMAL(5,4)    NOT NULL DEFAULT 0,                    -- 转化率（快照）
    peak_viewers     BIGINT          NOT NULL DEFAULT 0,                    -- 峰值在线人数（快照）
    top_scripts      TEXT,                                                  -- 高效话术（JSON 数组）
    weak_points      TEXT,                                                  -- 薄弱环节（JSON 数组）
    report_content   TEXT,                                                  -- AI 生成的完整复盘报告
    model_used       VARCHAR(64),                                           -- 使用的 AI 模型
    tokens_used      BIGINT          NOT NULL DEFAULT 0,                    -- 消耗 Token 数
    status           INTEGER         NOT NULL DEFAULT 0,                    -- 状态：0=分析中 1=完成 2=失败
    deleted          INTEGER         NOT NULL DEFAULT 0,                    -- 逻辑删除
    create_time      TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,    -- 创建时间
    update_time      TIMESTAMP                DEFAULT CURRENT_TIMESTAMP     -- 更新时间
);

CREATE INDEX IF NOT EXISTS idx_ai_live_review_owner_time   ON ai_live_review (owner_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_ai_live_review_session      ON ai_live_review (session_id);

COMMENT ON TABLE  ai_live_review                  IS '直播复盘表';
COMMENT ON COLUMN ai_live_review.session_id       IS '关联直播场次 ID（live_session.id）';
COMMENT ON COLUMN ai_live_review.top_scripts      IS '高效话术（JSON 数组）';
COMMENT ON COLUMN ai_live_review.weak_points      IS '薄弱环节（JSON 数组）';
COMMENT ON COLUMN ai_live_review.report_content   IS 'AI 生成的完整复盘报告';
COMMENT ON COLUMN ai_live_review.status           IS '状态：0=分析中 1=完成 2=失败';
