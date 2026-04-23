-- LF-04：与 Flyway V104 一致（手工执行或对照）
-- 完整脚本见 src/main/resources/db/migration/V104__sv_content_calendar.sql

CREATE TABLE IF NOT EXISTS sv_content_calendar (
    id               BIGSERIAL PRIMARY KEY,
    owner_id         BIGINT       NOT NULL,
    persona_id       BIGINT,
    plan_date        DATE         NOT NULL,
    content_type     VARCHAR(32)  NOT NULL,
    title            VARCHAR(256),
    brief            TEXT,
    script_id        BIGINT,
    project_id       BIGINT,
    shooting_task_id BIGINT,
    status           INTEGER      NOT NULL DEFAULT 0,
    priority         INTEGER      NOT NULL DEFAULT 0,
    publish_time     VARCHAR(16),
    account_id       BIGINT,
    tags             TEXT,
    deleted          INTEGER      NOT NULL DEFAULT 0,
    create_time      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sv_content_calendar IS '短视频内容日历：每日内容排布规划';
COMMENT ON COLUMN sv_content_calendar.content_type IS
  '内容类型：viral_clone/daily/soft_ad/review/tutorial/unboxing/seeding/skit/vlog/before_after/live_preview/trending';
COMMENT ON COLUMN sv_content_calendar.status IS
  '状态：0=计划中, 1=脚本已生成, 2=拍摄中, 3=已拍摄, 4=已发布';
COMMENT ON COLUMN sv_content_calendar.publish_time IS '计划发布时间 HH:mm';

CREATE INDEX IF NOT EXISTS idx_sv_content_cal_owner ON sv_content_calendar(owner_id, deleted, plan_date DESC);
CREATE INDEX IF NOT EXISTS idx_sv_content_cal_date ON sv_content_calendar(plan_date, status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sv_content_cal_persona ON sv_content_calendar(persona_id, plan_date) WHERE deleted = 0 AND persona_id IS NOT NULL;
