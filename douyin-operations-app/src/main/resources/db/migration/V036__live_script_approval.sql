-- V036: live_script_approval 话术审核记录表（与 Entity 对齐）
CREATE TABLE IF NOT EXISTS live_script_approval (
    id              BIGSERIAL       PRIMARY KEY,
    script_id       BIGINT          NOT NULL,
    session_id      BIGINT          NOT NULL,
    submitter_id    BIGINT          NOT NULL,
    reviewer_id     BIGINT,
    action          VARCHAR(16)    NOT NULL DEFAULT 'submit',
    status          INTEGER         NOT NULL DEFAULT 1,
    comments        TEXT,
    review_time     TIMESTAMP,
    deleted         INTEGER         NOT NULL DEFAULT 0,
    create_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_lsa_script_id ON live_script_approval(script_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_lsa_session_id ON live_script_approval(session_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_lsa_reviewer ON live_script_approval(reviewer_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_lsa_status ON live_script_approval(status) WHERE deleted = 0;
