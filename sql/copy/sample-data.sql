-- ============================================================
-- copy 模块 - 示例数据
-- 模块：文案库管理（Copy Management）
-- 数据库：PostgreSQL
-- 版本：1.0
-- 更新日期：2026-02-25
-- 说明：此脚本提供示例数据，可选执行
-- ============================================================

-- ============================================================
-- 1. 插入示例文案库数据
-- ============================================================

INSERT INTO copy_library (user_id, title, content, category, tags, word_count, use_count, rating, status, deleted, create_time, update_time)
VALUES
  (1, '夏季T恤火热上新', '这款夏季T恤采用100%纯棉面料，透气舒适，颜色鲜艳饱满。多种尺码可选，适合全年龄段穿着。现在购买享受9折优惠，数量有限，先到先得！', '商品描述', '夏季,T恤,棉,透气', 89, 5, 5, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1, '限时折扣活动通知', '尊敬的用户，我们为您带来年度最大促销活动，全场商品折扣力度前所未有！从日用百货到电子产品，应有尽有。活动时间有限，不要错过这个难得的机会，立即下单享受优惠吧！', '推广', '活动,折扣,促销', 87, 12, 4, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1, '新客户首单优惠', '欢迎新朋友加入我们的大家庭！作为新客户，您将获得独特的首单优惠。使用优惠码"WELCOME2024"，立即享受20%的折扣。此优惠仅限新用户使用，数量限制，请尽快领取！', '营销', '新客,优惠,首单', 82, 8, 5, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, '待审核文案示例', '这是一篇待审核的文案示例，内容关于产品特点和优势的介绍。这篇文案展示了如何有效地传达产品信息给目标客户。', '其他', '示例,待审核', 65, 0, NULL, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================
-- 2. 插入示例文案模板数据
-- ============================================================

INSERT INTO copy_template (user_id, template_name, template_content, category, description, status, deleted, create_time, update_time)
VALUES
  (1, '商品推介模板', '亲爱的{用户名}，这款{产品名}采用{产品特点}工艺，具有{主要优势}特性。现在购买享受{折扣}优惠，库存仅剩{库存数量}件，先到先得！', '商品描述', '通用商品推介模板，包含用户名、产品信息、优惠信息等多个变量', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1, '营销推广模板', '{公司名}诚邀您参加{活动名称}活动！时间：{活动时间}，地点：{活动地点}。参与活动可获得{奖励内容}。更多详情请点击{链接}了解。', '推广', '营销活动推广模板，包含活动信息、奖励等变量', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1, '邮件开场模板', '尊敬的{用户名}，感谢您一直以来对{品牌名}的支持。{问候语}，我们为您准备了{特殊内容}。', '营销', '邮件开场模板，用于邮件营销的开头段落', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================
-- 3. 插入示例审核记录
-- ============================================================

INSERT INTO copy_approval (copy_id, user_id, approval_status, comments, approval_time, deleted, create_time, update_time)
VALUES
  (1, 1, 1, '文案质量优秀，表述清晰。建议发布。', CURRENT_TIMESTAMP - INTERVAL '2 days', 0, CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),
  (2, 1, 1, '推广文案吸引力强，符合品牌调性。', CURRENT_TIMESTAMP - INTERVAL '1 day', 0, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day'),
  (3, 1, 1, '优惠信息清晰，易于理解，建议发布。', CURRENT_TIMESTAMP - INTERVAL '12 hours', 0, CURRENT_TIMESTAMP - INTERVAL '12 hours', CURRENT_TIMESTAMP - INTERVAL '12 hours');

-- ============================================================
-- 注意：
-- 1. 以上示例数据假设至少存在一个 user_id=1 的用户
-- 2. 请根据实际环境修改用户ID和具体内容
-- 3. 生产环境中应谨慎使用此脚本，建议先在测试环境验证
-- ============================================================
