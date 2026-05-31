-- V151.1: agent_message logical delete flag
-- Ownership: intelligence/agent. Messages are filtered by deleted=0 in AgentMessageRepository.

ALTER TABLE agent_message ADD COLUMN IF NOT EXISTS deleted INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_agent_message_deleted ON agent_message(deleted) WHERE deleted = 0;
