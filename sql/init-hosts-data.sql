-- ============================================================
-- 5 位主播运营数据初始化
-- 李阳阳、田玲红、王智慧、肖瑶、肖蝉
-- ============================================================

-- 1. 创建用户账号（密码统一 admin123）
INSERT INTO auth_user (username, password_hash, nickname, role_code, status, deleted, create_time, update_time)
SELECT 'liyangyang', '$2a$10$tEp/V/6Ij98O6LgLsCjDXetlMR./FCXgfMcIRB49BcqI/jBnM/5yS', '李阳阳', 'talent', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM auth_user WHERE username = 'liyangyang' AND deleted = 0);

INSERT INTO auth_user (username, password_hash, nickname, role_code, status, deleted, create_time, update_time)
SELECT 'tianlinghong', '$2a$10$tEp/V/6Ij98O6LgLsCjDXetlMR./FCXgfMcIRB49BcqI/jBnM/5yS', '田玲红', 'talent', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM auth_user WHERE username = 'tianlinghong' AND deleted = 0);

INSERT INTO auth_user (username, password_hash, nickname, role_code, status, deleted, create_time, update_time)
SELECT 'wangzhihui', '$2a$10$tEp/V/6Ij98O6LgLsCjDXetlMR./FCXgfMcIRB49BcqI/jBnM/5yS', '王智慧', 'talent', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM auth_user WHERE username = 'wangzhihui' AND deleted = 0);

INSERT INTO auth_user (username, password_hash, nickname, role_code, status, deleted, create_time, update_time)
SELECT 'xiaoyao', '$2a$10$tEp/V/6Ij98O6LgLsCjDXetlMR./FCXgfMcIRB49BcqI/jBnM/5yS', '肖瑶', 'talent', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM auth_user WHERE username = 'xiaoyao' AND deleted = 0);

INSERT INTO auth_user (username, password_hash, nickname, role_code, status, deleted, create_time, update_time)
SELECT 'xiaochan', '$2a$10$tEp/V/6Ij98O6LgLsCjDXetlMR./FCXgfMcIRB49BcqI/jBnM/5yS', '肖蝉', 'talent', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM auth_user WHERE username = 'xiaochan' AND deleted = 0);

-- 2. 创建抖音账号
DO $$
DECLARE
  uid_lyy BIGINT; uid_tlh BIGINT; uid_wzh BIGINT; uid_xy BIGINT; uid_xc BIGINT;
  acc_lyy BIGINT; acc_tlh BIGINT; acc_wzh BIGINT; acc_xy BIGINT; acc_xc BIGINT;
  per_lyy BIGINT; per_tlh BIGINT; per_wzh BIGINT; per_xy BIGINT; per_xc BIGINT;
  sess_lyy BIGINT; sess_tlh BIGINT; sess_wzh BIGINT; sess_xy BIGINT; sess_xc BIGINT;
BEGIN
  SELECT id INTO uid_lyy FROM auth_user WHERE username = 'liyangyang' AND deleted = 0;
  SELECT id INTO uid_tlh FROM auth_user WHERE username = 'tianlinghong' AND deleted = 0;
  SELECT id INTO uid_wzh FROM auth_user WHERE username = 'wangzhihui' AND deleted = 0;
  SELECT id INTO uid_xy FROM auth_user WHERE username = 'xiaoyao' AND deleted = 0;
  SELECT id INTO uid_xc FROM auth_user WHERE username = 'xiaochan' AND deleted = 0;

  RAISE NOTICE '用户ID: 李阳阳=%, 田玲红=%, 王智慧=%, 肖瑶=%, 肖蝉=%', uid_lyy, uid_tlh, uid_wzh, uid_xy, uid_xc;

  -- 2.1 抖音账号
  INSERT INTO douyin_account (user_id, account_name, account_id, fan_count, video_count, total_likes, description, status, deleted)
  SELECT uid_lyy, '阳阳护肤小课堂', 'dy_liyangyang_001', 42000, 128, 260000, '护肤品成分解析、套盒开箱、实测分享', 1, 0
  WHERE NOT EXISTS (SELECT 1 FROM douyin_account WHERE account_id = 'dy_liyangyang_001' AND deleted = 0)
  RETURNING id INTO acc_lyy;
  IF acc_lyy IS NULL THEN SELECT id INTO acc_lyy FROM douyin_account WHERE account_id = 'dy_liyangyang_001' AND deleted = 0; END IF;

  INSERT INTO douyin_account (user_id, account_name, account_id, fan_count, video_count, total_likes, description, status, deleted)
  SELECT uid_tlh, '田总护肤严选', 'dy_tianlinghong_001', 185000, 356, 1200000, '田总严选·院线护肤·专属福利·品质保证', 1, 0
  WHERE NOT EXISTS (SELECT 1 FROM douyin_account WHERE account_id = 'dy_tianlinghong_001' AND deleted = 0)
  RETURNING id INTO acc_tlh;
  IF acc_tlh IS NULL THEN SELECT id INTO acc_tlh FROM douyin_account WHERE account_id = 'dy_tianlinghong_001' AND deleted = 0; END IF;

  INSERT INTO douyin_account (user_id, account_name, account_id, fan_count, video_count, total_likes, description, status, deleted)
  SELECT uid_wzh, '智慧说护肤', 'dy_wangzhihui_001', 68000, 215, 450000, '护肤品科学测评·成分党·理性种草', 1, 0
  WHERE NOT EXISTS (SELECT 1 FROM douyin_account WHERE account_id = 'dy_wangzhihui_001' AND deleted = 0)
  RETURNING id INTO acc_wzh;
  IF acc_wzh IS NULL THEN SELECT id INTO acc_wzh FROM douyin_account WHERE account_id = 'dy_wangzhihui_001' AND deleted = 0; END IF;

  INSERT INTO douyin_account (user_id, account_name, account_id, fan_count, video_count, total_likes, description, status, deleted)
  SELECT uid_xy, '瑶瑶的变美日记', 'dy_xiaoyao_001', 35000, 96, 180000, '00后美妆博主·平价好物·学生党护肤', 1, 0
  WHERE NOT EXISTS (SELECT 1 FROM douyin_account WHERE account_id = 'dy_xiaoyao_001' AND deleted = 0)
  RETURNING id INTO acc_xy;
  IF acc_xy IS NULL THEN SELECT id INTO acc_xy FROM douyin_account WHERE account_id = 'dy_xiaoyao_001' AND deleted = 0; END IF;

  INSERT INTO douyin_account (user_id, account_name, account_id, fan_count, video_count, total_likes, description, status, deleted)
  SELECT uid_xc, '蝉姐聊生活', 'dy_xiaochan_001', 92000, 280, 620000, '蝉姐·聊家常·聊感情·偶尔带个好物', 1, 0
  WHERE NOT EXISTS (SELECT 1 FROM douyin_account WHERE account_id = 'dy_xiaochan_001' AND deleted = 0)
  RETURNING id INTO acc_xc;
  IF acc_xc IS NULL THEN SELECT id INTO acc_xc FROM douyin_account WHERE account_id = 'dy_xiaochan_001' AND deleted = 0; END IF;

  -- 3. 创建人设
  INSERT INTO dy_persona (owner_id, account_id, persona_name, persona_type, description, tone, target_audience, content_style, keywords, is_default, status, deleted)
  VALUES
    (uid_lyy, acc_lyy, '活力护肤体验官', 'commerce', '热情开朗的护肤品体验官，擅长用亲身试用+成分解读打动观众，语速偏快，充满活力，善于用对比和反差感制造种草效果', 'passionate', '25-40岁女性，追求性价比护肤', '先试用展示→再讲成分→数据对比→限时福利', '护肤,套盒,试用,成分,性价比', 1, 1, 0),
    (uid_tlh, acc_tlh, '田总·院线护肤专家', 'commerce', '资深护肤品创业者，10年行业经验，自有供应链，主打院线品质+工厂价。说话干练有气场，擅长用专业背书+限量策略成交。粉丝称"田总"', 'professional', '28-45岁女性，中高消费力，重品质', '专业背书→独家货源→限量策略→宠粉福利', '田总,专属,院线,抗皱,紧致,限量', 1, 1, 0),
    (uid_wzh, acc_wzh, '成分党科普达人', 'knowledge', '理性温和的护肤科普博主，有化妆品配方师背景，善于用通俗语言解释复杂成分，不夸大不忽悠，以数据和原理说服观众', 'professional', '22-38岁女性，注重成分和功效', '科普成分→原理解析→产品验证→理性推荐', '成分,配方,科学护肤,玻色因,烟酰胺,377', 1, 1, 0),
    (uid_xy, acc_xy, '元气少女种草机', 'commerce', '00后活泼女生，说话自带可爱感和感染力，善于分享平价好物和学生党护肤心得，互动性强，喜欢和粉丝聊天', 'casual', '18-28岁女性，学生和年轻上班族', '日常分享→亲测好物→闺蜜式推荐→抽奖互动', '平价,学生党,好物分享,种草,闺蜜', 1, 1, 0),
    (uid_xc, acc_xc, '蝉姐·知心大姐姐', 'entertainment', '35+温柔知性大姐姐，擅长聊家常、讲生活感悟、分享婆媳/夫妻/育儿话题，在聊天中自然植入好物推荐，粉丝黏性极高', 'warm', '30-50岁女性，家庭主妇/职场妈妈', '聊家常→情感共鸣→生活感悟→顺带推荐好物', '聊天,感悟,家庭,婆媳,育儿,好物', 1, 1, 0)
  ON CONFLICT DO NOTHING
  RETURNING id INTO per_lyy;

  -- 获取人设 ID
  SELECT id INTO per_lyy FROM dy_persona WHERE owner_id = uid_lyy AND persona_name = '活力护肤体验官' AND deleted = 0 LIMIT 1;
  SELECT id INTO per_tlh FROM dy_persona WHERE owner_id = uid_tlh AND persona_name = '田总·院线护肤专家' AND deleted = 0 LIMIT 1;
  SELECT id INTO per_wzh FROM dy_persona WHERE owner_id = uid_wzh AND persona_name = '成分党科普达人' AND deleted = 0 LIMIT 1;
  SELECT id INTO per_xy FROM dy_persona WHERE owner_id = uid_xy AND persona_name = '元气少女种草机' AND deleted = 0 LIMIT 1;
  SELECT id INTO per_xc FROM dy_persona WHERE owner_id = uid_xc AND persona_name = '蝉姐·知心大姐姐' AND deleted = 0 LIMIT 1;

  RAISE NOTICE '人设ID: 李阳阳=%, 田玲红=%, 王智慧=%, 肖瑶=%, 肖蝉=%', per_lyy, per_tlh, per_wzh, per_xy, per_xc;

  -- 4. 为每人复制商品（从 admin 的商品池中分配）
  -- 李阳阳：平价套盒为主（K+B 类，10-50元）
  INSERT INTO dy_product (user_id, product_name, product_category, price, cost_price, description, status, deleted, create_time, update_time)
  VALUES
    (uid_lyy, '致朵绿钻嫩肤清颜五件套保湿套装', 'K', 8.00, 3.00, '入门级保湿套盒，适合干皮入门', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_lyy, '致朵红石榴鲜润水嫩六件套', 'K', 10.00, 4.00, '红石榴抗氧化，补水保湿', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_lyy, '鱼子酱柔肤致颜六件套礼盒', 'K', 18.00, 7.00, '鱼子酱精华，紧致抗皱', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_lyy, '韩束红蛮腰水乳组合80ml', 'B', 39.90, 18.00, '补水保湿紧致抗皱淡纹提亮', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_lyy, 'KANS/韩束黑耀晶采精华水+乳+霜', 'B', 49.00, 22.00, '大牌品质，精华级护理', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_lyy, 'SIAYZU红石榴多肽保湿臻享礼盒', 'B', 39.90, 16.00, '红石榴多肽，深层保湿', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_lyy, 'PLER胶原蛋白玻尿酸盈润舒缓修护套盒', 'R', 69.00, 25.00, '胶原蛋白+玻尿酸双效修护', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_lyy, '兰蔻全新清滢保湿柔肤水125ml粉水', 'B', 36.90, 15.00, '兰蔻粉水，保湿补水紧致', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

  -- 田玲红（田总）：田总专属产品+高端品
  INSERT INTO dy_product (user_id, product_name, product_category, price, cost_price, description, status, deleted, create_time, update_time)
  VALUES
    (uid_tlh, '【田总专属】海茴香极光水乳', 'R', 49.90, 15.00, '不添加一滴水，紧致抗皱淡纹，提亮肤色', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_tlh, '【田总专属】SP水乳黑金胶原多肽白细胞抗皱冻龄水乳', 'R', 39.90, 12.00, '黑金胶原多肽，抗皱冻龄', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_tlh, '【田总专属】DIDK金致多肽花蜜臻养护手霜', 'B', 10.00, 3.00, '多肽花蜜，滋养修护双手', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_tlh, '【田总专属】肌采美白祛斑抗皱面膜（白猫眼）', 'B', 19.90, 6.00, '美白祛斑抗皱三效合一面膜', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_tlh, '【田总专属】佰珍堂藏红花酵萃抗皱紧致舒颜护肤套装', 'K', 25.00, 8.00, '藏红花精粹，抗皱紧致', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_tlh, '【田总专属】法颜蔻玻色因抗皱紧致护肤礼盒', 'K', 25.00, 8.00, '法国配方，玻色因抗皱紧致', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_tlh, '【田总专属】韩束光感莹透水嫩紧致礼盒套装', 'B', 138.00, 55.00, '韩束正品，光感莹透紧致', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_tlh, '【田总专属】美肤宝水份源礼盒5件套盒', 'B', 89.00, 35.00, '深层补水保湿精华乳套盒', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_tlh, '【田总粉丝专属福利】BPM赋能滋养淡纹红宝石水乳精华面霜四件套', 'B', 29.90, 10.00, '红宝石四件套，滋养淡纹', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_tlh, '【田总专属】LRNAS奢宠抗皱润颜双效滋润礼盒', 'K', 22.00, 7.00, '奢宠级抗皱滋润护理', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

  -- 王智慧：成分党偏好品（玻色因/377/多肽等功效成分）
  INSERT INTO dy_product (user_id, product_name, product_category, price, cost_price, description, status, deleted, create_time, update_time)
  VALUES
    (uid_wzh, '【377】肌采金钻美白祛斑抗皱七件套', 'R', 79.90, 28.00, '377成分，美白祛斑抗皱', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_wzh, '美尼姿玻色因胶原蛋白紧致抗皱面霜', 'R', 29.90, 10.00, '玻色因+胶原蛋白双效抗皱', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_wzh, 'CKEK肌采美白祛斑淡黑抗皱眼霜(377金)', 'B', 29.90, 12.00, '377配方眼霜，淡化细纹黑眼圈', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_wzh, '英国Mircutee玻色因活胶原鱼子微囊紧致舒缓溶纹水', 'B', 29.90, 12.00, '玻色因14天抗皱精华水', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_wzh, '柏兰梦十效合一577精华液', 'C', 13.14, 5.00, '美白祛斑抗皱紧致嫩肤多效合一', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_wzh, 'PLER肌采美白祛斑修护精华液', 'B', 29.90, 11.00, '美白祛斑修护三效精华', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_wzh, '初律玻尿酸原B5润颜精粹水', 'R', 22.90, 8.00, '玻尿酸+B5，深层补水', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_wzh, '珀莱雅红宝石洁水乳套装紧致抗皱', 'B', 68.90, 30.00, '珀莱雅红宝石系列，紧致保湿', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

  -- 肖瑶：平价好物+年轻化产品
  INSERT INTO dy_product (user_id, product_name, product_category, price, cost_price, description, status, deleted, create_time, update_time)
  VALUES
    (uid_xy, '致朵璀璨亮光定制护肤礼盒', 'B', 11.90, 4.00, '亮肤定制礼盒，学生党入门', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_xy, 'VEZE梵贞肤研美白祛斑套盒', 'K', 20.00, 7.00, '美白祛斑套盒，性价比超高', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_xy, '雅缇诗白松露栀子花精油香氛沐浴露', 'F', 19.90, 7.00, '白松露精油沐浴露，香氛持久', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_xy, '雅缇诗鱼子酱氨基酸控油去屑洗发水800ml', 'B', 19.90, 8.00, '氨基酸洗发水，控油去屑', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_xy, 'PLER山茶花八重氨基酸控油净透洁面乳', 'R', 13.14, 5.00, '山茶花洁面，温和控油', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_xy, 'CKEK玫瑰沁润修护精华面膜', 'R', 39.90, 14.00, '玫瑰精华面膜，修护保湿', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_xy, 'gooben/果本坚果依克多因奢漾礼盒', 'B', 49.00, 20.00, '果本坚果护肤礼盒，补水保湿', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_xy, '致朵视黄醇臻养沁颜十三件套', 'K', 15.00, 5.00, '视黄醇抗初老，13件超值套装', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

  -- 肖蝉：内容带货型（少量品，高停留）
  INSERT INTO dy_product (user_id, product_name, product_category, price, cost_price, description, status, deleted, create_time, update_time)
  VALUES
    (uid_xc, '芈汐玻色因虫草冻龄抗皱紧致水乳霜洁面四件套', 'B', 39.90, 15.00, '虫草冻龄，30+女性必备', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_xc, '百雀羚弹嫩护肤焕亮精华水乳霜礼盒', 'B', 76.90, 30.00, '百雀羚经典系列，舒缓保湿', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_xc, '美肤宝自然白护肤品套盒美白淡斑', 'B', 89.00, 35.00, '美肤宝国货，美白淡斑提亮', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

  -- 5. 创建直播场次
  -- 李阳阳：标准带货场（heavy_paid_category）
  INSERT INTO live_session (user_id, account_id, persona_id, live_title, script_style, live_format, session_type, live_description, scheduled_time, status, deleted, create_time, update_time)
  VALUES (uid_lyy, acc_lyy, per_lyy, '阳阳护肤实测专场｜套盒开箱全实拍', 'passionate', 'heavy_paid_category', 'standard',
    '今晚8点！8款护肤套盒逐一开箱实测，全程真脸上手，好不好用看效果！粉丝专属价+买一送一',
    CURRENT_TIMESTAMP + INTERVAL '1 day' + INTERVAL '20 hours', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  RETURNING id INTO sess_lyy;

  -- 田玲红（田总）：标准带货场（heavy_paid_category）
  INSERT INTO live_session (user_id, account_id, persona_id, live_title, script_style, live_format, session_type, live_description, scheduled_time, status, deleted, create_time, update_time)
  VALUES (uid_tlh, acc_tlh, per_tlh, '田总严选｜院线级护肤专属福利夜', 'professional', 'heavy_paid_category', 'standard',
    '田总亲选10款院线护肤品，全部工厂直供价！海茴香水乳/黑金冻龄/藏红花套盒全线开放，限时限量，错过等半年',
    CURRENT_TIMESTAMP + INTERVAL '1 day' + INTERVAL '19 hours', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  RETURNING id INTO sess_tlh;

  -- 王智慧：内容直播场（content_led）
  INSERT INTO live_session (user_id, account_id, persona_id, live_title, script_style, live_format, session_type, live_description, scheduled_time, status, deleted, create_time, update_time)
  VALUES (uid_wzh, acc_wzh, per_wzh, '成分党深度解析｜玻色因vs377到底怎么选', 'professional', 'content_led', 'standard',
    '今晚聊聊护肤界两大顶流成分：玻色因和377，谁抗皱谁美白？适合什么肤质？顺便看看几个含这俩成分的好产品',
    CURRENT_TIMESTAMP + INTERVAL '2 days' + INTERVAL '20 hours', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  RETURNING id INTO sess_wzh;

  -- 肖瑶：种草带货场（organic_micro_paid）
  INSERT INTO live_session (user_id, account_id, persona_id, live_title, script_style, live_format, session_type, live_description, scheduled_time, status, deleted, create_time, update_time)
  VALUES (uid_xy, acc_xy, per_xy, '学生党平价护肤｜100元搞定全套！', 'friendly', 'organic_micro_paid', 'standard',
    '姐妹们！今天给你们搭配一套100块以内的全套护肤方案，洁面+水+乳+面膜+沐浴露全齐，每一个都是我亲测回购的！',
    CURRENT_TIMESTAMP + INTERVAL '2 days' + INTERVAL '19 hours' + INTERVAL '30 minutes', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  RETURNING id INTO sess_xy;

  -- 肖蝉：内容带货场（content_commerce，2小时聊天式）
  INSERT INTO live_session (user_id, account_id, persona_id, live_title, script_style, live_format, session_type, live_description, scheduled_time, status, deleted, create_time, update_time)
  VALUES (uid_xc, acc_xc, per_xc, '蝉姐夜话｜聊聊婆媳那些事+好物分享', 'friendly', 'content_commerce', 'chat_2h',
    '今晚蝉姐陪你聊聊天，婆媳关系/夫妻相处/育儿烦恼通通可以聊！中间穿插几个蝉姐自用的护肤好物推荐，都是30+姐妹必备的',
    CURRENT_TIMESTAMP + INTERVAL '1 day' + INTERVAL '20 hours' + INTERVAL '30 minutes', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  RETURNING id INTO sess_xc;

  RAISE NOTICE '场次ID: 李阳阳=%, 田玲红=%, 王智慧=%, 肖瑶=%, 肖蝉=%', sess_lyy, sess_tlh, sess_wzh, sess_xy, sess_xc;

  -- 6. 绑定商品到场次
  -- 李阳阳：8 个商品
  INSERT INTO live_product (session_id, product_id, product_name, position, deleted, create_time)
  SELECT sess_lyy, p.id, p.product_name, ROW_NUMBER() OVER (ORDER BY p.id), 0, CURRENT_TIMESTAMP
  FROM dy_product p WHERE p.user_id = uid_lyy AND p.deleted = 0;

  -- 田玲红：10 个商品
  INSERT INTO live_product (session_id, product_id, product_name, position, deleted, create_time)
  SELECT sess_tlh, p.id, p.product_name, ROW_NUMBER() OVER (ORDER BY p.id), 0, CURRENT_TIMESTAMP
  FROM dy_product p WHERE p.user_id = uid_tlh AND p.deleted = 0;

  -- 王智慧：8 个商品
  INSERT INTO live_product (session_id, product_id, product_name, position, deleted, create_time)
  SELECT sess_wzh, p.id, p.product_name, ROW_NUMBER() OVER (ORDER BY p.id), 0, CURRENT_TIMESTAMP
  FROM dy_product p WHERE p.user_id = uid_wzh AND p.deleted = 0;

  -- 肖瑶：8 个商品
  INSERT INTO live_product (session_id, product_id, product_name, position, deleted, create_time)
  SELECT sess_xy, p.id, p.product_name, ROW_NUMBER() OVER (ORDER BY p.id), 0, CURRENT_TIMESTAMP
  FROM dy_product p WHERE p.user_id = uid_xy AND p.deleted = 0;

  -- 肖蝉：3 个商品（内容带货型，少量品）
  INSERT INTO live_product (session_id, product_id, product_name, position, deleted, create_time)
  SELECT sess_xc, p.id, p.product_name, ROW_NUMBER() OVER (ORDER BY p.id), 0, CURRENT_TIMESTAMP
  FROM dy_product p WHERE p.user_id = uid_xc AND p.deleted = 0;

  -- 7. 创建话术槽位
  -- 7.1 李阳阳：heavy_paid_category 标准结构（开场+8品+7转场+结尾=17槽）
  INSERT INTO live_script (session_id, user_id, script_type, script_content, requirement, duration_limit_sec, sequence_no, deleted, create_time, update_time)
  VALUES
    (sess_lyy, uid_lyy, 'opening', '家人们晚上好！我是阳阳，欢迎来到护肤实测直播间！今晚给大家准备了8款超火的护肤套盒，每一款我都亲自上脸试过了！不吹不黑，好用才推荐！先点个关注不迷路，今晚全场买一送一，还有无门槛红包雨！来来来，先给阳阳扣个1，看看今晚多少姐妹在！', NULL, 60, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'product', '第一款来了！致朵绿钻嫩肤清颜五件套！姐妹们看这个包装，8块钱5件套，你没听错，8块钱！我上手给你们试试啊——这个水质地很清爽，一点都不黏腻，乳液推开秒吸收！干皮姐妹入门首选，8块钱买不了吃亏买不了上当！1号链接，赶紧拍！', '亏品快速过', 45, 2, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'transition', '好，第一款拍到的姐妹扣个"拍了"！没拍到的别急，后面还有更猛的！下一款可是大品牌哦~', NULL, 15, 3, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'product', '姐妹们看这个！致朵红石榴六件套！红石榴是什么？抗氧化之王啊！这个套盒10块钱，水+乳+霜+精华+洁面+面膜全齐了，你去专柜随便买一瓶红石榴的都不止这个价！我涂给你们看啊，这个质地……你看，多滋润！10块钱，闭眼入！2号链接！', '亏品快速过', 45, 4, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'transition', '好！两款亏品已经上完了，接下来给大家上点大牌好货！想看大牌的扣"大牌"！', NULL, 15, 5, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'product', '来了来了！鱼子酱六件套礼盒！鱼子酱姐妹们知道吧？贵妇级成分！这个套盒18块钱，鱼子酱精华+肽+玻尿酸三重配方，紧致抗皱的效果杠杠的！我已经用了一个月了，你们看我这个法令纹是不是淡了很多？18块，3号链接，手慢无！', '利润品重点推', 90, 6, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'transition', '好嘞！鱼子酱拍到的姐妹恭喜你们赚到了！接下来这款可是韩束，正品行货！', NULL, 15, 7, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'product', '韩束红蛮腰！姐妹们这个不用我多说了吧？韩束当家花旦！80ml水乳组合，补水保湿紧致抗皱淡纹提亮，一套搞定所有护肤需求！专柜卖多少钱？我们直播间39.9！而且今天买还送同款小样！4号链接，赶紧锁单！', '爆品深度讲', 180, 8, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'transition', '韩束抢到的姐妹太幸运了！39.9的韩束在哪找？只有阳阳直播间！下一款也是大牌哦~', NULL, 15, 9, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'product', '来！韩束黑耀晶采！精华水+乳+霜三件套！这个系列主打提亮焕肤，用了之后皮肤会有那种自内而外的光泽感！49块钱买韩束三件套，姐妹们这个价格我自己都觉得不可思议！5号链接！', '爆品深度讲', 120, 10, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'transition', '好！韩束两款都上完了，接下来给你们来个红石榴多肽！最近超火的！', NULL, 15, 11, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'product', 'SIAYZU红石榴多肽保湿臻享礼盒！这个牌子最近抖音超火的，红石榴+多肽双重配方，补水锁水一步到位！礼盒包装送人也特别有面子，自用送人两相宜！39.9！6号链接！', '利润品推荐', 90, 12, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'transition', '好！再来一个重磅的！PLER胶原蛋白！30+姐妹必看！', NULL, 15, 13, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'product', 'PLER胶原蛋白玻尿酸盈润舒缓修护套盒！胶原蛋白+玻尿酸，黄金搭档！修护屏障、抗皱保湿、舒缓泛红一套搞定！69块，你买任何一个大牌的胶原蛋白单品都不止这个价！7号链接！', '利润品重点推', 120, 14, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'transition', '最后一款！压轴的来了！兰蔻！对，你没听错，兰蔻！', NULL, 15, 15, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'product', '兰蔻粉水！新版清滢保湿柔肤水125ml！这个不用我多介绍了吧？兰蔻殿堂级保湿！专柜价420，我们直播间36.9！没有多的，限量50瓶，抢完下架！8号链接！3、2、1，上链接！', '爆品限量冲', 120, 16, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'closing', '好啦家人们！今晚8款全部上完了！拍到的姐妹赶紧去付款，有问题随时找客服！记得点关注，明天还有更多好物！爱你们，么么哒～晚安！', NULL, 45, 17, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

  -- 7.2 田玲红（田总）：heavy_paid_category 标准结构（开场+10品+9转场+结尾=21槽）
  INSERT INTO live_script (session_id, user_id, script_type, script_content, requirement, duration_limit_sec, sequence_no, deleted, create_time, update_time)
  VALUES
    (sess_tlh, uid_tlh, 'opening', '各位姐妹晚上好，我是田总。欢迎来到田总严选直播间。今晚给大家准备了10款院线级护肤品，全部是我亲自去工厂谈的价格，绕过所有中间商。你们在市面上买不到这个价格的。闲话少说，直接上货。新来的姐妹先点个关注，田总这里不忽悠，品质说话。', NULL, 60, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'product', '第一款，海茴香极光水乳。这款水乳是我找了法国原料商定制的配方，不添加一滴水，全部用海茴香萃取液替代。你们去查一下海茴香这个成分——紧致抗皱、淡纹提亮，医美级别的原料。专柜同类产品至少300+，田总给你们直接49.9。1号链接，限量200套。', '利润品主推', 120, 2, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'transition', '好，海茴香拍到的姐妹你们赚到了。下一款比这个更硬核——黑金胶原多肽。', NULL, 15, 3, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'product', 'SP水乳黑金胶原多肽。这款主打抗皱冻龄，用的是日本进口的胶原多肽原料。我自己用了3个月，你们看我这个颈纹——之前比这深多了。39.9一套水乳，这个价格我只能保三天。2号链接。', '利润品主推', 90, 4, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'transition', '两款利润品上完了。接下来给你们上个福利品，田总宠粉时间到了。', NULL, 15, 5, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'product', 'DIDK金致多肽花蜜臻养护手霜。这个护手霜是田总专门找供应商定制的，多肽+花蜜双效滋养。才10块钱，你们买个普通护手霜都不止这个价。3号链接，一人限拍3支。', '亏品引流', 30, 6, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'transition', '10块钱的护手霜拍了吗？好，下一款面膜来了。', NULL, 10, 7, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'product', '肌采美白祛斑抗皱面膜，白猫眼系列。美白祛斑抗皱三效合一，里面的精华液超多，一片相当于半瓶精华的量。19.9一盒5片，平均一片不到4块钱。4号链接。', '爆品推荐', 60, 8, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'transition', '面膜拍到了吗？接下来两款套盒，都是大气上档次的礼盒装。', NULL, 10, 9, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'product', '佰珍堂藏红花酵萃抗皱紧致舒颜护肤套装。藏红花这个原料你们知道有多贵吗？西藏产的，一克比黄金还贵。这个套盒把藏红花精粹融入了全线护肤品。25块钱，5号链接。', '亏品引流', 60, 10, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'transition', '藏红花拍完了。下一款法国配方的，成分更硬核。', NULL, 10, 11, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'product', '法颜蔻玻色因抗皱紧致护肤礼盒。玻色因这个成分是欧莱雅集团的专利原料，现在专利到期了，很多品牌都在用。这个礼盒用了高浓度玻色因，25块钱。6号链接。', '亏品引流', 60, 12, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'transition', '好，三款亏品上完了。接下来上大牌——韩束正品行货。', NULL, 10, 13, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'product', '韩束光感莹透水嫩紧致礼盒套装！正品全码可验货。这个套盒专柜价398，田总拿到的渠道价138。光感系列主打提亮紧致，适合25-40岁所有肤质。7号链接。', '爆品深度推', 180, 14, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'transition', '韩束抢到的姐妹恭喜。下一款美肤宝也是大牌。', NULL, 10, 15, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'product', '美肤宝水份源礼盒5件套。深层补水保湿，这个系列我自己用了两年了。89块5件套，专柜至少翻3倍。8号链接。', '爆品推荐', 90, 16, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'transition', '美肤宝拍好了。最后两款，全是田总粉丝专属福利价。', NULL, 10, 17, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'product', 'BPM赋能滋养淡纹红宝石水乳精华面霜四件套。田总粉丝专属福利！红宝石系列，主打滋养淡纹。29.9四件套，9号链接。', '利润品推荐', 60, 18, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'transition', '好，最后一款压轴的来了。', NULL, 10, 19, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'product', 'LRNAS奢宠抗皱润颜双效滋润礼盒。奢宠级抗皱滋润，22块钱的礼盒，你去哪找？最后一款亏品，田总今晚就是交个朋友。10号链接。', '亏品收尾', 45, 20, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'closing', '好了姐妹们，今晚10款全部上完了。拍到的赶紧付款，48小时不付款自动取消。有任何问题找客服，田总的售后你们放心。感谢今晚所有姐妹的支持，明天同一时间再见。', NULL, 45, 21, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

  -- 7.3 王智慧：content_led 标准结构（开场+8品+7转场+结尾，穿插内容）
  INSERT INTO live_script (session_id, user_id, script_type, script_content, requirement, duration_limit_sec, sequence_no, deleted, create_time, update_time)
  VALUES
    (sess_wzh, uid_wzh, 'opening', '大家好，我是智慧。今天这场直播主要跟大家聊一个话题——玻色因和377，这两个成分到底怎么选。我看到很多姐妹在评论区问我，说搞不清楚这两个成分的区别。没关系，今天我用最通俗的方式给你们讲清楚。先点个关注，有问题随时在公屏打出来，我看到就回答。', NULL, 90, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'product', '先说377。377的全名叫苯乙基间苯二酚，是目前最强的美白成分之一，比熊果苷强16倍。它的作用机理是抑制酪氨酸酶的活性，从根源上减少黑色素生成。适合有色斑、暗沉困扰的姐妹。我手边这款肌采金钻美白祛斑七件套就是高浓度377配方。你们看这个膏体——很细腻，上脸完全不闷。79.9七件套，1号链接有。', '科普+产品验证', 180, 2, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'transition', '刚才有姐妹问377会不会刺激？好问题。377本身刺激性很低，但浓度超过2%可能会有轻微刺痒，敏感肌建议先在耳后试用。好，接下来说玻色因。', NULL, 30, 3, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'product', '玻色因，学名羟丙基四氢吡喃三醇，是欧莱雅集团研发的专利成分。它的核心功效是促进糖胺聚糖的合成——简单说就是让真皮层变"饱满"，从而减少皱纹。这款美尼姿玻色因面霜用了5%的玻色因浓度，算是比较高的了。29.9一瓶，2号链接，适合想抗初老的姐妹。', '科普+产品验证', 180, 4, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'transition', '所以简单总结：美白选377，抗皱选玻色因。两个不冲突，可以搭配使用。眼部护理的话，看下一款。', NULL, 30, 5, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'product', '肌采377金眼霜。眼周皮肤最薄，也最容易出细纹和黑眼圈。这款眼霜用了377+胜肽双重配方，淡化黑眼圈+抗细纹。我自己用了2个月了，效果确实有。29.9，3号链接。', '产品推荐', 90, 6, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'transition', '有姐妹问有没有精华水推荐？有的，下面这款是含玻色因的精华水。', NULL, 20, 7, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'product', 'Mircutee玻色因活胶原鱼子微囊紧致舒缓溶纹水。这个品牌是英国的，玻色因+鱼子酱双效。14天密集修护，适合换季敏感期使用。29.9，4号链接。', '产品推荐', 60, 8, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'transition', '说完了单成分产品，再看看多效合一的。有些姐妹护肤步骤不想太多，一瓶搞定也OK。', NULL, 20, 9, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'product', '柏兰梦577精华液。注意看，是577不是377——它在377的基础上加了5%烟酰胺和7%VC衍生物，美白+祛斑+抗皱+紧致+嫩肤十效合一。13.14一瓶，控单价。5号链接。', '控单产品分析', 90, 10, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'transition', '刚才有姐妹问祛斑精华液推荐。PLER这款不错，我详细说说。', NULL, 20, 11, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'product', 'PLER肌采美白祛斑修护精华液。这款的配方表我仔细看过，主要活性成分是377+烟酰胺+光甘草定。三重美白成分叠加，祛斑效果会比单一成分更明显。29.9，6号链接。', '成分分析推荐', 90, 12, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'transition', '讲了这么多高浓度活性成分，补水保湿也不能忘。', NULL, 20, 13, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'product', '初律玻尿酸原B5润颜精粹水。玻尿酸负责补水，B5负责修护屏障。两个成分配合，补水的同时锁住水分。22.9一瓶，7号链接。适合所有肤质。', '产品推荐', 60, 14, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'transition', '最后一款，给想要一整套解决方案的姐妹准备的。', NULL, 15, 15, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'product', '珀莱雅红宝石水乳套装。珀莱雅现在是国货之光了，红宝石系列主打紧致抗皱，核心成分是六胜肽+维A醇。68.9一套，8号链接。国货品质，这个价格真的很良心。', '爆品推荐', 120, 16, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'closing', '好了姐妹们，今天的成分科普+产品推荐就到这里。总结一下：美白祛斑选377系列，抗皱紧致选玻色因系列，两个可以搭配用。有问题随时来直播间问我，我每周二四六晚8点开播。感谢大家，晚安。', NULL, 60, 17, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

  -- 7.4 肖瑶：organic_micro_paid 标准结构（开场+8品+7转场+结尾=17槽）
  INSERT INTO live_script (session_id, user_id, script_type, script_content, requirement, duration_limit_sec, sequence_no, deleted, create_time, update_time)
  VALUES
    (sess_xy, uid_xy, 'opening', '哈喽姐妹们！瑶瑶来啦！今天这场直播超级实用——我给你们搭配了一套100块以内搞定的全套护肤方案！从洁面到面膜到沐浴露全都有！每一个都是我自己回购过的，没有广告费，纯分享！关注瑶瑶，今晚抽3个姐妹送全套小样！先扣个"来了"让我看看人气~', NULL, 60, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'product', '第一个，洁面！致朵璀璨亮光定制护肤礼盒，11.9！姐妹们不要看价格便宜就觉得不好用，这个我用了两管了！洗完脸不紧绷不假滑，而且它是礼盒装的，送朋友也不丢面子！1号链接~', '闺蜜式种草', 90, 2, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'transition', '好嘞！洁面搞定了才11.9，省下来的钱买水乳！接着看~', NULL, 15, 3, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'product', 'VEZE梵贞美白祛斑套盒！20块钱的套盒，水+乳+霜都有了！我室友用了两周跟我说皮肤亮了一个色号，我当时还不信，后来自己也买了一套试，还真的！学生党美白入门首选！2号链接！', '闺蜜式种草', 90, 4, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'transition', '洁面+水乳霜加起来才31.9！还不到一杯奶茶+一顿外卖的钱！继续看~', NULL, 15, 5, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'product', '雅缇诗白松露栀子花精油香氛沐浴露！这个沐浴露我吹爆！洗完身上是栀子花的味道，持续到第二天早上都还有！而且泡沫超绵密，19.9一大瓶！3号链接！姐妹们冲！', '体验分享', 90, 6, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'transition', '身体洗护搞定了！头发也不能忘！下面这个洗发水也太好用了——', NULL, 15, 7, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'product', '雅缇诗鱼子酱氨基酸洗发水800ml！氨基酸的！控油去屑！而且800ml超大瓶，能用好久！我油头星人用了之后头发蓬松了好多！19.9！4号链接！', '体验分享', 60, 8, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'transition', '目前加起来71.7！还有30块钱预算，够买洁面乳+面膜！', NULL, 15, 9, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'product', 'PLER山茶花氨基酸洁面乳！13.14一支！山茶花+八重氨基酸，温和洁面不伤屏障。我是混油皮，这个洁面力度刚刚好，不会洗完干燥也不会洗不干净。5号链接！', '性价比分析', 60, 10, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'transition', '加起来84.84！还剩15块多，买个面膜刚刚好！', NULL, 15, 11, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'product', '等等这个面膜超出一点预算但真的太好用了——CKEK玫瑰精华面膜！39.9但是一盒有好几片呢！玫瑰精华修护保湿，敷完脸嫩得像剥壳鸡蛋！6号链接！好吧超了一点预算但值得！', '闺蜜式推荐', 90, 12, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'transition', '面膜虽然超了点预算但真的不能省！接下来两款是给想要升级的姐妹准备的~', NULL, 15, 13, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'product', 'gooben果本坚果礼盒！49块！依克多因成分，补水保湿超强。这个礼盒包装特别好看，我上次拿来当生日礼物送闺蜜的！7号链接！', '升级推荐', 60, 14, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'transition', '最后一款！视黄醇的！想抗初老的姐妹看过来！', NULL, 10, 15, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'product', '致朵视黄醇十三件套！15块钱！视黄醇是抗初老的黄金成分！13件套够你用好久的。20岁就可以开始用视黄醇了！8号链接！', '成分种草', 60, 16, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'closing', 'OK姐妹们！今天的百元护肤方案就到这里啦！最低方案84.84搞定洁面+水乳霜+沐浴露+洗发水+洁面乳，超级划算！现在抽奖时间——扣"瑶瑶最棒"的里面抽3个送全套小样！感谢陪伴，下次见啦拜拜~', NULL, 60, 17, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

  -- 7.5 肖蝉：content_commerce + chat_2h（14槽固定结构）
  INSERT INTO live_script (session_id, user_id, script_type, script_content, requirement, duration_limit_sec, sequence_no, deleted, create_time, update_time)
  VALUES
    (sess_xc, uid_xc, 'opening', '来了来了，姐妹们晚上好啊！蝉姐来了！今天咱们不着急卖货啊，先聊聊天。最近收到好多姐妹私信说婆媳关系处不好，今天蝉姐就陪你们好好唠唠这个话题。先点个关注，有什么心里话可以打在公屏上，蝉姐看到就帮你分析分析~', NULL, 60, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xc, uid_xc, 'chat', '聊聊婆媳关系。姐妹们，婆媳关系这个事儿啊，说白了就是两个女人爱同一个男人——一个是妈，一个是媳妇。蝉姐跟你们说啊，处理婆媳关系最重要的一个字就是"敬"。你敬她是长辈，她自然也会心疼你是晚辈。但是敬不是忍，忍是压着火，迟早要爆。有界限感地相处才是长久之道。公屏上谁有故事要分享的？蝉姐帮你支支招！', '聊家常（0-15分钟，夫妻/婆媳/励志/歇后语/名言等）', 540, 2, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xc, uid_xc, 'product', '哎对了，说到心情烦躁啊，蝉姐最近在用一个护肤品想跟你们分享——芈汐玻色因虫草冻龄水乳霜四件套。别以为我在打广告啊，真是自己用的。你看蝉姐这个脸，35+了皮肤还行吧？就是这个虫草加玻色因的配方，用着特别润但不油。心情不好的时候好好护肤也算善待自己嘛。39.9，1号链接有。', '利润品（约15-45分钟，人气蓄水后主推）', 45, 3, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xc, uid_xc, 'transition', '好了好了，咱继续聊天。刚才有个姐妹说她老公总是站婆婆那边——这个问题蝉姐太有发言权了！', NULL, 15, 4, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xc, uid_xc, 'chat', '接着聊。男人夹在中间确实难，但关键是他处理事情的方式。一个好老公应该做"翻译官"——把妈的关心翻译成媳妇能接受的方式，把媳妇的委屈翻译成妈能理解的语言。最怕的就是两头传话还添油加醋的那种！姐妹们你们老公是什么类型的？打在公屏上！是"翻译官"型的扣1，是"装死"型的扣2，是"添油加醋"型的扣3！', '聊家常（45-75分钟，婆媳/幽默/歇后语等）', 540, 5, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xc, uid_xc, 'product', '看到好多扣2的……哈哈"装死"型的最多！行，蝉姐教你们一个办法——对装死型的老公，你就别跟他说婆婆的事了，直接找婆婆谈，反而效果更好。好了穿插个好物——百雀羚精华水乳霜礼盒！国货老品牌了，蝉姐小时候就用百雀羚。这个礼盒76.9，舒缓保湿，30+姐妹用着特别合适。2号链接。', '利润品（约45-75分钟，高流量转化窗口）', 45, 6, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xc, uid_xc, 'transition', '百雀羚拍到的姐妹你们有眼光！咱继续聊，换个轻松话题——', NULL, 15, 7, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xc, uid_xc, 'chat', '说说育儿的事。蝉姐最近看到一句话特别有感触——"你的孩子不是你的孩子，他们是生命渴望自身的儿女。"就是说啊，孩子有他自己的人生，我们做父母的就是给他们一个安全的起点就够了。别把自己没实现的梦想强加给孩子。公屏上的妈妈们同意吗？蝉姐说得对不对扣个"对"！', '聊家常（75-105分钟，歇后语/名言金句/古诗等）', 540, 8, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xc, uid_xc, 'product', '好多"对"！姐妹们真是知音啊！来，最后推荐一款——美肤宝自然白套盒，89块。美白淡斑补水保湿，蝉姐自用了半年了。你们看蝉姐这个脸，去年这个时候可没这么亮堂。自然白嘛，就是让你白得自然。3号链接。', '利润品（约75-105分钟，高流量转化窗口）', 45, 9, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xc, uid_xc, 'transition', '好了今天三个好物都分享完了，全是蝉姐自用的。咱继续聊，还有二十分钟。', NULL, 15, 10, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xc, uid_xc, 'product', '对了差点忘了，前面拍了护肤品的姐妹，蝉姐再给你们加个赠品——私信发我订单号，每人送一片面膜小样。这是蝉姐自掏腰包哦！好了不说货了。', '备用品（约105-120分钟，快速过品）', 15, 11, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xc, uid_xc, 'transition', '姐妹们还有什么想聊的？公屏打出来，蝉姐最后再聊十分钟。', NULL, 10, 12, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xc, uid_xc, 'product', '有姐妹问蝉姐平时还用什么？其实蝉姐护肤很简单，就是水+乳+霜+面膜，从不跟风。适合自己的才是最好的。前面三款你们按需求选就行。', '备用品（约105-120分钟，快速过品）', 15, 13, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xc, uid_xc, 'closing', '好了姐妹们，今天聊了两个小时了！聊了婆媳关系、夫妻相处、育儿心得，还分享了3款蝉姐自用好物。有啥心里话随时来蝉姐直播间聊，蝉姐每周一三五晚8点半准时开播！爱你们，晚安啦~记得点关注！拍了东西的姐妹私信蝉姐领面膜小样！', NULL, 60, 14, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

  RAISE NOTICE '✅ 全部数据创建完成！';
  RAISE NOTICE '用户账号：liyangyang / tianlinghong / wangzhihui / xiaoyao / xiaochan（密码均为 admin123）';
END $$;
