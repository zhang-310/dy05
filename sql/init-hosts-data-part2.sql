-- ============================================================
-- 5 位主播运营数据初始化（第二部分：账号/人设/场次/商品/话术）
-- 用户已创建: 李阳阳=3, 田玲红=4, 王智慧=5, 肖瑶=6, 肖蝉=7
-- ============================================================

DO $$
DECLARE
  uid_lyy BIGINT := 3; uid_tlh BIGINT := 4; uid_wzh BIGINT := 5; uid_xy BIGINT := 6; uid_xc BIGINT := 7;
  acc_lyy BIGINT; acc_tlh BIGINT; acc_wzh BIGINT; acc_xy BIGINT; acc_xc BIGINT;
  per_lyy BIGINT; per_tlh BIGINT; per_wzh BIGINT; per_xy BIGINT; per_xc BIGINT;
  sess_lyy BIGINT; sess_tlh BIGINT; sess_wzh BIGINT; sess_xy BIGINT; sess_xc BIGINT;
BEGIN
  -- 2. 抖音账号
  INSERT INTO douyin_account (user_id, account_name, account_id, fan_count, video_count, total_likes, description, status, deleted)
  VALUES (uid_lyy, '阳阳护肤小课堂', 'dy_liyangyang_001', 42000, 128, 260000, '护肤品成分解析、套盒开箱、实测分享', 1, 0)
  ON CONFLICT DO NOTHING;
  SELECT id INTO acc_lyy FROM douyin_account WHERE account_id = 'dy_liyangyang_001' AND deleted = 0 LIMIT 1;

  INSERT INTO douyin_account (user_id, account_name, account_id, fan_count, video_count, total_likes, description, status, deleted)
  VALUES (uid_tlh, '田总护肤严选', 'dy_tianlinghong_001', 185000, 356, 1200000, '田总严选·院线护肤·专属福利·品质保证', 1, 0)
  ON CONFLICT DO NOTHING;
  SELECT id INTO acc_tlh FROM douyin_account WHERE account_id = 'dy_tianlinghong_001' AND deleted = 0 LIMIT 1;

  INSERT INTO douyin_account (user_id, account_name, account_id, fan_count, video_count, total_likes, description, status, deleted)
  VALUES (uid_wzh, '智慧说护肤', 'dy_wangzhihui_001', 68000, 215, 450000, '护肤品科学测评·成分党·理性种草', 1, 0)
  ON CONFLICT DO NOTHING;
  SELECT id INTO acc_wzh FROM douyin_account WHERE account_id = 'dy_wangzhihui_001' AND deleted = 0 LIMIT 1;

  INSERT INTO douyin_account (user_id, account_name, account_id, fan_count, video_count, total_likes, description, status, deleted)
  VALUES (uid_xy, '瑶瑶的变美日记', 'dy_xiaoyao_001', 35000, 96, 180000, '00后美妆博主·平价好物·学生党护肤', 1, 0)
  ON CONFLICT DO NOTHING;
  SELECT id INTO acc_xy FROM douyin_account WHERE account_id = 'dy_xiaoyao_001' AND deleted = 0 LIMIT 1;

  INSERT INTO douyin_account (user_id, account_name, account_id, fan_count, video_count, total_likes, description, status, deleted)
  VALUES (uid_xc, '蝉姐聊生活', 'dy_xiaochan_001', 92000, 280, 620000, '蝉姐·聊家常·聊感情·偶尔带个好物', 1, 0)
  ON CONFLICT DO NOTHING;
  SELECT id INTO acc_xc FROM douyin_account WHERE account_id = 'dy_xiaochan_001' AND deleted = 0 LIMIT 1;

  RAISE NOTICE '账号ID: lyy=%, tlh=%, wzh=%, xy=%, xc=%', acc_lyy, acc_tlh, acc_wzh, acc_xy, acc_xc;

  -- 3. 人设
  INSERT INTO dy_persona (owner_id, account_id, persona_name, persona_type, description, tone, target_audience, content_style, keywords, is_default, status, deleted)
  VALUES
    (uid_lyy, acc_lyy, '活力护肤体验官', 'commerce', '热情开朗的护肤品体验官，擅长用亲身试用+成分解读打动观众，语速偏快，充满活力，善于用对比和反差感制造种草效果', 'passionate', '25-40岁女性，追求性价比护肤', '先试用展示→再讲成分→数据对比→限时福利', '护肤,套盒,试用,成分,性价比', 1, 1, 0),
    (uid_tlh, acc_tlh, '田总·院线护肤专家', 'commerce', '资深护肤品创业者，10年行业经验，自有供应链，主打院线品质+工厂价。说话干练有气场，擅长用专业背书+限量策略成交。粉丝称"田总"', 'professional', '28-45岁女性，中高消费力，重品质', '专业背书→独家货源→限量策略→宠粉福利', '田总,专属,院线,抗皱,紧致,限量', 1, 1, 0),
    (uid_wzh, acc_wzh, '成分党科普达人', 'knowledge', '理性温和的护肤科普博主，有化妆品配方师背景，善于用通俗语言解释复杂成分，不夸大不忽悠，以数据和原理说服观众', 'professional', '22-38岁女性，注重成分和功效', '科普成分→原理解析→产品验证→理性推荐', '成分,配方,科学护肤,玻色因,烟酰胺,377', 1, 1, 0),
    (uid_xy, acc_xy, '元气少女种草机', 'commerce', '00后活泼女生，说话自带可爱感和感染力，善于分享平价好物和学生党护肤心得，互动性强，喜欢和粉丝聊天', 'casual', '18-28岁女性，学生和年轻上班族', '日常分享→亲测好物→闺蜜式推荐→抽奖互动', '平价,学生党,好物分享,种草,闺蜜', 1, 1, 0),
    (uid_xc, acc_xc, '蝉姐·知心大姐姐', 'entertainment', '35+温柔知性大姐姐，擅长聊家常、讲生活感悟、分享婆媳/夫妻/育儿话题，在聊天中自然植入好物推荐，粉丝黏性极高', 'warm', '30-50岁女性，家庭主妇/职场妈妈', '聊家常→情感共鸣→生活感悟→顺带推荐好物', '聊天,感悟,家庭,婆媳,育儿,好物', 1, 1, 0);

  SELECT id INTO per_lyy FROM dy_persona WHERE owner_id = uid_lyy AND persona_name = '活力护肤体验官' AND deleted = 0 LIMIT 1;
  SELECT id INTO per_tlh FROM dy_persona WHERE owner_id = uid_tlh AND persona_name = '田总·院线护肤专家' AND deleted = 0 LIMIT 1;
  SELECT id INTO per_wzh FROM dy_persona WHERE owner_id = uid_wzh AND persona_name = '成分党科普达人' AND deleted = 0 LIMIT 1;
  SELECT id INTO per_xy FROM dy_persona WHERE owner_id = uid_xy AND persona_name = '元气少女种草机' AND deleted = 0 LIMIT 1;
  SELECT id INTO per_xc FROM dy_persona WHERE owner_id = uid_xc AND persona_name = '蝉姐·知心大姐姐' AND deleted = 0 LIMIT 1;

  -- 4. 商品
  INSERT INTO dy_product (user_id, product_name, product_category, price, cost_price, description, status, deleted, create_time, update_time) VALUES
    (uid_lyy, '致朵绿钻嫩肤清颜五件套保湿套装', 'K', 8.00, 3.00, '入门级保湿套盒', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_lyy, '致朵红石榴鲜润水嫩六件套', 'K', 10.00, 4.00, '红石榴抗氧化补水保湿', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_lyy, '鱼子酱柔肤致颜六件套礼盒', 'K', 18.00, 7.00, '鱼子酱精华紧致抗皱', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_lyy, '韩束红蛮腰水乳组合80ml', 'B', 39.90, 18.00, '补水保湿紧致抗皱淡纹提亮', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_lyy, 'KANS/韩束黑耀晶采精华水+乳+霜', 'B', 49.00, 22.00, '大牌品质精华级护理', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_lyy, 'SIAYZU红石榴多肽保湿臻享礼盒', 'B', 39.90, 16.00, '红石榴多肽深层保湿', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_lyy, 'PLER胶原蛋白玻尿酸盈润舒缓修护套盒', 'R', 69.00, 25.00, '胶原蛋白+玻尿酸双效修护', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_lyy, '兰蔻全新清滢保湿柔肤水125ml粉水', 'B', 36.90, 15.00, '兰蔻粉水保湿补水紧致', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_tlh, '【田总专属】海茴香极光水乳', 'R', 49.90, 15.00, '不添加一滴水紧致抗皱', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_tlh, '【田总专属】SP水乳黑金胶原多肽抗皱冻龄水乳', 'R', 39.90, 12.00, '黑金胶原多肽抗皱冻龄', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_tlh, '【田总专属】DIDK金致多肽花蜜臻养护手霜', 'B', 10.00, 3.00, '多肽花蜜滋养修护', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_tlh, '【田总专属】肌采美白祛斑抗皱面膜', 'B', 19.90, 6.00, '美白祛斑抗皱三效面膜', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_tlh, '【田总专属】佰珍堂藏红花酵萃抗皱紧致舒颜护肤套装', 'K', 25.00, 8.00, '藏红花精粹抗皱紧致', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_tlh, '【田总专属】法颜蔻玻色因抗皱紧致护肤礼盒', 'K', 25.00, 8.00, '法国配方玻色因抗皱', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_tlh, '【田总专属】韩束光感莹透水嫩紧致礼盒套装', 'B', 138.00, 55.00, '韩束正品光感莹透紧致', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_tlh, '【田总专属】美肤宝水份源礼盒5件套盒', 'B', 89.00, 35.00, '深层补水保湿精华乳', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_tlh, '【田总粉丝专属】BPM赋能滋养淡纹红宝石水乳精华面霜四件套', 'B', 29.90, 10.00, '红宝石四件套滋养淡纹', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_tlh, '【田总专属】LRNAS奢宠抗皱润颜双效滋润礼盒', 'K', 22.00, 7.00, '奢宠级抗皱滋润', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_wzh, '【377】肌采金钻美白祛斑抗皱七件套', 'R', 79.90, 28.00, '377成分美白祛斑抗皱', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_wzh, '美尼姿玻色因胶原蛋白紧致抗皱面霜', 'R', 29.90, 10.00, '玻色因+胶原蛋白双效抗皱', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_wzh, 'CKEK肌采美白祛斑淡黑抗皱眼霜(377金)', 'B', 29.90, 12.00, '377配方淡化细纹黑眼圈', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_wzh, '英国Mircutee玻色因活胶原鱼子微囊紧致舒缓溶纹水', 'B', 29.90, 12.00, '玻色因14天抗皱精华水', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_wzh, '柏兰梦十效合一577精华液', 'C', 13.14, 5.00, '美白祛斑抗皱多效合一', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_wzh, 'PLER肌采美白祛斑修护精华液', 'B', 29.90, 11.00, '美白祛斑修护三效精华', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_wzh, '初律玻尿酸原B5润颜精粹水', 'R', 22.90, 8.00, '玻尿酸+B5深层补水', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_wzh, '珀莱雅红宝石洁水乳套装紧致抗皱', 'B', 68.90, 30.00, '珀莱雅红宝石紧致保湿', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_xy, '致朵璀璨亮光定制护肤礼盒', 'B', 11.90, 4.00, '亮肤定制礼盒学生党入门', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_xy, 'VEZE梵贞肤研美白祛斑套盒', 'K', 20.00, 7.00, '美白祛斑套盒性价比高', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_xy, '雅缇诗白松露栀子花精油香氛沐浴露', 'F', 19.90, 7.00, '白松露精油沐浴露香氛持久', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_xy, '雅缇诗鱼子酱氨基酸控油去屑洗发水800ml', 'B', 19.90, 8.00, '氨基酸洗发水控油去屑', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_xy, 'PLER山茶花八重氨基酸控油净透洁面乳', 'R', 13.14, 5.00, '山茶花洁面温和控油', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_xy, 'CKEK玫瑰沁润修护精华面膜', 'R', 39.90, 14.00, '玫瑰精华面膜修护保湿', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_xy, 'gooben/果本坚果依克多因奢漾礼盒', 'B', 49.00, 20.00, '果本坚果礼盒补水保湿', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_xy, '致朵视黄醇臻养沁颜十三件套', 'K', 15.00, 5.00, '视黄醇抗初老13件超值', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_xc, '芈汐玻色因虫草冻龄抗皱紧致水乳霜洁面四件套', 'B', 39.90, 15.00, '虫草冻龄30+女性必备', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_xc, '百雀羚弹嫩护肤焕亮精华水乳霜礼盒', 'B', 76.90, 30.00, '百雀羚经典系列舒缓保湿', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uid_xc, '美肤宝自然白护肤品套盒美白淡斑', 'B', 89.00, 35.00, '美肤宝国货美白淡斑', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

  -- 5. 场次
  INSERT INTO live_session (user_id, account_id, persona_id, live_title, script_style, live_format, session_type, live_description, scheduled_time, status, deleted, create_time, update_time)
  VALUES (uid_lyy, acc_lyy, per_lyy, '阳阳护肤实测专场｜套盒开箱全实拍', 'passionate', 'heavy_paid_category', 'standard', '今晚8点！8款护肤套盒逐一开箱实测', CURRENT_TIMESTAMP + INTERVAL '1 day' + INTERVAL '20 hours', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  RETURNING id INTO sess_lyy;

  INSERT INTO live_session (user_id, account_id, persona_id, live_title, script_style, live_format, session_type, live_description, scheduled_time, status, deleted, create_time, update_time)
  VALUES (uid_tlh, acc_tlh, per_tlh, '田总严选｜院线级护肤专属福利夜', 'professional', 'heavy_paid_category', 'standard', '田总亲选10款院线护肤品全部工厂直供价', CURRENT_TIMESTAMP + INTERVAL '1 day' + INTERVAL '19 hours', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  RETURNING id INTO sess_tlh;

  INSERT INTO live_session (user_id, account_id, persona_id, live_title, script_style, live_format, session_type, live_description, scheduled_time, status, deleted, create_time, update_time)
  VALUES (uid_wzh, acc_wzh, per_wzh, '成分党深度解析｜玻色因vs377到底怎么选', 'professional', 'content_led', 'standard', '玻色因和377的深度科普+产品推荐', CURRENT_TIMESTAMP + INTERVAL '2 days' + INTERVAL '20 hours', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  RETURNING id INTO sess_wzh;

  INSERT INTO live_session (user_id, account_id, persona_id, live_title, script_style, live_format, session_type, live_description, scheduled_time, status, deleted, create_time, update_time)
  VALUES (uid_xy, acc_xy, per_xy, '学生党平价护肤｜100元搞定全套！', 'friendly', 'organic_micro_paid', 'standard', '100块以内搞定全套护肤方案', CURRENT_TIMESTAMP + INTERVAL '2 days' + INTERVAL '19 hours' + INTERVAL '30 minutes', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  RETURNING id INTO sess_xy;

  INSERT INTO live_session (user_id, account_id, persona_id, live_title, script_style, live_format, session_type, live_description, scheduled_time, status, deleted, create_time, update_time)
  VALUES (uid_xc, acc_xc, per_xc, '蝉姐夜话｜聊聊婆媳那些事+好物分享', 'friendly', 'content_commerce', 'chat_2h', '蝉姐陪你聊天+好物分享', CURRENT_TIMESTAMP + INTERVAL '1 day' + INTERVAL '20 hours' + INTERVAL '30 minutes', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  RETURNING id INTO sess_xc;

  RAISE NOTICE '场次ID: lyy=%, tlh=%, wzh=%, xy=%, xc=%', sess_lyy, sess_tlh, sess_wzh, sess_xy, sess_xc;

  -- 6. 绑定商品
  INSERT INTO live_product (session_id, product_id, product_name, position, deleted, create_time)
  SELECT sess_lyy, p.id, p.product_name, ROW_NUMBER() OVER (ORDER BY p.id), 0, CURRENT_TIMESTAMP
  FROM dy_product p WHERE p.user_id = uid_lyy AND p.deleted = 0;

  INSERT INTO live_product (session_id, product_id, product_name, position, deleted, create_time)
  SELECT sess_tlh, p.id, p.product_name, ROW_NUMBER() OVER (ORDER BY p.id), 0, CURRENT_TIMESTAMP
  FROM dy_product p WHERE p.user_id = uid_tlh AND p.deleted = 0;

  INSERT INTO live_product (session_id, product_id, product_name, position, deleted, create_time)
  SELECT sess_wzh, p.id, p.product_name, ROW_NUMBER() OVER (ORDER BY p.id), 0, CURRENT_TIMESTAMP
  FROM dy_product p WHERE p.user_id = uid_wzh AND p.deleted = 0;

  INSERT INTO live_product (session_id, product_id, product_name, position, deleted, create_time)
  SELECT sess_xy, p.id, p.product_name, ROW_NUMBER() OVER (ORDER BY p.id), 0, CURRENT_TIMESTAMP
  FROM dy_product p WHERE p.user_id = uid_xy AND p.deleted = 0;

  INSERT INTO live_product (session_id, product_id, product_name, position, deleted, create_time)
  SELECT sess_xc, p.id, p.product_name, ROW_NUMBER() OVER (ORDER BY p.id), 0, CURRENT_TIMESTAMP
  FROM dy_product p WHERE p.user_id = uid_xc AND p.deleted = 0;

  -- 7. 话术（李阳阳 17槽 + 田玲红 21槽 + 王智慧 17槽 + 肖瑶 17槽 + 肖蝉 14槽）
  -- 7.1 李阳阳：heavy_paid_category
  INSERT INTO live_script (session_id, user_id, script_type, script_content, requirement, duration_limit_sec, sequence_no, deleted, create_time, update_time) VALUES
    (sess_lyy, uid_lyy, 'opening', '家人们晚上好！我是阳阳，欢迎来到护肤实测直播间！今晚给大家准备了8款超火的护肤套盒，每一款我都亲自上脸试过了！不吹不黑，好用才推荐！先点个关注不迷路，今晚全场买一送一，还有无门槛红包雨！来来来，先给阳阳扣个1，看看今晚多少姐妹在！', NULL, 60, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'product', '第一款来了！致朵绿钻嫩肤清颜五件套！姐妹们看这个包装，8块钱5件套，你没听错，8块钱！我上手给你们试试啊——这个水质地很清爽，一点都不黏腻，乳液推开秒吸收！干皮姐妹入门首选，8块钱买不了吃亏买不了上当！1号链接，赶紧拍！', '亏品快速过', 45, 2, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'transition', '好，第一款拍到的姐妹扣个"拍了"！没拍到的别急，后面还有更猛的！下一款可是大品牌哦~', NULL, 15, 3, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'product', '致朵红石榴六件套！红石榴是什么？抗氧化之王啊！10块钱，水+乳+霜+精华+洁面+面膜全齐了，你去专柜随便买一瓶红石榴的都不止这个价！我涂给你们看，多滋润！10块钱，闭眼入！2号链接！', '亏品快速过', 45, 4, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'transition', '两款亏品上完了，接下来给大家上点大牌好货！想看大牌的扣"大牌"！', NULL, 15, 5, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'product', '鱼子酱六件套礼盒！贵妇级成分！18块钱，鱼子酱精华+肽+玻尿酸三重配方，紧致抗皱效果杠杠的！我已经用了一个月了，你们看我这个法令纹是不是淡了很多？18块，3号链接，手慢无！', '利润品重点推', 90, 6, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'transition', '鱼子酱拍到的姐妹恭喜赚到了！接下来韩束正品行货！', NULL, 15, 7, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'product', '韩束红蛮腰！当家花旦！80ml水乳组合，补水保湿紧致抗皱淡纹提亮，一套搞定所有需求！专柜卖多少？我们直播间39.9！今天买还送同款小样！4号链接，赶紧锁单！', '爆品深度讲', 180, 8, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'transition', '39.9的韩束在哪找？只有阳阳直播间！下一款也是大牌~', NULL, 15, 9, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'product', '韩束黑耀晶采！精华水+乳+霜三件套！主打提亮焕肤，用了之后皮肤有那种自内而外的光泽感！49块买韩束三件套，这个价格不可思议！5号链接！', '爆品深度讲', 120, 10, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'transition', '韩束两款上完了，接下来红石榴多肽！最近超火！', NULL, 15, 11, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'product', 'SIAYZU红石榴多肽保湿臻享礼盒！红石榴+多肽双重配方，补水锁水一步到位！礼盒包装送人特别有面子！39.9！6号链接！', '利润品推荐', 90, 12, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'transition', '再来一个重磅的！PLER胶原蛋白！30+姐妹必看！', NULL, 15, 13, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'product', 'PLER胶原蛋白玻尿酸套盒！胶原蛋白+玻尿酸，黄金搭档！修护屏障、抗皱保湿、舒缓泛红一套搞定！69块，买大牌单品都不止这个价！7号链接！', '利润品重点推', 120, 14, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'transition', '最后一款！压轴的来了！兰蔻！对，兰蔻！', NULL, 15, 15, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'product', '兰蔻粉水！新版清滢保湿柔肤水125ml！殿堂级保湿！专柜420，我们直播间36.9！限量50瓶，抢完下架！8号链接！3、2、1，上链接！', '爆品限量冲', 120, 16, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_lyy, uid_lyy, 'closing', '家人们！今晚8款全部上完了！拍到的赶紧付款，有问题找客服！记得点关注，明天还有更多好物！爱你们，晚安！', NULL, 45, 17, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

  -- 7.2 田玲红 21槽
  INSERT INTO live_script (session_id, user_id, script_type, script_content, requirement, duration_limit_sec, sequence_no, deleted, create_time, update_time) VALUES
    (sess_tlh, uid_tlh, 'opening', '各位姐妹晚上好，我是田总。欢迎来到田总严选直播间。今晚10款院线级护肤品，全部工厂直供价，绕过所有中间商。闲话少说直接上货。新来的先点关注，田总不忽悠，品质说话。', NULL, 60, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'product', '第一款，海茴香极光水乳。法国原料商定制配方，不添加一滴水，全部海茴香萃取液替代。紧致抗皱、淡纹提亮，医美级原料。专柜同类300+，田总49.9。1号链接，限量200套。', '利润品主推', 120, 2, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'transition', '海茴香拍到的赚到了。下一款更硬核——黑金胶原多肽。', NULL, 15, 3, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'product', 'SP水乳黑金胶原多肽。主打抗皱冻龄，日本进口胶原多肽原料。我自己用了3个月，你们看这颈纹。39.9一套，这价格只保三天。2号链接。', '利润品主推', 90, 4, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'transition', '两款利润品上完。接下来福利品，田总宠粉时间。', NULL, 15, 5, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'product', 'DIDK金致多肽花蜜护手霜。田总定制，多肽+花蜜双效滋养。10块钱，普通护手霜都不止这价。3号链接，一人限3支。', '亏品引流', 30, 6, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'transition', '10块护手霜拍了吗？下一款面膜。', NULL, 10, 7, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'product', '肌采美白祛斑抗皱面膜白猫眼系列。三效合一，精华液超多，一片顶半瓶精华。19.9一盒5片，均不到4块。4号链接。', '爆品推荐', 60, 8, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'transition', '面膜拍好了。接下来两款套盒，大气上档次。', NULL, 10, 9, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'product', '佰珍堂藏红花酵萃抗皱紧致套装。西藏藏红花，一克比黄金贵。全线护肤品融入藏红花精粹。25块，5号链接。', '亏品引流', 60, 10, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'transition', '下一款法国配方，成分更硬核。', NULL, 10, 11, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'product', '法颜蔻玻色因抗皱紧致礼盒。欧莱雅专利原料玻色因，高浓度配方。25块。6号链接。', '亏品引流', 60, 12, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'transition', '三款亏品上完。接下来大牌——韩束正品行货。', NULL, 10, 13, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'product', '韩束光感莹透紧致礼盒！正品全码可验。专柜398，田总渠道价138。光感系列主打提亮紧致，25-40岁所有肤质。7号链接。', '爆品深度推', 180, 14, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'transition', '韩束抢到的恭喜。下一款美肤宝也是大牌。', NULL, 10, 15, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'product', '美肤宝水份源5件套。深层补水保湿，我自己用了两年。89块5件套，专柜翻3倍。8号链接。', '爆品推荐', 90, 16, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'transition', '最后两款，田总粉丝专属福利价。', NULL, 10, 17, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'product', 'BPM赋能滋养淡纹红宝石四件套。田总粉丝专属！29.9四件套。9号链接。', '利润品推荐', 60, 18, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'transition', '最后一款压轴。', NULL, 10, 19, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'product', 'LRNAS奢宠抗皱润颜礼盒。22块钱，最后一款亏品，田总今晚交个朋友。10号链接。', '亏品收尾', 45, 20, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_tlh, uid_tlh, 'closing', '姐妹们，10款全部上完。拍到的赶紧付款，48小时不付自动取消。有问题找客服，田总售后放心。感谢支持，明天同一时间再见。', NULL, 45, 21, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

  -- 7.3 王智慧 17槽
  INSERT INTO live_script (session_id, user_id, script_type, script_content, requirement, duration_limit_sec, sequence_no, deleted, create_time, update_time) VALUES
    (sess_wzh, uid_wzh, 'opening', '大家好，我是智慧。今天跟大家聊玻色因和377这两个成分到底怎么选。很多姐妹在评论区问我搞不清区别，今天用最通俗的方式讲清楚。先点关注，有问题随时公屏打出来。', NULL, 90, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'product', '先说377。全名苯乙基间苯二酚，目前最强美白成分之一，比熊果苷强16倍。作用机理是抑制酪氨酸酶活性，从根源减少黑色素。适合有色斑暗沉的姐妹。这款肌采金钻七件套就是高浓度377。膏体细腻上脸不闷。79.9七件套，1号链接。', '科普+产品验证', 180, 2, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'transition', '有姐妹问377会不会刺激？377本身刺激性很低，浓度超2%可能有轻微刺痒，敏感肌先耳后试用。好，说玻色因。', NULL, 30, 3, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'product', '玻色因，学名羟丙基四氢吡喃三醇，欧莱雅专利。核心功效是促进糖胺聚糖合成——让真皮层变饱满减少皱纹。这款美尼姿面霜5%玻色因浓度，算高的。29.9，2号链接，适合抗初老。', '科普+产品验证', 180, 4, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'transition', '简单总结：美白选377，抗皱选玻色因。两个不冲突可搭配。眼部看下一款。', NULL, 30, 5, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'product', '肌采377金眼霜。眼周最薄最容易出细纹黑眼圈。377+胜肽双重配方。我用了2个月确实有效果。29.9，3号链接。', '产品推荐', 90, 6, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'transition', '有姐妹问精华水推荐？下面这款含玻色因。', NULL, 20, 7, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'product', 'Mircutee玻色因活胶原鱼子微囊溶纹水。英国品牌，玻色因+鱼子酱双效。14天密集修护，适合换季敏感期。29.9，4号链接。', '产品推荐', 60, 8, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'transition', '说完单成分，看看多效合一的。护肤步骤不想太多的姐妹一瓶搞定也OK。', NULL, 20, 9, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'product', '柏兰梦577精华液。577不是377——在377基础上加了5%烟酰胺和7%VC衍生物，十效合一。13.14一瓶控单价。5号链接。', '控单产品分析', 90, 10, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'transition', '有姐妹问祛斑精华液。PLER这款不错。', NULL, 20, 11, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'product', 'PLER美白祛斑修护精华液。配方表主要活性成分377+烟酰胺+光甘草定。三重美白叠加效果更明显。29.9，6号链接。', '成分分析推荐', 90, 12, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'transition', '讲了这么多活性成分，补水保湿也不能忘。', NULL, 20, 13, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'product', '初律玻尿酸原B5精粹水。玻尿酸补水，B5修护屏障。补水同时锁住水分。22.9，7号链接。所有肤质适用。', '产品推荐', 60, 14, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'transition', '最后一款，想要整套方案的姐妹看。', NULL, 15, 15, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'product', '珀莱雅红宝石水乳套装。国货之光，核心成分六胜肽+维A醇。68.9一套，8号链接。国货品质，价格良心。', '爆品推荐', 120, 16, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_wzh, uid_wzh, 'closing', '今天的成分科普+产品推荐就到这里。总结：美白祛斑选377，抗皱紧致选玻色因，可搭配用。每周二四六晚8点开播，有问题随时来。晚安。', NULL, 60, 17, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

  -- 7.4 肖瑶 17槽
  INSERT INTO live_script (session_id, user_id, script_type, script_content, requirement, duration_limit_sec, sequence_no, deleted, create_time, update_time) VALUES
    (sess_xy, uid_xy, 'opening', '哈喽姐妹们！瑶瑶来啦！今天超实用——100块以内搞定全套护肤方案！洁面到面膜到沐浴露全有！每一个我自己回购过的，纯分享！关注瑶瑶，今晚抽3个姐妹送全套小样！先扣"来了"~', NULL, 60, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'product', '第一个洁面！致朵璀璨亮光定制护肤礼盒11.9！不要看价格便宜就觉得不好用，我用了两管！洗完不紧绷不假滑，礼盒装送朋友也不丢面子！1号链接~', '闺蜜式种草', 90, 2, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'transition', '洁面搞定才11.9，省下来的钱买水乳！接着看~', NULL, 15, 3, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'product', 'VEZE梵贞美白祛斑套盒！20块水+乳+霜都有！室友用两周说亮了一个色号，我后来也买了一套试，还真的！学生党美白入门首选！2号链接！', '闺蜜式种草', 90, 4, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'transition', '洁面+水乳霜才31.9！不到一杯奶茶+一顿外卖！继续~', NULL, 15, 5, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'product', '雅缇诗白松露栀子花沐浴露！我吹爆！洗完身上栀子花味道持续到第二天早上！泡沫超绵密，19.9一大瓶！3号链接冲！', '体验分享', 90, 6, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'transition', '身体洗护搞定！头发也不能忘！下面洗发水太好用了——', NULL, 15, 7, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'product', '雅缇诗鱼子酱氨基酸洗发水800ml！氨基酸的！控油去屑！800ml超大瓶用好久！油头星人用了头发蓬松好多！19.9！4号链接！', '体验分享', 60, 8, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'transition', '目前71.7！还有30块预算够买洁面乳+面膜！', NULL, 15, 9, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'product', 'PLER山茶花氨基酸洁面乳！13.14一支！温和洁面不伤屏障。混油皮洁面力度刚好，不干燥也不洗不干净。5号链接！', '性价比分析', 60, 10, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'transition', '加起来84.84！还剩15块多买个面膜！', NULL, 15, 11, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'product', 'CKEK玫瑰精华面膜！39.9超点预算但太好用了！玫瑰精华修护保湿，敷完脸嫩得像剥壳鸡蛋！6号链接！超了点但值得！', '闺蜜式推荐', 90, 12, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'transition', '面膜虽超预算但不能省！接下来给想升级的姐妹~', NULL, 15, 13, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'product', 'gooben果本坚果礼盒！49块！依克多因成分补水超强。礼盒包装好看，上次当生日礼物送闺蜜！7号链接！', '升级推荐', 60, 14, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'transition', '最后一款！视黄醇！想抗初老的姐妹看过来！', NULL, 10, 15, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'product', '致朵视黄醇十三件套！15块！视黄醇是抗初老黄金成分！13件套够用好久。20岁就可以开始用！8号链接！', '成分种草', 60, 16, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xy, uid_xy, 'closing', '姐妹们！百元护肤方案到这里啦！最低方案84.84搞定全套！现在抽奖——扣"瑶瑶最棒"抽3个送全套小样！感谢陪伴，下次见拜拜~', NULL, 60, 17, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

  -- 7.5 肖蝉 chat_2h 14槽
  INSERT INTO live_script (session_id, user_id, script_type, script_content, requirement, duration_limit_sec, sequence_no, deleted, create_time, update_time) VALUES
    (sess_xc, uid_xc, 'opening', '来了来了姐妹们晚上好！蝉姐来了！今天不着急卖货，先聊聊天。最近好多姐妹私信说婆媳关系处不好，今天蝉姐陪你们好好唠唠。先点关注，有心里话打公屏上，蝉姐帮你分析~', NULL, 60, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xc, uid_xc, 'chat', '婆媳关系这事儿啊，说白了就是两个女人爱同一个男人——一个是妈一个是媳妇。蝉姐跟你们说，处理婆媳关系最重要一个字"敬"。你敬她是长辈，她自然心疼你是晚辈。但敬不是忍，忍是压着火迟早爆。有界限感地相处才长久。公屏上谁有故事分享？蝉姐帮你支招！', '聊家常（0-15分钟，夫妻/婆媳/励志/歇后语/名言等）', 540, 2, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xc, uid_xc, 'product', '说到心情烦躁，蝉姐最近用一个护肤品想分享——芈汐玻色因虫草冻龄四件套。别以为打广告，真自己用的。蝉姐35+了皮肤还行吧？虫草加玻色因配方，用着润但不油。心情不好时好好护肤也算善待自己。39.9，1号链接。', '利润品（约15-45分钟，人气蓄水后主推）', 45, 3, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xc, uid_xc, 'transition', '继续聊天。刚才有姐妹说她老公总站婆婆那边——这个蝉姐太有发言权了！', NULL, 15, 4, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xc, uid_xc, 'chat', '男人夹中间确实难，关键是处理方式。好老公应该做"翻译官"——把妈的关心翻译成媳妇能接受的方式，把媳妇的委屈翻译成妈能理解的语言。最怕两头传话还添油加醋！你们老公什么类型？"翻译官"扣1，"装死"扣2，"添油加醋"扣3！', '聊家常（45-75分钟，婆媳/幽默/歇后语等）', 540, 5, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xc, uid_xc, 'product', '好多扣2的！"装死"型最多！教你们一招——装死型老公你就别跟他说婆婆的事，直接找婆婆谈反而更好。穿插个好物——百雀羚精华水乳霜礼盒！国货老品牌，蝉姐小时候就用百雀羚。76.9舒缓保湿，30+姐妹特别合适。2号链接。', '利润品（约45-75分钟，高流量转化窗口）', 45, 6, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xc, uid_xc, 'transition', '百雀羚拍到的有眼光！换个轻松话题——', NULL, 15, 7, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xc, uid_xc, 'chat', '说说育儿。蝉姐最近看到一句话特别有感触——"你的孩子不是你的孩子，他们是生命渴望自身的儿女。"孩子有自己的人生，我们做父母的给个安全起点就够了。别把没实现的梦强加给孩子。公屏妈妈们同意吗？蝉姐说得对扣"对"！', '聊家常（75-105分钟，歇后语/名言金句/古诗等）', 540, 8, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xc, uid_xc, 'product', '好多"对"！姐妹们真知音！最后一款——美肤宝自然白套盒89块。美白淡斑补水保湿，蝉姐自用半年。你们看蝉姐这脸去年可没这么亮堂。自然白嘛就是白得自然。3号链接。', '利润品（约75-105分钟，高流量转化窗口）', 45, 9, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xc, uid_xc, 'transition', '三个好物分享完了全是蝉姐自用的。继续聊还有二十分钟。', NULL, 15, 10, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xc, uid_xc, 'product', '前面拍了护肤品的姐妹，私信发订单号每人送一片面膜小样。蝉姐自掏腰包哦！好了不说货了。', '备用品（约105-120分钟，快速过品）', 15, 11, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xc, uid_xc, 'transition', '姐妹们还想聊什么？公屏打出来蝉姐最后聊十分钟。', NULL, 10, 12, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xc, uid_xc, 'product', '有姐妹问蝉姐平时还用什么？蝉姐护肤很简单，水+乳+霜+面膜从不跟风。适合自己的才最好。前面三款按需求选就行。', '备用品（约105-120分钟，快速过品）', 15, 13, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (sess_xc, uid_xc, 'closing', '姐妹们聊了两个小时！婆媳关系、夫妻相处、育儿心得都聊了，还分享了3款自用好物。有心里话随时来蝉姐直播间，每周一三五晚8点半准时开播！爱你们晚安~拍了东西的私信蝉姐领面膜小样！', NULL, 60, 14, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

  RAISE NOTICE '✅ 全部数据创建完成！';
END $$;
