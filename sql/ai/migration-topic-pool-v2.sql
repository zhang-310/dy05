-- 进化主题池 v2：分类边界清晰，按知识库前缀划分
-- 执行：docker cp sql/ai/migration-topic-pool-v2.sql dy-postgres:/tmp/; docker exec dy-postgres psql -U postgres -d douyin_operations -f /tmp/migration-topic-pool-v2.sql

-- 1. 软删除现有主题
UPDATE ai_evolve_topic SET deleted = 1, update_time = CURRENT_TIMESTAMP WHERE deleted = 0;

-- 2. 插入新主题池（分类边界清晰）
INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted, create_time, update_time) VALUES
-- douyin 知识库（旧 live/basic/data/vertical 等重新分类纳入）
(NULL, NULL, '抖音运营 直播 话术 粉丝互动', 'douyin_live', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '短视频 脚本 黄金3秒 内容创作', 'douyin_basic', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '数据分析 关键指标 优化决策', 'douyin_data', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '算法推荐 流量 爆款 运营', 'douyin_algorithm', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '商业化 广告 电商 知识付费 变现', 'douyin_commercial', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, 'AI AIGC 豆包 剪映AI 数字人', 'douyin_ai', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, 'AI 辅助创作 大模型 短视频 直播', 'douyin_ai', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '垂类策略 美妆 知识科普 剧情 户外', 'douyin_vertical', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '爆款拆解 脚本结构 分镜 黄金3秒', 'douyin_shortvideo', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '剪映 工具链 发布质检 平台规则', 'douyin_compliance', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '账号矩阵 团队管理 中高级战术', 'douyin_team', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '直播开场话术 留人 黄金3秒 欢迎', 'douyin_live', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '产品卖点话术 痛点 卖点 转化', 'douyin_live', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '互动留人话术 弹幕 评论 引导', 'douyin_live', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '促单收尾话术 下单 限时 秒杀', 'douyin_live', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- huashu 知识库
(NULL, NULL, '高扎心话术 人生真相 现实共鸣 情感冲击', 'huashu_emotional', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '高鸡汤话术 正能量 励志 情感价值', 'huashu_emotional', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '痛点共鸣话术 直接命中痛点 缺失感', 'huashu_emotional', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '治愈系话术 轻生活 高压人群 治愈', 'huashu_emotional', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '搞笑幽默话术 段子 梗 轻松调侃', 'huashu_humor', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '自嘲幽默话术 承认缺点 转折亮点 亲和力', 'huashu_humor', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '反焦虑反转话术 预期铺垫 意外转折 缓解压力', 'huashu_humor', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '毒鸡汤话术 反讽 夸张 生活热点 绝绝子', 'huashu_humor', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '歇后语俗语话术 接地气 民间智慧', 'huashu_literary', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '高抒情话术 诗意 排比 意境', 'huashu_literary', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '人生语录话术 金句 哲理 记忆点', 'huashu_literary', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '顺口溜押韵话术 节奏感 记忆点 高情商回复', 'huashu_literary', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '成长逆袭话术 时间延续 强对比 逆袭故事', 'huashu_literary', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '身份锚定留人话术 精准标签 降低决策门槛', 'huashu_persuasion', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '损失厌恶促单话术 紧迫感 稀缺感 库存回收', 'huashu_persuasion', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '家庭夫妻话术 婆媳 育儿 婚姻共鸣', 'huashu_scenario', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '爱情情感话术 夫妻关系 恋爱观', 'huashu_scenario', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '打工人职场话术 职场共鸣 摸鱼 加班心酸', 'huashu_scenario', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '地方特色话术 方言 接地气 地域共鸣', 'huashu_scenario', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '女性视角话术 经济自主 自我价值 婚姻疲惫', 'huashu_scenario', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '人设特色话术 记忆点 标签 差异化', 'huashu_persuasion', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '高创意话术 反转 意料之外 记忆点', 'huashu_literary', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- zhishi 知识库
(NULL, NULL, 'Spring Boot 微服务 架构 最佳实践', 'zhishi_backend', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, 'API 设计 REST 规范 接口文档', 'zhishi_backend', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '前端 React Vue 组件 工程化', 'zhishi_frontend', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '数据库 索引 查询优化 SQL 规范', 'zhishi_data', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, 'DevOps 部署 监控 日志 可观测性', 'zhishi_devops', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, NULL, '安全 认证 鉴权 加密 漏洞防护', 'zhishi_security', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
