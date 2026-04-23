-- ============================================================
-- V007: Phase 1 升级 — Prompt模板重构 + 外部API管理 + 直播/短视频增强
-- 日期：2026-03-17
-- ============================================================

-- ============================================================
-- 1. ai_prompt_template 表重构（兼容现有数据）
-- ============================================================

-- 新增字段（保留现有字段不删）
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS template_code VARCHAR(64);
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS variant_name VARCHAR(64) DEFAULT 'default';
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 1;
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS system_prompt TEXT;
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS user_prompt_tpl TEXT;
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS model_hint VARCHAR(64);
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS temperature FLOAT DEFAULT 0.7;
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS max_tokens INTEGER DEFAULT 2000;
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS is_default BOOLEAN DEFAULT FALSE;
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS usage_count INTEGER DEFAULT 0;
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS avg_score FLOAT;
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS p50_score FLOAT;
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS p90_score FLOAT;
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS owner_id BIGINT DEFAULT 0;

-- 唯一索引（code + variant + version）
CREATE UNIQUE INDEX IF NOT EXISTS idx_prompt_tpl_code_variant_ver
    ON ai_prompt_template(template_code, variant_name, version) WHERE deleted = 0;

-- ============================================================
-- 2. ai_model_benchmark 模型性能基准表
-- ============================================================

CREATE TABLE IF NOT EXISTS ai_model_benchmark (
    id               BIGSERIAL     PRIMARY KEY,
    model_id         VARCHAR(64)   NOT NULL,
    task_code        VARCHAR(64)   NOT NULL,
    call_date        DATE          NOT NULL,
    total_calls      INTEGER       DEFAULT 0,
    success_calls    INTEGER       DEFAULT 0,
    avg_latency_ms   INTEGER,
    p95_latency_ms   INTEGER,
    avg_input_tokens INTEGER,
    avg_output_tokens INTEGER,
    estimated_cost   NUMERIC(10,4),
    avg_quality_score FLOAT,
    create_time      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_model_bench_model_task_date
    ON ai_model_benchmark(model_id, task_code, call_date);

-- ============================================================
-- 3. external_api_config 外部API配置管理
-- ============================================================

CREATE TABLE IF NOT EXISTS external_api_config (
    id                   BIGSERIAL     PRIMARY KEY,
    provider_code        VARCHAR(64)   NOT NULL,
    provider_name        VARCHAR(128)  NOT NULL,
    category             VARCHAR(32)   NOT NULL,
    base_url             VARCHAR(512)  NOT NULL,
    api_key_encrypted    VARCHAR(1024),
    api_secret_encrypted VARCHAR(1024),
    is_enabled           BOOLEAN       DEFAULT TRUE,
    priority             INTEGER       DEFAULT 100,
    rate_limit_per_min   INTEGER,
    daily_quota          INTEGER,
    monthly_quota        INTEGER,
    last_health_check    TIMESTAMP,
    health_status        VARCHAR(16)   DEFAULT 'unknown',
    avg_latency_ms       INTEGER,
    success_rate_pct     FLOAT,
    extra_config         JSONB,
    deleted              INTEGER       DEFAULT 0,
    create_time          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_ext_api_cfg_code
    ON external_api_config(provider_code) WHERE deleted = 0;

-- ============================================================
-- 4. external_api_call_log 外部API调用日志
-- ============================================================

CREATE TABLE IF NOT EXISTS external_api_call_log (
    id               BIGSERIAL     PRIMARY KEY,
    provider_code    VARCHAR(64)   NOT NULL,
    endpoint         VARCHAR(256),
    method           VARCHAR(8),
    request_summary  TEXT,
    response_status  INTEGER,
    latency_ms       INTEGER,
    error_message    TEXT,
    caller_module    VARCHAR(32),
    caller_user_id   BIGINT,
    estimated_cost   NUMERIC(10,6),
    create_time      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ext_api_log_provider_date
    ON external_api_call_log(provider_code, create_time);

-- ============================================================
-- 5. live_script 新增字段
-- ============================================================

ALTER TABLE live_script ADD COLUMN IF NOT EXISTS prompt_template_id BIGINT;
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS platform VARCHAR(32);

-- ============================================================
-- 6. live_session 新增字段
-- ============================================================

ALTER TABLE live_session ADD COLUMN IF NOT EXISTS platform VARCHAR(32) DEFAULT 'douyin';

-- ============================================================
-- 7. live_approval_log 审批记录表
-- ============================================================

CREATE TABLE IF NOT EXISTS live_approval_log (
    id               BIGSERIAL     PRIMARY KEY,
    session_id       BIGINT        NOT NULL,
    script_id        BIGINT,
    action           VARCHAR(16)   NOT NULL,
    operator_id      BIGINT        NOT NULL,
    comment          TEXT,
    deleted          INTEGER       DEFAULT 0,
    create_time      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_approval_log_session
    ON live_approval_log(session_id, create_time);

-- ============================================================
-- 8. sv_script 新增字段
-- ============================================================

ALTER TABLE sv_script ADD COLUMN IF NOT EXISTS use_kb_ref BOOLEAN DEFAULT FALSE;
ALTER TABLE sv_script ADD COLUMN IF NOT EXISTS kb_ref_ids TEXT;
ALTER TABLE sv_script ADD COLUMN IF NOT EXISTS persona_check_score INTEGER;

-- ============================================================
-- 9. sv_daily_batch 一键日更批次
-- ============================================================

CREATE TABLE IF NOT EXISTS sv_daily_batch (
    id               BIGSERIAL     PRIMARY KEY,
    owner_id         BIGINT        NOT NULL,
    persona_id       BIGINT,
    source_type      VARCHAR(32),
    batch_size       INTEGER       DEFAULT 3,
    status           VARCHAR(16)   DEFAULT 'pending',
    result_summary   JSONB,
    deleted          INTEGER       DEFAULT 0,
    create_time      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 10. 初始化外部API配置（seed data）
-- ============================================================

INSERT INTO external_api_config (provider_code, provider_name, category, base_url, is_enabled, priority)
VALUES
    ('deepseek', 'DeepSeek API', 'llm', 'https://api.deepseek.com', true, 10),
    ('ollama', 'Ollama (本地)', 'llm', 'http://localhost:11434', true, 20),
    ('openai', 'OpenAI API', 'llm', 'https://api.openai.com', false, 30),
    ('xunfei_tts', '讯飞 TTS', 'tts', 'wss://tts-api.xfyun.cn', true, 10),
    ('tianapi', 'TianAPI', 'trend', 'https://api.tianapi.com', true, 10),
    ('guiguiya', '鬼鬼鸭热榜', 'trend', 'https://api.guiguiya.com', true, 20),
    ('baidu_bos', '百度 BOS', 'storage', 'https://bj.bcebos.com', true, 10),
    ('baidu_audit', '百度内容审核', 'audit', 'https://aip.baidubce.com', false, 10),
    ('kling', 'Kling 视频生成', 'video', 'https://api.klingai.com', false, 10),
    ('minimax', 'MiniMax/Hailuo', 'video', 'https://api.minimax.chat', false, 20),
    ('suno', 'Suno BGM', 'audio', 'https://api.sunoapi.org', false, 10),
    ('edge_tts', 'Edge-TTS', 'tts', 'http://localhost:5500', false, 20),
    ('sd_local', 'Stable Diffusion (本地)', 'image', 'http://localhost:7860', false, 10),
    ('comfyui', 'ComfyUI', 'image', 'http://localhost:8188', false, 20),
    ('wecom', '企业微信', 'messaging', 'https://qyapi.weixin.qq.com', true, 10)
ON CONFLICT DO NOTHING;
