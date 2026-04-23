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
