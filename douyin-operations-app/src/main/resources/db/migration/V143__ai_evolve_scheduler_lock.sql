-- 进化引擎调度器分布式锁（Redis 不可用时由 EvolveScheduler 使用 JdbcTemplate 抢占）
-- 见 sql/_archive/legacy-manual-migrations/upgrade-analysis-2026.sql 2b 节

CREATE TABLE IF NOT EXISTS ai_evolve_scheduler_lock (
    lock_key   VARCHAR(64) PRIMARY KEY,
    lock_value VARCHAR(128) NOT NULL,
    expire_at  TIMESTAMP NOT NULL
);

COMMENT ON TABLE ai_evolve_scheduler_lock IS '进化引擎调度器分布式锁（Redis 不可用时 DB 备用）';
COMMENT ON COLUMN ai_evolve_scheduler_lock.expire_at IS '锁过期时间，超时后可被其他实例抢占';
