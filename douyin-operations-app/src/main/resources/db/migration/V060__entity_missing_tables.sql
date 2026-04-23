-- V060: 补全 Entity 缺失表（与 schema-validation 对齐）
-- 参考：sql/storage/uploadable-schema.sql, sql/storage/migration-bos-file-metadata.sql,
--       sql/shortvideo/migration-bos-production.sql, sql/_archive/legacy-manual-migrations/upgrade-analysis-2026.sql
-- ADR-002: 无数据库外键，task_id 等仅逻辑关联

-- 1. sys_upload_task — 分块上传任务表
CREATE TABLE IF NOT EXISTS sys_upload_task (
    id                  BIGSERIAL       PRIMARY KEY,
    owner_id            BIGINT          NOT NULL,
    upload_id           VARCHAR(64)     NOT NULL UNIQUE,
    bos_upload_id       VARCHAR(256),
    original_filename   VARCHAR(256)    NOT NULL,
    storage_key         VARCHAR(512)    NOT NULL,
    file_size           BIGINT          NOT NULL,
    file_md5            VARCHAR(64),
    chunk_size          INTEGER         DEFAULT 5242880,
    total_chunks        INTEGER         NOT NULL,
    uploaded_chunks     INTEGER         DEFAULT 0,
    uploaded_bytes      BIGINT          DEFAULT 0,
    status              VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    failure_reason      VARCHAR(512),
    module              VARCHAR(64),
    deleted             INTEGER         NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at        TIMESTAMP,
    expire_at           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP + INTERVAL '7 days'
);
CREATE INDEX IF NOT EXISTS idx_task_owner_status ON sys_upload_task (owner_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_task_upload_id ON sys_upload_task (upload_id);
CREATE INDEX IF NOT EXISTS idx_task_expire ON sys_upload_task (expire_at) WHERE deleted = 0;

-- 2. sys_upload_chunk — 分块记录表（无外键，ADR-002）
CREATE TABLE IF NOT EXISTS sys_upload_chunk (
    id                  BIGSERIAL       PRIMARY KEY,
    task_id             BIGINT          NOT NULL,
    chunk_index         INTEGER         NOT NULL,
    chunk_size          BIGINT          NOT NULL,
    chunk_md5           VARCHAR(64)     NOT NULL,
    bos_etag            VARCHAR(64),
    bos_part_number     INTEGER,
    status              VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    retry_count         INTEGER         DEFAULT 0,
    uploaded_at         TIMESTAMP,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uc_chunk_task_index UNIQUE (task_id, chunk_index)
);
CREATE INDEX IF NOT EXISTS idx_chunk_status ON sys_upload_chunk (status, created_at DESC);

-- 3. bos_file_metadata — BOS 文件元数据表
CREATE TABLE IF NOT EXISTS bos_file_metadata (
    id                   BIGSERIAL    PRIMARY KEY,
    bos_key              VARCHAR(500) NOT NULL,
    user_id              BIGINT       NOT NULL,
    task_id              BIGINT,
    category             VARCHAR(50),
    file_size            BIGINT       DEFAULT 0,
    storage_cost_monthly DECIMAL(10, 4) DEFAULT 0,
    usage_count          INTEGER      DEFAULT 0,
    shot_id              BIGINT,
    source_url           VARCHAR(500),
    deleted              INTEGER      NOT NULL DEFAULT 0,
    create_time          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_bos_file_metadata_key ON bos_file_metadata (bos_key) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_bos_file_metadata_user ON bos_file_metadata (user_id, create_time DESC);

-- 4. sv_project — 短视频项目表
CREATE TABLE IF NOT EXISTS sv_project (
    id                     BIGSERIAL    PRIMARY KEY,
    owner_id               BIGINT       NOT NULL,
    account_id             BIGINT,
    title                  VARCHAR(255)  NOT NULL,
    project_type           VARCHAR(50)  NOT NULL,
    persona_id             BIGINT,
    schedule_date          DATE,
    shoot_status           VARCHAR(32),
    status                 VARCHAR(50)  NOT NULL DEFAULT 'draft',
    script_id              BIGINT,
    shot_list_id           BIGINT,
    final_video_url        VARCHAR(500),
    thumbnail_url         VARCHAR(500),
    character_reference_url VARCHAR(500),
    scene_reference_url    VARCHAR(500),
    duration               INTEGER,
    publish_title          VARCHAR(255),
    publish_platforms      TEXT,
    publish_time           TIMESTAMP,
    review_status          VARCHAR(50),
    reviewer_id            BIGINT,
    review_time            TIMESTAMP,
    review_comment         TEXT,
    deleted                INTEGER      NOT NULL DEFAULT 0,
    create_time            TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time            TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sv_project_owner ON sv_project (owner_id, status, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_sv_project_type ON sv_project (project_type);

-- 5. sv_script — 脚本表
CREATE TABLE IF NOT EXISTS sv_script (
    id                 BIGSERIAL    PRIMARY KEY,
    owner_id           BIGINT       NOT NULL,
    title              VARCHAR(255) NOT NULL,
    content            TEXT         NOT NULL,
    script_type        VARCHAR(50)  NOT NULL,
    generation_type    VARCHAR(50),
    reference_viral_id BIGINT,
    theme              VARCHAR(255),
    style              VARCHAR(50),
    duration           INTEGER,
    word_count         INTEGER,
    tags               TEXT,
    ai_prompt          TEXT,
    ai_model           VARCHAR(100),
    deleted            INTEGER      NOT NULL DEFAULT 0,
    create_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sv_script_owner ON sv_script (owner_id, create_time DESC);

-- 6. sv_shot_list — 分镜列表表
CREATE TABLE IF NOT EXISTS sv_shot_list (
    id                 BIGSERIAL    PRIMARY KEY,
    owner_id           BIGINT       NOT NULL,
    script_id          BIGINT       NOT NULL,
    shot_count         INTEGER      NOT NULL,
    deleted            INTEGER      NOT NULL DEFAULT 0,
    create_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sv_shot_list_owner ON sv_shot_list (owner_id);
CREATE INDEX IF NOT EXISTS idx_sv_shot_list_script ON sv_shot_list (script_id);

-- 7. sv_shot — 分镜详情表
CREATE TABLE IF NOT EXISTS sv_shot (
    id                 BIGSERIAL    PRIMARY KEY,
    shot_list_id       BIGINT       NOT NULL,
    shot_number        INTEGER      NOT NULL,
    time_range         VARCHAR(50),
    scene_description  TEXT,
    camera_angle       VARCHAR(100),
    camera_type        VARCHAR(50),
    camera_params      TEXT,
    quality_level      VARCHAR(20),
    ai_model           VARCHAR(50),
    quality_score      DECIMAL(5, 2),
    action             VARCHAR(255),
    dialogue           TEXT,
    mood               VARCHAR(100),
    keyframe_url       VARCHAR(500),
    keyframe_bos_key   VARCHAR(500),
    end_frame_url      VARCHAR(500),
    end_frame_bos_key  VARCHAR(500),
    video_url          VARCHAR(500),
    video_bos_key      VARCHAR(500),
    audio_url          VARCHAR(500),
    audio_bos_key      VARCHAR(500),
    dialogue_text      TEXT,
    sfx_hints          VARCHAR(500),
    tts_url            VARCHAR(500),
    bgm_url            VARCHAR(500),
    duration           INTEGER,
    review_status      VARCHAR(16)  DEFAULT 'pending',
    reviewer_note      TEXT,
    deleted            INTEGER      NOT NULL DEFAULT 0,
    create_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sv_shot_list_id ON sv_shot (shot_list_id);
CREATE INDEX IF NOT EXISTS idx_sv_shot_number ON sv_shot (shot_number);

-- 8. sv_material — 素材库表
CREATE TABLE IF NOT EXISTS sv_material (
    id                     BIGSERIAL    PRIMARY KEY,
    owner_id               BIGINT       NOT NULL,
    material_type          VARCHAR(50)  NOT NULL,
    url                    VARCHAR(500) NOT NULL,
    bos_key                VARCHAR(500),
    shot_id                BIGINT,
    project_id             BIGINT,
    file_size              BIGINT,
    duration               INTEGER,
    width                  INTEGER,
    height                 INTEGER,
    format_type            VARCHAR(50),
    generation_type        VARCHAR(50),
    ai_prompt              TEXT,
    ai_model               VARCHAR(100),
    post_processing_config TEXT,
    ai_provider            VARCHAR(50),
    deleted                INTEGER      NOT NULL DEFAULT 0,
    create_time            TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sv_material_owner ON sv_material (owner_id);
CREATE INDEX IF NOT EXISTS idx_sv_material_shot ON sv_material (shot_id);
CREATE INDEX IF NOT EXISTS idx_sv_material_project ON sv_material (project_id);

-- 9. sys_config_version_history — 配置变更历史
CREATE TABLE IF NOT EXISTS sys_config_version_history (
    id          BIGSERIAL    PRIMARY KEY,
    config_id   BIGINT       NOT NULL,
    config_key  VARCHAR(128) NOT NULL,
    old_value   TEXT,
    new_value   TEXT,
    operator_id BIGINT,
    create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_config_version_config_id ON sys_config_version_history(config_id);
CREATE INDEX IF NOT EXISTS idx_config_version_create_time ON sys_config_version_history(create_time DESC);
