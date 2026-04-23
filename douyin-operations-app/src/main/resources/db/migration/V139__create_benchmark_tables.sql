-- =====================================================
-- Benchmark Module Tables
-- 对标账号分析系统数据库表
-- =====================================================

-- 1. 对标账号表
CREATE TABLE benchmark_account (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    account_name VARCHAR(128) NOT NULL,
    platform VARCHAR(32) DEFAULT 'douyin' NOT NULL,
    account_url VARCHAR(512),
    sec_uid VARCHAR(128),
    douyin_id VARCHAR(128),
    category VARCHAR(64),
    fan_count BIGINT DEFAULT 0,
    video_count INTEGER DEFAULT 0,
    avg_view_count BIGINT DEFAULT 0,
    avg_like_count INTEGER DEFAULT 0,
    notes TEXT,
    is_active BOOLEAN DEFAULT true,
    last_collect_time TIMESTAMP,
    deleted INTEGER DEFAULT 0 NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE benchmark_account IS '对标账号表';
COMMENT ON COLUMN benchmark_account.id IS '主键ID';
COMMENT ON COLUMN benchmark_account.owner_id IS '所属用户ID（数据隔离）';
COMMENT ON COLUMN benchmark_account.account_name IS '账号名称';
COMMENT ON COLUMN benchmark_account.platform IS '平台（douyin/kuaishou等）';
COMMENT ON COLUMN benchmark_account.account_url IS '账号主页URL';
COMMENT ON COLUMN benchmark_account.sec_uid IS '抖音sec_uid';
COMMENT ON COLUMN benchmark_account.douyin_id IS '抖音ID';
COMMENT ON COLUMN benchmark_account.category IS '账号分类';
COMMENT ON COLUMN benchmark_account.fan_count IS '粉丝数';
COMMENT ON COLUMN benchmark_account.video_count IS '视频数';
COMMENT ON COLUMN benchmark_account.avg_view_count IS '平均播放量';
COMMENT ON COLUMN benchmark_account.avg_like_count IS '平均点赞数';
COMMENT ON COLUMN benchmark_account.notes IS '备注';
COMMENT ON COLUMN benchmark_account.is_active IS '是否启用';
COMMENT ON COLUMN benchmark_account.last_collect_time IS '最后采集时间';
COMMENT ON COLUMN benchmark_account.deleted IS '逻辑删除标记（0未删除，1已删除）';
COMMENT ON COLUMN benchmark_account.create_time IS '创建时间';
COMMENT ON COLUMN benchmark_account.update_time IS '更新时间';

CREATE INDEX idx_benchmark_account_owner ON benchmark_account(owner_id, deleted);
CREATE INDEX idx_benchmark_account_platform ON benchmark_account(platform, deleted);
CREATE INDEX idx_benchmark_account_sec_uid ON benchmark_account(sec_uid);
CREATE INDEX idx_benchmark_account_active ON benchmark_account(is_active, deleted);

-- 2. 对标视频表
CREATE TABLE benchmark_video (
    id BIGSERIAL PRIMARY KEY,
    benchmark_account_id BIGINT NOT NULL,
    video_id VARCHAR(128) NOT NULL,
    title VARCHAR(512),
    description TEXT,
    cover_url VARCHAR(512),
    video_url VARCHAR(512),
    duration INTEGER DEFAULT 0,
    view_count BIGINT DEFAULT 0,
    like_count INTEGER DEFAULT 0,
    comment_count INTEGER DEFAULT 0,
    share_count INTEGER DEFAULT 0,
    favorite_count INTEGER DEFAULT 0,
    publish_time TIMESTAMP,
    is_qualified BOOLEAN DEFAULT false,
    analysis_status VARCHAR(16) DEFAULT 'pending' NOT NULL,
    local_video_path VARCHAR(512),
    bos_video_url VARCHAR(512),
    deleted INTEGER DEFAULT 0 NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE benchmark_video IS '对标视频表';
COMMENT ON COLUMN benchmark_video.id IS '主键ID';
COMMENT ON COLUMN benchmark_video.benchmark_account_id IS '关联账号ID';
COMMENT ON COLUMN benchmark_video.video_id IS '视频ID';
COMMENT ON COLUMN benchmark_video.title IS '视频标题';
COMMENT ON COLUMN benchmark_video.description IS '视频描述';
COMMENT ON COLUMN benchmark_video.cover_url IS '封面URL';
COMMENT ON COLUMN benchmark_video.video_url IS '视频URL';
COMMENT ON COLUMN benchmark_video.duration IS '时长（秒）';
COMMENT ON COLUMN benchmark_video.view_count IS '播放量';
COMMENT ON COLUMN benchmark_video.like_count IS '点赞数';
COMMENT ON COLUMN benchmark_video.comment_count IS '评论数';
COMMENT ON COLUMN benchmark_video.share_count IS '分享数';
COMMENT ON COLUMN benchmark_video.favorite_count IS '收藏数';
COMMENT ON COLUMN benchmark_video.publish_time IS '发布时间';
COMMENT ON COLUMN benchmark_video.is_qualified IS '是否符合分析条件';
COMMENT ON COLUMN benchmark_video.analysis_status IS '分析状态（pending/processing/completed/failed）';
COMMENT ON COLUMN benchmark_video.local_video_path IS '本地视频路径';
COMMENT ON COLUMN benchmark_video.bos_video_url IS '百度云BOS URL';
COMMENT ON COLUMN benchmark_video.deleted IS '逻辑删除标记';
COMMENT ON COLUMN benchmark_video.create_time IS '创建时间';
COMMENT ON COLUMN benchmark_video.update_time IS '更新时间';

CREATE INDEX idx_benchmark_video_account ON benchmark_video(benchmark_account_id, deleted);
CREATE INDEX idx_benchmark_video_id ON benchmark_video(video_id);
CREATE INDEX idx_benchmark_video_status ON benchmark_video(analysis_status, deleted);
CREATE INDEX idx_benchmark_video_qualified ON benchmark_video(is_qualified, deleted);
CREATE INDEX idx_benchmark_video_like ON benchmark_video(like_count DESC);

-- 3. 深度分析结果表
CREATE TABLE benchmark_analysis (
    id BIGSERIAL PRIMARY KEY,
    benchmark_video_id BIGINT NOT NULL,
    transcript_text TEXT,
    ocr_text TEXT,
    api_description TEXT,
    merged_content TEXT,
    scene_count INTEGER DEFAULT 0,
    key_frames_json TEXT,
    scene_description TEXT,
    creative_type VARCHAR(64),
    hook_strategy TEXT,
    content_structure TEXT,
    emotional_curve VARCHAR(256),
    pacing_analysis TEXT,
    viral_factors TEXT,
    strengths TEXT,
    weaknesses TEXT,
    replicable_elements TEXT,
    ai_summary TEXT,
    script_breakdown TEXT,
    improvement_suggestions TEXT,
    target_audience VARCHAR(256),
    comparison_report TEXT,
    differentiation_points TEXT,
    ai_model_used VARCHAR(64),
    tokens_used BIGINT DEFAULT 0,
    analysis_duration_ms INTEGER DEFAULT 0,
    deleted INTEGER DEFAULT 0 NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE benchmark_analysis IS '深度分析结果表';
COMMENT ON COLUMN benchmark_analysis.id IS '主键ID';
COMMENT ON COLUMN benchmark_analysis.benchmark_video_id IS '关联视频ID';
COMMENT ON COLUMN benchmark_analysis.transcript_text IS 'ASR语音识别文案';
COMMENT ON COLUMN benchmark_analysis.ocr_text IS 'OCR字幕识别文案';
COMMENT ON COLUMN benchmark_analysis.api_description IS '抖音API获取的描述';
COMMENT ON COLUMN benchmark_analysis.merged_content IS '合并后的完整文案';
COMMENT ON COLUMN benchmark_analysis.scene_count IS '场景数量';
COMMENT ON COLUMN benchmark_analysis.key_frames_json IS '关键帧JSON（时间戳+图片路径）';
COMMENT ON COLUMN benchmark_analysis.scene_description IS '场景描述';
COMMENT ON COLUMN benchmark_analysis.creative_type IS '创意类型';
COMMENT ON COLUMN benchmark_analysis.hook_strategy IS '钩子策略';
COMMENT ON COLUMN benchmark_analysis.content_structure IS '内容结构';
COMMENT ON COLUMN benchmark_analysis.emotional_curve IS '情绪曲线';
COMMENT ON COLUMN benchmark_analysis.pacing_analysis IS '节奏分析';
COMMENT ON COLUMN benchmark_analysis.viral_factors IS '爆款因素';
COMMENT ON COLUMN benchmark_analysis.strengths IS '优势分析';
COMMENT ON COLUMN benchmark_analysis.weaknesses IS '弊端分析';
COMMENT ON COLUMN benchmark_analysis.replicable_elements IS '可复制要素';
COMMENT ON COLUMN benchmark_analysis.ai_summary IS 'AI综合总结';
COMMENT ON COLUMN benchmark_analysis.script_breakdown IS '话术拆解';
COMMENT ON COLUMN benchmark_analysis.improvement_suggestions IS '改进建议';
COMMENT ON COLUMN benchmark_analysis.target_audience IS '目标受众';
COMMENT ON COLUMN benchmark_analysis.comparison_report IS '竞品对比报告';
COMMENT ON COLUMN benchmark_analysis.differentiation_points IS '差异化要点';
COMMENT ON COLUMN benchmark_analysis.ai_model_used IS '使用的AI模型';
COMMENT ON COLUMN benchmark_analysis.tokens_used IS '消耗的token数';
COMMENT ON COLUMN benchmark_analysis.analysis_duration_ms IS '分析耗时（毫秒）';
COMMENT ON COLUMN benchmark_analysis.deleted IS '逻辑删除标记';
COMMENT ON COLUMN benchmark_analysis.create_time IS '创建时间';
COMMENT ON COLUMN benchmark_analysis.update_time IS '更新时间';

CREATE INDEX idx_benchmark_analysis_video ON benchmark_analysis(benchmark_video_id, deleted);
CREATE INDEX idx_benchmark_analysis_type ON benchmark_analysis(creative_type, deleted);

-- 4. 抖音Cookie管理表
CREATE TABLE douyin_cookie (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    cookie_name VARCHAR(128) NOT NULL,
    cookie_value TEXT NOT NULL,
    platform VARCHAR(32) DEFAULT 'douyin' NOT NULL,
    account_name VARCHAR(128),
    expire_time TIMESTAMP,
    is_valid BOOLEAN DEFAULT true,
    last_check_time TIMESTAMP,
    check_status VARCHAR(32) DEFAULT 'unknown',
    usage_count INTEGER DEFAULT 0,
    last_used_time TIMESTAMP,
    notes TEXT,
    deleted INTEGER DEFAULT 0 NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE douyin_cookie IS '抖音Cookie管理表';
COMMENT ON COLUMN douyin_cookie.id IS '主键ID';
COMMENT ON COLUMN douyin_cookie.owner_id IS '所属用户ID';
COMMENT ON COLUMN douyin_cookie.cookie_name IS 'Cookie名称（用于标识）';
COMMENT ON COLUMN douyin_cookie.cookie_value IS 'Cookie值（加密存储）';
COMMENT ON COLUMN douyin_cookie.platform IS '平台';
COMMENT ON COLUMN douyin_cookie.account_name IS '关联账号名称';
COMMENT ON COLUMN douyin_cookie.expire_time IS '过期时间';
COMMENT ON COLUMN douyin_cookie.is_valid IS '是否有效';
COMMENT ON COLUMN douyin_cookie.last_check_time IS '最后验证时间';
COMMENT ON COLUMN douyin_cookie.check_status IS '验证状态（valid/invalid/unknown）';
COMMENT ON COLUMN douyin_cookie.usage_count IS '使用次数';
COMMENT ON COLUMN douyin_cookie.last_used_time IS '最后使用时间';
COMMENT ON COLUMN douyin_cookie.notes IS '备注';
COMMENT ON COLUMN douyin_cookie.deleted IS '逻辑删除标记';
COMMENT ON COLUMN douyin_cookie.create_time IS '创建时间';
COMMENT ON COLUMN douyin_cookie.update_time IS '更新时间';

CREATE INDEX idx_douyin_cookie_owner ON douyin_cookie(owner_id, deleted);
CREATE INDEX idx_douyin_cookie_valid ON douyin_cookie(is_valid, deleted);
CREATE INDEX idx_douyin_cookie_platform ON douyin_cookie(platform, deleted);

-- 5. 分析任务表
CREATE TABLE benchmark_task (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    benchmark_account_id BIGINT,
    task_type VARCHAR(32) NOT NULL,
    task_status VARCHAR(16) DEFAULT 'pending' NOT NULL,
    progress INTEGER DEFAULT 0,
    total_videos INTEGER DEFAULT 0,
    processed_videos INTEGER DEFAULT 0,
    failed_videos INTEGER DEFAULT 0,
    config_json TEXT,
    result_summary TEXT,
    error_message TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE benchmark_task IS '分析任务表';
COMMENT ON COLUMN benchmark_task.id IS '主键ID';
COMMENT ON COLUMN benchmark_task.owner_id IS '所属用户ID';
COMMENT ON COLUMN benchmark_task.benchmark_account_id IS '关联账号ID（可选）';
COMMENT ON COLUMN benchmark_task.task_type IS '任务类型（search_account/analyze_video/batch_analyze）';
COMMENT ON COLUMN benchmark_task.task_status IS '任务状态（pending/running/completed/failed）';
COMMENT ON COLUMN benchmark_task.progress IS '进度百分比（0-100）';
COMMENT ON COLUMN benchmark_task.total_videos IS '总视频数';
COMMENT ON COLUMN benchmark_task.processed_videos IS '已处理视频数';
COMMENT ON COLUMN benchmark_task.failed_videos IS '失败视频数';
COMMENT ON COLUMN benchmark_task.config_json IS '任务配置JSON';
COMMENT ON COLUMN benchmark_task.result_summary IS '结果摘要';
COMMENT ON COLUMN benchmark_task.error_message IS '错误信息';
COMMENT ON COLUMN benchmark_task.started_at IS '开始时间';
COMMENT ON COLUMN benchmark_task.completed_at IS '完成时间';
COMMENT ON COLUMN benchmark_task.create_time IS '创建时间';
COMMENT ON COLUMN benchmark_task.update_time IS '更新时间';

CREATE INDEX idx_benchmark_task_owner ON benchmark_task(owner_id);
CREATE INDEX idx_benchmark_task_status ON benchmark_task(task_status);
CREATE INDEX idx_benchmark_task_account ON benchmark_task(benchmark_account_id);
CREATE INDEX idx_benchmark_task_type ON benchmark_task(task_type);
