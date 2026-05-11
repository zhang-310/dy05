-- ============================================
-- 抖音违规知识库 - 敏感词库
-- 生成日期: 2026-05-10
-- 说明: 常见敏感词汇，用于快速筛查
-- ============================================

-- ============================================
-- 1. 政治敏感词
-- ============================================
INSERT INTO compliance_keyword (keyword, category, severity, replacement, status) VALUES
-- 政治人物（示例，实际使用时需完善）
('政治敏感词1', 'political', 'critical', NULL, 1),
('政治敏感词2', 'political', 'critical', NULL, 1),
('政治敏感词3', 'political', 'critical', NULL, 1);

-- ============================================
-- 2. 色情低俗词
-- ============================================
INSERT INTO compliance_keyword (keyword, category, severity, replacement, status) VALUES
('色情', 'porn', 'critical', NULL, 1),
('低俗', 'porn', 'high', NULL, 1),
('性暗示', 'porn', 'high', NULL, 1),
('裸露', 'porn', 'critical', NULL, 1),
('挑逗', 'porn', 'high', NULL, 1),
('性感', 'porn', 'medium', NULL, 1),
('诱惑', 'porn', 'medium', NULL, 1);

-- ============================================
-- 3. 暴力血腥词
-- ============================================
INSERT INTO compliance_keyword (keyword, category, severity, replacement, status) VALUES
('暴力', 'violence', 'high', NULL, 1),
('血腥', 'violence', 'high', NULL, 1),
('恐怖', 'violence', 'high', NULL, 1),
('打架', 'violence', 'medium', NULL, 1),
('斗殴', 'violence', 'medium', NULL, 1),
('虐待', 'violence', 'high', NULL, 1),
('杀人', 'violence', 'critical', NULL, 1),
('自杀', 'violence', 'critical', NULL, 1);

-- ============================================
-- 4. 虚假宣传词
-- ============================================
INSERT INTO compliance_keyword (keyword, category, severity, replacement, status) VALUES
('包治百病', 'fraud', 'critical', '有助于改善', 1),
('必瘦', 'fraud', 'critical', '有助于减重', 1),
('100%有效', 'fraud', 'high', '可能有效', 1),
('国家认证', 'fraud', 'high', '符合标准', 1),
('祖传秘方', 'fraud', 'high', '传统配方', 1),
('立竿见影', 'fraud', 'high', '逐步改善', 1),
('神效', 'fraud', 'high', '有效', 1),
('特效', 'fraud', 'high', '有效', 1),
('包治', 'fraud', 'critical', '有助于治疗', 1),
('包好', 'fraud', 'high', '可能改善', 1),
('绝对', 'fraud', 'medium', '可能', 1),
('一定', 'fraud', 'medium', '可能', 1);

-- ============================================
-- 5. 违规引流词
-- ============================================
INSERT INTO compliance_keyword (keyword, category, severity, replacement, status) VALUES
('加微信', 'divert', 'high', '站内咨询', 1),
('加VX', 'divert', 'high', '站内咨询', 1),
('私信我', 'divert', 'medium', '评论区留言', 1),
('淘宝搜索', 'divert', 'high', '抖音小店', 1),
('拼多多', 'divert', 'high', '抖音小店', 1),
('京东', 'divert', 'high', '抖音小店', 1),
('线下交易', 'divert', 'high', '线上下单', 1),
('私下联系', 'divert', 'high', '站内咨询', 1),
('扫码', 'divert', 'high', '点击链接', 1),
('二维码', 'divert', 'high', '商品链接', 1);

-- ============================================
-- 6. 诱导行为词
-- ============================================
INSERT INTO compliance_keyword (keyword, category, severity, replacement, status) VALUES
('关注送', 'induce', 'medium', NULL, 1),
('点赞送', 'induce', 'medium', NULL, 1),
('私信领', 'induce', 'medium', NULL, 1),
('分享送', 'induce', 'medium', NULL, 1),
('转发抽奖', 'induce', 'medium', NULL, 1),
('评论抽奖', 'induce', 'medium', NULL, 1);

-- ============================================
-- 7. 禁售商品词
-- ============================================
INSERT INTO compliance_keyword (keyword, category, severity, replacement, status) VALUES
('处方药', 'banned', 'critical', NULL, 1),
('医疗器械', 'banned', 'critical', NULL, 1),
('烟草', 'banned', 'critical', NULL, 1),
('香烟', 'banned', 'critical', NULL, 1),
('野生动物', 'banned', 'critical', NULL, 1),
('管制刀具', 'banned', 'critical', NULL, 1),
('枪支', 'banned', 'critical', NULL, 1),
('爆炸物', 'banned', 'critical', NULL, 1),
('毒品', 'banned', 'critical', NULL, 1),
('赌博', 'banned', 'critical', NULL, 1);

-- ============================================
-- 统计信息
-- ============================================

-- 查看敏感词数量
SELECT
    category,
    severity,
    COUNT(*) as keyword_count
FROM compliance_keyword
WHERE deleted = 0
GROUP BY category, severity
ORDER BY
    CASE severity
        WHEN 'critical' THEN 1
        WHEN 'high' THEN 2
        WHEN 'medium' THEN 3
        WHEN 'low' THEN 4
    END,
    category;

-- 查看所有敏感词
SELECT
    keyword,
    category,
    severity,
    replacement
FROM compliance_keyword
WHERE deleted = 0
ORDER BY
    CASE severity
        WHEN 'critical' THEN 1
        WHEN 'high' THEN 2
        WHEN 'medium' THEN 3
        WHEN 'low' THEN 4
    END,
    category,
    keyword;
