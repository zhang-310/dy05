-- M-1/M-2：用户可定义场次槽位结构（JSON）；C-4：直播竞品话术库
CREATE TABLE IF NOT EXISTS live_session_template (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(128) NOT NULL,
    code VARCHAR(64) NOT NULL,
    description VARCHAR(512),
    structure_json TEXT NOT NULL,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_live_session_template_owner_code
    ON live_session_template (owner_id, code) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_session_template_owner ON live_session_template (owner_id) WHERE deleted = 0;

CREATE TABLE IF NOT EXISTS live_competitor_script (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    title VARCHAR(256) NOT NULL,
    competitor_name VARCHAR(128),
    platform VARCHAR(32) DEFAULT 'douyin',
    script_content TEXT NOT NULL,
    source_url VARCHAR(1024),
    tags VARCHAR(256),
    notes VARCHAR(512),
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_live_competitor_script_owner ON live_competitor_script (owner_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_competitor_script_title ON live_competitor_script (owner_id, title) WHERE deleted = 0;

ALTER TABLE live_session ADD COLUMN IF NOT EXISTS template_id BIGINT;
