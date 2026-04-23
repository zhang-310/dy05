-- ============================================================
-- 5 位主播正式数据初始化（v3 - 纯 INSERT，不使用 DO 块）
-- 用户 ID：李阳阳=3, 田玲红=4, 王智慧=5, 肖瑶=6, 肖蝉=7
-- ============================================================

-- ============================================================
-- 1. 抖音账号（douyin_account）
-- ============================================================
INSERT INTO douyin_account (user_id, account_name, account_id, fan_count, follow_count, video_count, total_likes, description, status, deleted, create_time, update_time)
VALUES
(3, '阳阳护肤日记', 'lyy_skincare_2026', 186000, 520, 342, 2850000, '专注高端护肤品测评与推荐，擅长油皮/混油护理方案，直播间主打爆品秒杀', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, '田总的院线级护肤', 'tianzong_pro_2026', 325000, 380, 518, 5200000, '10年院线护肤经验，主打高端抗衰产品线，专业成分解读，直播间客单价800+', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, '智慧成分研究所', 'zhihui_lab_2026', 142000, 620, 286, 1680000, '成分党科普博主，用数据说话，擅长平价替代推荐和成分对比分析', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, '瑶瑶的美妆日常', 'yaoyao_beauty_2026', 228000, 890, 425, 3600000, '00后元气少女，Z世代审美，学生党友好种草，活力满满的直播风格', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, '蝉姐聊生活', 'chanjie_life_2026', 198000, 450, 368, 2100000, '知心大姐姐人设，暖心治愈系，擅长聊天互动+好物分享，粉丝粘性极高', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 2. 人设（dy_persona）— owner_id 对应 user_id
-- ============================================================
INSERT INTO dy_persona (owner_id, persona_name, persona_type, description, tone, target_audience, content_style, keywords, is_default, status, deleted, create_time, update_time, local_flavor, persona_traits, ip_type, content_ratio, live_style, age_range, positioning_tags)
VALUES
(3, '活力护肤体验官', 'live_host',
 '阳光开朗的护肤体验官，用真实试用体验打动粉丝。擅长现场上脸测试，用对比图和实测数据说服观众。语速适中偏快，互动感强，善用"家人们"等亲切称呼拉近距离。',
 '热情活泼、真诚可信', '25-35岁女性，油皮/混油肤质，月消费500-2000', '真人实测+效果对比+成分简析',
 '油皮亲妈,上脸实测,闭眼入,回购王', 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
 '东北直爽风', '真诚直率,亲和力强,善于互动', '护肤体验官', '产品60:互动25:科普15', '激情带货型', '25-30', '实测派,油皮救星,性价比之选'),

(4, '田总·院线护肤专家', 'live_host',
 '资深院线护肤顾问，专业严谨但不失亲和。直播中常引用临床数据和成分原理，用专业背书建立信任。客单价偏高，主打"少即是多"的精简护肤理念。语速沉稳，用词考究。',
 '专业权威、温和自信', '30-45岁女性，高消费力，注重抗衰抗氧化', '专业成分解读+院线对比+精简方案',
 '院线同款,医美级,抗衰金标准,贵妇平替', 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
 '知性优雅风', '专业权威,沉稳自信,品味高级', '院线护肤专家', '产品50:专业科普30:互动20', '专业讲解型', '35-42', '高端护肤,院线背书,抗衰专家'),

(5, '成分党科普达人', 'live_host',
 '理工科背景的成分分析师，用数据和论文说话。直播风格偏知识分享型，擅长用通俗语言解释复杂成分。善于做AB对比测试，用理性分析帮粉丝避坑。',
 '理性客观、深入浅出', '22-35岁女性，大学生/白领，追求性价比和科学护肤', '成分解析+论文引用+性价比推荐',
 '成分党,看配方,论文背书,理性种草', 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
 '学术风但不枯燥', '理性分析,数据说话,客观公正', '成分科普达人', '科普40:产品35:互动25', '知识科普型', '27-33', '成分党,平价替代,科学护肤'),

(6, '元气少女种草机', 'live_host',
 '00后活力少女，Z世代审美代表。直播间氛围轻松欢乐，善用网络热梗和可爱表情包互动。推荐学生党友好的平价好物，注重颜值和使用体验。',
 '活泼可爱、元气满满', '18-25岁女性，学生/初入职场，月消费200-800', '颜值种草+平价推荐+氛围感分享',
 '学生党必入,白菜价,颜值即正义,少女心', 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
 '甜美少女风', '元气满满,真实可爱,分享欲强', '元气种草机', '种草40:互动35:好物25', '氛围感互动型', '20-24', '学生党,平价好物,Z世代'),

(7, '蝉姐·知心大姐姐', 'live_host',
 '温暖治愈系主播，直播间像闺蜜聊天。善于倾听粉丝烦恼，在聊天中自然穿插好物推荐。纯娱乐场次以情感话题为主，辅以生活好物。粉丝忠诚度极高，复购率领先。',
 '温暖治愈、知心贴心', '28-40岁女性，宝妈/职场女性，追求生活品质', '情感共鸣+生活分享+走心推荐',
 '闺蜜推荐,走心好物,治愈系,蝉姐家的', 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
 '温暖亲切风', '善解人意,共情力强,值得信赖', '知心大姐姐', '聊天45:好物30:互动25', '情感治愈型', '32-38', '知心姐姐,走心推荐,粉丝粘性王')
ON CONFLICT DO NOTHING;

-- ============================================================
-- 3. 商品（dy_product）
-- ============================================================

-- 李阳阳（uid=3）：油皮护肤品，偏中端，主打性价比
INSERT INTO dy_product (user_id, product_name, product_category, description, price, cost_price, inventory, tags, status, featured, deleted, create_time, update_time, profit_margin_pct, ai_selling_points, version)
VALUES
(3, '致朵绿钻嫩肤清颜五件套保湿套装', 'B', '绿钻系列明星爆品，控油保湿双效合一，油皮亲测不闷痘', 298.00, 89.00, 500, '爆品,油皮亲妈,控油保湿', 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.70, '① 绿钻专利控油成分，8h持久清爽\n② 5件套一步到位，省心省钱\n③ 敏感肌可用，0酒精0香精\n④ 直播间独家赠品加赠', 1),
(3, '薇诺娜特护霜舒敏保湿', 'R', '医学护肤标杆，修护屏障王者', 268.00, 135.00, 300, '利润品,敏感肌,修护', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.50, '① 皮肤科医生推荐品牌\n② 马齿苋+青刺果双重修护\n③ 换季泛红急救必备', 1),
(3, '珀莱雅双抗精华2.0', 'B', '国货之光抗氧抗糖精华，年度回购王', 189.00, 65.00, 800, '爆品,抗氧化,国货', 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.66, '① 虾青素+麦角硫因双抗组合\n② 国货销量TOP1精华\n③ 质地轻薄，油皮友好', 1),
(3, '理肤泉B5面膜5片装', 'K', '引流亏品，法国药妆品牌背书', 99.00, 75.00, 1000, '亏品,引流,面膜', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.24, '① 法国药妆NO.1品牌\n② B5修护+玻尿酸保湿\n③ 限量放价，拍到就是赚到', 1),
(3, '自然堂小紫瓶精华液', 'R', '抗初老入门精华，性价比之王', 159.00, 55.00, 600, '利润品,抗初老,性价比', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.65, '① 喜马拉雅冰川水+烟酰胺\n② 适合25+入门抗老\n③ 吸收快不搓泥', 1),
(3, 'OLAY超A瓶精华', 'C', '控价控单品，稳定客单价', 249.00, 120.00, 200, '控单品,烟酰胺,美白', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.52, '① 烟酰胺鼻祖品牌\n② 28天可见提亮\n③ 专柜同款直播间特惠', 1),
(3, '完美日记卸妆油200ml', 'F', '平价卸妆标杆，引流过渡品', 59.00, 25.00, 1200, '平价品,卸妆,学生党', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.58, '① 氨基酸温和配方\n② 浓妆一抹即净\n③ 不到60块大容量', 1);

-- 田玲红（uid=4）：高端院线护肤，主打抗衰
INSERT INTO dy_product (user_id, product_name, product_category, description, price, cost_price, inventory, tags, status, featured, deleted, create_time, update_time, profit_margin_pct, ai_selling_points, version)
VALUES
(4, '海蓝之谜精粹水150ml', 'R', '贵妇级精粹水，院线级修护体验', 1150.00, 520.00, 100, '利润品,高端,修护', 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.55, '① 神奇活性精粹Miracle Broth™\n② 强韧肌肤屏障\n③ 田总亲测3个月对比图', 1),
(4, '赫莲娜黑绷带面霜50ml', 'B', '抗衰天花板，直播间独家套装', 2580.00, 1200.00, 50, '爆品,抗衰,玻色因', 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.53, '① 30%高浓度玻色因\n② 院线级抗衰金标准\n③ 直播间买正装送15ml中样', 1),
(4, '修丽可CE精华30ml', 'B', '抗氧化鼻祖精华，皮肤科推荐TOP1', 1350.00, 650.00, 80, '爆品,抗氧化,医美', 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.52, '① 专利左旋VC+VE+阿魏酸\n② 皮肤科医生最推荐抗氧精华\n③ 医美术后修护必备', 1),
(4, '雅诗兰黛小棕瓶精华75ml', 'C', '经典控单品，稳定节奏', 780.00, 380.00, 150, '控单品,修护,经典', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.51, '① 二裂酵母经典配方\n② 维稳修护万金油\n③ 大容量更划算', 1),
(4, '芙清密钥积雪草面膜20片', 'K', '医美面膜引流品', 128.00, 88.00, 500, '亏品,引流,医美面膜', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.31, '① 医院同款积雪草成分\n② 医美后即刻修护\n③ 限量100单秒杀价', 1),
(4, '兰蔻菁纯眼霜20ml', 'R', '抗衰眼霜利润品', 860.00, 380.00, 120, '利润品,眼霜,抗衰', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.56, '① 玫瑰精萃+玻色因\n② 淡化眼纹效果显著\n③ 田总用了5年的回购款', 1),
(4, '娇韵诗双萃精华50ml', 'F', '水油双萃经典，过渡品', 590.00, 280.00, 180, '平价品,精华,经典', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.53, '① 水油双萃技术\n② 任何肤质都能用\n③ 好吸收不油腻', 1);

-- 王智慧（uid=5）：成分党平价好物
INSERT INTO dy_product (user_id, product_name, product_category, description, price, cost_price, inventory, tags, status, featured, deleted, create_time, update_time, profit_margin_pct, ai_selling_points, version)
VALUES
(5, '至本舒颜修护洁面乳120g', 'K', '成分党公认最佳洁面之一', 59.00, 32.00, 1500, '亏品,引流,成分党', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.46, '① 氨基酸+APG复配体系\n② CosDNA全绿评分\n③ 成分党人手一支的洁面', 1),
(5, '优色林淡斑精华50ml', 'B', '德国药妆美白精华，论文背书', 239.00, 95.00, 400, '爆品,美白,德国药妆', 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.60, '① 光甘草定+烟酰胺双通路美白\n② 12周临床验证淡斑28%\n③ 德国百年药妆品牌', 1),
(5, '溪木源层孔菌精华液', 'B', '国货之光油皮精华，控油+修护', 169.00, 52.00, 600, '爆品,国货,油皮', 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.69, '① 层孔菌提取物调节皮脂\n② 三重神经酰胺修护屏障\n③ 质地清爽零负担', 1),
(5, 'CeraVe保湿乳473ml', 'R', '神经酰胺大碗保湿', 149.00, 68.00, 800, '利润品,保湿,大碗装', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.54, '① 3种神经酰胺+透明质酸\n② MVE缓释保湿技术\n③ 皮肤科推荐第一保湿乳', 1),
(5, '露得清A醇晚霜48g', 'R', '平价A醇入门首选', 189.00, 72.00, 350, '利润品,A醇,抗老', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.62, '① 视黄醇SA缓释技术\n② 减少刺激同时有效抗皱\n③ 性价比最高的A醇产品', 1),
(5, '芙丽芳丝洗面奶净润洗面霜', 'F', '温和洁面平价标杆', 98.00, 42.00, 900, '平价品,洁面,温和', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.57, '① 6种和汉植物精粹\n② 零皂基氨基酸体系\n③ 过敏肌安心之选', 1),
(5, '凡士林身体乳400ml', 'C', '控单品平价过渡', 49.00, 18.00, 2000, '控单品,身体乳,平价', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.63, '① 烟酰胺亮白身体乳\n② 大碗装全身涂不心疼\n③ 持久滋润不油腻', 1);

-- 肖瑶（uid=6）：Z世代平价彩妆护肤
INSERT INTO dy_product (user_id, product_name, product_category, description, price, cost_price, inventory, tags, status, featured, deleted, create_time, update_time, profit_margin_pct, ai_selling_points, version)
VALUES
(6, '花西子空气蜜粉饼', 'B', '国风彩妆颜值天花板', 129.00, 42.00, 700, '爆品,定妆,国风', 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.67, '① 花瓣浮雕国风设计\n② 微米级粉质隐形毛孔\n③ 控油定妆12h不暗沉', 1),
(6, 'INTO YOU唇泥系列', 'B', '断货王唇泥，色号齐全', 69.00, 22.00, 1000, '爆品,唇泥,学生党', 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.68, '① 泥状质地不拔干\n② 显色度MAX不挑皮\n③ 直播间3支装更划算', 1),
(6, '酵色腮红高光一体盘', 'R', '日杂氛围感腮红，Z世代最爱', 79.00, 28.00, 500, '利润品,腮红,氛围感', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.65, '① 腮红+高光二合一\n② 日系通透妆感\n③ 新手友好不易翻车', 1),
(6, '橘朵眼影盘9色', 'F', '平价眼影入门盘', 59.00, 18.00, 800, '平价品,眼影,配色好', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.69, '① 粉质细腻不飞粉\n② 大地色系百搭日常\n③ 学生党第一盘首选', 1),
(6, '谷雨光感水乳套装', 'R', '平价美白水乳套装', 158.00, 52.00, 600, '利润品,美白,水乳', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.67, '① 光甘草定+烟酰胺美白\n② 清爽质地油皮友好\n③ 学生党入门美白首选', 1),
(6, '半亩花田磨砂膏250g', 'K', '引流品身体护理', 39.00, 22.00, 1500, '亏品,引流,身体护理', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.44, '① 烟酰胺+果酸双重亮白\n② 大颗粒温和去角质\n③ 一杯奶茶钱的全身SPA', 1),
(6, 'ukiss卸妆膏90ml', 'C', '温和卸妆控单品', 89.00, 35.00, 400, '控单品,卸妆,温和', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.61, '① 以油溶油温和卸除\n② 膏体遇水乳化超干净\n③ 眼唇面一支搞定', 1);

-- 肖蝉（uid=7）：走心好物，护肤+生活
INSERT INTO dy_product (user_id, product_name, product_category, description, price, cost_price, inventory, tags, status, featured, deleted, create_time, update_time, profit_margin_pct, ai_selling_points, version)
VALUES
(7, '雪花秀滋阴水乳套装', 'B', '韩方护肤经典，蝉姐回购3年的心头好', 580.00, 260.00, 200, '爆品,韩方,滋润', 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.55, '① 人参精粹+韩方5味子\n② 蝉姐用了3年的自留款\n③ 秋冬干皮救星套装', 1),
(7, '欧舒丹护手霜6支礼盒', 'R', '送礼自用两相宜的利润品', 298.00, 130.00, 300, '利润品,护手霜,送礼', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.56, '① 普罗旺斯植物精粹\n② 6种香型满足不同心情\n③ 精美礼盒送人超有面子', 1),
(7, '三谷氨基酸洗发水500ml', 'F', '平价氨基酸洗护', 79.00, 28.00, 800, '平价品,洗护,氨基酸', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.65, '① 氨基酸温和配方\n② 蓬松控油72h\n③ 洗完头发不假滑', 1),
(7, '佰草集太极面膜泥', 'K', '中草药面膜引流品', 89.00, 55.00, 600, '亏品,引流,面膜', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.38, '① 太极阴阳双效调理\n② 深层清洁+滋养二合一\n③ 蝉姐每周必敷的肌底清洁', 1),
(7, 'WIS水润面霜50g', 'R', '保湿面霜利润品', 128.00, 42.00, 500, '利润品,面霜,保湿', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.67, '① 透明质酸+角鲨烷双保湿\n② 上脸就能感受到水润\n③ 性价比超高的保湿面霜', 1),
(7, '蜂花护发素450ml', 'C', '国民品牌控单品', 19.90, 6.50, 2000, '控单品,护发,国民品牌', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.67, '① 40年国民品牌\n② 柔顺不油腻\n③ 全网最低价直播间专享', 1),
(7, '袋鼠妈妈孕妇护肤套装', 'R', '孕妈专属安全护肤', 258.00, 95.00, 250, '利润品,孕妇,安全', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.63, '① 0添加孕期安全配方\n② 小麦胚芽精华温和滋养\n③ 蝉姐怀孕时用的同款', 1);

-- ============================================================
-- 4. 获取新建的人设ID和账号ID，创建直播场次
-- ============================================================
-- 需要用 DO 块来获取动态 ID
DO $$
DECLARE
    -- account IDs
    acc_lyy BIGINT; acc_tlh BIGINT; acc_wzh BIGINT; acc_xy BIGINT; acc_xc BIGINT;
    -- persona IDs
    per_lyy BIGINT; per_tlh BIGINT; per_wzh BIGINT; per_xy BIGINT; per_xc BIGINT;
    -- session IDs
    sess_lyy BIGINT; sess_tlh BIGINT; sess_wzh BIGINT; sess_xy BIGINT; sess_xc BIGINT;
    -- product IDs (for bindings and scripts)
    pid BIGINT;
    -- temp vars
    seq INT;
BEGIN
    -- 获取账号ID
    SELECT id INTO acc_lyy FROM douyin_account WHERE user_id = 3 AND deleted = 0 LIMIT 1;
    SELECT id INTO acc_tlh FROM douyin_account WHERE user_id = 4 AND deleted = 0 LIMIT 1;
    SELECT id INTO acc_wzh FROM douyin_account WHERE user_id = 5 AND deleted = 0 LIMIT 1;
    SELECT id INTO acc_xy FROM douyin_account WHERE user_id = 6 AND deleted = 0 LIMIT 1;
    SELECT id INTO acc_xc FROM douyin_account WHERE user_id = 7 AND deleted = 0 LIMIT 1;

    -- 获取人设ID
    SELECT id INTO per_lyy FROM dy_persona WHERE owner_id = 3 AND deleted = 0 LIMIT 1;
    SELECT id INTO per_tlh FROM dy_persona WHERE owner_id = 4 AND deleted = 0 LIMIT 1;
    SELECT id INTO per_wzh FROM dy_persona WHERE owner_id = 5 AND deleted = 0 LIMIT 1;
    SELECT id INTO per_xy FROM dy_persona WHERE owner_id = 6 AND deleted = 0 LIMIT 1;
    SELECT id INTO per_xc FROM dy_persona WHERE owner_id = 7 AND deleted = 0 LIMIT 1;

    RAISE NOTICE '账号ID: lyy=%, tlh=%, wzh=%, xy=%, xc=%', acc_lyy, acc_tlh, acc_wzh, acc_xy, acc_xc;
    RAISE NOTICE '人设ID: lyy=%, tlh=%, wzh=%, xy=%, xc=%', per_lyy, per_tlh, per_wzh, per_xy, per_xc;

    -- ================================================================
    -- 4.1 创建5个直播场次
    -- ================================================================

    -- 李阳阳：重付费投流场（heavy_paid_category）
    INSERT INTO live_session (user_id, account_id, live_title, live_description, scheduled_time, status, deleted, create_time, update_time, persona_id, session_type, live_format)
    VALUES (3, acc_lyy, '【阳阳专场】油皮逆袭！爆品秒杀夜', '油皮护肤专场，绿钻套装+珀莱雅双抗限量秒杀，全场满减叠加', CURRENT_TIMESTAMP + INTERVAL '2 days', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, per_lyy, 'standard', 'heavy_paid_category')
    RETURNING id INTO sess_lyy;

    -- 田玲红：重付费投流场（heavy_paid_category）
    INSERT INTO live_session (user_id, account_id, live_title, live_description, scheduled_time, status, deleted, create_time, update_time, persona_id, session_type, live_format)
    VALUES (4, acc_tlh, '【田总严选】院线级抗衰专场·大牌直降', '田总亲选高端抗衰产品线，赫莲娜/修丽可/海蓝之谜品牌专场，全场买赠升级', CURRENT_TIMESTAMP + INTERVAL '3 days', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, per_tlh, 'standard', 'heavy_paid_category')
    RETURNING id INTO sess_tlh;

    -- 王智慧：内容主导型（content_led）
    INSERT INTO live_session (user_id, account_id, live_title, live_description, scheduled_time, status, deleted, create_time, update_time, persona_id, session_type, live_format)
    VALUES (5, acc_wzh, '【成分研究所】论文级成分拆解·理性种草', '本期深度解析烟酰胺/A醇/神经酰胺三大成分，推荐真正有效的平价产品', CURRENT_TIMESTAMP + INTERVAL '2 days', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, per_wzh, 'standard', 'content_led')
    RETURNING id INTO sess_wzh;

    -- 肖瑶：微付费自然流（organic_micro_paid）
    INSERT INTO live_session (user_id, account_id, live_title, live_description, scheduled_time, status, deleted, create_time, update_time, persona_id, session_type, live_format)
    VALUES (6, acc_xy, '【瑶瑶好物局】学生党开学季平价种草', '开学季平价好物大集合！彩妆+护肤全覆盖，均价不过百', CURRENT_TIMESTAMP + INTERVAL '1 day', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, per_xy, 'standard', 'organic_micro_paid')
    RETURNING id INTO sess_xy;

    -- 肖蝉：内容电商型+聊天（content_commerce, chat_2h）
    INSERT INTO live_session (user_id, account_id, live_title, live_description, scheduled_time, status, deleted, create_time, update_time, persona_id, session_type, live_format)
    VALUES (7, acc_xc, '【蝉姐夜话】闺蜜聊天局·走心好物分享', '今晚聊聊婆媳关系和职场减压，顺便分享蝉姐自用的走心好物', CURRENT_TIMESTAMP + INTERVAL '1 day', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, per_xc, 'chat_2h', 'content_commerce')
    RETURNING id INTO sess_xc;

    RAISE NOTICE '场次ID: lyy=%, tlh=%, wzh=%, xy=%, xc=%', sess_lyy, sess_tlh, sess_wzh, sess_xy, sess_xc;

    -- ================================================================
    -- 4.2 商品绑定到场次（live_product）
    -- ================================================================

    -- 李阳阳场次绑定7个商品
    INSERT INTO live_product (session_id, product_id, product_name, position, user_id, deleted, create_time, update_time)
    SELECT sess_lyy, p.id, p.product_name, ROW_NUMBER() OVER (ORDER BY p.id), 3, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM dy_product p WHERE p.user_id = 3 AND p.deleted = 0;

    -- 田玲红场次绑定7个商品
    INSERT INTO live_product (session_id, product_id, product_name, position, user_id, deleted, create_time, update_time)
    SELECT sess_tlh, p.id, p.product_name, ROW_NUMBER() OVER (ORDER BY p.id), 4, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM dy_product p WHERE p.user_id = 4 AND p.deleted = 0;

    -- 王智慧场次绑定7个商品
    INSERT INTO live_product (session_id, product_id, product_name, position, user_id, deleted, create_time, update_time)
    SELECT sess_wzh, p.id, p.product_name, ROW_NUMBER() OVER (ORDER BY p.id), 5, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM dy_product p WHERE p.user_id = 5 AND p.deleted = 0;

    -- 肖瑶场次绑定7个商品
    INSERT INTO live_product (session_id, product_id, product_name, position, user_id, deleted, create_time, update_time)
    SELECT sess_xy, p.id, p.product_name, ROW_NUMBER() OVER (ORDER BY p.id), 6, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM dy_product p WHERE p.user_id = 6 AND p.deleted = 0;

    -- 肖蝉场次绑定7个商品
    INSERT INTO live_product (session_id, product_id, product_name, position, user_id, deleted, create_time, update_time)
    SELECT sess_xc, p.id, p.product_name, ROW_NUMBER() OVER (ORDER BY p.id), 7, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM dy_product p WHERE p.user_id = 7 AND p.deleted = 0;

    -- ================================================================
    -- 5. 话术槽位（live_script）
    -- ================================================================
    -- 使用通用的 INSERT 语句创建各主播的话术

    -- =============== 李阳阳（heavy_paid_category）===============
    -- 标准 14 槽结构：opening + 5*(product + transition) + closing + 2*reserve
    -- 简化为关键槽位

    INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status)
    VALUES
    -- 开场
    (sess_lyy, 3, 'opening', 1,
     '家人们晚上好！我是你们的阳阳！今天这个专场我准备了整整一周，为什么？因为今天每一款产品都是我亲测至少30天以上的真爱款！油皮姐妹听好了，今天的价格我跟品牌方磨了三天三夜，直接给到全网最低！先点个关注不迷路，一会儿开福袋抽免单！3、2、1，咱们正式开始！',
     120, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    -- 产品1：致朵绿钻（爆品B）
    (sess_lyy, 3, 'product', 2,
     '第一款！直接上我们的王炸！致朵绿钻嫩肤清颜五件套！这个套装我自己用了三个月，你们看我这个出油量对比图——左边是三个月前，右边是现在。同一个手机同一个光线拍的，肉眼可见的哑光感！绿钻专利控油成分，8小时不脱妆不暗沉。而且5件套全含：洁面+水+精华+乳+霜，一套解决所有问题。原价598，今天直播间直接对折——298拿走！再送一个正装洁面！限量500套，倒计时开始！',
     300, '爆品主推，强调实测效果和限时优惠', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    -- 过渡1
    (sess_lyy, 3, 'transition', 3,
     '抢到的姐妹扣"抢到了"让我看看！没抢到的别急，后面还有更猛的。趁这个空档跟大家说一下，今天全场满299减30，满499减80，叠加优惠券更划算，先去领券！',
     60, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    -- 产品2：珀莱雅双抗（爆品B）
    (sess_lyy, 3, 'product', 4,
     '接下来这款，国货之光中的战斗机——珀莱雅双抗精华2.0！虾青素的抗氧化能力是维C的6000倍，麦角硫因抗糖化。我之前做过一个28天实测，额头的暗沉真的淡了一个色号。质地就是水状精华，油皮上脸零负担。原价269，今天189！买两瓶再减20！姐妹们这个用量一瓶大概用两个月，囤两瓶直接用到年底！',
     300, '国货爆品，强调数据和性价比', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    -- 过渡2
    (sess_lyy, 3, 'transition', 5,
     '双抗精华拍了的姐妹记得回来晒单，我每周抽3个晒单送小样！来，喝口水，下一个产品也是个狠角色。新来的姐妹先点个关注，一会儿有福袋！',
     60, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    -- 产品3：薇诺娜（利润品R）
    (sess_lyy, 3, 'product', 6,
     '第三款！敏感肌的救命稻草——薇诺娜特护霜！这个品牌不用我多说了吧？皮肤科医生开处方推荐的。马齿苋舒缓泛红，青刺果修护屏障。换季烂脸的时候，薄涂一层第二天就能感觉到稳定下来了。我混油皮换季必备，冬天T区起皮也靠它。268元，买一送同款15ml中样+舒缓面膜2片。敏感肌直接闭眼入！',
     240, '利润品，强调医学背书和修护效果', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    -- 过渡3
    (sess_lyy, 3, 'transition', 7,
     '护肤最重要的就是屏障健康，屏障好了其他问题都好解决。好的，咱们继续！来看看评论区大家还想看什么——有人说要卸妆，别急，后面安排了！',
     60, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    -- 产品4：理肤泉面膜（亏品K）
    (sess_lyy, 3, 'product', 8,
     '福利来了！这款不赚钱纯粹给家人们发福利——理肤泉B5面膜5片装！法国药妆NO.1品牌，B5修护+玻尿酸保湿。专柜单片就要39，今天5片装99块！相当于一片不到20！库存只有1000份，卖完涨回原价。3号链接，倒计时上架！',
     180, '亏品引流，强调价格优势和限量', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    -- 过渡4
    (sess_lyy, 3, 'transition', 9,
     '面膜秒完了！你们手速真的快！没抢到的姐妹等一下，第二轮我再放200份。关注+粉丝团的优先哈！',
     45, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    -- 产品5：自然堂（利润品R）
    (sess_lyy, 3, 'product', 10,
     '接下来是一个抗初老入门款——自然堂小紫瓶精华！25岁以上的姐妹注意了，抗老要趁早！喜马拉雅冰川水打底，加上烟酰胺提亮，用完皮肤透亮感很明显。这款是我推荐给我妹妹的入门抗老精华，她大学刚毕业预算有限但又想开始抗老。159块钱，买一送同款30ml中样，相当于买一送半！',
     240, '利润品，面向年轻群体讲抗初老', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    -- 产品6：OLAY超A瓶（控单品C）
    (sess_lyy, 3, 'product', 11,
     '然后是大家呼声很高的——OLAY超A瓶！烟酰胺鼻祖品牌，美白提亮的实力不用多说。28天就能看到肤色均匀度提升。249元专柜价，我们直播间同价但送旅行装3件套！这个品控价很严，能给到赠品已经是极限了。想要美白的姐妹直接拍！',
     180, '控单品，稳定客单价', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    -- 产品7：完美日记卸妆油（平价F）
    (sess_lyy, 3, 'product', 12,
     '最后一个！评论区喊了无数遍的卸妆——完美日记卸妆油200ml！氨基酸温和配方，浓妆淡妆一抹就净。大容量200ml能用好几个月。59块！不到一杯奶茶钱的两倍！学生党直接冲！这个卸妆油我拿来卸防晒也好用，乳化速度超快。',
     180, '平价引流品，面向学生党', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    -- 收尾
    (sess_lyy, 3, 'closing', 13,
     '好啦家人们！今天的7款产品全部过完了！大家记得确认收货后来晒单，每周五我抽10个晒单送正装好礼！还没领到优惠券的赶紧去领，满减活动到今晚12点截止！感谢每一位家人的陪伴，阳阳爱你们！明天下午3点我做一场护肤答疑专场，有任何皮肤问题都可以来问我！晚安，比心！',
     120, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed');


    -- =============== 田玲红（heavy_paid_category）===============
    INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status)
    VALUES
    (sess_tlh, 4, 'opening', 1,
     '晚上好，欢迎来到田总严选。今天是我们的院线级抗衰专场。在座的姐妹可能都有一个困惑：为什么花了很多钱，皮肤状态还是不理想？问题出在哪？今天我会用专业的视角，带大家认识真正值得投资的抗衰产品。每一款我都会讲清楚成分原理，为什么它值这个价。咱们不交智商税，只选对的。',
     120, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_tlh, 4, 'product', 2,
     '开场第一款，我直接拿出压箱底的——赫莲娜黑绷带面霜。为什么叫它抗衰天花板？因为它含有30%高浓度玻色因。玻色因是欧莱雅集团的专利成分，临床数据证实它能促进胶原蛋白合成。30%是什么概念？市面上大部分产品能做到5%就已经在宣传了。黑绷带直接30%，这就是院线级和普通产品的差距。质地是绷带膜感，上脸有即时提拉感。今天2580，我们加赠15ml中样和黑白绷带体验套装。这个价格全年只有今天。',
     360, '核心爆品，详细讲解成分原理', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_tlh, 4, 'transition', 3,
     '有姐妹问"我35岁用黑绷带会不会太早"——完全不会。抗衰是预防大于治疗，25+就可以开始。好，下一款和黑绷带是绝配的。',
     60, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_tlh, 4, 'product', 4,
     '修丽可CE精华——如果说黑绷带管"修"，那CE精华管"防"。左旋维C+维E+阿魏酸，这个配方被称为Duke抗氧化体系，有超过20年的临床研究。它能中和自由基，预防光老化。使用顺序：早上CE精华打底+防晒，晚上黑绷带修护。这一攻一守，你的抗衰方案就完整了。1350元，今天买送修丽可防晒中样和色修精华体验装。',
     300, '搭配销售，讲清使用逻辑', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_tlh, 4, 'transition', 5,
     '姐妹们记住一句话：白天抗氧化，晚上抗衰老。这是皮肤科医生的共识。好的，接下来这款是我自己用了5年的。',
     45, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_tlh, 4, 'product', 6,
     '海蓝之谜精粹水——这款我必须多说两句。很多人觉得1000多的水不值得，但你一旦用过就回不去了。Miracle Broth™ 活性精粹是海蓝之谜的核心科技，它在发酵过程中产生了上千种微量活性成分。上脸是微微的黏稠感，但吸收后皮肤柔软度完全不一样。我的使用方法：双手按压法，掌心温热后轻拍至吸收。1150元，今天下单送同品牌洁面乳正装。',
     300, '高端产品，讲使用方法和体验', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_tlh, 4, 'product', 7,
     '兰蔻菁纯眼霜——抗衰千万不能忽略眼周！眼周皮肤厚度只有面部的1/3，最先出现细纹的地方。菁纯系列用的是玫瑰精萃+玻色因，双管齐下。我用了这款之后最明显的感受是眼周的干纹少了，上妆不卡粉。860元，送菁纯小黑瓶5ml。',
     240, '利润品，强调眼周护理重要性', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_tlh, 4, 'transition', 8,
     '已经有姐妹下单了一整套，这位姐妹眼光很好——全套搭配效果1+1>2。下面这款是福利品，纯粹回馈大家。',
     45, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_tlh, 4, 'product', 9,
     '芙清积雪草面膜20片装——很多医美机构术后都用这个品牌。积雪草苷是修护领域的明星成分，促进胶原合成、舒缓炎症。医美后第一天就可以用。128元20片，一片6块多，比你去药店买都便宜。限量500份。',
     180, '亏品引流，医美面膜', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_tlh, 4, 'product', 10,
     '雅诗兰黛小棕瓶75ml大容量——这个不用我多介绍了，二裂酵母经典修护精华。今天主要讲为什么推荐75ml大瓶装：第一，大容量每毫升更便宜；第二，精华类产品坚持用3个月以上效果最明显。780元75ml，加赠同款15ml和眼霜5ml。这个组合用到明年都够了。',
     240, '控单品，讲大容量性价比', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_tlh, 4, 'product', 11,
     '最后一款——娇韵诗双萃精华50ml。水油双萃技术，任何肤质、任何年龄都能用。我把它推荐给不知道选什么精华的姐妹，因为它不会出错。590元，加赠双萃眼精华中样。这款作为护肤流程中的万能精华，性价比很高。',
     180, '平价品过渡，万能推荐', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_tlh, 4, 'closing', 12,
     '今天的院线级抗衰专场到这里，感谢每一位姐妹的信任。护肤是一个长期主义的事情，选对产品、坚持使用，时间会给你答案。有任何使用问题可以在粉丝群里找我，我会一一解答。下周三同一时间，我们做一场敏感肌修护专场，提前关注不错过。晚安。',
     90, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed');


    -- =============== 王智慧（content_led）===============
    INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status)
    VALUES
    (sess_wzh, 5, 'opening', 1,
     '大家好，欢迎来到成分研究所。我是王智慧，今天这期主题是"护肤三大基石成分深度解析"。我会用论文数据带大家搞清楚：烟酰胺到底怎么美白？A醇抗老的真实效果如何？神经酰胺为什么被称为屏障修护之王？搞懂了原理，你就不会再交智商税了。先点关注，今天干货密度很高。',
     120, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    -- 内容段：成分科普
    (sess_wzh, 5, 'chat', 2,
     '先说烟酰胺。它的美白原理是什么？简单说就是抑制黑色素转运——不是阻止你产生黑色素，而是阻止已经产生的黑色素跑到皮肤表面。2005年有一篇发表在British Journal of Dermatology上的临床研究显示，5%浓度的烟酰胺使用12周后，色斑面积减少35%。但注意，低于2%浓度基本无效，高于10%可能刺激。所以选产品看浓度很关键。',
     300, '科普烟酰胺，引用论文', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    -- 产品推荐1：优色林（爆品B）
    (sess_wzh, 5, 'product', 3,
     '讲完原理来推荐产品——优色林淡斑精华。这款用的是光甘草定+烟酰胺双通路美白方案。光甘草定直接抑制酪氨酸酶活性，从源头减少黑色素生成；烟酰胺负责阻断转运。两条路径同时走，效率翻倍。而且优色林是德国百年药妆品牌，他们自己做了为期12周的临床测试：淡斑效果28%。239元50ml，这个价格对于一款有临床数据支撑的美白精华来说，非常合理。',
     300, '爆品，用科学数据说话', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_wzh, 5, 'transition', 4,
     '有同学问"烟酰胺不耐受怎么办"——先从低浓度产品建立耐受，比如2%开始，一周后逐渐增加使用频率。好，接下来讲第二个成分。',
     60, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_wzh, 5, 'chat', 5,
     'A醇，也就是视黄醇。它是目前唯一有充分临床证据证明能刺激真皮层胶原蛋白新生的非处方成分。注意我说的是"非处方"——处方级的维A酸更强，但刺激也大。A醇需要在皮肤里经过两步转化才能变成视黄酸发挥作用，所以它比处方药温和得多，但效果也需要更长时间。一般8-12周开始看到改善。使用黄金法则：晚上用、从低浓度开始、一定要配合防晒。',
     300, '科普A醇抗老', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_wzh, 5, 'product', 6,
     '推荐的A醇产品——露得清A醇晚霜。它用的是视黄醇SA缓释技术，什么意思呢？就是A醇不是一下子全部释放，而是缓慢渗透。这样既减少了刺激，又保持了长时间的有效性。189元48g，这个价格买到缓释技术的A醇产品，市面上找不到第二个。这是我推荐给所有想入门A醇的朋友的第一选择。',
     240, '利润品，A醇推荐', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_wzh, 5, 'chat', 7,
     '第三个成分——神经酰胺。它本身就是皮肤屏障的组成成分，占角质层脂质的50%。当你觉得皮肤"屏障受损"的时候，本质上就是神经酰胺流失了。补充外源性神经酰胺可以快速修复屏障，减少水分流失。而且它和胆固醇、脂肪酸按1:1:1的黄金比例搭配效果最好。',
     240, '科普神经酰胺', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_wzh, 5, 'product', 8,
     'CeraVe保湿乳——如果你只能买一款保湿产品，我推荐这个。三种神经酰胺+透明质酸+MVE缓释保湿技术。MVE技术类似于洋葱层层包裹，保湿成分缓慢释放，持续24小时。473ml大碗装149元，折算下来每毫升3毛钱，皮肤科推荐第一保湿乳，性价比天花板。',
     240, '利润品，神经酰胺产品', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_wzh, 5, 'product', 9,
     '洁面推荐——至本舒颜修护洁面乳。这款在CosDNA上评分全绿，氨基酸+APG复合体系，既温和又有足够的清洁力。59元120g，成分党几乎人手一支。还有芙丽芳丝净润洁面霜，98元，六种和汉植物精粹+零皂基配方，适合极度敏感的肌肤。这两款选其一都不会出错。',
     240, '洁面推荐，对比两款', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_wzh, 5, 'product', 10,
     '最后推荐一个身体护理——凡士林烟酰胺身体乳400ml。49块钱，烟酰胺美白成分在身体乳里也有效。大碗装用起来不心疼，秋冬全身涂。这个作为凑单品很合适，把满减额度用起来。',
     120, '控单品，快速过', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_wzh, 5, 'closing', 11,
     '今天三大成分讲完了，总结一下：美白选烟酰胺，抗老选A醇，修护选神经酰胺。三条路线互不冲突，可以组合使用。我把今天提到的所有论文链接整理在粉丝群里了，感兴趣的同学加群自取。下期预告：防晒成分大比拼——物理防晒vs化学防晒到底选哪个？关注我不错过。',
     90, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed');


    -- =============== 肖瑶（organic_micro_paid）===============
    INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status)
    VALUES
    (sess_xy, 6, 'opening', 1,
     '宝子们来啦！你们的瑶瑶上线！今天是开学季特别企划——"百元以内搞定全套妆容+护肤"！没错你没听错，今天所有东西加一起不用500块！学生党和刚毕业的姐妹冲冲冲！先说今天的活动规则：关注+加粉丝团，全场再减10块，评论区扣"开学快乐"抽3个人送免单！',
     90, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_xy, 6, 'product', 2,
     '第一个必须是我的最爱！花西子空气蜜粉饼！你们看这个包装——花瓣浮雕，绝绝子好吗！拿出来补妆同学都会问你"这是什么"。但颜值只是加分项，重点是粉质！微米级粉体，上脸隐形毛孔，控油定妆12个小时不暗沉。我上课一天下来T区都不泛油光！129块！直播间拍还送一个迷你随身装！姐妹们这个颜值你忍得住吗？',
     240, '爆品，强调颜值和学生场景', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_xy, 6, 'product', 3,
     'INTO YOU唇泥！断货王来了！这个泥状质地真的绝，涂上去是哑光雾面感，但完全不拔干！我选了三个最百搭的色号：蜜桃奶茶色、脏橘色和玫瑰豆沙。日常素颜涂一个蜜桃色就超级好看。69块一支，但今天三支装只要179！平均一支不到60！我跟品牌方争取了好久的价格。喜欢的赶紧！',
     240, '爆品，色号推荐', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_xy, 6, 'transition', 4,
     '彩妆就先到这里，接下来给大家看护肤好物！在这之前先开个福袋——关注+评论"开学快乐"就能参与，3分钟后开奖！',
     60, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_xy, 6, 'product', 5,
     '谷雨光感水乳套装！美白界的性价比之王！光甘草定+烟酰胺双美白成分，清爽质地油皮也OK。水+乳一套158，学生党入门美白不用纠结了，就这个！我室友用了一个月，手臂和脖子色差明显变小了。早晚用，配合防晒效果更好。',
     180, '利润品，学生党美白', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_xy, 6, 'product', 6,
     '酵色腮红高光一体盘！这个是我最近的新宠！一盘搞定腮红和高光，日杂风那种通透妆感。颜色超级自然，新手也不容易翻车。79块！好看的颜色都选了，这种品质的腮红盘在日本买至少要200+。',
     180, '利润品，日系氛围感', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_xy, 6, 'product', 7,
     '橘朵9色眼影盘——学生党的第一盘眼影就选它！大地色系日常百搭，上课约会都能用。粉质细腻不飞粉，配色是专业彩妆师调的。59块9个颜色，一个颜色才6块多！旁边还有试色图，大家看看。',
     150, '平价品，眼影入门', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_xy, 6, 'transition', 8,
     '福袋开奖！恭喜这三位宝子！截图找客服领取哦。没中奖的别难过，一会儿还有秒杀福利！',
     45, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_xy, 6, 'product', 9,
     '身体护理来一个——半亩花田磨砂膏250g！烟酰胺+果酸双重亮白去角质。一杯奶茶的钱39块！洗澡的时候搓一搓，皮肤滑溜溜的。夏天露胳膊露腿之前必须安排上！',
     120, '亏品引流', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_xy, 6, 'product', 10,
     '卸妆必备——ukiss卸妆膏90ml。以油溶油温和卸除，膏体遇水乳化超干净。89块，眼唇面一支搞定，不用再单独买眼唇卸了！学生党省钱利器！',
     150, '控单品', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    (sess_xy, 6, 'closing', 11,
     '今天的开学季好物全部介绍完啦！算一下总价：蜜粉129+唇泥179+腮红79+眼影59+水乳158+磨砂膏39+卸妆膏89=732，加上满减优惠和粉丝券，600块就能搞定！全套妆容+护肤不到600，还要什么自行车！赶紧下单！记得关注瑶瑶，每周都有学生党专场福利！下次见，爱你们，么么哒！',
     90, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed');


    -- =============== 肖蝉（content_commerce + chat_2h）===============
    -- chat_2h 14 槽固定结构
    INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status)
    VALUES
    (sess_xc, 7, 'opening', 1,
     '家人们晚上好，蝉姐来了。今天不急着卖东西，咱们先聊聊天。最近有粉丝私信我说婆婆带娃的问题闹得家里鸡飞狗跳，其实这个事儿吧，换个角度想就通了。先倒杯水，咱们慢慢聊。新来的朋友先点个关注，蝉姐直播间就像你家客厅，随时来坐坐。',
     120, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    -- chat槽1：破冰暖场
    (sess_xc, 7, 'chat', 2,
     '说到婆媳关系，蝉姐有个观点：不是所有问题都需要解决，有些问题需要的是"和解"。我之前也因为育儿方式跟婆婆有分歧，她觉得孩子哭了就要抱，我觉得要培养独立性。后来我想通了一件事——婆婆不是来帮你带娃的，她是因为爱孙子才帮忙的。心态一变，很多事就释然了。评论区有类似经历的姐妹扣1，咱们一起聊聊。',
     600, '破冰暖场：婆媳关系', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    -- product槽1：雪花秀
    (sess_xc, 7, 'product', 3,
     '聊到心态好皮肤才好，正好给大家分享一个蝉姐的心头好——雪花秀滋阴水乳套装。这套我已经回购3年了，从来没换过。为什么？因为韩方的"滋阴"理念和我们说的"养内"是一个道理——先把底子养好，而不是光在表面折腾。人参精粹+五味子，越用皮肤底子越好。580元一套，今天下单我额外送一片雪花秀面膜。不急，一会儿再拍，蝉姐继续跟你们聊天。',
     300, '自然植入，不强推', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    -- transition
    (sess_xc, 7, 'transition', 4,
     '对了刚才有姐妹说"30岁之后觉得越来越焦虑"——这个话题太好了，等下咱们好好聊。先喝口水。',
     60, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    -- chat槽2：人生智慧
    (sess_xc, 7, 'chat', 5,
     '30岁焦虑这件事，蝉姐太有感触了。我30岁那年也焦虑，觉得该有的都没有。但你知道吗，焦虑的本质是"你在用别人的时钟衡量自己的人生"。每个人的花期不同，有人20岁就开了，有人35岁才绽放。我最喜欢的一句话是："不要因为走得太慢而焦虑，只要你还在走就好。"姐妹们，人生没有标准答案。你觉得好的节奏，就是最好的节奏。',
     600, '聊天：30岁焦虑', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    -- product槽2：欧舒丹
    (sess_xc, 7, 'product', 6,
     '说到善待自己，蝉姐觉得生活中需要一些"小确幸"。比如涂护手霜这件小事——每天无数次洗手，手一干就觉得烦躁。但如果涂上一支好闻的护手霜，心情真的会好。欧舒丹这个6支礼盒，6种香型换着用。298元，自己用或者送闺蜜送妈妈都合适。好东西就是要跟身边的人一起分享。',
     240, '自然过渡到产品', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    -- chat槽3
    (sess_xc, 7, 'chat', 7,
     '有姐妹说"蝉姐讲个笑话呗"——好，来一个歇后语：外甥打灯笼——照旧（舅）！哈哈。还有一个我小时候外婆教我的：八月十五蒸年糕——趁早。告诉我们什么道理呢？想做的事情就趁早做，别等"准备好了"再开始，因为你永远准备不好的。这不也是我开始做直播的原因嘛，刚开始手都在抖，现在不也挺好的。',
     420, '幽默互动', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    -- product槽3：三谷洗发水
    (sess_xc, 7, 'product', 8,
     '对了，后台好多人问我头发怎么保养的。其实也没什么秘诀，就是洗发水选对了。三谷氨基酸洗发水，79块500ml，氨基酸温和配方蓬松控油。我之前用那种假滑的洗发水头发越洗越塌，换了三谷之后蓬松了很多。而且它是那种清淡的草本香味，不是浓烈的工业香精味。链接挂了，需要的自己拍，不强推哈。',
     180, '日用品自然推荐', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    -- chat槽4
    (sess_xc, 7, 'chat', 9,
     '来，读一首古诗。苏轼的："竹杖芒鞋轻胜马，谁怕？一蓑烟雨任平生。"我特别喜欢苏东坡，这个人被贬了一辈子但始终乐观。他被贬到黄州发明了东坡肉，被贬到惠州说"日啖荔枝三百颗"，被贬到海南还能教书育人。你看，人生不顺的时候，心态好就是最大的本钱。姐妹们日子再难也要记得笑一笑。',
     420, '心灵鸡汤，古诗词', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    -- product槽4
    (sess_xc, 7, 'product', 10,
     '继续分享好物——佰草集太极面膜泥，89元。这个是我每周日固定的"脸部SPA时间"。太极阴阳概念，深层清洁+滋养二合一。洗完之后皮肤有一种"会呼吸"的感觉，特别通透。我觉得女人给自己留15分钟敷面膜的时间，也是一种自我关爱。',
     180, '面膜推荐', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    -- chat槽5
    (sess_xc, 7, 'chat', 11,
     '评论区有位宝妈说"孩子3岁了还没上幼儿园，老公说我不赚钱只会花钱"——姐妹，蝉姐替你生气！全职妈妈不赚钱？你算算保姆费、早教费、家务费、伙食费，一个月至少值一万块。全职妈妈是世界上最辛苦的工作，因为没有下班时间。跟你老公说：你这辈子雇不起你老婆这么好的员工。姐妹们觉得对的扣1！',
     480, '共情力话题', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    -- product槽5
    (sess_xc, 7, 'product', 12,
     '说到宝妈，给宝妈们推荐一个——袋鼠妈妈孕妇护肤套装。258元，0添加孕期安全配方。蝉姐怀孕的时候用的就是这个，小麦胚芽精华温和滋养。我当时什么都不敢用就用它，生完之后皮肤状态还不错。孕期的姐妹或者身边有孕妈的，可以帮她拍一套。',
     180, '场景化推荐', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed'),

    -- closing
    (sess_xc, 7, 'closing', 13,
     '时间过得真快，两个小时不知不觉就过去了。今天跟大家聊了婆媳关系、年龄焦虑、全职妈妈的价值……每次跟你们聊天，蝉姐自己也被治愈了。记住蝉姐说的：你很好，你值得被爱，你的节奏就是最好的节奏。今天分享的好物链接都在购物车里，需要的自取。下次直播咱们聊"夫妻之间如何好好说话"，先关注先预约。晚安，爱你们的蝉姐。',
     120, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed');

END $$;
