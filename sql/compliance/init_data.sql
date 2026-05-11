-- ============================================
-- 抖音违规知识库 - 初始数据
-- 生成日期: 2026-05-10
-- 说明: 基于抖音官方文档整理的违规规则
-- ============================================

-- 清空现有数据（开发环境）
-- TRUNCATE TABLE compliance_rule CASCADE;

-- ============================================
-- 1. 直播内容违规规则
-- ============================================

-- 1.1 政治敏感
INSERT INTO compliance_rule (rule_code, category, sub_category, rule_name, description, severity, punishment, examples, keywords, patterns, effective_date, status) VALUES
('LIVE_CONTENT_POLITICAL', 'live', 'content', '政治敏感内容', '涉及国家领导人、政治事件、敏感话题的内容', 'critical', '永久封禁',
'["提及政治人物", "讨论政治话题", "发表政治观点"]',
'["政治", "领导人", "政府", "党", "敏感话题"]',
'[]',
'2024-01-01', 1);

-- 1.2 色情低俗
INSERT INTO compliance_rule (rule_code, category, sub_category, rule_name, description, severity, punishment, examples, keywords, patterns, effective_date, status) VALUES
('LIVE_CONTENT_PORN', 'live', 'content', '色情低俗内容', '色情、低俗、性暗示内容', 'critical', '封禁 7-30 天',
'["穿着暴露", "性暗示动作", "低俗语言", "色情图片"]',
'["色情", "低俗", "性暗示", "裸露", "挑逗"]',
'[]',
'2024-01-01', 1);

-- 1.3 暴力血腥
INSERT INTO compliance_rule (rule_code, category, sub_category, rule_name, description, severity, punishment, examples, keywords, patterns, effective_date, status) VALUES
('LIVE_CONTENT_VIOLENCE', 'live', 'content', '暴力血腥内容', '暴力、血腥、恐怖内容', 'high', '封禁 7-30 天',
'["打架斗殴", "血腥画面", "恐怖场景", "虐待动物"]',
'["暴力", "血腥", "恐怖", "打架", "斗殴", "虐待"]',
'[]',
'2024-01-01', 1);

-- 1.4 虚假宣传
INSERT INTO compliance_rule (rule_code, category, sub_category, rule_name, description, severity, punishment, examples, keywords, patterns, effective_date, status) VALUES
('LIVE_CONTENT_FALSE_AD', 'live', 'content', '虚假宣传', '夸大功效、虚假承诺、误导消费者', 'high', '封禁 3-7 天 + 罚款',
'["包治百病", "7天瘦20斤", "100%有效", "国家认证（无证据）", "祖传秘方", "立竿见影"]',
'["包治", "百病", "必瘦", "100%", "国家认证", "祖传", "秘方", "立竿见影", "神效", "特效"]',
'["\\d+天瘦\\d+斤", "\\d+%有效", "包[治疗]"]',
'2024-01-01', 1);

-- 1.5 违法违规
INSERT INTO compliance_rule (rule_code, category, sub_category, rule_name, description, severity, punishment, examples, keywords, patterns, effective_date, status) VALUES
('LIVE_CONTENT_ILLEGAL', 'live', 'content', '违法违规内容', '涉及违法犯罪、赌博、毒品', 'critical', '永久封禁 + 报警',
'["售卖违禁品", "教唆犯罪", "赌博", "毒品", "枪支"]',
'["赌博", "毒品", "枪支", "爆炸物", "违禁品", "黑市"]',
'[]',
'2024-01-01', 1);

-- ============================================
-- 2. 直播行为违规规则
-- ============================================

-- 2.1 诱导行为
INSERT INTO compliance_rule (rule_code, category, sub_category, rule_name, description, severity, punishment, examples, keywords, patterns, effective_date, status) VALUES
('LIVE_BEHAVIOR_INDUCE', 'live', 'behavior', '诱导行为', '诱导关注、点赞、分享、私信', 'medium', '限流 + 封禁 1-3 天',
'["关注主播送礼物", "点赞破1000抽奖", "私信领优惠券", "分享到朋友圈送福利"]',
'["关注送", "点赞送", "私信领", "分享送", "转发抽奖"]',
'["关注.*送", "点赞.*抽奖", "私信.*领", "分享.*送"]',
'2024-01-01', 1);

-- 2.2 刷量作弊
INSERT INTO compliance_rule (rule_code, category, sub_category, rule_name, description, severity, punishment, examples, keywords, patterns, effective_date, status) VALUES
('LIVE_BEHAVIOR_FAKE', 'live', 'behavior', '刷量作弊', '刷粉丝、刷观看、刷互动', 'high', '封禁 7-30 天',
'["购买僵尸粉", "机器人互动", "刷观看量", "刷点赞"]',
'["刷粉", "刷量", "僵尸粉", "机器人", "假数据"]',
'[]',
'2024-01-01', 1);

-- 2.3 违规引流
INSERT INTO compliance_rule (rule_code, category, sub_category, rule_name, description, severity, punishment, examples, keywords, patterns, effective_date, status) VALUES
('LIVE_BEHAVIOR_DIVERT', 'live', 'behavior', '违规引流', '引导用户到站外交易', 'high', '封禁 3-7 天',
'["加微信下单", "淘宝搜索XXX", "展示二维码", "展示联系方式", "私下交易"]',
'["加微信", "加VX", "淘宝", "拼多多", "京东", "私下", "线下", "二维码", "扫码"]',
'["微信[：:号]?\\s*[a-zA-Z0-9_-]+", "VX[：:号]?\\s*[a-zA-Z0-9_-]+", "1[3-9]\\d{9}"]',
'2024-01-01', 1);

-- 2.4 恶意营销
INSERT INTO compliance_rule (rule_code, category, sub_category, rule_name, description, severity, punishment, examples, keywords, patterns, effective_date, status) VALUES
('LIVE_BEHAVIOR_SPAM', 'live', 'behavior', '恶意营销', '骚扰用户、恶意竞争', 'medium', '封禁 3-7 天',
'["辱骂竞品", "恶意刷屏", "骚扰用户", "恶意举报"]',
'["垃圾", "骚扰", "恶意", "刷屏"]',
'[]',
'2024-01-01', 1);

-- ============================================
-- 3. 直播商品违规规则
-- ============================================

-- 3.1 假货三无
INSERT INTO compliance_rule (rule_code, category, sub_category, rule_name, description, severity, punishment, examples, keywords, patterns, effective_date, status) VALUES
('LIVE_PRODUCT_FAKE', 'live', 'product', '假货三无产品', '售卖假货、三无产品', 'critical', '永久封禁 + 罚款',
'["无生产日期", "无厂家", "无合格证", "假冒品牌", "山寨产品"]',
'["假货", "三无", "山寨", "仿品", "高仿"]',
'[]',
'2024-01-01', 1);

-- 3.2 禁售商品
INSERT INTO compliance_rule (rule_code, category, sub_category, rule_name, description, severity, punishment, examples, keywords, patterns, effective_date, status) VALUES
('LIVE_PRODUCT_BANNED', 'live', 'product', '禁售商品', '售卖国家禁止的商品', 'critical', '永久封禁',
'["药品（需资质）", "医疗器械（需资质）", "烟草", "野生动物制品", "管制刀具"]',
'["药品", "处方药", "医疗器械", "烟草", "香烟", "野生动物", "管制刀具"]',
'[]',
'2024-01-01', 1);

-- 3.3 虚假发货
INSERT INTO compliance_rule (rule_code, category, sub_category, rule_name, description, severity, punishment, examples, keywords, patterns, effective_date, status) VALUES
('LIVE_PRODUCT_NO_SHIP', 'live', 'product', '虚假发货', '不发货、虚假物流', 'high', '封禁 7-30 天 + 赔偿',
'["下单后不发货", "物流信息造假", "空包裹"]',
'["不发货", "虚假物流", "空包"]',
'[]',
'2024-01-01', 1);

-- 3.4 价格欺诈
INSERT INTO compliance_rule (rule_code, category, sub_category, rule_name, description, severity, punishment, examples, keywords, patterns, effective_date, status) VALUES
('LIVE_PRODUCT_PRICE_FRAUD', 'live', 'product', '价格欺诈', '虚假原价、价格误导', 'high', '封禁 3-7 天 + 罚款',
'["原价999现价99（无证据）", "先提价再打折", "虚假促销"]',
'["原价", "现价", "打折", "促销", "限时"]',
'["原价\\s*\\d+.*现价\\s*\\d+", "\\d+折"]',
'2024-01-01', 1);

-- ============================================
-- 4. 短视频内容违规规则
-- ============================================

-- 4.1 标题党
INSERT INTO compliance_rule (rule_code, category, sub_category, rule_name, description, severity, punishment, examples, keywords, patterns, effective_date, status) VALUES
('VIDEO_CONTENT_CLICKBAIT', 'video', 'content', '标题党', '标题党、误导性标题', 'medium', '限流 + 删除视频',
'["震惊！XXX", "不看后悔", "XXX竟然...", "你绝对想不到"]',
'["震惊", "后悔", "竟然", "想不到", "吓死", "惊呆"]',
'["震惊[！!]", "不看后悔", "竟然[.。！!]", "绝对.*不到"]',
'2024-01-01', 1);

-- 4.2 封面违规
INSERT INTO compliance_rule (rule_code, category, sub_category, rule_name, description, severity, punishment, examples, keywords, patterns, effective_date, status) VALUES
('VIDEO_CONTENT_COVER', 'video', 'content', '封面违规', '封面与内容不符、低俗封面', 'medium', '限流 + 删除视频',
'["性感封面", "血腥封面", "封面与内容不符"]',
'["封面", "标题图"]',
'[]',
'2024-01-01', 1);

-- 4.3 搬运抄袭
INSERT INTO compliance_rule (rule_code, category, sub_category, rule_name, description, severity, punishment, examples, keywords, patterns, effective_date, status) VALUES
('VIDEO_CONTENT_COPY', 'video', 'content', '搬运抄袭', '搬运他人视频、抄袭创意', 'high', '限流 + 删除视频',
'["直接搬运", "去水印", "抄袭创意"]',
'["搬运", "抄袭", "盗用"]',
'[]',
'2024-01-01', 1);

-- ============================================
-- 5. 素材违规规则
-- ============================================

-- 5.1 音乐侵权
INSERT INTO compliance_rule (rule_code, category, sub_category, rule_name, description, severity, punishment, examples, keywords, patterns, effective_date, status) VALUES
('MATERIAL_MUSIC_COPYRIGHT', 'material', 'copyright', '音乐侵权', '使用未授权音乐', 'medium', '删除视频 + 扣分',
'["使用商业音乐", "使用明星歌曲", "未授权背景音乐"]',
'["音乐", "歌曲", "背景音乐", "BGM"]',
'[]',
'2024-01-01', 1);

-- 5.2 图片侵权
INSERT INTO compliance_rule (rule_code, category, sub_category, rule_name, description, severity, punishment, examples, keywords, patterns, effective_date, status) VALUES
('MATERIAL_IMAGE_COPYRIGHT', 'material', 'copyright', '图片侵权', '使用未授权图片', 'medium', '删除视频 + 扣分',
'["使用他人摄影作品", "使用商业图片", "盗用图片"]',
'["图片", "照片", "摄影"]',
'[]',
'2024-01-01', 1);

-- 5.3 视频素材侵权
INSERT INTO compliance_rule (rule_code, category, sub_category, rule_name, description, severity, punishment, examples, keywords, patterns, effective_date, status) VALUES
('MATERIAL_VIDEO_COPYRIGHT', 'material', 'copyright', '视频素材侵权', '使用未授权视频素材', 'high', '删除视频 + 扣分',
'["使用电影片段", "使用电视剧片段", "使用综艺片段"]',
'["电影", "电视剧", "综艺", "片段"]',
'[]',
'2024-01-01', 1);

-- 5.4 字体侵权
INSERT INTO compliance_rule (rule_code, category, sub_category, rule_name, description, severity, punishment, examples, keywords, patterns, effective_date, status) VALUES
('MATERIAL_FONT_COPYRIGHT', 'material', 'copyright', '字体侵权', '使用未授权商业字体', 'low', '删除视频 + 扣分',
'["方正字体", "汉仪字体", "微软雅黑（商用）"]',
'["方正", "汉仪", "微软雅黑", "字体"]',
'[]',
'2024-01-01', 1);

-- ============================================
-- 统计信息
-- ============================================

-- 查看已插入的规则数量
SELECT
    category,
    sub_category,
    severity,
    COUNT(*) as rule_count
FROM compliance_rule
WHERE deleted = 0
GROUP BY category, sub_category, severity
ORDER BY category, sub_category, severity;

-- 查看所有规则
SELECT
    rule_code,
    rule_name,
    category,
    sub_category,
    severity,
    punishment
FROM compliance_rule
WHERE deleted = 0
ORDER BY
    CASE severity
        WHEN 'critical' THEN 1
        WHEN 'high' THEN 2
        WHEN 'medium' THEN 3
        WHEN 'low' THEN 4
    END,
    category,
    sub_category;
