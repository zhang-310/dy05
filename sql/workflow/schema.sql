-- ============================================================
-- workflow 模块 - 数据库表结构
-- 说明：工作流定义与步骤，支持 live_script 的 generate→iterate→violation_check→save
-- ============================================================

-- ============================================================
-- 1. workflow_definition - 工作流定义表
-- Entity: cn.gaifan.douyinOperations.module.workflow.entity.WorkflowDefinition
-- ============================================================
CREATE TABLE IF NOT EXISTS workflow_definition (
    id              BIGSERIAL       PRIMARY KEY,
    workflow_code   VARCHAR(64)     NOT NULL,                           -- 工作流编码：live_script_full 等
    workflow_name   VARCHAR(128)    NOT NULL,                           -- 名称
    description     VARCHAR(512),                                       -- 描述
    owner_id        BIGINT          NOT NULL,                           -- P0-1: 所有者ID（数据隔离）
    status          INTEGER         NOT NULL DEFAULT 1,                -- 0=禁用 1=启用
    deleted         INTEGER         NOT NULL DEFAULT 0,
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_workflow_code ON workflow_definition (workflow_code) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_workflow_def_owner ON workflow_definition(owner_id) WHERE deleted = 0;

COMMENT ON TABLE  workflow_definition            IS '工作流定义表';
COMMENT ON COLUMN workflow_definition.workflow_code IS '工作流编码：live_script_full=直播话术完整流程';
COMMENT ON COLUMN workflow_definition.owner_id   IS 'P0-1: 所有者ID（数据隔离）';

-- ============================================================
-- 2. workflow_step - 工作流步骤表
-- Entity: cn.gaifan.douyinOperations.module.workflow.entity.WorkflowStep
-- ============================================================
CREATE TABLE IF NOT EXISTS workflow_step (
    id                  BIGSERIAL       PRIMARY KEY,
    definition_id       BIGINT          NOT NULL,                       -- 关联 workflow_definition.id
    step_code           VARCHAR(64)     NOT NULL,                       -- 步骤编码：generate/iterate/violation_check/save
    step_name           VARCHAR(128),                                   -- 步骤名称
    sequence_no         INTEGER         NOT NULL DEFAULT 0,             -- 执行顺序
    step_config         TEXT,                                           -- 步骤配置 JSON
    owner_id            BIGINT          NOT NULL,                       -- P0-1: 所有者ID（数据隔离）
    deleted             INTEGER         NOT NULL DEFAULT 0,
    create_time         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_workflow_step_def ON workflow_step (definition_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_workflow_step_owner ON workflow_step(owner_id) WHERE deleted = 0;

COMMENT ON TABLE  workflow_step                  IS '工作流步骤表';
COMMENT ON COLUMN workflow_step.step_code       IS '步骤编码：generate=生成, iterate=迭代, violation_check=质检, save=保存';
COMMENT ON COLUMN workflow_step.owner_id        IS 'P0-1: 所有者ID（数据隔离）';
