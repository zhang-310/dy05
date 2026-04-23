-- ============================================================
-- OAuth Token 存储表
-- ============================================================

CREATE TABLE IF NOT EXISTS oauth_token (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(32) NOT NULL,
    open_id VARCHAR(128) NOT NULL,
    access_token VARCHAR(512) NOT NULL,
    refresh_token VARCHAR(512),
    expires_at TIMESTAMP NOT NULL,
    scope VARCHAR(256),
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_oauth_token_user_provider ON oauth_token(user_id, provider, deleted);
CREATE INDEX idx_oauth_token_openid_provider ON oauth_token(open_id, provider, deleted);
CREATE INDEX idx_oauth_token_expires ON oauth_token(expires_at) WHERE deleted = 0;

-- 注释
COMMENT ON TABLE oauth_token IS 'OAuth Token 存储表';
COMMENT ON COLUMN oauth_token.provider IS 'OAuth 提供商: douyin / wechat / qq';
COMMENT ON COLUMN oauth_token.open_id IS '第三方平台用户唯一标识';
COMMENT ON COLUMN oauth_token.expires_at IS 'Token 过期时间';
COMMENT ON COLUMN oauth_token.scope IS '授权范围';
