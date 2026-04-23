-- V040: live_session 增加 planned_end_time、platform 列（与 Entity 对齐）
ALTER TABLE live_session ADD COLUMN IF NOT EXISTS planned_end_time TIMESTAMP;
ALTER TABLE live_session ADD COLUMN IF NOT EXISTS platform VARCHAR(32);
