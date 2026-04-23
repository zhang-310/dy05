-- ============================================================
-- 抖音运营平台 - Docker 环境数据库初始化
-- 用法: 在项目根目录执行
--   docker run --rm -v ${PWD}:/workspace -w /workspace --network docker_dy-net
--     -e PGPASSWORD=postgresql postgres:15-alpine
--     psql -h dy-postgres -U postgres -d douyin_operations -f sql/init-docker.sql
-- ============================================================

\encoding UTF8
SET client_encoding = 'UTF8';

-- 1. auth
\i sql/auth/schema.sql
\i sql/auth/resource-data.sql

-- 2. log
\i sql/log/schema.sql

-- 3. config
\i sql/config/schema.sql

-- 4. storage
\i sql/storage/schema.sql
\i sql/storage/resource-data.sql

-- 5. douyin
\i sql/douyin/schema.sql
\i sql/douyin/fan-profile-schema.sql
\i sql/douyin/resource-data.sql

-- 5.1 douyinapi
\i sql/douyinapi/oauth-token-schema.sql

-- 6. copy
\i sql/copy/schema.sql
\i sql/copy/resource-data.sql

-- 7. script
\i sql/script/schema.sql
\i sql/script/resource-data.sql

-- 8. shortvideo
\i sql/shortvideo/schema.sql
\i sql/shortvideo/resource-data.sql

-- 9. live
\i sql/live/schema.sql
\i sql/live/resource-data.sql

-- 10. product
\i sql/product/schema.sql
\i sql/product/product-script-schema.sql
\i sql/product/resource-data.sql

-- 11. abtest
\i sql/abtest/schema.sql
\i sql/abtest/resource-data.sql

-- 12. agent
\i sql/agent/schema.sql
\i sql/agent/resource-data.sql

-- 13. ai
\i sql/ai/schema.sql
\i sql/ai/evolution-schema.sql
\i sql/ai/evolve-schema.sql
\i sql/ai/kb-schema.sql
\i sql/ai/media-schema.sql
\i sql/ai/call-log-schema.sql
\i sql/ai/resource-data.sql

-- 14. wecom
\i sql/wecom/schema.sql
\i sql/wecom/resource-data.sql

-- 15. system
\i sql/system/schema.sql

-- 16. attribution
\i sql/attribution/schema.sql

-- 17. performance
\i sql/performance/indexes-optimization.sql

SELECT 'Database initialization completed successfully!' AS status;

-- 13. wecom
\i sql/wecom/schema.sql
\i sql/wecom/resource-data.sql

SELECT 'Database initialization completed successfully!' AS status;
