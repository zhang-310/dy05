-- 补充缺失的性能索引

-- douyin_video: 按账号+发布时间查询优化
CREATE INDEX IF NOT EXISTS idx_douyin_video_account_publish ON douyin_video(account_id, publish_time);

-- dy_fan_profile_stats: 复合索引优化按类型查询
CREATE INDEX IF NOT EXISTS idx_fan_stats_account_type_deleted ON dy_fan_profile_stats(account_id, stat_type, deleted);
