-- Role home BFF endpoints and role bindings.
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order, deleted, create_time, update_time)
SELECT 'api', '/api/v1/admin/home', 'Admin home BFF', 'dashboard', 'POST', 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = '/api/v1/admin/home' AND deleted = 0);

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order, deleted, create_time, update_time)
SELECT 'api', '/api/v1/org/home', 'Organization home BFF', 'dashboard', 'POST', 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = '/api/v1/org/home' AND deleted = 0);

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order, deleted, create_time, update_time)
SELECT 'api', '/api/v1/talent/home', 'Talent home BFF', 'dashboard', 'POST', 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = '/api/v1/talent/home' AND deleted = 0);

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order, deleted, create_time, update_time)
SELECT 'api', '/api/v1/user/home', 'User home BFF', 'dashboard', 'POST', 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = '/api/v1/user/home' AND deleted = 0);

INSERT INTO auth_role_resource (role_id, resource_id, create_time)
SELECT r.id, res.id, CURRENT_TIMESTAMP
FROM auth_role r, auth_resource res
WHERE r.role_code = 'admin' AND r.deleted = 0
  AND res.resource_code = '/api/v1/admin/home' AND res.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM auth_role_resource arr WHERE arr.role_id = r.id AND arr.resource_id = res.id);

INSERT INTO auth_role_resource (role_id, resource_id, create_time)
SELECT r.id, res.id, CURRENT_TIMESTAMP
FROM auth_role r, auth_resource res
WHERE r.role_code = 'institution' AND r.deleted = 0
  AND res.resource_code = '/api/v1/org/home' AND res.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM auth_role_resource arr WHERE arr.role_id = r.id AND arr.resource_id = res.id);

INSERT INTO auth_role_resource (role_id, resource_id, create_time)
SELECT r.id, res.id, CURRENT_TIMESTAMP
FROM auth_role r, auth_resource res
WHERE r.role_code = 'talent' AND r.deleted = 0
  AND res.resource_code = '/api/v1/talent/home' AND res.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM auth_role_resource arr WHERE arr.role_id = r.id AND arr.resource_id = res.id);

INSERT INTO auth_role_resource (role_id, resource_id, create_time)
SELECT r.id, res.id, CURRENT_TIMESTAMP
FROM auth_role r, auth_resource res
WHERE r.role_code = 'user' AND r.deleted = 0
  AND res.resource_code = '/api/v1/user/home' AND res.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM auth_role_resource arr WHERE arr.role_id = r.id AND arr.resource_id = res.id);
