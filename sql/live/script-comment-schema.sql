-- Q2-4: 脚本行内评论
-- Script Inline Comments

CREATE TABLE IF NOT EXISTS live_script_comment (
    id              BIGSERIAL PRIMARY KEY,
    script_id       BIGINT NOT NULL,
    session_id      BIGINT,
    content         TEXT NOT NULL,
    author_id       BIGINT,
    author_name     VARCHAR(100),
    resolved        BOOLEAN DEFAULT false,
    resolved_by     BIGINT,
    resolved_at     TIMESTAMP,
    parent_id       BIGINT,
    owner_id        BIGINT,
    deleted         INTEGER DEFAULT 0,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_live_script_comment_script
    ON live_script_comment (script_id) WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_live_script_comment_session
    ON live_script_comment (session_id) WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_live_script_comment_parent
    ON live_script_comment (parent_id) WHERE deleted = 0;

COMMENT ON TABLE live_script_comment IS '话术行内评论';
COMMENT ON COLUMN live_script_comment.script_id IS '关联话术 ID';
COMMENT ON COLUMN live_script_comment.session_id IS '关联直播场次 ID';
COMMENT ON COLUMN live_script_comment.content IS '评论内容';
COMMENT ON COLUMN live_script_comment.author_id IS '评论作者用户 ID';
COMMENT ON COLUMN live_script_comment.author_name IS '评论作者名称';
COMMENT ON COLUMN live_script_comment.resolved IS '是否已解决';
COMMENT ON COLUMN live_script_comment.resolved_by IS '解决人用户 ID';
COMMENT ON COLUMN live_script_comment.resolved_at IS '解决时间';
COMMENT ON COLUMN live_script_comment.parent_id IS '父评论 ID（用于评论回复线程）';
COMMENT ON COLUMN live_script_comment.owner_id IS '所属用户 ID（数据隔离）';
