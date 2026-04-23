-- ============================================================
-- 迁移：所有模块 API/按钮 的 parent_id 指向对应顶级菜单
-- 问题：资源管理页点击各菜单时右侧为空，因 API/button 的 parent_id=0
-- 解决：按 module 将 api/button 的 parent_id 设为对应菜单的 id
-- 用法：docker cp sql/auth/migration-all-resource-parent.sql dy-postgres:/tmp/
--       docker exec dy-postgres psql -U postgres -d douyin_operations -f /tmp/migration-all-resource-parent.sql
-- ============================================================

-- 1. auth 模块 → /auth 菜单
UPDATE auth_resource r SET parent_id = m.id
FROM (SELECT id FROM auth_resource WHERE resource_code = '/auth'   AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m
WHERE r.module = 'auth' AND r.resource_type IN ('api','button') AND r.deleted = 0 AND (r.parent_id IS NULL OR r.parent_id = 0);

-- 2. log 模块 → /log 菜单
UPDATE auth_resource r SET parent_id = m.id
FROM (SELECT id FROM auth_resource WHERE resource_code = '/log'    AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m
WHERE r.module = 'log' AND r.resource_type IN ('api','button') AND r.deleted = 0 AND (r.parent_id IS NULL OR r.parent_id = 0);

-- 3. config 模块 → /config 菜单
UPDATE auth_resource r SET parent_id = m.id
FROM (SELECT id FROM auth_resource WHERE resource_code = '/config' AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m
WHERE r.module = 'config' AND r.resource_type IN ('api','button') AND r.deleted = 0 AND (r.parent_id IS NULL OR r.parent_id = 0);

-- 4. storage 模块 → /storage 菜单（若尚未迁移）
UPDATE auth_resource r SET parent_id = m.id
FROM (SELECT id FROM auth_resource WHERE resource_code = '/storage' AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m
WHERE r.module = 'storage' AND r.resource_type IN ('api','button') AND r.deleted = 0 AND (r.parent_id IS NULL OR r.parent_id = 0);

-- 5. douyin 模块 → /douyin 菜单
UPDATE auth_resource r SET parent_id = m.id
FROM (SELECT id FROM auth_resource WHERE resource_code = '/douyin' AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m
WHERE r.module = 'douyin' AND r.resource_type IN ('api','button') AND r.deleted = 0 AND (r.parent_id IS NULL OR r.parent_id = 0);

-- 6. copy 模块 → /copy 菜单
UPDATE auth_resource r SET parent_id = m.id
FROM (SELECT id FROM auth_resource WHERE resource_code = '/copy'   AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m
WHERE r.module = 'copy' AND r.resource_type IN ('api','button') AND r.deleted = 0 AND (r.parent_id IS NULL OR r.parent_id = 0);

-- 7. script 模块 → /live 菜单（话术在直播下）
UPDATE auth_resource r SET parent_id = m.id
FROM (SELECT id FROM auth_resource WHERE resource_code = '/live'   AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m
WHERE r.module = 'script' AND r.resource_type IN ('api','button') AND r.deleted = 0 AND (r.parent_id IS NULL OR r.parent_id = 0);

-- 8. live 模块 → /live 菜单
UPDATE auth_resource r SET parent_id = m.id
FROM (SELECT id FROM auth_resource WHERE resource_code = '/live'   AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m
WHERE r.module = 'live' AND r.resource_type IN ('api','button') AND r.deleted = 0 AND (r.parent_id IS NULL OR r.parent_id = 0);

-- 9. product 模块 → /product 菜单
UPDATE auth_resource r SET parent_id = m.id
FROM (SELECT id FROM auth_resource WHERE resource_code = '/product' AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m
WHERE r.module = 'product' AND r.resource_type IN ('api','button') AND r.deleted = 0 AND (r.parent_id IS NULL OR r.parent_id = 0);

-- 10. ai 模块 → /ai 菜单
UPDATE auth_resource r SET parent_id = m.id
FROM (SELECT id FROM auth_resource WHERE resource_code = '/ai'     AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m
WHERE r.module = 'ai' AND r.resource_type IN ('api','button') AND r.deleted = 0 AND (r.parent_id IS NULL OR r.parent_id = 0);
