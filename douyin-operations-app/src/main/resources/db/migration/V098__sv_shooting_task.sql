-- 五主播短视频拍摄任务工单（对齐 upgrade-plan-20260321/13）
CREATE TABLE IF NOT EXISTS sv_shooting_task (
    id                        BIGSERIAL PRIMARY KEY,
    owner_id                  BIGINT       NOT NULL,
    anchor_user_id            BIGINT,
    persona_id                BIGINT,
    photographer_id           BIGINT,
    script_id                 BIGINT,
    project_id                BIGINT,
    title                     VARCHAR(256) NOT NULL,
    description               TEXT,
    script_content            TEXT,
    shooting_brief            TEXT,
    shoot_date                DATE         NOT NULL,
    priority                  INTEGER      NOT NULL DEFAULT 0,
    status                    INTEGER      NOT NULL DEFAULT 0,
    material_urls             TEXT,
    review_notes              TEXT,
    reviewed_by               BIGINT,
    reference_video_url       VARCHAR(512),
    reference_video_task_id   BIGINT,
    reference_generated_at    TIMESTAMP,
    deleted                   INTEGER      NOT NULL DEFAULT 0,
    create_time               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sv_shooting_task_owner ON sv_shooting_task (owner_id, deleted, shoot_date DESC);
CREATE INDEX IF NOT EXISTS idx_sv_shooting_task_photo ON sv_shooting_task (photographer_id, status) WHERE photographer_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_sv_shooting_task_date ON sv_shooting_task (shoot_date, status);

COMMENT ON TABLE sv_shooting_task IS '短视频拍摄任务：运营派单、摄影师执行、可选关联脚本/项目';
COMMENT ON COLUMN sv_shooting_task.status IS '0待分配 1已分配 2拍摄中 3已上传 4已审核 5已发布';
