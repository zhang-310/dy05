-- V166: Messaging message history table
-- Brings sql/messaging/schema.sql table into Flyway for older Docker databases.

CREATE TABLE IF NOT EXISTS msg_message_history (
    id BIGSERIAL PRIMARY KEY,
    config_id BIGINT NOT NULL,
    platform VARCHAR(32) NOT NULL,
    message_id VARCHAR(128),
    sender_id VARCHAR(128),
    receiver_id VARCHAR(128),
    message_type VARCHAR(32),
    content TEXT,
    raw_payload TEXT,
    direction VARCHAR(16) NOT NULL,
    agent_id BIGINT,
    agent_response TEXT,
    status INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_msg_history_platform CHECK (platform IN ('wecom', 'feishu')),
    CONSTRAINT chk_msg_history_direction CHECK (direction IN ('inbound', 'outbound')),
    CONSTRAINT chk_msg_history_status CHECK (status IN (0, 1, 2))
);

CREATE INDEX IF NOT EXISTS idx_msg_history_config
    ON msg_message_history(config_id, deleted);

CREATE INDEX IF NOT EXISTS idx_msg_history_sender
    ON msg_message_history(sender_id, create_time DESC) WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_msg_history_agent
    ON msg_message_history(agent_id, create_time DESC) WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_msg_history_status
    ON msg_message_history(status, create_time) WHERE deleted = 0;
