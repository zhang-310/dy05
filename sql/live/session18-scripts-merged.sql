-- ============================================================
-- 李阳阳过品直播间 · Session 18 · 全场话术（合并版）
-- 64产品快速过品 + 主推377/红石榴深度收割
-- 直播顺序：position 64→1（倒序），前排亏品拉数据，后排主推收割
--
-- 执行方式：
--   psql -h localhost -p 5433 -U postgres -d douyin_operations -f session18-scripts-merged.sql
-- ============================================================

BEGIN;

-- ─── 开场话术 (3-5分钟) ─────────────────────────────────────
UPDATE live_script SET script_content =
'【开场话术 · 3-5分钟 · 破冰起量】

家人们来了啊！我是阳阳！！！哎呀妈呀你们今天来得可真齐——拖鞋穿着就跟你们唠上了！我婆婆在后头喊「又对着手机傻乐呢」——我说妈这回不一样！今天阳阳要搞个大的——64个产品一口气给你们过！

你没听错！64个！「八仙过海——各显神通」——今天64个品全是神通！你说阳阳嘴皮子受得了吗？受不了也得受——谁让我疼你们呢！

先说好啊！今天是过品直播间！前面的品全是亏品——阳阳赔钱拉人气！8块！10块！12块！一整套护肤品！你没听错！「天上掉馅饼——不是陷阱就是惊喜」——今天就是实打实的惊喜！这些品阳阳亏着卖，就为了跟你们交个朋友！你拍了就赚了——你不拍阳阳也亏了！所以拜托你们——帮阳阳亏得有价值一点行不行？哈哈！

但是！重点来了！你们一定一定要等到两个主推品！377金钻七件套79块9！红石榴套盒89块！这两个是今天的重头戏！前面的亏品是开胃菜——后面才是满汉全席！「好戏在后头」——你要是只吃了开胃菜就走了那才叫「捡了芝麻丢了西瓜」！

新来的先把关注点上！「有缘千里来相会」——你划到阳阳就是缘分！不点关注的等下抢福利找不到直播间——到时候「大海捞针」啊姐妹！铃铛打开！

今天节奏是这样——前面亏品快速过！10秒20秒一个！你看上了直接拍！别犹豫！犹豫一秒就没了！后面主推品阳阳给你细细讲、慢慢唠——「磨刀不误砍柴工」！

觉得今天要捡大便宜的扣「来了」！准备好钱包的扣「准备好了」！钱包已经哭了的扣「别哭了」！

好！废话不多说！第一个产品上来了——8块钱！你没看错！白菜价都不止8块——走起！',
duration_limit_sec = 240,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2355;
-- ============================================================
-- Session 18 · 亏品快速过品话术 (Position 64→56, 最先上播)
-- K类/B类亏品，8-12元，10-20秒快速过
-- ============================================================

-- ─── #64 致朵绿钻嫩肤清颜五件套 ¥8 (product_id=68) ──────────
UPDATE live_script SET script_content =
'第一个品！致朵绿钻嫩肤清颜五件套！五件套！多少钱？8块！8块钱！你去买瓶矿泉水都要3块——8块钱五件套！洁面水乳霜全齐了！「白菜价」都不止这个数！8块钱你买个啥不行？但你买个护肤套装——赚了！喜欢的直接拍！不用想！8块钱想啥呢！拍！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2482;

-- ─── #63 致朵红石榴鲜润水嫩六件套 ¥10 (product_id=69) ──────
UPDATE live_script SET script_content =
'下一个！致朵红石榴六件套！红石榴的！抗氧化补水！六件套10块！10块钱！你出门打个车起步价都不止10块——10块钱六件套往家搬！「买不了吃亏买不了上当」——10块钱你试试又不会怀孕！拍！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2480;

-- ─── #62 上朵美白祛斑五件套(377) ¥10 (product_id=70) ────────
UPDATE live_script SET script_content =
'来了来了！上朵377美白祛斑五件套！注意——377成分的！美白祛斑！10块钱！你等下看到阳阳主推的377七件套79块9就知道这个成分多值钱了——现在10块钱先尝个鲜！「先下手为强」——10块钱买个377体验装！冲！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2478;

-- ─── #61 蜂壳蜂王浆多肽奢颜十五件套 ¥12 (product_id=71) ────
UPDATE live_script SET script_content =
'哎呀妈呀！蜂壳蜂王浆十五件套！你数数——十五件！12块！12块钱15样东西！平均一样不到1块钱！你去超市买包纸巾都不止这价！蜂王浆多肽的——抗皱滋养！「物美价廉」四个字就是为这个品发明的！12块——拍了不亏！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2476;

-- ─── #60 韩束红蛮腰水乳组合 ¥39.9 (product_id=72) ──────────
UPDATE live_script SET script_content =
'韩束！韩束红蛮腰水乳！大牌！补水保湿紧致抗皱淡纹提亮——全给你安排了！韩束专柜随便拿一瓶都上百——39块9水乳组合带回家！「打肿脸充胖子」的钱咱不花——39块9买韩束！这叫「花小钱办大事」！拍！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2474;

-- ─── #59 韩束黑耀晶采精华水+乳+霜 ¥49 (product_id=73) ──────
UPDATE live_script SET script_content =
'又是韩束！黑耀晶采精华水乳霜三件套！49块！韩束的品质你放心——「是骡子是马拉出来遛遛」！这个系列主打晶采提亮，用完脸上那个光泽感——49块钱买个大牌三件套你还犹豫啥？拍！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2472;

-- ─── #58 兰蔻粉水125ml ¥36.9 (product_id=74) ───────────────
UPDATE live_script SET script_content =
'兰蔻！兰蔻粉水！姐妹们你没看错——兰蔻！125ml新款粉水！保湿补水紧致滋润！36块9！三十六块九买兰蔻！专柜400多——36块9！你说这是不是「做梦都不敢想」？不是做梦！是阳阳直播间！喜欢兰蔻的赶紧拍——「过了这村没这店」！',
duration_limit_sec = 20,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2470;

-- ─── #57 后天气丹水乳3件套 ¥64.9 (product_id=75) ────────────
UPDATE live_script SET script_content =
'后！天气丹！韩国顶级护肤——后天气丹光耀焕活紧颜水乳3件套！60ml！64块9！「后」这个牌子在韩国什么地位？皇室级！你去免税店买一套大几百——64块9！紧致焕活补水保湿一步到位！送礼自用都有面子！「千里送鹅毛——礼轻情意重」——但这个情意可不轻！拍！',
duration_limit_sec = 20,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2468;

-- ─── #56 致朵璀璨亮光定制护肤礼盒 ¥11.9 (product_id=76) ────
UPDATE live_script SET script_content =
'致朵璀璨亮光定制礼盒！11块9！十一块九！这个礼盒包装精美——送人拿得出手，自用超值！11块9你买盒烟都不够——但你能买一套护肤礼盒！「便宜没好货」？不！便宜也有好货——就看你跟不跟对人！跟阳阳就对了！拍！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2466;
-- ============================================================
-- Session 18 · 过品话术 (Position 55→42, 中前段品)
-- R类/B类/K类混合，13-155元，15-20秒快过
-- ============================================================

-- ─── #55 海茴香极光水乳 ¥49.9 (product_id=77) ───────────────
UPDATE live_script SET script_content =
'海茴香极光水乳！田总专属！不添加一滴水——你听清了，一滴水都不加！全是精华！紧致抗皱淡纹提亮肤色！49块9——不加水的水乳你去外面找找？找不到！「真金不怕火炼」——好东西经得起考验！拍！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2464;

-- ─── #54 SP水乳黑金胶原多肽抗皱冻龄水乳 ¥39.9 (product_id=78)
UPDATE live_script SET script_content =
'SP黑金胶原多肽冻龄水乳！田总专属！黑金的——看这包装多高级！多肽+胶原蛋白，抗皱冻龄！39块9——你去美容院做一次脸三五百起步——39块9用两个月！「四两拨千斤」！拍了不后悔！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2462;

-- ─── #53 DIDK金致多肽花蜜护手霜 ¥10 (product_id=79) ─────────
UPDATE live_script SET script_content =
'护手霜来了！DIDK金致多肽花蜜护手霜！10块！手是女人的第二张脸——你脸保养了手不管？「顾头不顾尾」可不行！10块钱养好第二张脸！拍两支换着用！',
duration_limit_sec = 10,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2460;

-- ─── #52 鱼子酱柔肤致颜六件套礼盒 ¥18 (product_id=80) ──────
UPDATE live_script SET script_content =
'鱼子酱六件套礼盒！18块！鱼子酱成分的——你知道鱼子酱多贵吗？吃的鱼子酱一勺就上百——护肤的鱼子酱成分整套18块！「麻雀虽小五脏俱全」——六件套全齐了！18块不到一杯奶茶——拍！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2458;

-- ─── #51 VEZE梵贞美白祛斑套盒 ¥20 (product_id=81) ───────────
UPDATE live_script SET script_content =
'梵贞美白祛斑套盒！20块！美白祛斑一套搞定——20块你出门买杯咖啡都要30！「省下一杯咖啡——换来一张好脸」！拍！',
duration_limit_sec = 12,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2456;

-- ─── #50 百雀羚弹嫩护肤焕亮精华水乳霜礼盒 ¥76.9 (product_id=82)
UPDATE live_script SET script_content =
'百雀羚！国货之光！弹嫩焕亮精华水乳霜礼盒——76块9！百雀羚这牌子不用我多说了吧？你妈用你奶奶都用——现在年轻化了，这个系列主打抗皱紧致！专柜两三百——76块9带走！「酒香不怕巷子深」——百雀羚的口碑摆在那儿！拍！',
duration_limit_sec = 18,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2454;

-- ─── #49 SIAYZU红石榴多肽保湿臻享礼盒 ¥39.9 (product_id=83) ─
UPDATE live_script SET script_content =
'红石榴多肽保湿礼盒！39块9！红石榴抗氧化+多肽紧致——黄金搭档！这个礼盒包装精美，自用送人都拿得出手！39块9的品质不输百元级——「内行看门道」——懂的姐妹直接拍！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2452;

-- ─── #48 PLER胶原蛋白玻尿酸修护套盒 ¥69 (product_id=84) ─────
UPDATE live_script SET script_content =
'PLER胶原蛋白玻尿酸修护套盒！69块！胶原蛋白+玻尿酸——一个管弹一个管水！你脸上又弹又水那叫什么？叫「少女肌」！69块买回少女肌——「这买卖划算」！拍！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2450;

-- ─── #47 肌采美白祛斑抗皱面膜（白猫眼）¥19.9 (product_id=85) ─
UPDATE live_script SET script_content =
'肌采美白祛斑面膜！白猫眼！田总专属！19块9！美白祛斑抗皱——一片面膜三个功效！你出去做一次美白小气泡200起步——19块9一盒面膜在家做！「自己动手丰衣足食」！拍！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2448;

-- ─── #46 致朵白千松露双抗控油平衡六件套 ¥18 (product_id=86) ───
UPDATE live_script SET script_content =
'致朵白千松露六件套！双抗控油！18块！油皮姐妹看这里！控油平衡——不是让你脸上跑油田！18块钱解决出油问题——「对症下药」！油皮必拍！',
duration_limit_sec = 12,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2446;

-- ─── #45 柏兰梦577精华液 ¥13.14 (product_id=87) ─────────────
UPDATE live_script SET script_content =
'全网独家！柏兰梦577精华液！十效合一！美白祛斑抗皱紧致嫩肤——全管！多少钱？13块14！一生一世！「一生一世」的价格买一瓶十效精华——这不是浪漫是什么？送给自己一生一世的好皮肤！拍！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2444;

-- ─── #44 PLER肌采美白祛斑修护精华液 ¥29.9 (product_id=88) ────
UPDATE live_script SET script_content =
'肌采美白祛斑修护精华液！29块9！精华液是护肤的灵魂——「擒贼先擒王」——精华到位了其他都是锦上添花！29块9的精华液你还犹豫？拍！',
duration_limit_sec = 12,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2442;

-- ─── #43 佰珍堂藏红花酵萃抗皱紧致套装 ¥25 (product_id=89) ────
UPDATE live_script SET script_content =
'佰珍堂藏红花套装！田总专属！藏红花——名贵中药材！抗皱紧致舒颜！25块！藏红花按克卖比黄金还贵——25块买一整套藏红花护肤品！「物超所值」四个字不够形容——得加个「太」！拍！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2440;

-- ─── #42 自然堂凝时鲜颜水乳 ¥155 (product_id=90) ────────────
UPDATE live_script SET script_content =
'自然堂！大品牌！凝时鲜颜水乳抗皱紧致组合！刮码正品！155块——你去专柜买一瓶水都不止这价！自然堂的凝时系列主打冻龄——155块买一套冻龄组合！「你本来就很美」——自然堂说的，阳阳帮你实现！喜欢大牌的姐妹别错过！拍！',
duration_limit_sec = 18,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2438;
-- ============================================================
-- Session 18 · 主推品深度话术 #41 377金钻七件套 ¥79.9
-- 这是全场核心爆品，需要2-5分钟深度讲解+转化
-- script_id=2436, product_id=91, position=41
-- ============================================================

UPDATE live_script SET script_content =
'【主推品 · 377金钻美白祛斑抗皱七件套 · 79.9元 · 深度收割2-5分钟】

好！家人们！前面那些亏品尝到甜头了吧？8块10块12块——阳阳是不是真心实意对你们好？「路遥知马力日久见人心」——阳阳用价格证明诚意！

现在！全场最重要的品来了！你们等的就是它！377金钻美白祛斑抗皱七件套！

【种草·歇后语密集版】
从洁面到眼霜一条龙服务！「三个臭皮匠——顶个诸葛亮」——这七个凑一起直接是诸葛亮PLUS！
核心成分377——高浓度添加的亮肤成分！添加量给足了！「深入敌后」直击黑色素老巢！
洁面打开通道——「小葱拌豆腐一清二白」；
精华水铺路——「磨刀不误砍柴工」；
亮肤精华主攻——「擒贼先擒王」；
乳液锁住——「兵来将挡水来土掩」；
面霜封层——「铜墙铁壁」；
面膜加强——「乘胜追击」；
眼霜收尾——「画龙点睛」！
七步成白，一套到位！

【优势·段子版】
第一，377核心亮肤成分——白纸黑字有专利的！「是骡子是马拉出来遛遛」！不是阳阳胡说八道，你自己去查成分！
第二，七件套全覆盖——你不用东拼西凑！以前你买洁面一个牌子、精华一个牌子、面霜一个牌子——搭配得「七荤八素」——皮肤：我到底听谁的？现在一套搞定——一条龙服务！
第三，含同类核心成分的产品随便买都好几百一瓶。七件套79块9！你去商场买个洗面奶都不止这个价！「买椟还珠——咱不干那傻事」！别人花钱买面子，我们花钱养底子！
第四，清仓价！品牌方给的底价！卖完这批就没了！「过了这村没这店」——不是吓你是真的！

【利益·鸡汤版】
用了它你会怎样？第一周——暗沉开始退。第二周——上妆不卡粉了，以前粉底像刷墙漆，现在服帖得像原装皮肤。一个月——素颜出门也敢见人！
79块9——一天只要1块3！你早上一根烤肠都3块钱——烤肠吃完没了，七件套用完你脸还在发光！
你对脸好一分，脸还你十分！79块9就是你底气的起步价！

【证据·真实故事版】
阳阳自己用了两个月。去年过年回东北——二姨说「阳阳你脸咋有斑了」——「哑巴吃黄连有苦说不出」！
后来用了两个月——发自拍到家族群，二姨回：你是不是去做医美了？哈哈！她不信——79块9能有这效果？
我婆婆也用了——她说「老儿媳妇脸咋发光呢」——我说妈那叫皮肤好自带高光！
「门缝里看人——把人看扁了」——别小看79块9！

【转化·冲单】
好！上链接了！七件套79块9！
「天上掉馅饼——不是陷阱就是惊喜」——这个是实打实的惊喜！
有运费险！不满意7天退！「不入虎穴焉得虎子」——不试试你怎么知道？
你今天犹豫的这一分钟，可能就是三个月后看见同事变白了你追悔莫及的一分钟！
「过了这村没这店」！清仓价！卖完就没了！七步成白，一套到位！
来！3、2、1——拍！拍到的扣「拿下」！没拍的——你是「起了个大早赶了个晚集」！还有的快！',
duration_limit_sec = 300,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2436;
-- ============================================================
-- Session 18 · 过品话术 (Position 40→33, 377之后的中段品)
-- ============================================================

-- ─── #40 美尼姿玻色因胶原蛋白面霜 ¥29.9 (product_id=92) ─────
UPDATE live_script SET script_content =
'刚拍完377的姐妹别走！下面还有好东西！美尼姿玻色因胶原蛋白面霜！29块9！面霜是护肤的最后一道锁——「铜墙铁壁」！玻色因紧致+胶原蛋白弹润——29块9锁住你前面所有的护肤功夫！配合377用效果翻倍！拍！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2434;

-- ─── #39 LTI美白祛斑霜 ¥39.9 (product_id=93) ───────────────
UPDATE live_script SET script_content =
'韩国品牌LTI！美白祛斑霜！改善暗沉淡斑净白透亮！39块9！韩国的祛斑技术你不服不行——人家研究这个几十年了！39块9买韩国品牌祛斑霜——「占便宜」这三个字就是今天发明的！拍！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2432;

-- ─── #38 ROTiSS嫩肤美白祛斑霜 ¥39.9 (product_id=94) ────────
UPDATE live_script SET script_content =
'ROTiSS祛斑霜！批发清仓价！39块9！美白保湿滋润提亮——一瓶霜四个功效！清仓的意思是什么？卖完就没了！不是品质不好——是仓库要腾地方！你的便宜就是从仓库腾出来的！「捡漏」就现在！拍！',
duration_limit_sec = 12,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2430;

-- ─── #37 雅缇诗白松露香氛沐浴露 ¥19.9 (product_id=95) ──────
UPDATE live_script SET script_content =
'来个沐浴露换换品！雅缇诗白松露栀子花精油香氛沐浴露！19块9！洗完澡全身栀子花香——你老公闻了得多看你两眼！「人靠衣装——佛靠金装」——你靠香装！19块9让你香喷喷的！拍！',
duration_limit_sec = 12,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2428;

-- ─── #36 雅缇诗鱼子酱洗发水800ml ¥19.9 (product_id=96) ──────
UPDATE live_script SET script_content =
'洗发水！雅缇诗鱼子酱氨基酸控油去屑洗发水！800ml大瓶！19块9！氨基酸温和不刺激——控油去屑——800ml用半年！19块9用半年你算算一天多少钱？「精打细算过日子」——东北人最会！拍！',
duration_limit_sec = 12,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2426;

-- ─── #35 致朵视黄醇臻养沁颜十三件套 ¥15 (product_id=97) ─────
UPDATE live_script SET script_content =
'致朵视黄醇十三件套！视黄醇——抗老界的扛把子成分！十三件套15块！15块钱13样东西！一样一块多——你去地摊买个发卡都不止这价！视黄醇抗皱紧致——15块钱的抗老方案！还等啥？拍！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2424;

-- ─── #34 珀莱雅红宝石洁水乳套装 ¥68.9 (product_id=98) ───────
UPDATE live_script SET script_content =
'珀莱雅！又一个国货大牌！红宝石系列洁水乳套装！紧致抗皱保湿补水淡纹！68块9！珀莱雅红宝石系列是他家的明星产品——专柜随便一瓶精华就三四百——68块9一整套！「好钢用在刀刃上」——68块9花在珀莱雅上就对了！喜欢国货大牌的姐妹冲！',
duration_limit_sec = 18,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2422;

-- ─── #33 美肤宝自然白礼盒6件套 ¥89 (product_id=99) ──────────
UPDATE live_script SET script_content =
'美肤宝！自然白护肤品套盒！6件套礼盒！美白淡斑去黄提亮补水保湿——全齐了！89块！美肤宝也是老牌国货——品质有保障！6件套89块你去超市看看美肤宝什么价？阳阳帮你省了一半都不止！「会过日子的女人最有魅力」——89块把美白安排得明明白白！拍！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2420;
-- ============================================================
-- Session 18 · 主推品深度话术 #32 红石榴玻色因多肽套盒 ¥89
-- 第二核心爆品，2-5分钟深度收割
-- script_id=2418, product_id=100, position=32
-- ============================================================

UPDATE live_script SET script_content =
'【主推品 · CKEK红石榴玻色因多肽双抗赋活弹嫩淡纹套盒 · 89元 · 深度收割2-5分钟】

家人们！第二个重头戏来了！你们等的——CKEK红石榴玻色因多肽套盒！

【种草·扎心+歇后语版】
刚才377解决了亮肤提亮——但姐妹们你光白了不够！白了但松弛下垂？那叫什么？「白白胖胖」——不对，叫「白了但垮了」！
女人25岁开始每年流失胶原蛋白——到35岁累计流失可不少！法令纹深了、苹果肌塌了、嘴角下垂了——「墙倒众人推」——胶原蛋白一掉，皱纹松弛暗沉全来了！
30+的姐妹有没有这种感觉——以前照镜子是「孔雀开屏自我欣赏」，现在照镜子是「猪八戒照镜子里外不是人」？别怕！不是你不努力——是你缺了紧致抗皱这一步！
所以光亮肤不够——还得紧致抗皱！两手都要抓——「一个巴掌拍不响」！377管亮肤，红石榴管紧致——「里应外合」！

【优势·段子+怼竞品版】
第一，红石榴精粹——抗氧化实力派！你脸上暗沉发黄就是氧化——红石榴帮你「还原」回去！「兵来将挡水来土掩」——氧化来了红石榴挡！
第二，玻色因——紧致领域的明星成分！促进胶原蛋白合成让皮肤自己「撑」起来！含有同类核心成分的产品动辄好几百——「打肿脸充胖子」的钱咱不花！整套89块！「四两拨千斤」！
第三，多肽——修护界的好帮手！帮你把受损的皮肤一点点修好。你皮肤泛红敏感脱皮——都是城墙塌了，多肽帮你「修桥补路」！
第四，三重协同！红石榴抗氧化+玻色因紧致+多肽修护——「好事成三」！一弹紧致，二弹抗氧，三弹回春！

【利益·鸡汤+场景版】
用了红石榴你会怎样？两周——脸上暗沉退了，以前照镜子像黄脸婆，现在照镜子像小少妇。一个月——法令纹浅了！苹果肌有了弹性！你按一下脸颊——弹回来了！
跟377搭配用效果翻倍——一个管亮肤一个管紧致！两套下来不到170块——你去美容院做一次脸都不止这价！
白是底气，紧是骨气，润是福气——377+红石榴全给你安排上！
「30岁不是终点，而是新的起点」——89块给自己的皮肤一个重新开始的机会！

【证据·真实故事版】
阳阳自己30+了用了三个月——法令纹浅了一大截！苹果肌饱满度提升！没做医美——你觉得阳阳舍得花那个钱吗？「门缝里看人——把人看扁了」！我就是个精打细算的东北姐姐！
含有同类核心成分的产品动辄好几百——你买的是什么？是那个logo、那个专柜灯光——「买椟还珠——咱不干那傻事」！
「路遥知马力日久见人心」——阳阳用了三个月确认有变化才推！

【转化·冲单】
红石榴套盒开链接！89块！整套89块！
30+的姐妹一定拍这个！法令纹抬头纹鱼尾纹——玻色因帮你从里面撑起来！
跟377搭配——两套加起来不到170块！你去吃顿火锅都不止这个数！
「花小钱办大事」——阳阳最擅长了！
你现在不紧致——过了40岁花的是10倍的钱去打针！「亡羊补牢」不如「未雨绸缪」！
3、2、1——去拍！拍了的你是「人间清醒」——没拍的回头你是「后悔莫及」！
已经拍了377的姐妹——加拍红石榴！白是底气，紧是骨气——两套不到170块！',
duration_limit_sec = 300,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2418;
-- ============================================================
-- Session 18 · 过品话术 (Position 31→20, 红石榴之后的品)
-- 防晒/眼霜/韩束/面膜等，15-20秒快过
-- ============================================================

-- ─── #31 韩伊美白隔离防晒乳 ¥19.9 (product_id=101) ──────────
UPDATE live_script SET script_content =
'防晒来了！韩伊美白隔离防晒乳！SPF50+ PA+++！防晒黑防晒伤不搓泥防水！19块9！姐妹们你们护肤做了一大堆不防晒等于白搭——「竹篮打水一场空」！19块9把防晒安排上！一年四季都要用！拍！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2416;

-- ─── #30 娇婵美白补水修颜隔离防晒乳 ¥29.9 (product_id=102) ──
UPDATE live_script SET script_content =
'又一款防晒！娇婵美白补水修颜隔离防晒乳！SPF50+ PA++++！四个加号！防晒力拉满！29块9！比刚才那个多一个加号——防晒力更强！你要是经常在外面跑的姐妹拍这个！29块9——「宁可防过不可漏过」！拍！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2414;

-- ─── #29 韩婵防晒霜SPF50+ ¥39.9 (product_id=103) ────────────
UPDATE live_script SET script_content =
'韩婵防晒霜！SPF50+ PA+++！39块9！这款质地更润更滋养——干皮姐妹防晒选这个！防晒不拔干——「两全其美」！39块9给脸穿上防护衣！拍！',
duration_limit_sec = 12,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2412;

-- ─── #28 CKEK肌采377眼霜 ¥29.9 (product_id=104) ─────────────
UPDATE live_script SET script_content =
'CKEK 377眼霜！美白祛斑淡黑抗皱！377成分的眼霜！眼睛是心灵的窗户——窗户框子你不保养？「画龙点睛」——这一步不能省！29块9——配合377七件套用绝了！拍了377的姐妹加拍这个眼霜！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2410;

-- ─── #27 法颜蔻玻色因抗皱紧致礼盒 ¥25 (product_id=105) ─────
UPDATE live_script SET script_content =
'法颜蔻玻色因抗皱紧致礼盒！田总专属！25块！玻色因成分的！紧致抗皱！25块钱买个玻色因礼盒——你去搜搜玻色因产品什么价？「拔了萝卜地皮宽」——25块钱把抗皱搞定！拍！',
duration_limit_sec = 12,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2408;

-- ─── #26 韩束光感莹透水嫩紧致礼盒 ¥138 (product_id=106) ─────
UPDATE live_script SET script_content =
'韩束！第三次出场！韩束光感莹透水嫩紧致礼盒套装！138块！韩束这个系列主打水嫩光感——用完脸上那个透亮！正品清爽不油腻！138块买韩束正品礼盒——专柜三四百起步！喜欢韩束的姐妹今天可以囤了！拍！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2406;

-- ─── #25 DRCQRZEN美白祛斑修护抗皱套盒 ¥99 (product_id=107) ──
UPDATE live_script SET script_content =
'DRCQRZEN美白祛斑修护抗皱套盒！99块！美白+祛斑+修护+抗皱——四合一套盒！99块四个功效你还想咋地？「一箭四雕」！不对应该是「一套四得」！拍！',
duration_limit_sec = 12,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2404;

-- ─── #24 Mircutee玻色因鱼子微囊抗皱精华水 ¥29.9 (product_id=108)
UPDATE live_script SET script_content =
'英国Mircutee！玻色因活胶原鱼子微囊紧致精华水！14天抗皱！29块9！英国品牌的玻色因精华水——14天见效！「十四天一个周期」——你试试两周看看脸有没有变化！29块9的试错成本你承担得起吧？拍！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2402;

-- ─── #23 新西兰红茶焕肤控油爽肤水 ¥25 (product_id=109) ──────
UPDATE live_script SET script_content =
'新西兰红茶焕肤水！田总专属！控油抗皱紧致舒缓！25块！新西兰的红茶——你喝过红茶没？喝进去暖胃——拍在脸上暖肤！控油的姐妹看这里！25块拍！',
duration_limit_sec = 12,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2400;

-- ─── #22 UC意大利进口美白祛斑水组合 ¥39.9 (product_id=110) ───
UPDATE live_script SET script_content =
'意大利进口！UC美白祛斑水+焕颜紧致微晶水组合装！39块9！意大利的——人家欧洲人搞护肤是认真的！进口品质国产价格——39块9两瓶水带回家！新疆都包邮！「全国一盘棋」——哪儿都包邮！拍！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2398;

-- ─── #21 初律玻尿酸原B5润颜精粹水 ¥22.9 (product_id=111) ────
UPDATE live_script SET script_content =
'初律玻尿酸B5精粹水！22块9！玻尿酸+B5——补水修护双管齐下！你脸干得像沙漠——这瓶水就是你的绿洲！22块9——「沙漠变绿洲」就这么简单！拍！',
duration_limit_sec = 12,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2396;

-- ─── #20 CKEK白松露抗皱莹亮精华面膜 ¥39.9 (product_id=112) ──
UPDATE live_script SET script_content =
'CKEK白松露面膜来了！贵妇膜！白松露精华面膜！白松露有多贵你们知道吗？「地下黄金」！但咱这款——39块9！你贴一片上去，20分钟揭下来脸上那个水润——跟刚从温泉里出来似的！搭配377一起用绝了——晚上377做基础，每周2-3次贵妇膜加强——「天衣无缝」！39块9一杯奶茶的钱——奶茶喝完啥也没剩，面膜贴完脸是自己的！拍！',
duration_limit_sec = 20,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2394;
-- ============================================================
-- Session 18 · 过品话术 (Position 19→10, 后段品快速收尾)
-- ============================================================

-- ─── #19 CKEK玫瑰沁润修护精华面膜 ¥39.9 (product_id=113) ────
UPDATE live_script SET script_content =
'又一款面膜！CKEK玫瑰沁润修护精华面膜！39块9！玫瑰精华修护——敏感肌也能用！你最近换季脸泛红脱皮的——这款就是你的「灭火器」！39块9灭一次火——值！拍！',
duration_limit_sec = 12,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2392;

-- ─── #18 葆玛之谜多肽鱼子酱七件套 ¥25 (product_id=114) ──────
UPDATE live_script SET script_content =
'葆玛之谜多肽鱼子酱七件套！25块！又是七件套！多肽+鱼子酱——抗皱滋养！25块钱七件套你还想咋地？「贪心不足蛇吞象」——但25块的七件套你贪心一下完全没问题！拍！',
duration_limit_sec = 12,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2390;

-- ─── #17 果本坚果依克多因奢漾礼盒 ¥49 (product_id=115) ──────
UPDATE live_script SET script_content =
'果本！坚果依克多因奢漾礼盒！补水保湿！全码正品！49块！果本这个牌子主打天然植物——依克多因是抗压成分！你天天加班熬夜皮肤压力大——依克多因帮你减压！49块给脸减个压！拍！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2388;

-- ─── #16 美肤宝水份源礼盒5件套 ¥89 (product_id=116) ─────────
UPDATE live_script SET script_content =
'美肤宝水份源礼盒！5件套！深层补水保湿清爽精华乳舒缓！田总专属！89块！美肤宝第二次出场——这个系列主打深层补水！「干皮的救星——油皮的好伙伴」！89块大牌5件套！拍！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2386;

-- ─── #15 BPM赋能滋养淡纹红宝石水乳精华面霜四件套 ¥29.9 (product_id=117)
UPDATE live_script SET script_content =
'BPM红宝石四件套！田总粉丝专属福利！水乳精华面霜全齐！29块9！29块9四件套——「天底下还有这种好事」？有！就在阳阳直播间！拍！',
duration_limit_sec = 12,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2384;

-- ─── #14 芈汐玻色因虫草冻龄四件套 ¥39.9 (product_id=118) ────
UPDATE live_script SET script_content =
'芈汐玻色因虫草冻龄四件套！水乳霜洁面全有！玻色因+虫草——一个管紧致一个管滋养！39块9——冻龄不是梦是39块9的事！拍！',
duration_limit_sec = 12,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2382;

-- ─── #13 致朵黑金玫瑰臻宠抗皱贵妇七件套 ¥18 (product_id=119)
UPDATE live_script SET script_content =
'致朵黑金玫瑰贵妇七件套！18块！注意——贵妇七件套！18块你就是贵妇！谁说贵妇一定要花大价钱？「英雄不问出处——贵妇不问价格」！18块当一回贵妇！拍！',
duration_limit_sec = 12,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2380;

-- ─── #12 韩束臻白淡斑匀亮紧致礼盒 ¥128 (product_id=120) ────
UPDATE live_script SET script_content =
'韩束第四次出场！臻白淡斑匀亮紧致礼盒！128块！韩束的臻白系列主打淡斑提亮紧致——三效合一！128块买韩束正品礼盒——你去专柜看看要多少钱？阳阳帮你省了一大半！「韩束铁粉」必拍！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2378;

-- ─── #11 CKEK肌采美白祛斑修护乳 ¥19.9 (product_id=121) ──────
UPDATE live_script SET script_content =
'CKEK美白祛斑修护乳！19块9！单品乳液——你要是不想买套装就买这个单品！19块9试试CKEK的品质——好不好用你自己判断！「试了才知道」！拍！',
duration_limit_sec = 10,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2376;

-- ─── #10 PLER山茶花八重氨基酸洁面乳 ¥13.14 (product_id=122) ─
UPDATE live_script SET script_content =
'PLER山茶花氨基酸洁面乳！13块14！又是一生一世的价格！八重氨基酸控油净透——洗脸是护肤第一步！「万丈高楼平地起」——地基不打好上面都白搭！13块14把洁面搞定！拍！',
duration_limit_sec = 12,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2374;
-- ============================================================
-- Session 18 · 过品话术 (Position 9→1, 最后收尾的品)
-- ============================================================

-- ─── #9 ORANINORANGE多肽氨基酸洁面乳 ¥29.9 (product_id=123) ─
UPDATE live_script SET script_content =
'多肽氨基酸小气泡洁面乳！田总专属！29块9！小气泡——你在美容院做一次小气泡200起步——29块9在家做小气泡洁面！「自己动手丰衣足食」！多肽修护+氨基酸温和——洗完脸不紧绷！拍！',
duration_limit_sec = 12,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2372;

-- ─── #8 LRNAS奢宠抗皱润颜双效滋润礼盒 ¥22 (product_id=124) ─
UPDATE live_script SET script_content =
'LRNAS奢宠抗皱润颜礼盒！田总专属！22块！抗皱+滋润双效！22块钱一个礼盒——送闺蜜送妈妈送自己都合适！「礼轻情意重」——22块的心意无价！拍！',
duration_limit_sec = 12,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2370;

-- ─── #7 CKEK肤研美白祛斑滋养贵妇膏 ¥39.9 (product_id=125) ──
UPDATE live_script SET script_content =
'CKEK美白祛斑贵妇膏！39块9！贵妇膏——一抹就能提亮遮瑕！素颜涂一层出门——省了底妆的事！懒人护肤神器！39块9让你素颜也好看——「懒人有懒福」！拍！',
duration_limit_sec = 12,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2368;

-- ─── #6 Prefetch玻色因套盒 ¥49.9 (product_id=126) ───────────
UPDATE live_script SET script_content =
'玻色因抗皱紧致护肤套盒！49块9！清洁+补水+保湿+抗皱一套搞定！省心省钱不用东拼西凑！1000多条真实好评——回头客一大堆！运费险+7天无理由——零风险下单！49块9——拍！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2366;

-- ─── #5 致朵奢润燕窝焕颜套盒 ¥20 (product_id=127) ──────────
UPDATE live_script SET script_content =
'致朵燕窝焕颜套盒！20块！燕窝——你吃一碗燕窝多少钱？几十上百！20块买一套燕窝护肤品——「外敷比内服来得直接」！20块——拍了不亏！',
duration_limit_sec = 10,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2364;

-- ─── #4 后天气丹6件套 ¥169 (product_id=128) ─────────────────
UPDATE live_script SET script_content =
'又见后天气丹！6件套185ml！从水到霜一步到位！169块！「后」这个品牌——韩国LG旗下顶奢！皇室级护肤体验！6件套169——你去免税店看看什么价？精美礼盒送礼首选！7天无理由+过敏包退！169块买「后」——这叫「捡到宝了」！拍！',
duration_limit_sec = 18,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2362;

-- ─── #3 艾斯黎水光盈润美颜精萃套盒 ¥199 (product_id=129) ────
UPDATE live_script SET script_content =
'Dr.exlee艾斯黎水光盈润美颜精萃套盒！199块！这个套盒主打水光肌——用完脸上那个光泽感就像做了水光针！不打针不医美——199块在家实现水光肌！想要那种从里面透出来的水光感的姐妹拍这个！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2360;

-- ─── #2 Whsa植物酵素清新健齿牙膏 ¥29.9 (product_id=130) ─────
UPDATE live_script SET script_content =
'换个品类！牙膏！Whsa植物酵素清新健齿牙膏！田总专属！29块9！你光脸好看牙不好——「金玉其外」可不行！笑起来一口好牙才是真的美！植物酵素温和不刺激——29块9给牙齿也做个SPA！顺手拍一支！',
duration_limit_sec = 12,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2358;

-- ─── #1 PLER鱼子酱多肽双抗紧致焕颜亮肤四件套 ¥39.9 (product_id=131)
UPDATE live_script SET script_content =
'最后一个品！PLER鱼子酱多肽双抗紧致焕颜亮肤四件套！39块9！鱼子酱+多肽——双抗紧致亮肤！这是今天的收尾品——39块9四件套！还没下手的姐妹这是最后的机会——「压轴的都是好戏」！拍！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2356;
-- ============================================================
-- Session 18 · 过渡话术 (transition) + 收场话术 (closing)
-- 过渡话术穿插在产品之间，用于拉停留/互动/情绪铺垫
-- ============================================================

-- ─── 过渡1: #64→#63之间 (seq3, id=2357) ─────────────────────
UPDATE live_script SET script_content =
'好！第一个8块的拍了没？拍了的扣「拍了」！没拍的——8块钱你都犹豫？「属蜗牛的——出手太慢」！下一个！',
duration_limit_sec = 8,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2357;

-- ─── 过渡2: #63→#62之间 (seq5, id=2359) ─────────────────────
UPDATE live_script SET script_content =
'节奏快的啊家人们！眼疾手快的才能捡到便宜——「先下手为强后下手遭殃」！下一个！',
duration_limit_sec = 6,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2359;

-- ─── 过渡3: #62→#61之间 (seq7, id=2361) ─────────────────────
UPDATE live_script SET script_content =
'三个品过完了——8块、10块、10块！你花了28块买了三套护肤品！出门打个车都不止这钱！继续！',
duration_limit_sec = 6,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2361;

-- ─── 过渡4: #61→#60之间 (seq9, id=2363) ─────────────────────
UPDATE live_script SET script_content =
'亏品还在继续！新来的宝宝赶紧点关注——不点关注等下找不到阳阳！「有缘千里来相会」——你可别让这缘分断了！下一个！',
duration_limit_sec = 8,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2363;

-- ─── 过渡5: #60→#59之间 (seq11, id=2365) ────────────────────
UPDATE live_script SET script_content =
'韩束来了啊！大牌品要上了！前面K类亏品吃够了甜头——接下来上硬菜了！',
duration_limit_sec = 5,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2365;

-- ─── 过渡6: #59→#58之间 (seq13, id=2367) ────────────────────
UPDATE live_script SET script_content =
'韩束拍了的扣「韩束真香」！接下来这个牌子你们绝对想不到——等着！',
duration_limit_sec = 5,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2367;

-- ─── 过渡7: #58→#57之间 (seq15, id=2369) ────────────────────
UPDATE live_script SET script_content =
'兰蔻36块9——你们是不是觉得阳阳疯了？没疯！阳阳清醒得很——就是要让你们占便宜！「舍不得孩子套不着狼」——阳阳舍了利润套你们的心！下一个更猛！',
duration_limit_sec = 8,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2369;

-- ─── 过渡8: #57→#56之间 (seq17, id=2371) ────────────────────
UPDATE live_script SET script_content =
'后天气丹64块9！拍到的你今天赚大了！没拍到的别急后面还有一个后天气丹6件套！继续过品！',
duration_limit_sec = 6,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2371;

-- ─── 过渡9: #56→#55之间 (seq19, id=2373) ────────────────────
UPDATE live_script SET script_content =
'帮阳阳把点赞打上去！「众人拾柴火焰高」！点赞过1万阳阳加送一波福利！快点！手速快的都是好姐妹！',
duration_limit_sec = 8,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2373;

-- ─── 过渡10: #55→#54之间 (seq21, id=2375) ───────────────────
UPDATE live_script SET script_content =
'田总专属的品来了几个了？都是好东西！田总给阳阳的价格是真的低——「杀熟」都不带这么杀的！继续！',
duration_limit_sec = 6,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2375;

-- ─── 过渡11: #54→#53之间 (seq23, id=2377) ───────────────────
UPDATE live_script SET script_content =
'水乳套装过了好几个了——接下来上个小单品！手霜！女人的第二张脸不能忘！',
duration_limit_sec = 5,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2377;

-- ─── 过渡12: #53→#52之间 (seq25, id=2379) ───────────────────
UPDATE live_script SET script_content =
'手保养好了——脸继续！下一个鱼子酱的来了！',
duration_limit_sec = 4,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2379;

-- ─── 过渡13: #52→#51之间 (seq27, id=2381) ───────────────────
UPDATE live_script SET script_content =
'18块钱买六件套——你跟阳阳说说还有比这更划算的吗？「打着灯笼都找不到」！下一个！',
duration_limit_sec = 6,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2381;

-- ─── 过渡14: #51→#50之间 (seq29, id=2383) ───────────────────
UPDATE live_script SET script_content =
'好！接下来是国货大牌时间！百雀羚来了！觉得国货牛的扣「国货之光」！',
duration_limit_sec = 6,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2383;

-- ─── 过渡15: #50→#49之间 (seq31, id=2385) ───────────────────
UPDATE live_script SET script_content =
'百雀羚76块9拍了没？国货就得支持！「自己人不帮自己人谁帮」！继续！',
duration_limit_sec = 5,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2385;

-- ─── 过渡16: #49→#48之间 (seq33, id=2387) ───────────────────
UPDATE live_script SET script_content =
'红石榴系列出了好几个了——你们发现没？阳阳直播间红石榴特别多！为啥？因为抗氧化是护肤的根本——「万变不离其宗」！下一个！',
duration_limit_sec = 6,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2387;

-- ─── 过渡17: #48→#47之间 (seq35, id=2389) ───────────────────
UPDATE live_script SET script_content =
'套盒过了——来个面膜换换口味！面膜是急救神器——约会前一片状态直接拉满！',
duration_limit_sec = 5,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2389;

-- ─── 过渡18: #47→#46之间 (seq37, id=2391) ───────────────────
UPDATE live_script SET script_content =
'面膜拍了的姐妹好眼光！下一个——油皮姐妹注意了！控油的来了！',
duration_limit_sec = 5,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2391;

-- ─── 过渡19: #46→#45之间 (seq39, id=2393) ───────────────────
UPDATE live_script SET script_content =
'油皮搞定了！下面来个精华液——精华液是护肤的灵魂！而且这个价格你们绝对想不到！',
duration_limit_sec = 5,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2393;

-- ─── 过渡20: #45→#44之间 (seq41, id=2395) ───────────────────
UPDATE live_script SET script_content =
'13块14一生一世！浪漫不浪漫？下一个精华液也很给力！',
duration_limit_sec = 4,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2395;

-- ─── 过渡21: #44→#43之间 (seq43, id=2397) ───────────────────
UPDATE live_script SET script_content =
'精华液过了两个了——接下来这个套装用了藏红花！名贵中药材！',
duration_limit_sec = 5,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2397;

-- ─── 过渡22: #43→#42之间 (seq45, id=2399) ───────────────────
UPDATE live_script SET script_content =
'藏红花25块！「物超所值」！接下来上一个重量级大牌——自然堂！「你本来就很美」！',
duration_limit_sec = 5,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2399;

-- ─── 过渡23: #42→#41之间 (seq47, id=2401) ───────────────────
-- *** 这是通向377主推品的关键过渡 ***
UPDATE live_script SET script_content =
'家人们！自然堂过完了——接下来！全场最重要的品！你们从开场等到现在的！准备好了吗？「好戏在后头」——后头到了！377金钻七件套！79块9！坐稳了！深呼吸！阳阳要认真讲了！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2401;

-- ─── 过渡24: #41→#40之间 (seq49, id=2403) ───────────────────
-- *** 377刚讲完的过渡 ***
UPDATE live_script SET script_content =
'377拍到的扣「拿下」！没拍到的——你是不是在犹豫？「当断不断反受其乱」！79块9你还纠结啥？好！继续过品！刚才拍了377的姐妹别走——后面还有好搭配！',
duration_limit_sec = 10,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2403;

-- ─── 过渡25: #40→#39之间 (seq51, id=2405) ───────────────────
UPDATE live_script SET script_content =
'玻色因面霜拍了的——配合377用简直绝配！继续！下一个祛斑霜！',
duration_limit_sec = 5,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2405;

-- ─── 过渡26: #39→#38之间 (seq53, id=2407) ───────────────────
UPDATE live_script SET script_content =
'韩国品牌祛斑霜拍了没？下一个也是祛斑霜——清仓价！',
duration_limit_sec = 4,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2407;

-- ─── 过渡27: #38→#37之间 (seq55, id=2409) ───────────────────
UPDATE live_script SET script_content =
'祛斑霜过了两个——换个品类！沐浴露和洗发水来了！从头到脚阳阳都给你安排！「全方位无死角」！',
duration_limit_sec = 6,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2409;

-- ─── 过渡28: #37→#36之间 (seq57, id=2411) ───────────────────
UPDATE live_script SET script_content =
'沐浴露香不香？下一个洗发水也是同一个牌子——凑一对！',
duration_limit_sec = 4,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2411;

-- ─── 过渡29: #36→#35之间 (seq59, id=2413) ───────────────────
UPDATE live_script SET script_content =
'洗头洗澡都安排了——回到护肤！视黄醇来了！抗老界的扛把子成分！',
duration_limit_sec = 5,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2413;

-- ─── 过渡30: #35→#34之间 (seq61, id=2415) ───────────────────
UPDATE live_script SET script_content =
'15块13件套——你们今天是来捡便宜的吧？哈哈！下一个又是国货大牌——珀莱雅！',
duration_limit_sec = 5,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2415;

-- ─── 过渡31: #34→#33之间 (seq63, id=2417) ───────────────────
UPDATE live_script SET script_content =
'珀莱雅68块9——大牌控快拍！下一个美肤宝！国货大牌一个接一个！',
duration_limit_sec = 5,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2417;

-- ─── 过渡32: #33→#32之间 (seq65, id=2419) ───────────────────
-- *** 通向红石榴主推品的关键过渡 ***
UPDATE live_script SET script_content =
'美肤宝89块——值！接下来！第二个重头戏！你们等的红石榴套盒来了！CKEK红石榴玻色因多肽套盒！89块！刚拍了377的姐妹一定要加拍这个——「一个巴掌拍不响」——两套一起才是王炸！坐稳了阳阳要好好讲！',
duration_limit_sec = 15,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2419;

-- ─── 过渡33: #32→#31之间 (seq67, id=2421) ───────────────────
-- *** 红石榴刚讲完的过渡 ***
UPDATE live_script SET script_content =
'红石榴拍了的扣「拿下」！377+红石榴两套都拍了的扣「全拿下」！白是底气紧是骨气——不到170块两个都齐了！继续过品——后面还有防晒眼霜面膜！',
duration_limit_sec = 10,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2421;

-- ─── 过渡34: #31→#30之间 (seq69, id=2423) ───────────────────
UPDATE live_script SET script_content =
'防晒！一年四季都要用！姐妹们护肤不防晒等于白搭——「竹篮打水一场空」！',
duration_limit_sec = 5,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2423;

-- ─── 过渡35: #30→#29之间 (seq71, id=2425) ───────────────────
UPDATE live_script SET script_content =
'19块9防晒拍了没？下一个防晒力更强——四个加号的！',
duration_limit_sec = 4,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2425;

-- ─── 过渡36: #29→#28之间 (seq73, id=2427) ───────────────────
UPDATE live_script SET script_content =
'防晒过了三个——总有一款适合你！下面上眼霜——377成分的！拍了377的姐妹配套！',
duration_limit_sec = 5,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2427;

-- ─── 过渡37: #28→#27之间 (seq75, id=2429) ───────────────────
UPDATE live_script SET script_content =
'眼霜搞定！下面玻色因礼盒来了——25块！',
duration_limit_sec = 4,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2429;

-- ─── 过渡38: #27→#26之间 (seq77, id=2431) ───────────────────
UPDATE live_script SET script_content =
'25块搞定抗皱！下面韩束又来了——韩束今天出了好几次了吧？谁让人家品质好呢！',
duration_limit_sec = 5,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2431;

-- ─── 过渡39: #26→#25之间 (seq79, id=2433) ───────────────────
UPDATE live_script SET script_content =
'韩束138拍的姐妹好眼光！下一个美白祛斑套盒！99块四合一！',
duration_limit_sec = 4,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2433;

-- ─── 过渡40: #25→#24之间 (seq81, id=2435) ───────────────────
UPDATE live_script SET script_content =
'99块四个功效——「一箭四雕」！下面英国品牌来了！',
duration_limit_sec = 4,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2435;

-- ─── 过渡41: #24→#23之间 (seq83, id=2437) ───────────────────
UPDATE live_script SET script_content =
'英国玻色因精华水——14天见效！下一个新西兰红茶水！今天是国际专场啊！英国意大利新西兰韩国——「联合国」都凑齐了！',
duration_limit_sec = 6,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2437;

-- ─── 过渡42: #23→#22之间 (seq85, id=2439) ───────────────────
UPDATE live_script SET script_content =
'新西兰红茶水25块——控油姐妹必拍！下面意大利进口的来了！',
duration_limit_sec = 4,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2439;

-- ─── 过渡43: #22→#21之间 (seq87, id=2441) ───────────────────
UPDATE live_script SET script_content =
'意大利进口39块9——新疆都包邮！下面补水神器来了！',
duration_limit_sec = 4,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2441;

-- ─── 过渡44: #21→#20之间 (seq89, id=2443) ───────────────────
UPDATE live_script SET script_content =
'补水搞定！下面重头面膜来了——白松露贵妇膜！这个要多讲几句！',
duration_limit_sec = 5,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2443;

-- ─── 过渡45: #20→#19之间 (seq91, id=2445) ───────────────────
UPDATE live_script SET script_content =
'白松露面膜拍了没？搭配377一起用绝了！下面又一款面膜——玫瑰修护的！',
duration_limit_sec = 5,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2445;

-- ─── 过渡46: #19→#18之间 (seq93, id=2447) ───────────────────
UPDATE live_script SET script_content =
'面膜过了两款——急救修护都有了！下面鱼子酱七件套来了！',
duration_limit_sec = 4,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2447;

-- ─── 过渡47: #18→#17之间 (seq95, id=2449) ───────────────────
UPDATE live_script SET script_content =
'25块七件套——你们今天真是捡便宜捡到手软！下面果本来了！天然植物系！',
duration_limit_sec = 5,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2449;

-- ─── 过渡48: #17→#16之间 (seq97, id=2451) ───────────────────
UPDATE live_script SET script_content =
'果本49块——植物系护肤！下面美肤宝又来了！第二次出场！',
duration_limit_sec = 4,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2451;

-- ─── 过渡49: #16→#15之间 (seq99, id=2453) ───────────────────
UPDATE live_script SET script_content =
'美肤宝89块5件套——大牌就是大牌！下面田总粉丝专属福利来了！',
duration_limit_sec = 4,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2453;

-- ─── 过渡50: #15→#14之间 (seq101, id=2455) ──────────────────
UPDATE live_script SET script_content =
'29块9四件套——「天底下还有这种好事」！下面玻色因虫草冻龄的来了！',
duration_limit_sec = 4,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2455;

-- ─── 过渡51: #14→#13之间 (seq103, id=2457) ──────────────────
UPDATE live_script SET script_content =
'冻龄39块9——不是梦！下面又是贵妇系列——18块当贵妇！',
duration_limit_sec = 4,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2457;

-- ─── 过渡52: #13→#12之间 (seq105, id=2459) ──────────────────
UPDATE live_script SET script_content =
'18块贵妇——「贵」在品质不在价格！下面韩束第四次出场了！铁粉们准备好！',
duration_limit_sec = 5,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2459;

-- ─── 过渡53: #12→#11之间 (seq107, id=2461) ──────────────────
UPDATE live_script SET script_content =
'韩束128拍了的都是韩束铁粉！下面CKEK单品来了——试试看！',
duration_limit_sec = 4,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2461;

-- ─── 过渡54: #11→#10之间 (seq109, id=2463) ──────────────────
UPDATE live_script SET script_content =
'单品19块9——无压力！下面洁面乳来了——护肤的第一步！',
duration_limit_sec = 4,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2463;

-- ─── 过渡55: #10→#9之间 (seq111, id=2465) ───────────────────
UPDATE live_script SET script_content =
'13块14一生一世的洁面！下面又一款洁面——小气泡的！马上收尾了姐妹们！',
duration_limit_sec = 5,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2465;

-- ─── 过渡56: #9→#8之间 (seq113, id=2467) ────────────────────
UPDATE live_script SET script_content =
'小气泡洁面拍了！下面礼盒来了——22块送人自用都合适！快收尾了！',
duration_limit_sec = 4,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2467;

-- ─── 过渡57: #8→#7之间 (seq115, id=2469) ────────────────────
UPDATE live_script SET script_content =
'22块礼盒——便宜又有面子！下面贵妇膏来了——素颜神器！',
duration_limit_sec = 4,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2469;

-- ─── 过渡58: #7→#6之间 (seq117, id=2471) ────────────────────
UPDATE live_script SET script_content =
'贵妇膏39块9——懒人必备！下面玻色因套盒——千条好评的口碑款！',
duration_limit_sec = 4,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2471;

-- ─── 过渡59: #6→#5之间 (seq119, id=2473) ────────────────────
UPDATE live_script SET script_content =
'好评过千的口碑款49块9！下面燕窝套盒来了！20块！',
duration_limit_sec = 4,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2473;

-- ─── 过渡60: #5→#4之间 (seq121, id=2475) ────────────────────
UPDATE live_script SET script_content =
'燕窝20块——外敷比内服直接！下面重量级来了——后天气丹6件套！第二次出场！',
duration_limit_sec = 5,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2475;

-- ─── 过渡61: #4→#3之间 (seq123, id=2477) ────────────────────
UPDATE live_script SET script_content =
'后天气丹169——韩国顶奢！下面水光盈润套盒——想要水光肌的来！',
duration_limit_sec = 4,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2477;

-- ─── 过渡62: #3→#2之间 (seq125, id=2479) ────────────────────
UPDATE live_script SET script_content =
'水光肌199搞定！换个品类——牙膏来了！从头到脚全方位无死角！',
duration_limit_sec = 4,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2479;

-- ─── 过渡63: #2→#1之间 (seq127, id=2481) ────────────────────
UPDATE live_script SET script_content =
'牙膏也安排了！最后一个品了家人们！压轴的来了！',
duration_limit_sec = 4,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2481;
-- ============================================================
-- Session 18 · 收场话术 (closing) · 2-3分钟
-- script_id=2483, seq=129
-- ============================================================

UPDATE live_script SET script_content =
'【收场话术 · 2-3分钟 · 总结+留人+预告】

家人们！64个品全过完了！阳阳嗓子冒烟了！哈哈！

今天咱们从8块钱的亏品一路过到199的水光套盒——64个产品一个不落！你们的手速阳阳是服了——「快刀斩乱麻」！

阳阳最后帮你们捋一遍重点——

第一，377金钻七件套79块9！高浓度核心亮肤成分，七步成白一套到位——这是今天的「镇场之宝」！还没拍的最后机会！
第二，CKEK红石榴套盒89块！一弹紧致二弹抗氧三弹回春——30+姐妹的标配！
两套加一起不到170块——白是底气，紧是骨气！

第三，前面的亏品你们捡了多少便宜？8块10块12块的套装——「白菜价」都不止！拍到的你们今天赚大了！

姐妹们，阳阳知道你们很多是哄完孩子才打开手机的宝妈。白天上班晚上带娃，半夜给自己留一小时——你真的太不容易了。阳阳就想跟你说一句——你值得花十分钟好好护肤，跟自己说一声：辛苦了。你的脸不是消费，是你最贵的不动产！

苏东坡说「人生如逆旅我亦是行人」——能在直播间遇到就是缘分。「有缘千里来相会」——不管你今天买没买，你来了我在，这就够了。

买了的有问题随时找客服——阳阳的售后不比售前差！「金杯银杯不如老百姓的口碑」——你的信任比卖多少货都珍贵。

没关注的赶紧点关注！铃铛打开！下次直播不走丢！阳阳每周都在——下次见的时候你更好看了！

季羡林说「不完满才是人生」——你丢了一单生意、跟老公吵了一架、孩子没考好——都是不完满。但加在一起就是你完整的人生。别太苛求自己了——「多大点事儿」！你已经很好了。

好了家人们！今天播了够久了！谢谢你们每一个人——买了的、没买的、就来陪我唠嗑的——你们都是我的宝！

最后那句话——你对脸好一分，脸还你十分！79块9也好89也好——不是消费是投资！投资你自己那张不动产！

下次见！么么哒！比心！爱你们！下播——',
duration_limit_sec = 180,
style = 'liyangyangStyle',
update_time = CURRENT_TIMESTAMP
WHERE id = 2483;

COMMIT;

-- ─── 验证 ────────────────────────────────────────────────────
SELECT
  COUNT(*) as total_scripts,
  COUNT(*) FILTER (WHERE script_content != '[待填写]') as filled,
  COUNT(*) FILTER (WHERE script_content = '[待填写]') as unfilled
FROM live_script
WHERE session_id = 18 AND deleted = 0;
