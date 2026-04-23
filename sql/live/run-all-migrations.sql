-- ============================================================
-- live 模块 - 一键执行所有迁移脚本
-- 执行：psql -U postgres -d douyin_operations -f sql/live/run-all-migrations.sql
-- 说明：按顺序执行，已存在的对象会跳过（IF NOT EXISTS / ADD COLUMN IF NOT EXISTS）
-- ============================================================

\echo '=== live 模块迁移开始 ==='

-- 1. migration-persona
\echo '--- 1. migration-persona ---'
\ir migration-persona.sql

-- 2. migration-fields
\echo '--- 2. migration-fields ---'
\ir migration-fields.sql

-- 3. migration-data-sync（live_session_data, live_product_data）
\echo '--- 3. migration-data-sync ---'
\ir migration-data-sync.sql

-- 4. migration-ai-review（ai_review_id 关联 AI 复盘报告）
\echo '--- 4. migration-ai-review ---'
\ir migration-ai-review.sql

-- 5. migration-ai-analysis
\echo '--- 5. migration-ai-analysis ---'
\ir migration-ai-analysis.sql

-- 6. migration-script-template
\echo '--- 6. migration-script-template ---'
\ir migration-script-template.sql

-- 7. migration-monitor-fields
\echo '--- 7. migration-monitor-fields ---'
\ir migration-monitor-fields.sql

-- 8. migration-monitor-archive（归档表，供 LiveMonitorArchiveScheduler 使用）
\echo '--- 8. migration-monitor-archive ---'
\ir migration-monitor-archive.sql

-- 9. migration-scheduled-end（计划结束时间）
\echo '--- 9. migration-scheduled-end ---'
\ir migration-scheduled-end.sql

-- 10. migration-product-type（产品类型：利润品/亏品/平价品/爆品）
\echo '--- 10. migration-product-type ---'
\ir migration-product-type.sql

-- 11. migration-script-refine（话术精细化：时长限制、需求）
\echo '--- 11. migration-script-refine ---'
\ir migration-script-refine.sql

-- 12. migration-script-snapshot（产品话术引用快照）
\echo '--- 12. migration-script-snapshot ---'
\ir migration-script-snapshot.sql

\echo '=== live 模块迁移完成 ==='
