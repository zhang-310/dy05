-- 粉丝画像表
CREATE TABLE IF NOT EXISTS dy_fan_profile (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    age_range VARCHAR(32),
    gender VARCHAR(16),
    province VARCHAR(64),
    city VARCHAR(64),
    interest_tags TEXT,
    active_time VARCHAR(32),
    device_type VARCHAR(32),
    fan_count BIGINT DEFAULT 0,
    sync_time TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fan_profile_account_id ON dy_fan_profile(account_id);
CREATE INDEX idx_fan_profile_deleted ON dy_fan_profile(deleted);
CREATE INDEX idx_fan_profile_sync_time ON dy_fan_profile(sync_time);

COMMENT ON TABLE dy_fan_profile IS '粉丝画像表';
COMMENT ON COLUMN dy_fan_profile.account_id IS '抖音账号ID';
COMMENT ON COLUMN dy_fan_profile.age_range IS '年龄段：18-24/25-30/31-40/41-50/50+';
COMMENT ON COLUMN dy_fan_profile.gender IS '性别：male/female/unknown';
COMMENT ON COLUMN dy_fan_profile.province IS '省份';
COMMENT ON COLUMN dy_fan_profile.city IS '城市';
COMMENT ON COLUMN dy_fan_profile.interest_tags IS '兴趣标签（JSON数组）';
COMMENT ON COLUMN dy_fan_profile.active_time IS '活跃时段：morning/afternoon/evening/night';
COMMENT ON COLUMN dy_fan_profile.device_type IS '设备类型：ios/android';
COMMENT ON COLUMN dy_fan_profile.fan_count IS '粉丝数量';
COMMENT ON COLUMN dy_fan_profile.sync_time IS '同步时间';
COMMENT ON COLUMN dy_fan_profile.deleted IS '软删除标记';

-- 粉丝画像统计表（聚合数据）
CREATE TABLE IF NOT EXISTS dy_fan_profile_stats (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    stat_type VARCHAR(32) NOT NULL,
    stat_key VARCHAR(64) NOT NULL,
    stat_value VARCHAR(128),
    count BIGINT DEFAULT 0,
    percentage DECIMAL(5,2),
    sync_time TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fan_stats_account_id ON dy_fan_profile_stats(account_id);
CREATE INDEX idx_fan_stats_type ON dy_fan_profile_stats(stat_type);
CREATE INDEX idx_fan_stats_deleted ON dy_fan_profile_stats(deleted);

COMMENT ON TABLE dy_fan_profile_stats IS '粉丝画像统计表';
COMMENT ON COLUMN dy_fan_profile_stats.account_id IS '抖音账号ID';
COMMENT ON COLUMN dy_fan_profile_stats.stat_type IS '统计类型：age/gender/province/city/interest/active_time/device';
COMMENT ON COLUMN dy_fan_profile_stats.stat_key IS '统计键';
COMMENT ON COLUMN dy_fan_profile_stats.stat_value IS '统计值';
COMMENT ON COLUMN dy_fan_profile_stats.count IS '数量';
COMMENT ON COLUMN dy_fan_profile_stats.percentage IS '占比（%）';
COMMENT ON COLUMN dy_fan_profile_stats.sync_time IS '同步时间';
