-- =============================================================================
-- 直播话术「商品存在性」排查脚本
-- 用途：查找 live_script / live_product 中引用已删除或不存在商品的脏数据
-- 执行：psql -U postgres -d douyin_operations -f sql/live/product-orphan-check.sql
-- =============================================================================

\echo '=== 1. live_script 中引用不存在/已删除商品的槽位 ==='
\echo '（product_id 指向 dy_product 中不存在或 deleted=1 的记录）'
SELECT
    s.id AS script_id,
    s.session_id,
    s.script_type,
    s.product_id,
    s.sequence_no,
    s.generation_status
FROM live_script s
WHERE s.deleted = 0
  AND s.script_type = 'product'
  AND s.product_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM dy_product p
    WHERE p.id = s.product_id AND p.deleted = 0
  )
ORDER BY s.session_id, s.sequence_no;

\echo ''
\echo '=== 2. live_product 中引用不存在/已删除商品的关联 ==='
\echo '（product_id 指向 dy_product 中不存在或 deleted=1 的记录）'
SELECT
    lp.id AS live_product_id,
    lp.session_id,
    lp.product_id,
    lp.position,
    lp.script_source
FROM live_product lp
WHERE lp.deleted = 0
  AND lp.product_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM dy_product p
    WHERE p.id = lp.product_id AND p.deleted = 0
  )
ORDER BY lp.session_id, lp.position;

\echo ''
\echo '=== 3. 汇总统计 ==='
SELECT
    (SELECT COUNT(*) FROM live_script s
     WHERE s.deleted = 0 AND s.script_type = 'product' AND s.product_id IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM dy_product p WHERE p.id = s.product_id AND p.deleted = 0)
    ) AS orphan_script_count,
    (SELECT COUNT(*) FROM live_product lp
     WHERE lp.deleted = 0 AND lp.product_id IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM dy_product p WHERE p.id = lp.product_id AND p.deleted = 0)
    ) AS orphan_live_product_count;

-- =============================================================================
-- 可选：修复脚本（谨慎执行，建议先备份）
-- 方案 A：将脏数据的 product_id 置空（保留槽位结构，需用户重新选品）
-- 方案 B：逻辑删除脏数据（根据业务需求选择）
-- =============================================================================
/*
-- 方案 A 示例：将 live_script 中无效 product_id 置空
UPDATE live_script s
SET product_id = NULL, update_time = CURRENT_TIMESTAMP
WHERE s.deleted = 0
  AND s.script_type = 'product'
  AND s.product_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM dy_product p WHERE p.id = s.product_id AND p.deleted = 0);
*/
