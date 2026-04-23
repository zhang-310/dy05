-- ============================================================
-- AI 运行时配置 - 管理员可在「系统配置」中动态修改，无需重启
-- 配置分组：ai，在配置管理页面选择「AI」选项卡查看/编辑
-- 执行：在 ai-providers-data 之后执行
-- ============================================================

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'ai.index-queue.consumer.interval-ms', '60000', 'number', 0, 'ai', '索引队列消费间隔（毫秒），如 60000=1分钟、30000=30秒', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.index-queue.consumer.interval-ms' AND deleted = 0);

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'ai.index-queue.consumer.batch-size', '20', 'number', 0, 'ai', '索引队列每轮处理条数', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.index-queue.consumer.batch-size' AND deleted = 0);

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'ai.evolve.interval-minutes', '60', 'number', 0, 'ai', '知识进化执行间隔（分钟），如 60=每小时、10=每10分钟', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.evolve.interval-minutes' AND deleted = 0);

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'ai.dual-write.compensation.batch-size', '20', 'number', 0, 'ai', '双写补偿每轮处理条数', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.dual-write.compensation.batch-size' AND deleted = 0);

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'ai.quota.daily-max', '5000', 'number', 0, 'ai', '每日每用户 AI 调用额度上限（新用户/新日期生效）', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.quota.daily-max' AND deleted = 0);

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'ai.query-rewrite.profile_override', '', 'string', 0, 'ai', '查询改写/进化用户画像覆盖（留空则用抖音账号数据）。示例：粉丝数 50000，账号描述：护肤品套盒直播、直播间话术', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.query-rewrite.profile_override' AND deleted = 0);
