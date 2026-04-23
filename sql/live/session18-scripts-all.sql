-- ============================================================
-- Session 18 · 李阳阳过品直播间 · 全场话术 · 一键执行
--
-- 64产品快速过品 + 377/红石榴深度收割
-- 风格：东北大姐 · 歇后语+段子+鸡汤+怼人
-- 直播顺序：position 64→1（倒序）
--
-- 执行方式：
--   psql -h localhost -p 5433 -U postgres -d douyin_operations -f session18-scripts-all.sql
-- ============================================================

BEGIN;

-- 1. 开场话术
\i session18-scripts-opening.sql

-- 2. 亏品快速过品 (position 64→56)
\i session18-scripts-products-part1.sql

-- 3. 中前段过品 (position 55→42)
\i session18-scripts-products-part2.sql

-- 4. 主推品377七件套 (position 41) - 深度2-5分钟
\i session18-scripts-377-main.sql

-- 5. 377之后过品 (position 40→33)
\i session18-scripts-products-part3.sql

-- 6. 主推品红石榴套盒 (position 32) - 深度2-5分钟
\i session18-scripts-redpomegranate-main.sql

-- 7. 红石榴之后过品 (position 31→20)
\i session18-scripts-products-part4.sql

-- 8. 后段收尾过品 (position 19→1)
\i session18-scripts-products-part5.sql
\i session18-scripts-products-part6.sql

-- 9. 所有过渡话术
\i session18-scripts-transitions.sql

-- 10. 收场话术
\i session18-scripts-closing.sql

COMMIT;

-- 验证
SELECT
  COUNT(*) as total_scripts,
  COUNT(*) FILTER (WHERE script_content != '[待填写]') as filled,
  COUNT(*) FILTER (WHERE script_content = '[待填写]') as unfilled
FROM live_script
WHERE session_id = 18 AND deleted = 0;
