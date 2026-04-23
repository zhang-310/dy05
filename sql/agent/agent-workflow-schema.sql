-- ============================================================
-- agent_workflow - 智能体协作编排工作流表
-- 支持多智能体顺序执行和上下文传递
-- ============================================================
CREATE TABLE IF NOT EXISTS agent_workflow (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,                           -- 创建用户ID
    name            VARCHAR(128)    NOT NULL,                           -- 工作流名称
    description     VARCHAR(512),                                       -- 工作流描述
    version         INTEGER         NOT NULL DEFAULT 0,                 -- 版本号
    status          INTEGER         NOT NULL DEFAULT 1,                 -- 状态：1=启用 0=禁用
    deleted         INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除：0=正常 1=已删除
    create_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,           -- 创建时间
    update_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP            -- 更新时间
);

CREATE INDEX IF NOT EXISTS idx_agent_workflow_user_id ON agent_workflow (user_id) WHERE deleted = 0;

COMMENT ON TABLE  agent_workflow              IS '智能体协作编排工作流表';
COMMENT ON COLUMN agent_workflow.name        IS '工作流名称';
COMMENT ON COLUMN agent_workflow.description IS '工作流描述';
COMMENT ON COLUMN agent_workflow.status      IS '状态：1=启用 0=禁用';

-- ============================================================
-- agent_workflow_step - 工作流步骤表
-- ============================================================
CREATE TABLE IF NOT EXISTS agent_workflow_step (
    id              BIGSERIAL       PRIMARY KEY,
    workflow_id     BIGINT          NOT NULL,                           -- 所属工作流ID
    step_order      INTEGER         NOT NULL,                           -- 执行顺序（1-based）
    agent_id        BIGINT          NOT NULL,                           -- 关联的智能体ID
    step_name       VARCHAR(128),                                       -- 步骤名称（如"第一步：分析"）
    input_template  VARCHAR(2000),                                      -- 输入模板（如"用户问题：${input}，上一步结果：${prev.output}"）
    output_key      VARCHAR(128),                                       -- 输出结果的 key（存储到上下文的 key）
    skip_condition  VARCHAR(512),                                        -- 跳过条件（SpEL 表达式或简单关键字匹配）
    timeout_seconds INTEGER         NOT NULL DEFAULT 120,                -- 超时时间（秒）
    deleted         INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除
    create_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_agent_workflow_step_workflow_id ON agent_workflow_step (workflow_id) WHERE deleted = 0;

COMMENT ON TABLE  agent_workflow_step                IS '工作流步骤表';
COMMENT ON COLUMN agent_workflow_step.input_template IS '输入模板，支持 ${input}/${prev.output} 等占位符';
COMMENT ON COLUMN agent_workflow_step.output_key     IS '输出结果的 key，用于后续步骤引用';
COMMENT ON COLUMN agent_workflow_step.skip_condition  IS '跳过条件表达式';
