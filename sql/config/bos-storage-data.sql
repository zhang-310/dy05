-- ============================================================
-- 百度云 BOS 对象存储配置 - 后台「配置管理」中维护
-- 执行：在 config/schema 之后，demo-all 或 init 时执行
-- ============================================================

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'storage.bos.endpoint', 'http://bj.bcebos.com', 'string', 0, 'storage', 'BOS 服务端点', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'storage.bos.endpoint' AND deleted = 0);

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'storage.bos.bucket', '', 'string', 0, 'storage', 'BOS Bucket 名称', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'storage.bos.bucket' AND deleted = 0);

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'storage.bos.accessKey', '', 'password', 1, 'storage', 'BOS Access Key', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'storage.bos.accessKey' AND deleted = 0);

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'storage.bos.secretKey', '', 'password', 1, 'storage', 'BOS Secret Key', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'storage.bos.secretKey' AND deleted = 0);

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'storage.bos.maxConnections', '1000', 'number', 0, 'storage', 'HTTP 最大连接数', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'storage.bos.maxConnections' AND deleted = 0);

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'storage.bos.connectionTimeoutInMillis', '360000', 'number', 0, 'storage', 'TCP 连接超时（毫秒）', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'storage.bos.connectionTimeoutInMillis' AND deleted = 0);

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'storage.bos.socketTimeoutInMillis', '360000', 'number', 0, 'storage', 'Socket 传输超时（毫秒）', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'storage.bos.socketTimeoutInMillis' AND deleted = 0);

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'storage.bos.cdnDomain', '', 'string', 0, 'storage', 'CDN 加速域名（可选）', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'storage.bos.cdnDomain' AND deleted = 0);

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'storage.bos.region', 'bj', 'string', 0, 'storage', '区域（如 bj/gz/su）', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'storage.bos.region' AND deleted = 0);
