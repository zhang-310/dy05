-- 弹幕历史记录表：持久化直播弹幕用于分析
CREATE TABLE IF NOT EXISTS live_danmaku_record (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    sentiment VARCHAR(20),
    author_nickname VARCHAR(100),
    danmaku_time TIMESTAMP,
    douyin_comment_id VARCHAR(100),
    user_id BIGINT,
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_live_danmaku_douyin_cid ON live_danmaku_record(douyin_comment_id) WHERE deleted = 0 AND douyin_comment_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_live_danmaku_session ON live_danmaku_record(session_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_danmaku_time ON live_danmaku_record(danmaku_time) WHERE deleted = 0;
