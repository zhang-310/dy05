-- 测试环境初始数据：admin 用户 (密码 admin123)
-- BCrypt 哈希来自 demo-all.sql，与 admin123 对应
INSERT INTO auth_role (role_code, role_name, status, sort_order, deleted, create_time, update_time)
VALUES ('admin', '平台管理员', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO auth_user (username, password_hash, nickname, role_code, status, deleted, create_time, update_time)
VALUES ('admin', '$2a$10$tEp/V/6Ij98O6LgLsCjDXetlMR./FCXgfMcIRB49BcqI/jBnM/5yS', '管理员', 'admin', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
