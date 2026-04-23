-- ============================================================
-- 商品管理模块 - 初始化数据（使用 auth_resource）
-- ============================================================

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('menu', '/product', '商品管理', 'product', NULL, 0, 1),
    ('api', '/api/v1/product/list', '查看商品列表', 'product', 'POST', 0, 0),
    ('api', '/api/v1/product/save', '创建/编辑商品', 'product', 'POST', 0, 0),
    ('api', '/api/v1/product/delete', '删除商品', 'product', 'POST', 0, 0),
    ('api', '/api/v1/product/category', '查看商品分类', 'product', 'POST', 0, 0),
    ('api', '/api/v1/product/inventory', '查看商品库存', 'product', 'POST', 0, 0),
    ('api', '/api/v1/product/sales', '查看销售历史', 'product', 'POST', 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO auth_role_resource (role_id, resource_id)
SELECT r.id, res.id FROM auth_role r, auth_resource res
WHERE r.role_code = 'admin' AND r.deleted = 0
  AND res.resource_code IN ('/product', '/api/v1/product/list', '/api/v1/product/save', '/api/v1/product/delete', '/api/v1/product/category', '/api/v1/product/inventory', '/api/v1/product/sales')
  AND res.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM auth_role_resource WHERE role_id = r.id AND resource_id = res.id);
