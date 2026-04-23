-- 话术行内评论表
CREATE TABLE IF NOT EXISTS live_script_comment (
    id              BIGSERIAL PRIMARY KEY,
    script_id       BIGINT NOT NULL,
    session_id      BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    user_name       VARCHAR(64),
    content         TEXT NOT NULL,
    resolved        INTEGER DEFAULT 0,      -- 0=未解决 1=已解决
    resolved_by     BIGINT,
    resolved_at     TIMESTAMP,
    parent_id       BIGINT,                 -- reply thread: null=top-level, else FK to parent comment
    deleted         INTEGER DEFAULT 0,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_live_script_comment_script ON live_script_comment(script_id);
CREATE INDEX IF NOT EXISTS idx_live_script_comment_session ON live_script_comment(session_id);
