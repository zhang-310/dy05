-- ============================================================
-- script 模块 - 示例数据
-- ============================================================

INSERT INTO script_library (user_id, title, content, category, tags, use_count, status, deleted, create_time, update_time)
VALUES
  (1, '直播开场话术', '大家好，欢迎来到直播间！今天给大家带来超多福利，新进来的朋友先点个关注，不迷路！我们马上开始今天的好物分享~', '直播', '开场,直播,欢迎', 28, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1, '商品介绍话术', '这款产品是我们家的明星爆款，月销10万+，好评率99%。面料是纯棉的，上身特别舒服。今天直播间专属价，只要{价格}，买到就是赚到！', '商品', '商品,介绍,促销', 45, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1, '逼单话术', '最后30秒了家人们！库存只剩最后{数量}件，拍完就没了！犹豫的宝子赶紧下单，错过今天再等半年！', '促销', '逼单,限时,紧迫', 33, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO violation_word (word, level, reason, replacement, status, deleted, create_time, update_time)
VALUES
  ('最好', 2, '绝对化用语，违反广告法', '非常好', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('第一', 3, '绝对化用语，违反广告法', '领先', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('100%', 2, '绝对化用语', '接近100%', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
