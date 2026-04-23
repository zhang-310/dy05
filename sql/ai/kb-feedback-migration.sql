-- P2 用户反馈表：有用/无用 → 更新 boost_factor
CREATE TABLE IF NOT EXISTS kb_feedback (
    id BIGSERIAL PRIMARY KEY,
    query VARCHAR(512) NOT NULL,
    doc_id VARCHAR(128) NOT NULL,
    rating INTEGER NOT NULL,
    comment VARCHAR(512),
    user_id BIGINT NOT NULL,
    search_mode VARCHAR(16),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_kb_feedback_doc_id ON kb_feedback(doc_id);
CREATE INDEX IF NOT EXISTS idx_kb_feedback_user_id ON kb_feedback(user_id);

COMMENT ON TABLE kb_feedback IS '知识反馈：1=有用 -1=无用 0=不确定，用于更新 boost_factor';
