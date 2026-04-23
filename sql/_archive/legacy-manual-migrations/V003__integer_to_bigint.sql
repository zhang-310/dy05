-- V003: 高增长字段从 INTEGER 升级为 BIGINT

-- sv_video_data: 视频数据可能超过 INTEGER 上限
ALTER TABLE sv_video_data ALTER COLUMN views TYPE BIGINT;
ALTER TABLE sv_video_data ALTER COLUMN comments TYPE BIGINT;
ALTER TABLE sv_video_data ALTER COLUMN shares TYPE BIGINT;

-- live_session: 直播数据
ALTER TABLE live_session ALTER COLUMN peak_viewers TYPE BIGINT;
