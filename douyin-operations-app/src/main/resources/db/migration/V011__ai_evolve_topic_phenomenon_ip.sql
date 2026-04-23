-- ============================================================
-- 现象级IP知识主题池 - 基于知识库进化报告与玩法模式提炼
-- 版本：1.0 | 创建日期：2026-03-14
-- 来源：原生爆款、剧情号、素人爆火、跨域迁移、付费推广、粉丝质量
-- ============================================================

-- 确保 account_id 列存在（部分环境可能未执行 add-evolve-topic-account-id）
ALTER TABLE ai_evolve_topic ADD COLUMN IF NOT EXISTS account_id BIGINT;

-- ============================================================
-- P0：原生爆款模式（BGM驱动、情绪密度、平台新功能）
-- ============================================================
INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, 'BGM驱动冷启动 3秒钩子 情绪锁定 原生爆款', 'douyin_basic', 90, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = 'BGM驱动冷启动 3秒钩子 情绪锁定 原生爆款' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '情绪密度打包 15秒情绪过山车 情绪转折点', 'douyin_basic', 90, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '情绪密度打包 15秒情绪过山车 情绪转折点' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '平台新功能抢先 新特效首用 抖音功能红利', 'douyin_algorithm', 90, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '平台新功能抢先 新特效首用 抖音功能红利' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '挖呀挖 张同学 原生爆款案例 可复制结构', 'douyin_shortvideo', 90, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '挖呀挖 张同学 原生爆款案例 可复制结构' AND deleted = 0);

-- ============================================================
-- P1：剧情号矩阵与内容系列化
-- ============================================================
INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '剧情号系列化 3集定律 粉丝增长瓶颈突破', 'douyin_vertical', 95, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '剧情号系列化 3集定律 粉丝增长瓶颈突破' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '短视频评论区互动设计 追更留悬念 高赞评论下集', 'douyin_basic', 95, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '短视频评论区互动设计 追更留悬念 高赞评论下集' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '剧情号主号小号联动 跨账号导流', 'douyin_team', 95, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '剧情号主号小号联动 跨账号导流' AND deleted = 0);

-- ============================================================
-- P2：素人爆火与情感共鸣
-- ============================================================
INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '素人爆火 痛点情感双挂钩 情感共鸣设计', 'douyin_basic', 95, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '素人爆火 痛点情感双挂钩 情感共鸣设计' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '二创引导 合拍挑战 用户参与裂变', 'douyin_commercial', 95, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '二创引导 合拍挑战 用户参与裂变' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '实时热点嫁接 热搜结合 选题时机', 'douyin_algorithm', 95, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '实时热点嫁接 热搜结合 选题时机' AND deleted = 0);

-- ============================================================
-- P3：跨域迁移玩法（游戏/金融/制造/开源）
-- ============================================================
INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '粉丝通行证 每日签到 积分徽章 月互动天数', 'douyin_algorithm', 92, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '粉丝通行证 每日签到 积分徽章 月互动天数' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '直播话术压力测试 合规预演 封号风险规避', 'douyin_compliance', 92, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '直播话术压力测试 合规预演 封号风险规避' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '爆单供应链预警 库存发货看板 爆款后端保障', 'douyin_commercial', 92, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '爆单供应链预警 库存发货看板 爆款后端保障' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '垂类贡献者激励 创作者生态 免费工具引流', 'douyin_commercial', 92, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '垂类贡献者激励 创作者生态 免费工具引流' AND deleted = 0);

-- ============================================================
-- P4：付费推广与平台玩法
-- ============================================================
INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, 'DOU+投放测试 5%用户24小时 人群包迭代', 'douyin_commercial', 93, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = 'DOU+投放测试 5%用户24小时 人群包迭代' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '挑战赛设计 合拍 话题营销 低门槛高展示', 'douyin_commercial', 93, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '挑战赛设计 合拍 话题营销 低门槛高展示' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '品牌任务 全民任务 创作者流量激励', 'douyin_commercial', 93, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '品牌任务 全民任务 创作者流量激励' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '直播连麦 PK 粉丝互换 人设匹配', 'douyin_live', 93, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '直播连麦 PK 粉丝互换 人设匹配' AND deleted = 0);

-- ============================================================
-- P5：粉丝质量与阶段化策略
-- ============================================================
INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '冷启动0-1万粉 增长期1-10万 爆发期10万+ 稳定期50万+', 'douyin_team', 94, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '冷启动0-1万粉 增长期1-10万 爆发期10万+ 稳定期50万+' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '粉丝质量 月互动天数 活跃粉丝 算法文化双驱动', 'douyin_data', 94, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '粉丝质量 月互动天数 活跃粉丝 算法文化双驱动' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '现象级IP 算法推荐 文化共鸣 双重驱动', 'douyin_algorithm', 91, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '现象级IP 算法推荐 文化共鸣 双重驱动' AND deleted = 0);
