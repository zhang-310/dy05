-- ============================================================
-- 知识主题池 - 业务范围全覆盖补充（第三轮深度评估）
-- 版本：1.0 | 创建日期：2026-03-14
-- 补充：口播文案、护肤彩妆细分、合规、发布、千川、私域、复盘、大场、huashu
-- ============================================================

-- ============================================================
-- P0：业务范围强相关（口播、套盒、护肤彩妆、美妆合规）
-- ============================================================
INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '口播文案 纯口播不卖货 励志爱情情感鸡汤 拉停留促共鸣', 'douyin_vertical', 84, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '口播文案 纯口播不卖货 励志爱情情感鸡汤 拉停留促共鸣' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '套盒成分讲解 搭配种草 护肤品套盒达人', 'douyin_vertical', 84, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '套盒成分讲解 搭配种草 护肤品套盒达人' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '精华面膜眼霜 保湿抗老 护肤达人选题', 'douyin_vertical', 84, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '精华面膜眼霜 保湿抗老 护肤达人选题' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '底妆唇妆眼影 妆容教程 平价测评', 'douyin_vertical', 84, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '底妆唇妆眼影 妆容教程 平价测评' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '美妆直播合规 绝对化用语 医疗功效 违规规避', 'douyin_compliance', 84, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '美妆直播合规 绝对化用语 医疗功效 违规规避' AND deleted = 0);

-- ============================================================
-- P1：平台生态（发布、千川、私域、复盘、高停留）
-- ============================================================
INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '发布时机 粉丝活跃时段 标题封面推荐', 'douyin_algorithm', 83, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '发布时机 粉丝活跃时段 标题封面推荐' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '千川投流 巨量千川 直播投流 Feed流', 'douyin_commercial', 83, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '千川投流 巨量千川 直播投流 Feed流' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '私域引流 粉丝群 公转私 社群运营', 'douyin_commercial', 83, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '私域引流 粉丝群 公转私 社群运营' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '直播复盘 话术效果归因 数据复盘 迭代优化', 'douyin_live', 83, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '直播复盘 话术效果归因 数据复盘 迭代优化' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '高停留话术 停留时长 完播率 留人技巧', 'douyin_live', 83, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '高停留话术 停留时长 完播率 留人技巧' AND deleted = 0);

-- ============================================================
-- P2：运营活动（大场、矩阵、混剪、粉丝画像）
-- ============================================================
INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '大场策划 节点营销 双11 618 年货节', 'douyin_commercial', 82, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '大场策划 节点营销 双11 618 年货节' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '矩阵起号 1拖N 批量化 多账号', 'douyin_team', 82, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '矩阵起号 1拖N 批量化 多账号' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '混剪爆款 原创度 搬运风险 二创合规', 'douyin_shortvideo', 82, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '混剪爆款 原创度 搬运风险 二创合规' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '粉丝画像 人群洞察 精准定向 标签体系', 'douyin_data', 82, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '粉丝画像 人群洞察 精准定向 标签体系' AND deleted = 0);

-- ============================================================
-- P3：产品链路（AI视频、TTS、AB测试、知识付费）
-- ============================================================
INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '文生视频 图生视频 AI剪辑 短视频生产', 'douyin_ai', 81, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '文生视频 图生视频 AI剪辑 短视频生产' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, 'TTS配音 口播配音 AI配音', 'douyin_ai', 81, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = 'TTS配音 口播配音 AI配音' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '话术AB测试 效果对比 变体选优', 'douyin_data', 81, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '话术AB测试 效果对比 变体选优' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '知识付费 课程 会员 咨询变现', 'douyin_commercial', 81, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '知识付费 课程 会员 咨询变现' AND deleted = 0);

-- ============================================================
-- P4：合规风控
-- ============================================================
INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '违规词 平台规则 限流规避 封号预防', 'douyin_compliance', 80, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '违规词 平台规则 限流规避 封号预防' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '美妆广告法 宣传禁区 合规话术', 'douyin_compliance', 80, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '美妆广告法 宣传禁区 合规话术' AND deleted = 0);

-- ============================================================
-- P5：huashu 知识库补充
-- ============================================================
INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '口播文案话术 励志金句 情感共鸣 鸡汤记忆点', 'huashu_emotional', 84, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '口播文案话术 励志金句 情感共鸣 鸡汤记忆点' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '护肤成分话术 彩妆种草 成分党 种草文案', 'huashu_persuasion', 84, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '护肤成分话术 彩妆种草 成分党 种草文案' AND deleted = 0);
