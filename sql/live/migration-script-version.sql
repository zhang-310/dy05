CREATE TABLE IF NOT EXISTS live_script_version (
    id                      BIGSERIAL       PRIMARY KEY,
    script_id               BIGINT          NOT NULL,
    session_id              BIGINT          NOT NULL,
    version_no              INTEGER         NOT NULL,
    version_label           VARCHAR(64),
    script_content          TEXT            NOT NULL,
    script_type             VARCHAR(32),
    remark                  TEXT,
    version_status          VARCHAR(16)     DEFAULT 'draft',
    effectiveness_score     NUMERIC(5, 2),
    liked_count             INTEGER         DEFAULT 0,
    usage_count             INTEGER         DEFAULT 0,
    last_used_time          TIMESTAMP,
    owner_id                BIGINT          NOT NULL,
    is_recommended          INTEGER         DEFAULT 0,
    recommend_reason        TEXT,
    recommend_score         NUMERIC(5, 2),
    based_on_version_id     BIGINT,
    change_summary          JSONB,
    deleted                 INTEGER         NOT NULL DEFAULT 0,
    create_time             TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_time             TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_live_script_version_script_id ON live_script_version(script_id);
CREATE INDEX IF NOT EXISTS idx_live_script_version_session_id ON live_script_version(session_id);
CREATE INDEX IF NOT EXISTS idx_live_script_version_owner_id ON live_script_version(owner_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_live_script_version_script_vno ON live_script_version(script_id, version_no) WHERE deleted = 0;

COMMENT ON TABLE live_script_version IS '直播话术版本表 - 用于版本管理、对比、推荐等功能';
COMMENT ON COLUMN live_script_version.script_id IS '原始话术ID（live_script.id）';
COMMENT ON COLUMN live_script_version.session_id IS '直播场次ID（冗余字段，用于快速查询）';
COMMENT ON COLUMN live_script_version.version_no IS '版本号（从1开始）';
COMMENT ON COLUMN live_script_version.version_label IS '版本标签（如 v1.0/优化版/最终版）';
COMMENT ON COLUMN live_script_version.script_content IS '话术内容';
COMMENT ON COLUMN live_script_version.script_type IS '话术类型（opening/product/transition/closing/interaction/promotion）';
COMMENT ON COLUMN live_script_version.version_status IS '版本状态（draft=草稿 / active=活跃 / archived=已归档）';
COMMENT ON COLUMN live_script_version.owner_id IS '创建人ID（用于数据隔离）';
COMMENT ON COLUMN live_script_version.is_recommended IS '是否为推荐版本（0=否 1=是）';
COMMENT ON COLUMN live_script_version.deleted IS '逻辑删除：0=正常 1=已删除';
