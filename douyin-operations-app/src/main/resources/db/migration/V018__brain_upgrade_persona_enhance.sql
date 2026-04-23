-- V018: 最强大脑升级 - 人设增强字段
-- 为 dy_persona 表添加 IP 类型、内容比例、直播风格等字段

ALTER TABLE dy_persona ADD COLUMN IF NOT EXISTS ip_type VARCHAR(32);
ALTER TABLE dy_persona ADD COLUMN IF NOT EXISTS content_ratio VARCHAR(512);
ALTER TABLE dy_persona ADD COLUMN IF NOT EXISTS live_style VARCHAR(32);
ALTER TABLE dy_persona ADD COLUMN IF NOT EXISTS video_content_ratio VARCHAR(512);
ALTER TABLE dy_persona ADD COLUMN IF NOT EXISTS age_range VARCHAR(16);
ALTER TABLE dy_persona ADD COLUMN IF NOT EXISTS positioning_tags VARCHAR(256);

COMMENT ON COLUMN dy_persona.ip_type IS 'IP类型: phenomenal(现象级) / top(顶级)';
COMMENT ON COLUMN dy_persona.content_ratio IS '内容比例配置JSON, 如 {"emotional":40,"humor":30,"chicken_soup":20,"interaction":10}';
COMMENT ON COLUMN dy_persona.live_style IS '直播风格: high_energy / deep_value / emotional';
COMMENT ON COLUMN dy_persona.video_content_ratio IS '短视频内容比例JSON, 如 {"viral_clone":40,"daily":30,"soft_ad":30}';
COMMENT ON COLUMN dy_persona.age_range IS '主播年龄段, 如 20-28 / 30-45';
COMMENT ON COLUMN dy_persona.positioning_tags IS '定位标签, 逗号分隔, 如 恋爱军师,情感闺蜜';
