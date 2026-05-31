-- V212: photo_avatar_task 增强 — 添加错误信息与进度字段
ALTER TABLE photo_avatar_task ADD COLUMN IF NOT EXISTS error_message VARCHAR(512);
ALTER TABLE photo_avatar_task ADD COLUMN IF NOT EXISTS progress INT DEFAULT 0;
