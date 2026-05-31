-- V195: 直播话术效果扩展字段
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS execution_duration_sec INT DEFAULT 0;
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS viewer_peak INT DEFAULT 0;
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS conversion_count INT DEFAULT 0;
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS click_count INT DEFAULT 0;

COMMENT ON COLUMN live_script.execution_duration_sec IS '实际执行时长(秒)';
COMMENT ON COLUMN live_script.viewer_peak IS '执行期间最高观看人数';
COMMENT ON COLUMN live_script.conversion_count IS '转化数';
