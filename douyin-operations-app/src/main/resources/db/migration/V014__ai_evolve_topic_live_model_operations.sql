-- ============================================================
-- 直播模型与完整运营体系 - 最强抖音运营知识库
-- 版本：1.0 | 创建日期：2026-03-14
-- 补充：单品/过品/仓播、亏品爆款利润品、排品、话术规则、运营体系、huashu
-- ============================================================

-- ============================================================
-- P0：直播模型与产品角色
-- ============================================================
INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '单品直播间 1-5品 深度讲解 彩妆直播', 'douyin_live', 79, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '单品直播间 1-5品 深度讲解 彩妆直播' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '过品直播间 50-120款 甩货节奏 套盒直播', 'douyin_live', 79, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '过品直播间 50-120款 甩货节奏 套盒直播' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '仓播直播间 仓库清货 多品 亏品爆款利润品', 'douyin_live', 79, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '仓播直播间 仓库清货 多品 亏品爆款利润品' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '亏品 微亏 爆款 利润品 排品策略 话术时长', 'douyin_live', 79, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '亏品 微亏 爆款 利润品 排品策略 话术时长' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '刺激成交密度 刺激GMV 客单价 高客单价', 'douyin_live', 79, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '刺激成交密度 刺激GMV 客单价 高客单价' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '直播模型定位 单品vs过品vs仓播 账号类型', 'douyin_live', 79, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '直播模型定位 单品vs过品vs仓播 账号类型' AND deleted = 0);

-- ============================================================
-- P1：排品与话术规则
-- ============================================================
INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '排品顺序 亏品引流 爆款炸场 利润品收割', 'douyin_live', 78, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '排品顺序 亏品引流 爆款炸场 利润品收割' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '亏品话术 3-10秒极速过品 引流', 'douyin_live', 78, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '亏品话术 3-10秒极速过品 引流' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '爆款话术 1-5分钟深度讲解 炸场', 'douyin_live', 78, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '爆款话术 1-5分钟深度讲解 炸场' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '利润品话术 30-60秒 人气高时主推', 'douyin_live', 78, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '利润品话术 30-60秒 人气高时主推' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '控单憋单 控单产品 90-150秒', 'douyin_live', 78, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '控单憋单 控单产品 90-150秒' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '人气策略 人气高主推爆品高客单 做转化', 'douyin_live', 78, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '人气策略 人气高主推爆品高客单 做转化' AND deleted = 0);

-- ============================================================
-- P2：完整运营体系
-- ============================================================
INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '直播定位 账号类型 直播间模型选择', 'douyin_live', 77, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '直播定位 账号类型 直播间模型选择' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '选品排品 亏品爆款利润品组合 选品看板', 'douyin_live', 77, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '选品排品 亏品爆款利润品组合 选品看板' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '开播节奏 过品执行 仓播执行 SOP', 'douyin_live', 77, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '开播节奏 过品执行 仓播执行 SOP' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '直播复盘 按模型复盘 排品复盘 GMV拆解', 'douyin_live', 77, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '直播复盘 按模型复盘 排品复盘 GMV拆解' AND deleted = 0);

-- ============================================================
-- P3：最强抖音运营
-- ============================================================
INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '最强抖音运营 完整运营体系 定位选品话术复盘', 'douyin_live', 76, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '最强抖音运营 完整运营体系 定位选品话术复盘' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, 'GMV目标 转化率 客单价 成交密度', 'douyin_data', 76, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = 'GMV目标 转化率 客单价 成交密度' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '直播间数据 停留 互动 转化 复盘指标', 'douyin_data', 76, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '直播间数据 停留 互动 转化 复盘指标' AND deleted = 0);

-- ============================================================
-- P4：huashu 仓播过品话术
-- ============================================================
INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '仓播过品话术 极速过品 一句话报价 福利', 'huashu_persuasion', 79, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '仓播过品话术 极速过品 一句话报价 福利' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '亏品引流话术 秒杀 抢购 限量', 'huashu_persuasion', 79, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '亏品引流话术 秒杀 抢购 限量' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '爆款炸场话术 深度讲解 成分 卖点', 'huashu_persuasion', 79, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '爆款炸场话术 深度讲解 成分 卖点' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '利润品促单话术 高客单 价值感 稀缺', 'huashu_persuasion', 79, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '利润品促单话术 高客单 价值感 稀缺' AND deleted = 0);
