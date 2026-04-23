-- ============================================================
-- AI 知识进化引擎 - 表结构
-- 版本：1.0
-- 更新日期：2026-02-25
-- ============================================================

-- ============================================================
-- 1. ai_evolve_topic — 进化主题池表
-- Entity: cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTopic
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_evolve_topic (
    id              BIGSERIAL       PRIMARY KEY,
    kb_id           BIGINT,                                                -- 知识库ID，NULL=全局主题
    topic           VARCHAR(256)    NOT NULL,                              -- 主题短语
    category        VARCHAR(32)             DEFAULT 'basic',              -- 分类
    priority        INTEGER         NOT NULL DEFAULT 100,                   -- 优先级（越小越优先，0=最高）
    source          VARCHAR(16)     NOT NULL DEFAULT 'initial',            -- 来源：initial/expanded/deepened/manual
    used_count      INTEGER                 DEFAULT 0,                     -- 已使用次数
    last_used_time  TIMESTAMP,                                              -- 最后使用时间
    score_avg       DECIMAL(5,2),                                           -- 产出报告平均得分
    status          INTEGER         NOT NULL DEFAULT 1,                     -- 1=启用 0=禁用
    deleted         INTEGER         NOT NULL DEFAULT 0,                     -- 逻辑删除
    create_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_evolve_topic_category_priority ON ai_evolve_topic (category, priority, status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_ai_evolve_topic_source_time ON ai_evolve_topic (source, create_time DESC) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_ai_evolve_topic_kb_priority ON ai_evolve_topic (kb_id, priority, status) WHERE deleted = 0;

COMMENT ON TABLE  ai_evolve_topic              IS '进化主题池表';
COMMENT ON COLUMN ai_evolve_topic.topic       IS '主题短语';
COMMENT ON COLUMN ai_evolve_topic.category    IS '分类：live/ai/vertical/commercial/algorithm/data/team/compliance/cross_domain/brand/ad/competition/basic';
COMMENT ON COLUMN ai_evolve_topic.priority    IS '优先级（数值越小越优先）';
COMMENT ON COLUMN ai_evolve_topic.source      IS '来源：initial/expanded/deepened/manual';

-- ============================================================
-- 2. ai_evolve_task — 进化任务表
-- Entity: cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTask
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_evolve_task (
    id                  BIGSERIAL       PRIMARY KEY,
    kb_id               BIGINT,                                            -- 进化所属知识库ID
    task_no             VARCHAR(64)     NOT NULL UNIQUE,                   -- 任务编号
    topic_ids           VARCHAR(512),                                      -- 本轮采样的主题ID（JSON数组）
    topic_texts         TEXT,                                              -- 本轮采样的主题文本（JSON数组）
    evolve_angle        VARCHAR(32),                                       -- 进化角度（11选1）
    gather_mode         VARCHAR(16)             DEFAULT 'hybrid',          -- 上下文收集方式
    context_length      INTEGER,                                            -- 上下文长度（字符数）
    model_used          VARCHAR(64),                                       -- 实际使用的模型
    fallback_tier       INTEGER                 DEFAULT 1,                 -- 回退层级
    prompt_tokens       INTEGER,                                            -- Prompt Token数
    completion_tokens   INTEGER,                                            -- 生成 Token数
    total_tokens        INTEGER,                                            -- 总 Token数
    duration_ms         BIGINT,                                             -- 总耗时（毫秒）
    score_total         INTEGER,                                            -- 质量总分（0-100）
    score_detail        TEXT,                                               -- 质量评分详情（JSON）
    expanded_count      INTEGER                 DEFAULT 0,                 -- 本轮扩展的新主题数
    deepened_count     INTEGER                 DEFAULT 0,                   -- 本轮深化排队的主题数
    had_quality_hint    INTEGER         NOT NULL DEFAULT 0,                 -- 是否追加质量提醒
    status              VARCHAR(16)     NOT NULL DEFAULT 'pending',        -- 状态
    error_message       VARCHAR(512),                                       -- 失败原因
    create_time         TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP                DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_evolve_task_status ON ai_evolve_task (status, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_ai_evolve_task_score ON ai_evolve_task (score_total, create_time DESC) WHERE score_total IS NOT NULL;

COMMENT ON TABLE  ai_evolve_task              IS '进化任务表';
COMMENT ON COLUMN ai_evolve_task.status      IS 'pending/gathering/generating/scoring/expanding/indexing/completed/failed';

-- ============================================================
-- 3. ai_evolve_report — 进化报告表
-- Entity: cn.gaifan.douyinOperations.module.ai.entity.AiEvolveReport
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_evolve_report (
    id                      BIGSERIAL       PRIMARY KEY,
    task_id                 BIGINT          NOT NULL,                      -- 关联进化任务ID
    report_title            VARCHAR(256),                                   -- 报告标题
    methodology_section     TEXT,                                           -- 方法论提炼内容
    deepen_section          TEXT,                                           -- 待深化问题内容
    iterate_section         TEXT,                                           -- 可迭代建议内容
    full_content            TEXT            NOT NULL,                       -- 完整报告内容（Markdown）
    methodology_count       INTEGER                 DEFAULT 0,             -- 方法论条目数
    deepen_count            INTEGER                 DEFAULT 0,              -- 深化问题数
    has_failure_case        INTEGER         NOT NULL DEFAULT 0,             -- 是否含失败案例
    has_sop                 INTEGER         NOT NULL DEFAULT 0,             -- 是否含可执行SOP
    has_benchmark           INTEGER         NOT NULL DEFAULT 0,              -- 是否含行业基准
    index_status            VARCHAR(16)     NOT NULL DEFAULT 'pending',   -- 索引状态
    indexed_time            TIMESTAMP,                                       -- 索引完成时间
    create_time             TIMESTAMP                DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_evolve_report_task ON ai_evolve_report (task_id);
CREATE INDEX IF NOT EXISTS idx_ai_evolve_report_index_status ON ai_evolve_report (index_status, create_time DESC);

COMMENT ON TABLE  ai_evolve_report            IS '进化报告表';
COMMENT ON COLUMN ai_evolve_report.index_status IS 'pending/queued/indexing/indexed/skipped/failed';

-- ============================================================
-- 4. ai_evolve_pending_deepen — 待深化问题表
-- 供主题自扩展使用，从报告「待深化问题」章节提取
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_evolve_pending_deepen (
    id              BIGSERIAL       PRIMARY KEY,
    report_id       BIGINT          NOT NULL,                              -- 来源报告ID
    task_id         BIGINT          NOT NULL,                              -- 关联任务ID
    question_text   TEXT            NOT NULL,                              -- 待深化问题文本
    priority_level  INTEGER         NOT NULL DEFAULT 1,                    -- 优先级：0=P0 1=P1
    status          VARCHAR(16)     NOT NULL DEFAULT 'pending',            -- pending/processed
    create_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_evolve_pending_priority ON ai_evolve_pending_deepen (priority_level, status) WHERE status = 'pending';

COMMENT ON TABLE  ai_evolve_pending_deepen    IS '待深化问题表，供主题扩展使用';

-- ============================================================
-- 默认进化主题池（首次部署时执行，重复执行会插入重复数据）
-- ============================================================
INSERT INTO ai_evolve_topic (kb_id, topic, category, priority, source) VALUES
(null, '抖音运营 直播 话术 粉丝互动', 'live', 100, 'initial'),
(null, '短视频 脚本 黄金3秒 内容创作', 'basic', 100, 'initial'),
(null, '数据分析 关键指标 优化决策', 'data', 100, 'initial'),
(null, '算法推荐 流量 爆款 运营', 'algorithm', 100, 'initial'),
(null, '商业化 广告 电商 知识付费 变现', 'commercial', 100, 'initial'),
(null, 'AI AIGC 豆包 剪映AI 数字人', 'ai', 100, 'initial'),
(null, 'AI 辅助创作 大模型 短视频 直播', 'ai', 100, 'initial'),
(null, '垂类策略 美妆 知识科普 剧情 户外', 'vertical', 100, 'initial'),
(null, '剪映 工具链 发布质检 平台规则', 'compliance', 100, 'initial'),
(null, '账号矩阵 团队管理 中高级战术', 'team', 100, 'initial');
