-- ============================================================
-- 数据库迁移脚本执行指南
-- ============================================================
-- 本文件提供数据库迁移的快速参考
-- 详细文档请参考: sql/README.md（Flyway SSOT：src/main/resources/db/migration）
-- ============================================================

-- ============================================================
-- 快速执行
-- ============================================================

-- 方式 1: 历史手工汇总（不推荐新环境；已归档）
-- \i sql/_archive/legacy-manual-migrations/migrate-all.sql

-- 方式 2: 按需执行单个迁移
-- \i sql/auth/migration-v2-to-v3.sql
-- \i sql/auth/migration-role-v3.sql
-- \i sql/live/migration-persona.sql
-- \i sql/live/migration-data-sync.sql
-- \i sql/script/migration-template-uservw.sql
-- \i sql/shortvideo/migration-script-template.sql
-- \i sql/product/product-script-schema.sql
-- \i sql/douyin/fan-profile-schema.sql
-- \i sql/douyinapi/oauth-token-schema.sql
-- \i sql/ai/call-log-migration-attribution.sql
-- \i sql/performance/indexes-optimization.sql

-- ============================================================
-- 迁移脚本说明
-- ============================================================

-- 统一迁移入口（已归档，仅考古）
-- 文件: sql/_archive/legacy-manual-migrations/migrate-all.sql

-- 回滚脚本（已归档）
-- 文件: sql/_archive/legacy-manual-migrations/rollback-v*.sql
-- 执行示例: \i sql/_archive/legacy-manual-migrations/rollback-v1.7.0.sql

-- ============================================================
-- 版本检查
-- ============================================================

-- 检查当前版本
SELECT 'Version Check' AS status;

-- 检查 v1.2.0 (直播模块)
SELECT
    CASE
        WHEN EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'live_session' AND column_name = 'persona_id'
        ) THEN 'v1.2.0+'
        ELSE 'v1.1.0 or earlier'
    END AS version;

-- 检查 v1.5.0 (产品话术)
SELECT
    CASE
        WHEN EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_name = 'dy_product_script'
        ) THEN 'v1.5.0+'
        ELSE 'v1.4.0 or earlier'
    END AS version;

-- 检查 v1.6.0 (粉丝画像)
SELECT
    CASE
        WHEN EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_name = 'dy_fan_profile'
        ) THEN 'v1.6.0+'
        ELSE 'v1.5.0 or earlier'
    END AS version;

-- 检查 v1.7.0 (OAuth 令牌)
SELECT
    CASE
        WHEN EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_name = 'dy_oauth_token'
        ) THEN 'v1.7.0+'
        ELSE 'v1.6.0 or earlier'
    END AS version;

-- 检查 v1.8.0 (AI 调用日志效果归因)
SELECT
    CASE
        WHEN EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'ai_call_log' AND column_name = 'content_effect'
        ) THEN 'v1.8.0+'
        ELSE 'v1.7.0 or earlier'
    END AS version;

-- ============================================================
-- 详细文档
-- ============================================================
-- 完整的迁移指南、版本历史、回滚策略等请参考:
-- sql/MIGRATION.md
-- ============================================================
