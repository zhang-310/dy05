-- 账号短视频采集任务表
CREATE TABLE IF NOT EXISTS sv_account_collect_task (
    id                 BIGSERIAL    PRIMARY KEY,
    owner_id           BIGINT       NOT NULL,
    account_id         BIGINT,
    account_url        VARCHAR(512),
    account_name       VARCHAR(128),
    sec_uid            VARCHAR(256),
    status             VARCHAR(32)  NOT NULL DEFAULT 'pending',
    total_videos       INTEGER      NOT NULL DEFAULT 0,
    collected_videos   INTEGER      NOT NULL DEFAULT 0,
    analyzed_videos    INTEGER      NOT NULL DEFAULT 0,
    indexed_videos     INTEGER      NOT NULL DEFAULT 0,
    target_kb_id       BIGINT,
    error_message      TEXT,
    deleted            INTEGER      NOT NULL DEFAULT 0,
    create_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sv_act_owner ON sv_account_collect_task(owner_id, deleted);
CREATE INDEX IF NOT EXISTS idx_sv_act_status ON sv_account_collect_task(status) WHERE deleted = 0;

-- sv_viral_video 新增 collect_task_id
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS collect_task_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_sv_vv_collect_task ON sv_viral_video(collect_task_id) WHERE collect_task_id IS NOT NULL;
