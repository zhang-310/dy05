-- ============================================================
-- ai 模块 - 数据库表结构
-- 模块：AI 服务管理（AI Service Management）
-- 数据库：PostgreSQL
-- 版本：1.0
-- 更新日期：2026-02-25
-- 说明：字段名、类型、默认值已与 Entity 层完全对齐
-- ============================================================

-- ============================================================
-- 1. ai_model - AI 模型配置表
-- Entity: cn.gaifan.douyinOperations.module.ai.entity.AiModel
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_model (
    id                      BIGSERIAL       PRIMARY KEY,
    model_name              VARCHAR(64)     NOT NULL,                           -- 模型名称：gpt-4, claude-3等
    model_provider          VARCHAR(32)     NOT NULL,                           -- 模型提供商：gpt, claude, local
    model_version           VARCHAR(32)     NOT NULL,                           -- 模型版本：4, 3-opus等
    api_key                 VARCHAR(512),                                       -- API密钥（加密存储）
    max_tokens              INTEGER         NOT NULL DEFAULT 2048,              -- 最大Token数
    temperature             NUMERIC(4,2)    NOT NULL DEFAULT 0.70,              -- 温度参数（0.0-2.0）
    status                  INTEGER         NOT NULL DEFAULT 1,                 -- 状态：1=可用 0=不可用
    cost_per_1k_tokens      NUMERIC(10,6)   NOT NULL DEFAULT 0,                 -- 每1000Token的成本
    quota_limit             BIGINT                   DEFAULT 0,                 -- 配额限制
    quota_used              BIGINT          NOT NULL DEFAULT 0,                 -- 已使用配额
    deleted                 INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除：0=正常 1=已删除
    create_time             TIMESTAMP                DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    update_time             TIMESTAMP                DEFAULT CURRENT_TIMESTAMP  -- 更新时间
);

CREATE INDEX IF NOT EXISTS idx_ai_model_provider ON ai_model (model_provider) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_ai_model_status ON ai_model (status) WHERE deleted = 0;

COMMENT ON TABLE  ai_model                      IS 'AI 模型配置表';
COMMENT ON COLUMN ai_model.model_name           IS '模型名称：gpt-4, claude-3等';
COMMENT ON COLUMN ai_model.model_provider       IS '模型提供商：gpt, claude, local';
COMMENT ON COLUMN ai_model.model_version        IS '模型版本：4, 3-opus等';
COMMENT ON COLUMN ai_model.api_key              IS 'API密钥（加密存储）';
COMMENT ON COLUMN ai_model.max_tokens           IS '最大Token数';
COMMENT ON COLUMN ai_model.temperature          IS '温度参数（0.0-2.0）';
COMMENT ON COLUMN ai_model.status               IS '状态：1=可用 0=不可用';
COMMENT ON COLUMN ai_model.cost_per_1k_tokens   IS '每1000Token的成本';
COMMENT ON COLUMN ai_model.quota_limit          IS '配额限制';
COMMENT ON COLUMN ai_model.quota_used           IS '已使用配额';
COMMENT ON COLUMN ai_model.deleted              IS '逻辑删除：0=正常 1=已删除';

-- ============================================================
-- 2. ai_prompt_template - Prompt 模板表
-- Entity: cn.gaifan.douyinOperations.module.ai.entity.AiPromptTemplate
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_prompt_template (
    id                      BIGSERIAL       PRIMARY KEY,
    user_id                 BIGINT          NOT NULL,                           -- 用户ID
    template_name           VARCHAR(128)    NOT NULL,                           -- 模板名称
    template_content        TEXT            NOT NULL,                           -- 模板内容（支持{变量}语法）
    category                VARCHAR(32),                                        -- 分类：内容生成、翻译、优化等
    variables               TEXT,                                               -- 变量定义（JSON格式）
    status                  INTEGER         NOT NULL DEFAULT 1,                 -- 状态：1=可用 0=不可用
    deleted                 INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除：0=正常 1=已删除
    create_time             TIMESTAMP                DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    update_time             TIMESTAMP                DEFAULT CURRENT_TIMESTAMP  -- 更新时间
);

CREATE INDEX IF NOT EXISTS idx_ai_prompt_user_id ON ai_prompt_template (user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_ai_prompt_category ON ai_prompt_template (category) WHERE deleted = 0;

COMMENT ON TABLE  ai_prompt_template              IS 'Prompt 模板表';
COMMENT ON COLUMN ai_prompt_template.user_id     IS '用户ID';
COMMENT ON COLUMN ai_prompt_template.template_name IS '模板名称';
COMMENT ON COLUMN ai_prompt_template.template_content IS '模板内容（支持{变量}语法）';
COMMENT ON COLUMN ai_prompt_template.category    IS '分类：内容生成、翻译、优化等';
COMMENT ON COLUMN ai_prompt_template.variables   IS '变量定义（JSON格式）';
COMMENT ON COLUMN ai_prompt_template.status      IS '状态：1=可用 0=不可用';
COMMENT ON COLUMN ai_prompt_template.deleted     IS '逻辑删除：0=正常 1=已删除';

-- ============================================================
-- 3. ai_knowledge_base - 知识库表
-- Entity: cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_knowledge_base (
    id                      BIGSERIAL       PRIMARY KEY,
    user_id                 BIGINT          NOT NULL,                           -- 用户ID
    kb_name                 VARCHAR(128)    NOT NULL,                           -- 知识库名称
    description             TEXT,                                               -- 描述信息
    total_documents         INTEGER         NOT NULL DEFAULT 0,                 -- 文档总数
    total_tokens            BIGINT          NOT NULL DEFAULT 0,                 -- 总Token数
    embedding_model         VARCHAR(64),                                        -- 向量模型名称
    status                  INTEGER         NOT NULL DEFAULT 0,                 -- 状态：1=就绪 0=构建中
    deleted                 INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除：0=正常 1=已删除
    create_time             TIMESTAMP                DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    update_time             TIMESTAMP                DEFAULT CURRENT_TIMESTAMP  -- 更新时间
);

CREATE INDEX IF NOT EXISTS idx_ai_kb_user_id ON ai_knowledge_base (user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_ai_kb_status ON ai_knowledge_base (status) WHERE deleted = 0;

COMMENT ON TABLE  ai_knowledge_base              IS '知识库表';
COMMENT ON COLUMN ai_knowledge_base.user_id     IS '用户ID';
COMMENT ON COLUMN ai_knowledge_base.kb_name     IS '知识库名称';
COMMENT ON COLUMN ai_knowledge_base.description IS '描述信息';
COMMENT ON COLUMN ai_knowledge_base.total_documents IS '文档总数';
COMMENT ON COLUMN ai_knowledge_base.total_tokens IS '总Token数';
COMMENT ON COLUMN ai_knowledge_base.embedding_model IS '向量模型名称';
COMMENT ON COLUMN ai_knowledge_base.status      IS '状态：1=就绪 0=构建中';
COMMENT ON COLUMN ai_knowledge_base.deleted     IS '逻辑删除：0=正常 1=已删除';

-- ============================================================
-- 4. ai_generation_task - AI 生成任务表
-- Entity: cn.gaifan.douyinOperations.module.ai.entity.AiGenerationTask
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_generation_task (
    id                      BIGSERIAL       PRIMARY KEY,
    user_id                 BIGINT          NOT NULL,                           -- 用户ID
    task_type               VARCHAR(32)     NOT NULL,                           -- 任务类型：内容生成、翻译、优化等
    input_content           TEXT,                                               -- 输入内容
    prompt                  TEXT,                                               -- 使用的Prompt
    model_used              VARCHAR(64),                                        -- 使用的模型
    output_content          TEXT,                                               -- 输出内容
    tokens_used             BIGINT          NOT NULL DEFAULT 0,                 -- 使用的Token数
    task_status             INTEGER         NOT NULL DEFAULT 0,                 -- 任务状态：0=待处理 1=处理中 2=完成 3=失败
    error_msg               TEXT,                                               -- 错误信息
    deleted                 INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除：0=正常 1=已删除
    create_time             TIMESTAMP                DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    update_time             TIMESTAMP                DEFAULT CURRENT_TIMESTAMP  -- 更新时间
);

CREATE INDEX IF NOT EXISTS idx_ai_task_user_id ON ai_generation_task (user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_ai_task_status ON ai_generation_task (task_status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_ai_task_type ON ai_generation_task (task_type) WHERE deleted = 0;

COMMENT ON TABLE  ai_generation_task              IS 'AI 生成任务表';
COMMENT ON COLUMN ai_generation_task.user_id    IS '用户ID';
COMMENT ON COLUMN ai_generation_task.task_type  IS '任务类型：内容生成、翻译、优化等';
COMMENT ON COLUMN ai_generation_task.input_content IS '输入内容';
COMMENT ON COLUMN ai_generation_task.prompt     IS '使用的Prompt';
COMMENT ON COLUMN ai_generation_task.model_used IS '使用的模型';
COMMENT ON COLUMN ai_generation_task.output_content IS '输出内容';
COMMENT ON COLUMN ai_generation_task.tokens_used IS '使用的Token数';
COMMENT ON COLUMN ai_generation_task.task_status IS '任务状态：0=待处理 1=处理中 2=完成 3=失败';
COMMENT ON COLUMN ai_generation_task.error_msg  IS '错误信息';
COMMENT ON COLUMN ai_generation_task.deleted    IS '逻辑删除：0=正常 1=已删除';
