-- 为 dy_fan_profile 和 dy_fan_profile_stats 添加 owner_id 数据隔离列
-- 执行前请确认已备份数据

-- 1. 添加 owner_id 列
ALTER TABLE dy_fan_profile ADD COLUMN IF NOT EXISTS owner_id BIGINT;
ALTER TABLE dy_fan_profile_stats ADD COLUMN IF NOT EXISTS owner_id BIGINT;

-- 2. 回填 owner_id（从 douyin_account.user_id 关联）
UPDATE dy_fan_profile fp
SET owner_id = da.user_id
FROM douyin_account da
WHERE fp.account_id = da.id AND fp.owner_id IS NULL;

UPDATE dy_fan_profile_stats fps
SET owner_id = da.user_id
FROM douyin_account da
WHERE fps.account_id = da.id AND fps.owner_id IS NULL;

-- 3. 设置 NOT NULL（回填完成后执行）
-- ALTER TABLE dy_fan_profile ALTER COLUMN owner_id SET NOT NULL;
-- ALTER TABLE dy_fan_profile_stats ALTER COLUMN owner_id SET NOT NULL;

-- 4. 添加索引
CREATE INDEX IF NOT EXISTS idx_fan_profile_owner_id ON dy_fan_profile(owner_id);
CREATE INDEX IF NOT EXISTS idx_fan_stats_owner_id ON dy_fan_profile_stats(owner_id);
CREATE INDEX IF NOT EXISTS idx_fan_stats_account_type_deleted ON dy_fan_profile_stats(account_id, stat_type, deleted);

COMMENT ON COLUMN dy_fan_profile.owner_id IS '数据所有者用户ID';
COMMENT ON COLUMN dy_fan_profile_stats.owner_id IS '数据所有者用户ID';
