-- V155: agent 表添加 conversation_count 字段（使用次数统计，用于市场排序）
-- 支持按热度排序智能体

ALTER TABLE agent ADD COLUMN IF NOT EXISTS conversation_count INTEGER NOT NULL DEFAULT 0;
