-- ============================================================
-- 主播数据修正 + 李阳阳 2 小时顶级话术
-- ============================================================

-- ============================================================
-- 1. 人设修正
-- ============================================================

-- 李阳阳：东北人，治愈系知心姐姐，歇后语+名言金句
UPDATE dy_persona SET
    persona_name = '阳阳·东北知心姐姐',
    description = '东北姑娘，性格直爽但内心细腻。直播间像邻家大姐姐唠嗑，用东北话拉家常，时不时蹦出歇后语和名言金句，既幽默又走心。擅长在聊天中自然穿插好物推荐，粉丝觉得不是在看直播而是在跟闺蜜聊天。核心竞争力：真实、共情力强、东北式幽默治愈人心。',
    tone = '东北直爽、幽默走心、治愈温暖',
    target_audience = '25-40岁女性，职场女性/宝妈，追求真实和情感共鸣',
    content_style = '情感话题+歇后语幽默+名言金句+好物自然穿插',
    keywords = '东北姐姐,歇后语,名言金句,走心推荐,治愈系',
    local_flavor = '东北方言+歇后语+直爽幽默',
    persona_traits = '直爽真诚,幽默治愈,共情力爆表,东北式温暖',
    ip_type = '东北知心姐姐',
    content_ratio = '聊天50:好物25:互动25',
    live_style = '情感治愈+东北幽默型',
    age_range = '28-32',
    positioning_tags = '东北姐姐,歇后语王者,治愈系,知心大姐姐'
WHERE owner_id = 3 AND deleted = 0;

-- 田玲红：主播老大，过品清仓直播间50-100SKU
UPDATE dy_persona SET
    persona_name = '田总·团队大姐大',
    description = '全队大姐大，10年护肤品行业老兵。既是资深主播也是团队负责人，带领李阳阳、王智慧等主播。主攻过品直播间，一场清仓50-100款套盒，节奏极快，控场能力一流。专业严谨但气场强大，粉丝信任度极高。',
    tone = '专业权威、干练果断、气场全开',
    target_audience = '25-45岁女性，追求高性价比批量囤货，注重品牌品质',
    content_style = '快节奏过品+专业成分简析+限时秒杀',
    keywords = '田总严选,过品专场,套盒清仓,50款起步,闭眼入',
    live_style = '快节奏过品清仓型',
    content_ratio = '产品70:互动20:科普10',
    positioning_tags = '团队大姐大,过品女王,套盒清仓,50-100SKU'
WHERE owner_id = 4 AND deleted = 0;

-- 王智慧：跟田玲红一样做过品直播间
UPDATE dy_persona SET
    persona_name = '智慧·成分党过品师',
    description = '成分党+过品师双重身份。在田玲红带领下做护肤品过品直播间，一场50-100款套盒清仓。区别在于智慧会快速点评每款核心成分，用理性数据帮粉丝做选择。节奏快但每品都有干货。',
    tone = '理性专业、节奏明快、数据说话',
    target_audience = '22-38岁女性，理性消费者，追求成分透明和性价比',
    content_style = '快速过品+核心成分点评+性价比分析',
    keywords = '成分过品,理性种草,套盒清仓,配方拆解,性价比王',
    live_style = '成分解读+快节奏过品型',
    content_ratio = '产品65:成分科普25:互动10',
    positioning_tags = '成分党过品师,理性种草,套盒清仓,科学选品'
WHERE owner_id = 5 AND deleted = 0;

-- 肖瑶：20岁刚毕业学生，覆盖学生+刚毕业人群
UPDATE dy_persona SET
    persona_name = '瑶瑶·学生党代言人',
    description = '20岁，刚从学校毕业踏入社会的元气少女。直播间覆盖在校学生和刚毕业的年轻人。用同龄人视角推荐平价好物，分享毕业求职、初入职场的真实感受。没有距离感，就像身边的小姐妹在安利好东西。',
    tone = '元气满满、真实亲切、同龄人共鸣',
    target_audience = '18-23岁，在校学生+应届毕业生+初入社会1-2年，月消费200-800',
    content_style = '平价种草+学生党经验分享+毕业求职话题',
    keywords = '学生党代言人,毕业季,平价好物,同龄人推荐,元气少女',
    age_range = '20',
    positioning_tags = '学生党代言,20岁视角,平价好物,毕业季'
WHERE owner_id = 6 AND deleted = 0;

-- 肖蝉：25岁职场女性
UPDATE dy_persona SET
    persona_name = '蝉姐·职场新锐',
    description = '25岁职场女性，干练独立有主见。3年职场经验，分享真实的职场生存法则和好物推荐。目标人群是同龄的职场女性——工作压力大、需要高效护肤和生活好物。直播风格偏职场闺蜜聊天，既有职场干货也有好物种草。',
    tone = '干练独立、真诚务实、职场闺蜜感',
    target_audience = '23-30岁职场女性，白领/初入管理层，追求效率和品质',
    content_style = '职场经验分享+高效好物推荐+职场穿搭护肤',
    keywords = '职场新锐,高效护肤,职场女性,独立自信,品质生活',
    age_range = '25',
    ip_type = '职场新锐女性',
    live_style = '职场闺蜜聊天+好物种草型',
    positioning_tags = '职场女性,25岁视角,高效好物,独立自信'
WHERE owner_id = 7 AND deleted = 0;

-- ============================================================
-- 2. 场次修正
-- ============================================================

-- 田玲红：过品清仓 warehouse 格式
UPDATE live_session SET
    live_title = '【田总过品】护肤套盒清仓专场·50+款一次过完',
    live_description = '田总亲选50+款护肤套盒清仓，全场1-3折起，每款30秒快速过品，看中直接拍',
    live_format = 'warehouse',
    session_type = 'standard'
WHERE id = 12 AND deleted = 0;

-- 王智慧：过品清仓 multi_sku_speed 格式
UPDATE live_session SET
    live_title = '【智慧过品】成分党严选·60款套盒快速清仓',
    live_description = '成分党过品师智慧严选60款护肤套盒，每款快速点评核心成分+性价比，看配方选产品',
    live_format = 'multi_sku_speed',
    session_type = 'standard'
WHERE id = 13 AND deleted = 0;

-- 肖瑶：面向学生+应届生
UPDATE live_session SET
    live_title = '【瑶瑶好物局】毕业季平价种草·学生党闭眼入',
    live_description = '20岁瑶瑶的真实推荐！学生党和刚毕业的姐妹看过来，全场均价不过百'
WHERE id = 14 AND deleted = 0;

-- 肖蝉：25岁职场女性
UPDATE live_session SET
    live_title = '【蝉姐职场局】打工人的高效好物·职场闺蜜聊天夜',
    live_description = '25岁职场蝉姐的真心推荐，聊聊职场生存法则，顺便分享打工人必备好物',
    live_format = 'content_commerce'
WHERE id = 15 AND deleted = 0;

-- ============================================================
-- 3. 李阳阳：新建 chat_2h 聊天式2小时场次
-- ============================================================
DO $$
DECLARE
    acc_lyy BIGINT;
    per_lyy BIGINT;
    sess_id BIGINT;
BEGIN
    SELECT id INTO acc_lyy FROM douyin_account WHERE user_id = 3 AND deleted = 0 LIMIT 1;
    SELECT id INTO per_lyy FROM dy_persona WHERE owner_id = 3 AND deleted = 0 LIMIT 1;

    -- 创建 chat_2h 场次
    INSERT INTO live_session (user_id, account_id, live_title, live_description, scheduled_time, status, deleted, create_time, update_time, persona_id, session_type, live_format)
    VALUES (3, acc_lyy,
        '【阳阳夜话】东北姐姐唠嗑局·歇后语+金句+走心好物',
        '东北阳阳的治愈聊天夜！婆媳关系、职场解压、人生感悟，穿插歇后语和名言金句，好物自然种草不硬推',
        CURRENT_TIMESTAMP + INTERVAL '1 day',
        0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, per_lyy, 'chat_2h', 'content_commerce')
    RETURNING id INTO sess_id;

    RAISE NOTICE '李阳阳 chat_2h 场次ID: %', sess_id;

    -- 绑定商品到新场次
    INSERT INTO live_product (session_id, product_id, product_name, position, user_id, deleted, create_time, update_time)
    SELECT sess_id, p.id, p.product_name, ROW_NUMBER() OVER (ORDER BY p.id), 3, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM dy_product p WHERE p.user_id = 3 AND p.deleted = 0;

    -- ================================================================
    -- 李阳阳 chat_2h 话术（13槽 · 2小时完整版）
    -- 结构：opening + 5*(chat + product) + 2*emotional + closing
    -- 核心风格：东北直爽 + 歇后语 + 名言金句 + 治愈走心
    -- ================================================================

    -- ▎1. 开场（3分钟）
    INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status)
    VALUES (sess_id, 3, 'opening', 1,
'家人们！你们的阳阳来啦！哎呀妈呀今天外面冷得跟啥似的，我搁家穿着棉拖鞋就给你们开播了——

先说好啊，今天不是卖货专场！今天是阳阳的「唠嗑夜」，咱就是唠，唠到哪算哪。有啥烦心事儿的在评论区说，阳阳给你支招。当然中间碰到好东西我会跟你们分享，但绝对不硬推，你们随意哈。

我妈常说一句话：「黄鼠狼给鸡拜年——没安好心」——但阳阳不是黄鼠狼，阳阳是真心想跟你们唠嗑的！哈哈哈！

新来的家人先点个关注，万一你今天心情不好，以后就知道上哪找阳阳倒苦水了。来，咱今天第一个话题——你们最近有没有因为啥事儿生气上火的？评论区告诉我！',
    180, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed');

    -- ▎2. 聊天槽1：婆媳关系+夫妻相处（10分钟）
    INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status)
    VALUES (sess_id, 3, 'chat', 2,
'好多人说婆媳关系——来来来，这个话题阳阳有话说！

我跟你们讲个事儿。我婆婆去年来我家住了仨月，我俩差点没干起来——不是打架啊，是做饭。她非要往我炖的排骨里放花椒大料，我说妈你别放那么多，她说「你不懂，我儿子从小就吃这个味儿」。

我当时心里那个火啊——「狗咬吕洞宾，不识好人心」，我好心好意炖排骨你还嫌弃！

但后来我想通了。曾国藩说过一句话：「家和万事兴，无须终日口不停。」你跟婆婆争谁做饭好吃，争赢了又怎样？她心里不舒服，你老公夹在中间更难受。

所以我后来换了个策略：我做一个菜、她做一个菜，桌上一半东北味一半她的味儿。你猜怎么着？我俩现在经常交换菜谱，她教我做她家乡的糖醋鱼，我教她做东北锅包肉！

家人们记住阳阳一句话：「婆媳关系的秘诀不是谁让谁，是找到你们俩都舒服的位置。」就像那个歇后语说的——「擀面杖吹火——一窍不通」，有时候不是对方不讲理，是你俩说的不是一个频道。换个频道试试，说不定就通了。

评论区有类似经历的姐妹扣「过来人」三个字——',
    600, '破冰暖场：婆媳关系，东北歇后语+曾国藩名言', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed');

    -- ▎3. 产品槽1：致朵绿钻套装（5分钟·自然过渡）
    INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status)
    VALUES (sess_id, 3, 'product', 3,
'哎对了说到婆婆，我婆婆上次来的时候看我化妆台上摆了一排瓶瓶罐罐，说「你这脸上抹这么多东西，跟刷墙似的」——哈哈哈我差点没笑喷。

但说真的，我现在护肤已经极简了。就靠一套——致朵绿钻嫩肤清颜五件套。

姐妹们我不吹牛，这套我自己用了三个月，对比图你们看——同一个手机拍的，T区出油量明显少了。五件套一步到位：洁面、水、精华、乳、霜，不用东拼西凑。

我婆婆后来偷偷用了我的，跟我说「哎这个挺好使，不油腻」——你看，连我婆婆都认可了！老话说得好：「是骡子是马拉出来遛遛」，好不好用你试了才知道。

298块五件套，今天直播间还送一个正装洁面。需要的姐妹自己拍，不拍也没关系，咱继续唠——',
    300, '自然从婆媳话题过渡到护肤品', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed');

    -- ▎4. 过渡
    INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status)
    VALUES (sess_id, 3, 'transition', 4,
'好，产品放着不急，咱继续唠。刚才有个姐妹说她老公不理解她——来，这个话题阳阳也有话说！',
    45, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed');

    -- ▎5. 聊天槽2：夫妻关系+职场压力（10分钟）
    INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status)
    VALUES (sess_id, 3, 'chat', 5,
'老公不理解你什么？不理解你上班累还要回来做饭带娃？还是不理解你买护肤品花钱？

姐妹们，阳阳送你们一句话，鬼谷子说的：「欲闻其声，反默；欲张，反敛。」什么意思呢？就是你想让他听你的心声，你先别急着说，先听他说。很多时候夫妻吵架不是因为谁对谁错，是因为两个人都在说、没人在听。

我跟我老公有个规矩：吵架的时候可以说「我需要冷静10分钟」，这10分钟谁都不许说话。你别小看这10分钟——「热锅上的蚂蚁——团团转」，越急越说不清楚。冷静下来再说，80%的架都吵不起来了。

还有一个杀手锏：下次你老公惹你生气了，你不要发火，你就笑着说一句——「周瑜打黄盖——一个愿打一个愿挨，我愿意嫁给你，我就得认了呗」。你信不信他立马软了？哈哈哈！

玩笑归玩笑，但阳阳说句正经的：杨绛先生说过——「夫妻间最重要的是朋友关系，朋友关系才是最能持久的。」别把老公当敌人，把他当队友。你们是一起打怪升级的，不是来互相伤害的。

评论区在婚姻里感到幸福的扣「幸福」，觉得委屈的扣「抱抱」——阳阳都看着呢！',
    600, '夫妻关系话题，鬼谷子+杨绛名言+歇后语', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed');

    -- ▎6. 产品槽2：珀莱雅双抗+薇诺娜（5分钟）
    INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status)
    VALUES (sess_id, 3, 'product', 6,
'说到善待自己——姐妹们，对自己好一点不丢人！你为家庭付出那么多，花点钱护肤怎么了？

给你们推荐两个阳阳的自用款。第一个是珀莱雅双抗精华2.0——国货之光！虾青素抗氧化+麦角硫因抗糖化，两手抓两手硬。我用了一个月，额头暗沉真的淡了，同事问我是不是换了粉底——我说没有，是精华换了。189块，油皮上脸零负担。

第二个是薇诺娜特护霜——这个留给换季烂脸的姐妹。皮肤科医生推荐的品牌，马齿苋+青刺果，烂脸急救能手。268块。

老话说「磨刀不误砍柴工」，你把皮肤底子养好了，后面省多少钱啊！今天两瓶一起拍我额外送化妆棉和小样套装。需要的自己拍——

不需要的没关系！咱接着唠，下面要说一个重磅话题——',
    300, '双产品推荐，自然过渡', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed');

    -- ▎7. 情感金句槽1（5分钟·名言密集型）
    INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status)
    VALUES (sess_id, 3, 'emotional', 7,
'家人们，阳阳跟你们说几句心里话。

最近后台私信我收到最多的一句话是：「阳阳，我觉得我活得好累。」

累在哪呢？上有老下有小，工作压力大，回到家还要做饭洗碗辅导作业。觉得自己像个陀螺，一直在转，停不下来。

我特别想跟你们分享三句话——

第一句，莫言说的：「世界上的事情，最忌讳的就是个十全十美。你看那天上的月亮，一旦圆满了，马上就要亏厌。」别追求完美，你已经够好了。

第二句，是我们东北的土话：「车到山前必有路，船到桥头自然直。」你现在觉得过不去的坎，回头看都是小土包子。

第三句是阳阳自己的：「你不是超人，你只是一个想把每件事都做好的普通人。允许自己偶尔做得不好，这不丢人，这叫——人。」

歇后语怎么说来着？「竹篮打水——一场空」？不是！你的付出不是一场空，只是有些收获需要时间才能看见。就像种庄稼，春天播种秋天才收，你不能六月份就说「怎么还没结果」。

姐妹们，如果你今天走进阳阳直播间正好心情不好——阳阳抱抱你。深呼吸，跟我念：我很好，我在努力，我值得被爱。

来，评论区打出来：「我很好」——',
    300, '治愈向名言金句密集段，莫言+东北话+自创', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed');

    -- ▎8. 聊天槽3：育儿+家长里短（8分钟）
    INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status)
    VALUES (sess_id, 3, 'chat', 8,
'好，擦擦眼泪——阳阳不能老催泪弹，来点轻松的！

说个育儿趣事。我家那个小崽子，三岁半，昨天在幼儿园学了个词叫「合作」。回家跟我说：「妈妈我们合作吧！你负责做饭，我负责吃。」我说你这合作分工也太不均匀了吧！

老话说「三岁看大七岁看老」——我看我儿子以后是当老板的料，从小就会分配任务，自己干最轻松的那个。哈哈哈！

说到孩子教育，阳阳分享一个心得：别总跟孩子讲道理，讲故事比讲道理管用一万倍。你跟三岁小孩说「你要分享」他听不懂，你跟他说「小熊把蜂蜜分给小兔子，小兔子好开心」，他立马就懂了。

鲁迅说过：「教育是植根于爱的。」你对孩子的耐心就是最好的教育。

有时候我辅导我儿子写数字，5写成反的，我深吸一口气心里默念三遍「亲生的亲生的亲生的」——「哑巴吃黄连——有苦说不出」啊！但你不能发火，因为你一发火他就怕了，怕了就更写不好。

宝妈们是不是深有同感？评论区扣「亲生的」让我看看有多少同道中人——哈哈哈！',
    480, '育儿趣事，轻松幽默+鲁迅名言+歇后语', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed');

    -- ▎9. 产品槽3：理肤泉面膜+自然堂（福利型·4分钟）
    INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status)
    VALUES (sess_id, 3, 'product', 9,
'辛苦的宝妈们听好了——阳阳给你们发福利！

理肤泉B5面膜5片装——法国药妆NO.1，B5修护+玻尿酸保湿。你晚上哄完孩子睡了，给自己敷一片，15分钟安静时光送给自己。99块5片！一片不到20，比外面做脸便宜多了。

还有自然堂小紫瓶精华——25岁以上抗初老入门款。喜马拉雅冰川水+烟酰胺，用完皮肤透亮。159块买一送30ml中样，相当于买一瓶半。

俗话说「舍不得孩子套不着狼」——你连100多块的护肤品都不舍得给自己买，怎么有精力照顾一家人？先爱自己才能爱别人！

链接挂着，需要的自己拍。我再给评论区抽5个「亲生的」送面膜试用装——',
    240, '福利品+自然过渡', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed');

    -- ▎10. 互动槽：歇后语接龙+才艺（7分钟）
    INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status)
    VALUES (sess_id, 3, 'interaction', 10,
'来来来！轻松环节到了！阳阳要跟你们玩——「歇后语接龙大赛」！

规则很简单：我说上半句，你们在评论区打下半句，最快答对的我记住你，今天抽奖优先！

第一题：「外甥打灯笼」—— 对！「照旧（舅）」！太简单了是吧？

第二题：「猪八戒照镜子」—— 对！「里外不是人」！哈哈这个特应景，有时候在婆家既要讨好婆婆又要照顾老公，就是这个感觉！

第三题难一点：「老虎挂念珠」—— 「假慈悲」！恭喜这位姐妹答对了！

第四题：「孔夫子搬家」—— 「净是书（输）」！哈哈哈阳阳赌博从来没赢过就是这个道理！

最后一题超级难：「阎王爷贴告示」——「鬼话连篇」！答对的你是高手！

好好好，你们文化水平都比我高！来，这五道题全答对的截图发我，阳阳送你正装好礼！

顺便开个福袋！关注+评论「歇后语」三个字就能参与，3分钟后开奖！',
    420, '互动游戏：歇后语接龙，活跃氛围', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed');

    -- ▎11. 聊天槽4：人生智慧+古诗词（8分钟）
    INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status)
    VALUES (sess_id, 3, 'chat', 11,
'福袋等着开奖，阳阳跟你们聊点走心的。

有个姐妹私信我说：「阳阳，我35岁了，什么都没有，很焦虑。」

我问她：你有家人吗？她说有。你有工作吗？有。你身体健康吗？健康。那你怎么叫什么都没有？

我们这代人最大的问题就是——拿自己的生活跟朋友圈比。人家晒包你焦虑，人家晒车你焦虑，人家晒娃你还焦虑。但你知道吗？你朋友圈看到的只是「冰山一角」，水面下面的苦你看不见。

苏东坡被贬黄州的时候写了一首词：「莫听穿林打叶声，何妨吟啸且徐行。」什么意思？就是——管他风吹雨打，我慢慢走就好了。

你们知道苏东坡被贬了多少次吗？三次！杭州、黄州、惠州、海南——越贬越远。但他到哪就吃到哪，到黄州发明东坡肉，到惠州说「日啖荔枝三百颗」，到海南开始教书育人。

这才是真正的人生智慧：「既来之则安之，既然改变不了环境，就改变心态。」

我们东北有句话：「天塌下来有高个子顶着」——焦虑有用吗？没用。那就不焦虑了呗！把今天的日子过好，明天的事儿明天再说。

林清玄先生说：「以清净心看世界，以欢喜心过生活，以平常心生情味，以柔软心除挂碍。」

姐妹们，你现在的生活可能不完美，但它是你一步一步走出来的，这本身就了不起。35岁怎么了？阳阳觉得35岁是女人最美的年纪——该经历的都经历了，该放下的也放下了，剩下的就是好好爱自己。

来，深呼吸。跟我说一遍：「我的节奏，就是最好的节奏。」',
    480, '走心话题：年龄焦虑，苏东坡+林清玄名言+东北话', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed');

    -- ▎12. 产品槽4：OLAY+完美日记（3分钟·轻推）
    INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status)
    VALUES (sess_id, 3, 'product', 12,
'聊完心灵鸡汤，来点实在的。

两个快速推荐：OLAY超A瓶——烟酰胺鼻祖品牌，28天提亮，249块。还有完美日记卸妆油200ml——59块大碗装，浓妆一抹就净，学生党直接冲。

这两个就不多说了，口碑放在那。「酒香不怕巷子深」，好东西自己会说话。链接在购物车，需要的自己拍。

好！福袋开奖了——恭喜以下三位姐妹！截图找客服领取哈。咱们继续唠最后一个话题——',
    180, '快速过品，不强推', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed');

    -- ▎13. 情感金句槽2：收尾总结（5分钟·高潮）
    INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status)
    VALUES (sess_id, 3, 'emotional', 13,
'家人们，今天最后跟你们说的话，是阳阳真心想说的——

你们有没有想过，为什么每天晚上会打开阳阳的直播间？

不一定是为了买东西。有人是因为下班了一个人在家太安静，想听个人说话；有人是喂完奶孩子终于睡了，想偷一会儿闲；有人是跟老公吵完架了，想找个地方缓缓。

你们来了，阳阳就在。

季羡林老先生说过：「每个人都争取一个完满的人生。然而，自古及今，海内海外，一个百分之百完满的人生是没有的。所以我说，不完满才是人生。」

不完满才是人生——这句话我读了一百遍，每读一遍都有新的感悟。

我不是什么大主播，也不是什么人生导师。我就是一个东北丫头，做了妈妈、做了媳妇、做了直播——哪个角色都不完美，但每个角色我都在认真演。

你们也一样。你可能不是最好的妈妈，但你是那个半夜起来喂奶的人；你可能不是最好的妻子，但你是那个记得他喜欢吃什么的人；你可能不是最好的员工，但你是那个每天按时上班从不迟到的人。

这些——就够了。

老话说得好：「金无足赤，人无完人。」你不需要做到100分，80分的你，已经很了不起了。

最后送大家一句，是我自己写在墙上每天看的：

「生活不是等暴风雨过去，而是学会在风雨中起舞。」

谢谢你们今天陪阳阳唠了两个小时。阳阳爱每一个走进这个直播间的人。',
    300, '收尾情感高潮：季羡林名言+自创金句+真情流露', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed');

    -- ▎14. 收尾
    INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status)
    VALUES (sess_id, 3, 'closing', 14,
'好啦！两个小时不知不觉就过去了！

总结一下今天的好物：绿钻五件套298、珀莱雅双抗189、薇诺娜268、理肤泉面膜99、自然堂小紫瓶159、OLAY超A瓶249、完美日记卸妆油59——全在购物车里，需要的自己拍，不需要的没关系，阳阳不强求。

今天更重要的是跟你们聊了婆媳、夫妻、育儿、焦虑这些话题。记住阳阳说的：

「黄鼠狼给鸡拜年」那是别人，你家人给你的爱是真的；
「竹篮打水」不是一场空，你的付出终会被看见；
「车到山前必有路」，别急，慢慢来。

明天阳阳还在，后天也在。你随时来，我随时唠。

关注阳阳，明天下午3点我做一场「东北阳阳的护肤答疑」，有任何皮肤问题都可以来问。

晚安家人们！大东北的抱抱送给你们！么么哒——

「大姑娘上花轿——头一回」？不是！阳阳跟你们说晚安已经不知道多少回了！但每一回都是真心的！

好啦，下播！爱你们！',
    120, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed');

END $$;
