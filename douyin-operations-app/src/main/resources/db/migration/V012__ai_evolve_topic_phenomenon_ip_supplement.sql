-- ============================================================
-- 现象级IP知识主题池 - 全量补充（深度分析缺口）
-- 版本：1.0 | 创建日期：2026-03-14
-- 补充：垂类、B端、钩子、案例、跨域、投放、主播、质量
-- ============================================================

-- ============================================================
-- P0：美妆护肤垂类（dy01 业务强相关）
-- ============================================================
INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '护肤品套盒 甩货节奏 过品 直播 美妆', 'douyin_live', 88, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '护肤品套盒 甩货节奏 过品 直播 美妆' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '彩妆直播 单品深度 品类数 转化', 'douyin_live', 88, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '彩妆直播 单品深度 品类数 转化' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '供应链女王 产业生态 B端沉淀 田玲红', 'douyin_commercial', 88, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '供应链女王 产业生态 B端沉淀 田玲红' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, 'B端现象级 行业影响力 决策者信任', 'douyin_commercial', 88, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = 'B端现象级 行业影响力 决策者信任' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '美妆爆款 护肤成分 彩妆试色 品类脚本差异', 'douyin_vertical', 89, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '美妆爆款 护肤成分 彩妆试色 品类脚本差异' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '敏感肌 成分科普 护肤达人 种草测评', 'douyin_vertical', 89, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '敏感肌 成分科普 护肤达人 种草测评' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, 'B端话术 供应链说服 产业认知 决策路径', 'douyin_live', 89, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = 'B端话术 供应链说服 产业认知 决策路径' AND deleted = 0);

-- ============================================================
-- P1：玩法与案例细化（钩子、情绪、案例）
-- ============================================================
INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '黄金3秒钩子类型 悬念反转 痛点刺激 身份代入 数据冲击 反常识', 'douyin_basic', 87, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '黄金3秒钩子类型 悬念反转 痛点刺激 身份代入 数据冲击 反常识' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '短视频情绪节奏 0-3秒 3-8秒 8-18秒 转折点', 'douyin_basic', 87, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '短视频情绪节奏 0-3秒 3-8秒 8-18秒 转折点' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '刘畊宏 王心凌 怀旧情怀 跨代共鸣 爆款', 'douyin_shortvideo', 87, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '刘畊宏 王心凌 怀旧情怀 跨代共鸣 爆款' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '毒舌电影 朱一旦 固定人设 系列编号 剧情号', 'douyin_vertical', 87, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '毒舌电影 朱一旦 固定人设 系列编号 剧情号' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '0-3秒留存 完播率 前3秒设计', 'douyin_algorithm', 89, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '0-3秒留存 完播率 前3秒设计' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '抖音新功能 语音弹幕 合拍 特效 首用红利', 'douyin_algorithm', 89, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '抖音新功能 语音弹幕 合拍 特效 首用红利' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '素人健身 全家跟练 正能量BGM 居家爆款', 'douyin_shortvideo', 89, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '素人健身 全家跟练 正能量BGM 居家爆款' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '素人极端才艺 意外爆火 强情感表达', 'douyin_basic', 89, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '素人极端才艺 意外爆火 强情感表达' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '3集定律 前3集数据 择优投入 系列决策', 'douyin_vertical', 89, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '3集定律 前3集数据 择优投入 系列决策' AND deleted = 0);

-- ============================================================
-- P2：跨域迁移与投放
-- ============================================================
INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '跨域迁移 文化贴现评估 文化适配', 'douyin_algorithm', 86, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '跨域迁移 文化贴现评估 文化适配' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, 'DOU+ AB测试 多版本等额 24小时选优', 'douyin_commercial', 86, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = 'DOU+ AB测试 多版本等额 24小时选优' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '投放ROI 粉丝获取成本 效率最优', 'douyin_commercial', 86, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '投放ROI 粉丝获取成本 效率最优' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '跨域迁移适用条件 行业对标 迁移可行性', 'douyin_algorithm', 88, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '跨域迁移适用条件 行业对标 迁移可行性' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '投放综合效率 互动成本 完播率 模版选择', 'douyin_commercial', 88, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '投放综合效率 互动成本 完播率 模版选择' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '供应链撮合 品牌工厂 渠道对接', 'douyin_commercial', 88, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '供应链撮合 品牌工厂 渠道对接' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '产业活动 峰会 沙龙 供应链对接', 'douyin_commercial', 88, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '产业活动 峰会 沙龙 供应链对接' AND deleted = 0);

-- ============================================================
-- P3：五位主播与流量协同
-- ============================================================
INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '校园流量 学生党 肖瑶 流量入口', 'douyin_algorithm', 85, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '校园流量 学生党 肖瑶 流量入口' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '五位主播 C端转B端 流量沉淀 协同', 'douyin_team', 85, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '五位主播 C端转B端 流量沉淀 协同' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '国风批发 文化供应链 智慧 B端信任', 'douyin_commercial', 87, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '国风批发 文化供应链 智慧 B端信任' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '职场转化 生活转化 肖蝉 阳阳 中腰部', 'douyin_team', 87, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '职场转化 生活转化 肖蝉 阳阳 中腰部' AND deleted = 0);

-- ============================================================
-- P4：质量与冷门
-- ============================================================
INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '完播率 算法推荐 留存曲线', 'douyin_algorithm', 86, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '完播率 算法推荐 留存曲线' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '原生爆款案例库 BGM 情绪转折 评论区', 'douyin_shortvideo', 86, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '原生爆款案例库 BGM 情绪转折 评论区' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '冷门检测 低效淘汰 知识库质量', 'douyin_data', 86, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '冷门检测 低效淘汰 知识库质量' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '方法论同质化 差异化突破 创新方向', 'douyin_algorithm', 88, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '方法论同质化 差异化突破 创新方向' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '悬念钩子 下集预告 评论猜测 追更', 'douyin_basic', 88, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '悬念钩子 下集预告 评论猜测 追更' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted)
SELECT NULL, NULL, '才艺嫁接热搜 技能热点结合', 'douyin_algorithm', 88, 'manual', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '才艺嫁接热搜 技能热点结合' AND deleted = 0);
