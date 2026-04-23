-- ============================================================
-- script 模块 - 资源数据初始化
-- 包含：菜单资源、API资源、admin角色绑定
-- 版本：1.0
-- 更新日期：2026-02-25
-- ============================================================

-- ============================================================
-- 1. 菜单资源
-- ============================================================

-- script 模块菜单
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
VALUES ('menu', '/script', '话术管理', 'script', 0, 50)
ON CONFLICT DO NOTHING;

-- 获取最新插入的script菜单ID（用于设置子菜单的parent_id）
-- 注：在实际SQL执行中，应通过应用层逻辑来处理parent_id

-- 话术库管理菜单
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
VALUES ('menu', '/script/library', '话术库管理', 'script',
    (SELECT id FROM auth_resource WHERE resource_code = '/script' AND resource_type = 'menu' LIMIT 1),
    51)
ON CONFLICT DO NOTHING;

-- 违规词管理菜单
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
VALUES ('menu', '/script/violations', '违规词管理', 'script',
    (SELECT id FROM auth_resource WHERE resource_code = '/script' AND resource_type = 'menu' LIMIT 1),
    52)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 2. API资源 - 话术库管理
-- ============================================================

-- 查询话术列表
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/script/library/search', '查询话术列表', 'script', 'POST')
ON CONFLICT DO NOTHING;

-- 获取话术详情
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/script/library/get', '获取话术详情', 'script', 'POST')
ON CONFLICT DO NOTHING;

-- 保存话术
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/script/library/save', '保存话术', 'script', 'POST')
ON CONFLICT DO NOTHING;

-- 删除话术
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/script/library/delete', '删除话术', 'script', 'POST')
ON CONFLICT DO NOTHING;

-- 检测内容违规词
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/script/library/check', '检测违规词', 'script', 'POST')
ON CONFLICT DO NOTHING;

-- 获取检测历史
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/script/library/check-history', '获取检测历史', 'script', 'POST')
ON CONFLICT DO NOTHING;

-- ============================================================
-- 3. API资源 - 违规词管理
-- ============================================================

-- 查询违规词列表
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/script/violation/search', '查询违规词列表', 'script', 'POST')
ON CONFLICT DO NOTHING;

-- 获取违规词详情
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/script/violation/get', '获取违规词详情', 'script', 'POST')
ON CONFLICT DO NOTHING;

-- 保存违规词
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/script/violation/save', '保存违规词', 'script', 'POST')
ON CONFLICT DO NOTHING;

-- 删除违规词
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/script/violation/delete', '删除违规词', 'script', 'POST')
ON CONFLICT DO NOTHING;

-- 导入违规词
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/script/violation/import', '导入违规词', 'script', 'POST')
ON CONFLICT DO NOTHING;

-- 导出违规词
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/script/violation/export', '导出违规词', 'script', 'POST')
ON CONFLICT DO NOTHING;

-- ============================================================
-- 4. API资源 - 检测记录
-- ============================================================

-- 查询检测记录
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/script/check/search', '查询检测记录', 'script', 'POST')
ON CONFLICT DO NOTHING;

-- 获取检测报告
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/script/check/report', '获取检测报告', 'script', 'POST')
ON CONFLICT DO NOTHING;

-- ============================================================
-- 5. 初始化违规词数据
-- ============================================================

-- 虚假宣传类违规词
INSERT INTO violation_word (word, level, reason, replacement, status)
VALUES ('假冒伪劣', 3, '虚假宣传', '产品真实', 1)
ON CONFLICT DO NOTHING;

INSERT INTO violation_word (word, level, reason, replacement, status)
VALUES ('保证', 2, '虚假宣传', '建议/推荐', 1)
ON CONFLICT DO NOTHING;

INSERT INTO violation_word (word, level, reason, replacement, status)
VALUES ('国家认可', 2, '虚假宣传', '业界认可', 1)
ON CONFLICT DO NOTHING;

-- 广告法类违规词
INSERT INTO violation_word (word, level, reason, replacement, status)
VALUES ('最新', 2, '广告法', '新颖/创新', 1)
ON CONFLICT DO NOTHING;

INSERT INTO violation_word (word, level, reason, replacement, status)
VALUES ('最好', 2, '广告法', '优质/高效', 1)
ON CONFLICT DO NOTHING;

INSERT INTO violation_word (word, level, reason, replacement, status)
VALUES ('第一', 2, '广告法', '领先/突出', 1)
ON CONFLICT DO NOTHING;

INSERT INTO violation_word (word, level, reason, replacement, status)
VALUES ('唯一', 2, '广告法', '独特/专属', 1)
ON CONFLICT DO NOTHING;

-- 敏感词类违规词
INSERT INTO violation_word (word, level, reason, replacement, status)
VALUES ('赌博', 3, '敏感词', '娱乐', 1)
ON CONFLICT DO NOTHING;

INSERT INTO violation_word (word, level, reason, replacement, status)
VALUES ('暴力', 3, '敏感词', '冲突', 1)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 6. 角色权限绑定 - admin角色绑定所有script API权限
-- ============================================================

-- 先获取admin角色的ID和所有script API的ID，然后进行绑定
-- 这部分应该通过应用层逻辑处理，以获取正确的ID

INSERT INTO auth_role_resource (role_id, resource_id)
SELECT
    (SELECT id FROM auth_role WHERE role_code = 'admin' AND deleted = 0 LIMIT 1),
    id
FROM auth_resource
WHERE module = 'script' AND deleted = 0
  AND id NOT IN (
      SELECT resource_id FROM auth_role_resource
      WHERE role_id = (SELECT id FROM auth_role WHERE role_code = 'admin' AND deleted = 0 LIMIT 1)
  )
ON CONFLICT DO NOTHING;
