-- V037: live_script_comment, live_script_effectiveness, live_script_pipeline, live_script_template, live_script_version, live_slot_type, live_style_preset（与 Entity 对齐）

-- 1. live_script_comment
CREATE TABLE IF NOT EXISTS live_script_comment (
    id          BIGSERIAL PRIMARY KEY,
    script_id   BIGINT    NOT NULL,
    session_id  BIGINT,
    user_id     BIGINT    NOT NULL,
    author_id   BIGINT,
    user_name   VARCHAR(64),
    author_name VARCHAR(100),
    content     TEXT      NOT NULL,
    resolved    INTEGER   NOT NULL DEFAULT 0,
    resolved_by BIGINT,
    resolved_at TIMESTAMP,
    parent_id   BIGINT,
    owner_id    BIGINT,
    deleted     INTEGER   NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_live_script_comment_script ON live_script_comment(script_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_script_comment_session ON live_script_comment(session_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_script_comment_parent ON live_script_comment(parent_id) WHERE deleted = 0;

-- 2. live_script_effectiveness
CREATE TABLE IF NOT EXISTS live_script_effectiveness (
    id BIGSERIAL PRIMARY KEY,
    script_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    version VARCHAR(16),
    conversion_rate NUMERIC(5,2) DEFAULT 0,
    likes BIGINT DEFAULT 0,
    comments INTEGER DEFAULT 0,
    completion_rate NUMERIC(5,2) DEFAULT 0,
    total_score NUMERIC(4,2) DEFAULT 0,
    score_formula VARCHAR(256),
    ranking INTEGER DEFAULT 1,
    ranking_trend INTEGER DEFAULT 0,
    tag VARCHAR(32),
    sample_size INTEGER DEFAULT 0,
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_live_script_effectiveness_script ON live_script_effectiveness(script_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_script_effectiveness_session ON live_script_effectiveness(session_id) WHERE deleted = 0;

-- 3. live_script_pipeline
CREATE TABLE IF NOT EXISTS live_script_pipeline (
    id                BIGSERIAL PRIMARY KEY,
    session_id        BIGINT    NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'pending',
    total_scripts     INTEGER   DEFAULT 0,
    completed_scripts INTEGER   DEFAULT 0,
    refined_scripts   INTEGER   DEFAULT 0,
    failed_scripts    INTEGER   DEFAULT 0,
    config_json       TEXT,
    owner_id          BIGINT    NOT NULL,
    deleted           INTEGER   NOT NULL DEFAULT 0,
    create_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_pipeline_session ON live_script_pipeline(session_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_pipeline_owner ON live_script_pipeline(owner_id) WHERE deleted = 0;

-- 4. live_script_template
CREATE TABLE IF NOT EXISTS live_script_template (
    id                      BIGSERIAL PRIMARY KEY,
    template_name           VARCHAR(128) NOT NULL,
    script_type             VARCHAR(32)  NOT NULL,
    category                VARCHAR(64),
    content                 TEXT         NOT NULL,
    variables               VARCHAR(512),
    effectiveness_score     DECIMAL(5,2) DEFAULT 0,
    usage_count             INTEGER      DEFAULT 0,
    avg_conversion_rate     DECIMAL(5,2) DEFAULT 0,
    source_script_id        BIGINT,
    source_session_id       BIGINT,
    industry_tags           VARCHAR(512),
    auto_collected          INTEGER      NOT NULL DEFAULT 0,
    template_content        TEXT,
    owner_id                BIGINT,
    status                  INTEGER      NOT NULL DEFAULT 1,
    deleted                 INTEGER      NOT NULL DEFAULT 0,
    create_time             TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time             TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_lst_script_type ON live_script_template(script_type);
CREATE INDEX IF NOT EXISTS idx_lst_effectiveness ON live_script_template(effectiveness_score DESC) WHERE deleted = 0;

-- 5. live_script_version（与 Entity 对齐，无 FK）
CREATE TABLE IF NOT EXISTS live_script_version (
    id                  BIGSERIAL PRIMARY KEY,
    script_id           BIGINT    NOT NULL,
    session_id          BIGINT    NOT NULL,
    version_no          INTEGER   NOT NULL,
    version_label       VARCHAR(64),
    script_content      TEXT      NOT NULL,
    script_type         VARCHAR(32),
    remark              TEXT,
    version_status      VARCHAR(16) DEFAULT 'draft',
    effectiveness_score DECIMAL(5,2),
    liked_count         INTEGER   DEFAULT 0,
    usage_count         INTEGER   DEFAULT 0,
    last_used_time      TIMESTAMP,
    owner_id            BIGINT    NOT NULL,
    is_recommended      INTEGER   DEFAULT 0,
    recommend_reason     TEXT,
    recommend_score      DECIMAL(5,2),
    based_on_version_id  BIGINT,
    change_summary      JSONB,
    deleted             INTEGER   NOT NULL DEFAULT 0,
    create_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_lsv_script_id ON live_script_version(script_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_lsv_session_id ON live_script_version(session_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_lsv_owner_id ON live_script_version(owner_id) WHERE deleted = 0;

-- 6. live_slot_type
CREATE TABLE IF NOT EXISTS live_slot_type (
    id                  BIGSERIAL PRIMARY KEY,
    slot_code           VARCHAR(32)  NOT NULL,
    slot_label          VARCHAR(64)  NOT NULL,
    default_requirement VARCHAR(128),
    sort_order          INTEGER DEFAULT 0,
    is_enabled          BOOLEAN DEFAULT TRUE,
    created_by          BIGINT,
    deleted             INTEGER NOT NULL DEFAULT 0,
    create_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_live_slot_type_code ON live_slot_type(slot_code) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_slot_type_sort ON live_slot_type(sort_order) WHERE deleted = 0;

-- 7. live_style_preset（无 deleted，Entity 无 @SQLRestriction）
CREATE TABLE IF NOT EXISTS live_style_preset (
    id              BIGSERIAL PRIMARY KEY,
    style_key       VARCHAR(50)  NOT NULL,
    label           VARCHAR(50)  NOT NULL,
    group_name      VARCHAR(30)  NOT NULL,
    prompt_template TEXT         NOT NULL,
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    active          INTEGER      NOT NULL DEFAULT 1,
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_live_style_preset_key ON live_style_preset(style_key);
CREATE INDEX IF NOT EXISTS idx_live_style_preset_active ON live_style_preset(active, sort_order);
