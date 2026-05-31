-- V214: Add description, genre, visibility columns to drama_project
ALTER TABLE drama_project ADD COLUMN IF NOT EXISTS description VARCHAR(512);
ALTER TABLE drama_project ADD COLUMN IF NOT EXISTS genre VARCHAR(64) DEFAULT 'other';
ALTER TABLE drama_project ADD COLUMN IF NOT EXISTS visibility VARCHAR(16) DEFAULT 'private';
