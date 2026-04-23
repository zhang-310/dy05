-- 生成任务表：持久化话术批量生成任务的进度与状态
CREATE TABLE IF NOT EXISTS live_generation_task (
    id              BIGSERIAL       PRIMARY KEY,
    session_id      BIGINT          NOT NULL,       -- 关联 live_session.id
    status          VARCHAR(20)     DEFAULT 'running',  -- running / completed / failed / cancelled
    total_slots     INTEGER         DEFAULT 0,
    completed_slots INTEGER         DEFAULT 0,
    failed_slots    INTEGER         DEFAULT 0,
    style           VARCHAR(50),
    model_id        BIGINT,
    use_kb_ref      BOOLEAN         DEFAULT false,
    hot_keywords    TEXT,               -- JSON array of hot keywords
    error_message   TEXT,
    owner_id        BIGINT,
    deleted         INTEGER         DEFAULT 0,
    create_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_live_gen_task_session ON live_generation_task(session_id);
CREATE INDEX IF NOT EXISTS idx_live_gen_task_owner   ON live_generation_task(owner_id);
CREATE INDEX IF NOT EXISTS idx_live_gen_task_status  ON live_generation_task(status);

COMMENT ON TABLE  live_generation_task               IS '话术生成任务';
COMMENT ON COLUMN live_generation_task.session_id     IS '关联直播场次 ID';
COMMENT ON COLUMN live_generation_task.status         IS '任务状态：running/completed/failed/cancelled';
COMMENT ON COLUMN live_generation_task.total_slots    IS '总插槽数（需要生成的话术段数）';
COMMENT ON COLUMN live_generation_task.completed_slots IS '已完成插槽数';
COMMENT ON COLUMN live_generation_task.failed_slots   IS '失败插槽数';
COMMENT ON COLUMN live_generation_task.style          IS '话术风格';
COMMENT ON COLUMN live_generation_task.model_id       IS '使用的 AI 模型 ID';
COMMENT ON COLUMN live_generation_task.use_kb_ref     IS '是否引用知识库';
COMMENT ON COLUMN live_generation_task.hot_keywords   IS '热门关键词（JSON 数组）';
COMMENT ON COLUMN live_generation_task.error_message  IS '错误信息';
COMMENT ON COLUMN live_generation_task.owner_id       IS '所属用户 ID（数据隔离）';
COMMENT ON COLUMN live_generation_task.deleted        IS '逻辑删除：0=正常 1=已删除';
