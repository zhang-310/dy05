-- ============================================================
-- 数据库迁移脚本 - 统一执行入口
-- ============================================================
-- 本脚本按版本顺序执行所有迁移
-- 适用于从 v1.0.0 升级到最新版本
-- ============================================================

-- ============================================================
-- 执行前检查
-- ============================================================
DO $$
BEGIN
    RAISE NOTICE '开始执行数据库迁移...';
    RAISE NOTICE '当前时间: %', NOW();
END $$;

-- ============================================================
-- v1.1.0 - 认证模块升级
-- ============================================================
\echo '执行 v1.1.0 迁移: 认证模块升级'

-- 检查是否需要执行
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'sys_role' AND column_name = 'role_level'
    ) THEN
        RAISE NOTICE '需要执行认证模块升级';
    ELSE
        RAISE NOTICE '认证模块已是最新版本，跳过';
    END IF;
END $$;

\i sql/auth/migration-v2-to-v3.sql
\i sql/auth/migration-role-v3.sql

-- ============================================================
-- v1.2.0 - 直播模块增强
-- ============================================================
\echo '执行 v1.2.0 迁移: 直播模块增强'

-- 检查是否需要执行
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'live_session' AND column_name = 'persona_id'
    ) THEN
        RAISE NOTICE '需要执行直播模块升级';
    ELSE
        RAISE NOTICE '直播模块已是最新版本，跳过';
    END IF;
END $$;

\i sql/live/migration-persona.sql
\i sql/live/migration-data-sync.sql

-- ============================================================
-- v1.3.0 - 话术模板升级
-- ============================================================
\echo '执行 v1.3.0 迁移: 话术模板升级'

\i sql/script/migration-template-uservw.sql

-- ============================================================
-- v1.4.0 - 短视频话术支持
-- ============================================================
\echo '执行 v1.4.0 迁移: 短视频话术支持'

\i sql/shortvideo/migration-script-template.sql

-- ============================================================
-- v1.5.0 - 产品话术管理
-- ============================================================
\echo '执行 v1.5.0 迁移: 产品话术管理'

-- 检查是否需要执行
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_name = 'dy_product_script'
    ) THEN
        RAISE NOTICE '需要创建产品话术表';
        -- 执行创建
    ELSE
        RAISE NOTICE '产品话术表已存在，跳过';
    END IF;
END $$;

\i sql/product/product-script-schema.sql

-- ============================================================
-- v1.6.0 - 粉丝画像同步
-- ============================================================
\echo '执行 v1.6.0 迁移: 粉丝画像同步'

-- 检查是否需要执行
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_name = 'dy_fan_profile'
    ) THEN
        RAISE NOTICE '需要创建粉丝画像表';
    ELSE
        RAISE NOTICE '粉丝画像表已存在，跳过';
    END IF;
END $$;

\i sql/douyin/fan-profile-schema.sql

-- ============================================================
-- v1.7.0 - OAuth 令牌管理
-- ============================================================
\echo '执行 v1.7.0 迁移: OAuth 令牌管理'

-- 检查是否需要执行
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_name = 'dy_oauth_token'
    ) THEN
        RAISE NOTICE '需要创建 OAuth 令牌表';
    ELSE
        RAISE NOTICE 'OAuth 令牌表已存在，跳过';
    END IF;
END $$;

\i sql/douyinapi/oauth-token-schema.sql

-- ============================================================
-- v1.8.0 - AI 调用日志效果归因
-- ============================================================
\echo '执行 v1.8.0 迁移: ai_call_log 效果归因字段'

-- 幂等：ADD COLUMN IF NOT EXISTS
\i sql/ai/call-log-migration-attribution.sql

-- v1.8.1 - 知识权重字段
\i sql/ai/kb-document-boost-migration.sql

-- v1.8.2 - 时效性检测 Agent 字段
\i sql/ai/kb-document-freshness-migration.sql

-- v1.8.3 - 知识来源类型（知识引用率统计）
\i sql/ai/kb-document-source-type-migration.sql

-- v1.8.4 - ai_call_log 全链路耗时（性能分析）
\i sql/ai/call-log-migration-stage-timings.sql

-- ============================================================
-- v3.2.0 - 短视频设计对齐（sv_drama_character/sv_shot/sv_generation_log）
-- ============================================================
\echo '执行 v3.2.0 迁移: 短视频设计对齐'

\i sql/shortvideo/migration-v32-design.sql

-- ============================================================
-- v3.3.0 - 短视频资源路径统一 + 爆款收藏表
-- ============================================================
\echo '执行 v3.3.0 迁移: 短视频 API 路径统一、爆款收藏'

\i sql/shortvideo/migration-resource-short-video.sql
\i sql/shortvideo/migration-viral-favorite.sql

-- ============================================================
-- 性能优化
-- ============================================================
\echo '执行性能优化: 创建索引'

\i sql/performance/indexes-optimization.sql

-- ============================================================
-- 分析报告驱动升级（docs/analysis 01～06 模块深度分析）
-- ============================================================
\echo '执行分析报告升级: 索引、live 逻辑删除、shortvideo/log 等'

\i sql/_archive/legacy-manual-migrations/upgrade-analysis-2026.sql

-- ============================================================
-- 迁移完成
-- ============================================================
DO $$
BEGIN
    RAISE NOTICE '数据库迁移完成！';
    RAISE NOTICE '完成时间: %', NOW();
END $$;

-- 验证关键表
SELECT 'Verification: Key tables' AS status;
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN (
    'sys_user', 'dy_account', 'live_session',
    'dy_product_script', 'dy_fan_profile', 'dy_oauth_token'
  )
ORDER BY table_name;

SELECT 'All migrations completed successfully!' AS status;
