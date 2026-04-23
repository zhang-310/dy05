-- ============================================================
-- AI 调用日志与额度表
-- 版本：1.0
-- 更新日期：2026-02-28
-- 说明：与 docs/modules/ai/02-数据库设计.md 对齐
-- ============================================================

-- ============================================================
-- 1. ai_call_log — AI 调用日志表（含效果归因字段）
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_call_log (
    id                  BIGSERIAL       PRIMARY KEY,
    user_id             BIGINT          NOT NULL,                           -- 调用用户 ID
    call_type           VARCHAR(64)     NOT NULL,                            -- 调用类型：copywriting/script/analysis/plan/knowledge_query/image/tts/video
    template_code       VARCHAR(128),                                        -- 使用的 Prompt 模板编码
    model_code          VARCHAR(64),                                         -- 实际使用的模型编码
    input_summary       VARCHAR(512),                                        -- 输入摘要（截取前 500 字）
    output_length       INTEGER,                                             -- 输出字符数
    prompt_tokens       INTEGER,                                             -- Prompt Token 数
    completion_tokens   INTEGER,                                             -- 生成 Token 数
    total_tokens        INTEGER,                                             -- 总 Token 数
    duration_ms         BIGINT,                                              -- 耗时（毫秒）
    status              INTEGER         NOT NULL DEFAULT 1,                  -- 1=成功 0=失败
    error_message       VARCHAR(512),                                        -- 失败原因
    is_fallback         INTEGER         NOT NULL DEFAULT 0,                  -- 是否走了备用模型
    referenced_chunk_ids TEXT,                                               -- 本次生成引用的知识条目 ID（JSON 数组）
    linked_video_id     BIGINT,                                              -- 关联的短视频 ID（用户发布后回填）
    linked_session_id   BIGINT,                                              -- 关联的直播场次 ID（用户发布后回填）
    content_effect      VARCHAR(32),                                         -- 内容效果：high_perform/normal/low_perform
    effect_score        DECIMAL(8,2),                                         -- 效果评分（播放量/均值的比值）
    create_time         TIMESTAMP                DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_call_log_user_time ON ai_call_log (user_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_ai_call_log_type_time ON ai_call_log (call_type, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_ai_call_log_user_type_time ON ai_call_log (user_id, call_type, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_ai_call_log_model_status ON ai_call_log (model_code, status, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_ai_call_log_linked_video ON ai_call_log (linked_video_id) WHERE linked_video_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_ai_call_log_attribution ON ai_call_log (content_effect, create_time DESC) WHERE content_effect IS NOT NULL;

COMMENT ON TABLE  ai_call_log              IS 'AI 调用日志表';
COMMENT ON COLUMN ai_call_log.call_type    IS 'copywriting/script/analysis/plan/knowledge_query/image/tts/video';
COMMENT ON COLUMN ai_call_log.referenced_chunk_ids IS '本次生成引用的知识条目 ID（JSON 数组）';
COMMENT ON COLUMN ai_call_log.linked_video_id IS '关联的短视频 ID（用户发布后回填）';
COMMENT ON COLUMN ai_call_log.linked_session_id IS '关联的直播场次 ID（用户发布后回填）';
COMMENT ON COLUMN ai_call_log.content_effect IS '内容效果：high_perform/normal/low_perform';
COMMENT ON COLUMN ai_call_log.effect_score IS '效果评分（播放量/均值的比值）';

-- ============================================================
-- 2. ai_call_quota — AI 调用额度表
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_call_quota (
    id                  BIGSERIAL       PRIMARY KEY,
    user_id             BIGINT          NOT NULL,                           -- 用户 ID
    quota_date          DATE            NOT NULL,                            -- 日期
    used_count          INTEGER         NOT NULL DEFAULT 0,                 -- 已用次数
    max_count           INTEGER         NOT NULL DEFAULT 10,                 -- 最大次数（根据套餐）
    create_time         TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, quota_date)
);

CREATE INDEX IF NOT EXISTS idx_ai_call_quota_user_date ON ai_call_quota (user_id, quota_date);

COMMENT ON TABLE  ai_call_quota              IS 'AI 调用额度表（按用户/日）';
