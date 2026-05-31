-- V213: Add error_message and progress columns to digital_human_task
ALTER TABLE digital_human_task ADD COLUMN IF NOT EXISTS error_message VARCHAR(512);
ALTER TABLE digital_human_task ADD COLUMN IF NOT EXISTS progress INT DEFAULT 0;
