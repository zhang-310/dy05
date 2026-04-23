-- 将 admin 密码修复为 admin123（BCrypt）
-- 用法: psql -h localhost -p 5433 -U postgres -d douyin_operations -f sql/fix-admin-password.sql
UPDATE auth_user SET password_hash = '$2a$10$tEp/V/6Ij98O6LgLsCjDXetlMR./FCXgfMcIRB49BcqI/jBnM/5yS' WHERE username = 'admin' AND deleted = 0;
