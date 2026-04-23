-- V020: AI 进化模块 Prompt 配置（P2-2）
-- 管理后台可在线编辑 sys_config，无需重启

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'ai.prompt.evolve.system', '你是一位抖音行业运营专家，同时具备方法论提炼、失败复盘和可执行方案设计能力。', 'string', 0, 'ai', '进化引擎系统 prompt（general/douyin 类型）', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.prompt.evolve.system' AND deleted = 0);

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'ai.prompt.evolve.huashu.system', '你是一位资深直播话术教练，擅长提炼可直接使用的直播话术。输出的每条话术都应是主播能直接照着念的完整句子，不要输出方法论或分析，只要实战话术。', 'string', 0, 'ai', '话术进化系统 prompt', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.prompt.evolve.huashu.system' AND deleted = 0);

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'ai.prompt.evolve.zhishi.system', '你是一位资深技术架构师，擅长提炼可执行的技术最佳实践。输出应含具体代码示例、配置要点或架构模式，避免空洞表述。', 'string', 0, 'ai', '知识进化系统 prompt', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.prompt.evolve.zhishi.system' AND deleted = 0);

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'ai.prompt.viral.system', '你是一位专业的短视频数据分析师，擅长分析爆款视频的成功因素。请用中文回答。', 'string', 0, 'ai', '爆款拆解系统 prompt（基础部分）', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.prompt.viral.system' AND deleted = 0);

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'ai.prompt.live_review.system', '你是一位专业的直播数据分析师，擅长复盘直播表现并给出改进建议。请用中文回答。', 'string', 0, 'ai', '直播复盘系统 prompt（基础部分）', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.prompt.live_review.system' AND deleted = 0);

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'ai.prompt.freshness.check', '你是知识时效性检测专家。', 'string', 0, 'ai', '时效性检查 prompt', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.prompt.freshness.check' AND deleted = 0);

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'ai.prompt.deep_evolve.system', '你是抖音运营领域深度研究专家，输出严谨、可落地的方法论。', 'string', 0, 'ai', '深度进化 prompt', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.prompt.deep_evolve.system' AND deleted = 0);

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'ai.prompt.multimodal.system', '你是一位视觉内容分析专家，擅长为图片和视频内容生成精准的文字描述，用于知识索引和检索。', 'string', 0, 'ai', '多模态知识索引 prompt', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.prompt.multimodal.system' AND deleted = 0);

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'ai.prompt.competitor.system', '你是一位资深抖音竞品分析师，擅长从竞品账号的热门内容中提取可学习的知识要点和运营策略。', 'string', 0, 'ai', '竞品知识补充 Agent prompt', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.prompt.competitor.system' AND deleted = 0);

INSERT INTO sys_config (config_key, config_value, value_type, is_sensitive, config_group, remark, deleted, create_time, update_time)
SELECT 'ai.prompt.feedback.system', '你是一位直播话术质量诊断专家，擅长从低效话术中分析失败原因并提取知识缺口。', 'string', 0, 'ai', '用户反馈驱动 Agent prompt', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.prompt.feedback.system' AND deleted = 0);
