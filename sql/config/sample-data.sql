-- ============================================================
-- config 模块 - 示例数据
-- ============================================================

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
VALUES
  ('site.name', '抖音运营平台', 'string', 0, 'basic', '站点名称', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('site.logo', '/assets/logo.png', 'string', 0, 'basic', '站点Logo路径', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('upload.max_size', '10485760', 'number', 0, 'storage', '文件上传最大大小（字节）', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('upload.allowed_types', 'jpg,jpeg,png,gif,mp4,mov', 'string', 0, 'storage', '允许上传的文件类型', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('notification.email_enabled', 'true', 'boolean', 0, 'notification', '是否启用邮件通知', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('api.rate_limit', '100', 'number', 0, 'security', 'API每分钟请求限制', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
