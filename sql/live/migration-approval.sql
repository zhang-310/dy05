-- 话术审核流：live_script 添加 approval_status + 审核记录表
-- 执行时机：Phase 3.1

-- 1. live_script 添加审核状态列
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS approval_status INTEGER NOT NULL DEFAULT 0;
COMMENT ON COLUMN live_script.approval_status IS '审核状态: 0=草稿, 1=待审核, 2=已通过, 3=已拒绝';

CREATE INDEX IF NOT EXISTS idx_live_script_approval ON live_script(approval_status) WHERE deleted = 0;

-- 2. 审核记录表
CREATE TABLE IF NOT EXISTS live_script_approval (
    id              BIGSERIAL       PRIMARY KEY,
    script_id       BIGINT          NOT NULL,
    session_id      BIGINT          NOT NULL,
    submitter_id    BIGINT          NOT NULL,
    reviewer_id     BIGINT,
    action          VARCHAR(16)     NOT NULL DEFAULT 'submit',
    status          INTEGER         NOT NULL DEFAULT 1,
    comments        TEXT,
    review_time     TIMESTAMP,
    deleted         INTEGER         NOT NULL DEFAULT 0,
    create_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE live_script_approval IS '话术审核记录';
COMMENT ON COLUMN live_script_approval.action IS '操作: submit=提交审核, approve=通过, reject=拒绝, revoke=撤回';
COMMENT ON COLUMN live_script_approval.status IS '审核状态: 1=待审核, 2=已通过, 3=已拒绝';

CREATE INDEX IF NOT EXISTS idx_lsa_script_id ON live_script_approval(script_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_lsa_session_id ON live_script_approval(session_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_lsa_reviewer ON live_script_approval(reviewer_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_lsa_status ON live_script_approval(status) WHERE deleted = 0;
