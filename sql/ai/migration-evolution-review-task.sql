-- 进化审核任务表
-- 管理灰色地带（40-50分）内容的人工审核工作流
CREATE TABLE IF NOT EXISTS ai_evolution_review_task (
    id              BIGSERIAL PRIMARY KEY,
    evolve_task_id  BIGINT,
    content_preview VARCHAR(500),
    quality_score   INTEGER,
    reviewer_id     BIGINT,
    review_status   VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    review_comment  TEXT,
    revised_content TEXT,
    reviewed_at     TIMESTAMP,
    auto_expired    BOOLEAN      DEFAULT FALSE,
    deleted         INTEGER      NOT NULL DEFAULT 0,
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_evolution_review_status ON ai_evolution_review_task(review_status);
CREATE INDEX IF NOT EXISTS idx_evolution_review_task_id ON ai_evolution_review_task(evolve_task_id);
