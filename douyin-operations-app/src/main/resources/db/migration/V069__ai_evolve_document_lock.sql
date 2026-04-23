-- 进化引擎文档级锁表，防止并发进化同一文档
CREATE TABLE IF NOT EXISTS ai_evolve_document_lock (
    doc_id       BIGINT       NOT NULL PRIMARY KEY,
    locked_by    VARCHAR(128) NOT NULL,
    locked_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    expires_at   TIMESTAMP    NOT NULL,
    owner_id     BIGINT,
    deleted      INTEGER      NOT NULL DEFAULT 0,
    create_time  TIMESTAMP    DEFAULT NOW(),
    update_time  TIMESTAMP    DEFAULT NOW()
);

COMMENT ON TABLE ai_evolve_document_lock IS '进化引擎文档级锁（DB 回退方案）';
COMMENT ON COLUMN ai_evolve_document_lock.doc_id IS '被锁定的文档 ID';
COMMENT ON COLUMN ai_evolve_document_lock.locked_by IS '锁持有者标识（hostname:thread）';
COMMENT ON COLUMN ai_evolve_document_lock.expires_at IS '锁过期时间';

CREATE INDEX IF NOT EXISTS idx_ai_evolve_doc_lock_expires ON ai_evolve_document_lock(expires_at);
