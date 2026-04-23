-- V152: 多智能体协作编排 DAG 支持
-- 1. agent_workflow / agent_workflow_step 表（可能由外部脚本创建）
-- 2. agent_workflow_step 增加 DAG 相关字段
-- 3. agent_workflow_execution 执行记录表

-- ============================================================
-- agent_workflow - 智能体协作编排工作流表
-- ============================================================
CREATE TABLE IF NOT EXISTS agent_workflow (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    name            VARCHAR(128)    NOT NULL,
    description     VARCHAR(512),
    version         INTEGER         NOT NULL DEFAULT 0,
    status          INTEGER         NOT NULL DEFAULT 1,
    deleted         INTEGER         NOT NULL DEFAULT 0,
    create_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_agent_workflow_user_id ON agent_workflow (user_id) WHERE deleted = 0;

-- ============================================================
-- agent_workflow_step - 工作流步骤表
-- ============================================================
CREATE TABLE IF NOT EXISTS agent_workflow_step (
    id              BIGSERIAL       PRIMARY KEY,
    workflow_id     BIGINT          NOT NULL,
    step_order      INTEGER         NOT NULL,
    agent_id        BIGINT          NOT NULL,
    step_name       VARCHAR(128),
    input_template  VARCHAR(2000),
    output_key      VARCHAR(128),
    skip_condition  VARCHAR(512),
    timeout_seconds INTEGER         NOT NULL DEFAULT 120,
    deleted         INTEGER         NOT NULL DEFAULT 0,
    create_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_agent_workflow_step_workflow_id ON agent_workflow_step (workflow_id) WHERE deleted = 0;

-- ============================================================
-- DAG 新增字段（仅在列不存在时添加，兼容已创建的场景）
-- ============================================================
ALTER TABLE agent_workflow_step ADD COLUMN IF NOT EXISTS depends_on TEXT;
ALTER TABLE agent_workflow_step ADD COLUMN IF NOT EXISTS execution_mode INTEGER DEFAULT 0;
ALTER TABLE agent_workflow_step ADD COLUMN IF NOT EXISTS retry_count INTEGER DEFAULT 1;

-- ============================================================
-- agent_workflow_execution 执行记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS agent_workflow_execution (
    id BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status INTEGER DEFAULT 0,
    -- status: 0=running, 1=completed, 2=failed, 3=cancelled
    current_step_order INTEGER DEFAULT 0,
    total_steps INTEGER DEFAULT 0,
    context_data TEXT,
    -- JSON: { "input": "...", "step1": "...", "step2": "..." }
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    error_message TEXT,
    create_time TIMESTAMP DEFAULT NOW(),
    update_time TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_execution_workflow_id ON agent_workflow_execution(workflow_id);
CREATE INDEX IF NOT EXISTS idx_execution_user_id ON agent_workflow_execution(user_id);
CREATE INDEX IF NOT EXISTS idx_execution_status ON agent_workflow_execution(status);
