-- Migration: 添加直播话术关键索引
-- Sprint 3: 优化查询性能

-- 场次+序号联合索引（话术列表按序查询）
CREATE INDEX IF NOT EXISTS idx_live_script_session_seq
    ON live_script(session_id, sequence_no)
    WHERE deleted = 0;

-- 用户+话术类型索引（用户话术筛选，依赖 migration-user-isolation.sql 添加 user_id 列）
CREATE INDEX IF NOT EXISTS idx_live_script_user_status
    ON live_script(user_id, script_type)
    WHERE deleted = 0;

-- 场次按用户索引（用户场次列表）
CREATE INDEX IF NOT EXISTS idx_live_session_user
    ON live_session(user_id)
    WHERE deleted = 0;

-- 话术按产品索引（产品话术关联查询）
CREATE INDEX IF NOT EXISTS idx_live_script_product
    ON live_script(product_id)
    WHERE deleted = 0 AND product_id IS NOT NULL;
