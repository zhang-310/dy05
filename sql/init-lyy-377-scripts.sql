-- 李阳阳 chat_2h 话术（以 377 金钻七件套为核心爆品）
-- 场次 ID: 16, user_id: 3

DO $$
DECLARE
    sid BIGINT := 16;
    uid BIGINT := 3;
BEGIN

-- 1. 开场（3分钟）
INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status, product_id)
VALUES (sid, uid, 'opening', 1,
E'家人们来了啊！我是阳阳！哎呀妈呀你们今天来得可真齐，我搁家穿着拖鞋就跟你们唠上了——\n\n今天晚上是阳阳的唠嗑夜，不急着卖货！咱先聊天，聊高兴了再说别的。但是我跟你们透个底：今天有一款产品，是我直播间卖得最火的，377金钻美白祛斑七件套，一会儿给你们好好讲讲。\n\n先别急！先坐下来。我妈总说——「心急吃不了热豆腐」，好东西得慢慢说才有味道。\n\n新来的家人先点个关注！万一你今天心情不好，以后就知道上哪找阳阳倒苦水。来，第一个问题——最近有没有觉得「我怎么老了」的瞬间？评论区告诉我！',
120, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed', NULL);


-- 2. 聊天槽1：年龄焦虑+婆媳（10分钟）
INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status, product_id)
VALUES (sid, uid, 'chat', 2,
E'哎哟这个话题一问，评论区全炸了——\n\n「照镜子发现法令纹了」「发现第一根白头发」「被叫阿姨的那一刻」——太真实了姐妹们！\n\n我跟你们说个我的经历。去年过年回东北老家，我二姨看见我说：「阳阳你这脸咋有斑了？以前水灵灵的小姑娘，现在看着老了。」\n\n当时我心里那个酸啊——「哑巴吃黄连，有苦说不出」。你说我能跟二姨顶嘴吗？不能。但晚上我躲在被窝里照镜子，是真的仔细看了半小时。法令纹有了，眼角纹有了，额头也有斑点了。\n\n后来我婆婆进来看见我照镜子，说了一句特别暖的话：「阳阳，你这个年纪正好看，别听你二姨的。」\n\n你看婆媳关系也不是天天打仗，关键时刻她站你这边，你就觉得——「千里送鹅毛，礼轻情意重」，不在于说啥大道理，在于那个心意。\n\n但是！阳阳要说一句正经的——杨绛先生说过：「人虽然渺小，人生虽然短，但人能学，人能修身，人能自我完善。」\n\n姐妹们，年龄不可怕，可怕的是你自己先放弃了。你可以接受皱纹，但不能接受不管自己。就像我二姨说我有斑——有斑怎么了？咱就不能想办法淡一淡吗？\n\n评论区有被「年龄暴击」过的扣「被暴击了」——阳阳给你们支招！',
600, '年龄焦虑+婆媳+杨绛名言', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed', NULL);


-- 3. 产品槽1：377金钻七件套 核心爆品（8分钟）
INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status, product_id)
VALUES (sid, uid, 'product', 3,
E'姐妹们！刚才聊到斑点——阳阳不光嘴上说，手上也得有东西对吧？\n\n来了！全场最牛的来了——【377】肌采金钻美白祛斑抗皱七件套！\n\n为什么说它最牛？因为它是我直播间卖得最火的产品，没有之一！复购率第一！\n\n先说核心成分——377是什么？学名叫「苯乙基间苯二酚」，美白界的扛把子成分！它的美白效力是熊果苷的550倍！听清楚了——550倍！不是5倍，不是50倍，是550倍！\n\n为什么叫七件套？你看啊——洁面、水、精华、乳、霜、面膜、眼霜，七步全含了！从清洁到保养一条龙，不用东拼西凑，不用纠结搭配，一套全搞定。\n\n我二姨说我有斑，我回去就用了这套——两个月之后我发了个自拍到家族群，你猜我二姨怎么说的？「阳阳你是不是去做医美了？」哈哈哈！我说没有，就是用了个护肤套装！她不信！\n\n老话说得好——「是骡子是马，拉出来遛遛」！我不跟你吹，你看效果说话。\n\n再说价格。七件套，外面拆开单买怎么也得三四百。我们直播间——79块9！你没听错，79块9七件套！\n\n「天上掉馅饼——不是陷阱就是惊喜」——这个真是惊喜！因为这是清仓价，品牌方给到的底价。卖完这批就没有了，我不是吓你们，是真的。\n\n操作方法跟你们说一下：\n第一步：早晚洁面打底清洁——把毛孔脏东西洗干净\n第二步：拍水——377精华水，打开吸收通道\n第三步：上精华——377祛斑精华，直击斑点暗沉\n第四步：乳液锁水——不让精华白用\n第五步：面霜封层——滋润抗皱双管齐下\n第六步：面膜加强——每周2-3次深层渗透\n第七步：眼霜——眼角纹、黑眼圈一起管\n\n姐妹们！79块9买七件套！你去商场买个洗面奶都不止这个价！\n\n现在拍的再送一包化妆棉！前100名拍的加送一片377面膜！\n\n犹豫啥呢？「过了这村没这店」，错过今天你找不到这个价！\n\n3号链接！79块9！倒计时上架——3、2、1，拍！',
480, '核心爆品377七件套·重磅讲解·痛点+成分+价格+逼单', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed', 91);


-- 4. 过渡
INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status, product_id)
VALUES (sid, uid, 'transition', 4,
E'抢到的扣「拿下」让我看看！好家伙这么多人拍了！姐妹们你们眼光真好！\n\n没抢到的别急——一会儿还有一轮！先跟阳阳继续唠，歇一会儿！喝口水——',
45, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed', NULL);


-- 5. 聊天槽2：职场压力+自我价值（10分钟）
INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status, product_id)
VALUES (sid, uid, 'chat', 5,
E'有个姐妹在评论区说：「阳阳，我不是怕老，是怕老了还没出息。」\n\n这句话扎心了。\n\n阳阳跟你们说实话——我以前也焦虑过。做直播之前我在东北老家做过好几份工作，卖过衣服，做过餐饮，都没做出什么名堂。家里亲戚都说「你看人家谁谁谁怎么怎么样」。\n\n「人比人得死，货比货得扔」——这歇后语我当时天天挂在嘴边。\n\n但后来我想通了。鬼谷子说过一句话：「欲高反下，欲取反与。」想要往上走，先学会沉下来。我做直播头三个月，直播间就三五个人看，我对着手机唠了三个月的独角戏。\n\n但我没放弃。曾国藩说：「天下古今之庸人，皆以一惰字致败。」最怕的不是你不行，是你懒得再试一次。\n\n姐妹们，你现在可能觉得自己很平凡，但平凡不等于平庸。平凡是每天认真上班、认真带娃、认真生活。平庸是明知道可以更好但不愿意行动。\n\n你走进阳阳直播间——这本身就说明你没放弃自己。你还在关注护肤、关注变美、关注生活品质。这就是在向上走！\n\n老话说——「铁杵磨成针——功到自然成」。你今天的每一点努力，都在给未来的自己攒筹码。\n\n来，评论区打一句话鼓励自己——想打什么就打什么，阳阳看着！',
600, '职场焦虑+鬼谷子+曾国藩名言+歇后语', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed', NULL);


-- 6. 产品槽2：377返场第二轮（5分钟）
INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status, product_id)
VALUES (sid, uid, 'product', 6,
E'说到不放弃自己——护肤也是不放弃自己的一种方式！\n\n377金钻七件套第二轮来了！刚才没抢到的姐妹注意了！\n\n再给大家强调一下为什么这款卖得最火：\n\n第一，成分硬——377祛斑成分是有专利的，不是那种虚假宣传\n第二，全套方案——七件套从洁面到眼霜全覆盖，不用你自己搭配\n第三，价格离谱——79块9七件套！我跟你们说白了，这是清仓价，品牌方不做了才放这个价\n第四，效果实在——我自己用，我婆婆用，我粉丝群里无数反馈，有图有真相\n\n有些姐妹可能担心——「便宜没好货」？我告诉你，这个不是便宜没好货，这是清仓甩卖！你去专柜买同样成分的产品，300块打不住。\n\n「该出手时就出手」——这歇后语用在这正合适！79块9就当给自己买个希望，万一用了真变白了呢？\n\n3号链接！第二轮200份！拍！',
300, '377返场·强化逼单', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed', 91);


-- 7. 情感金句槽1（5分钟）
INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status, product_id)
VALUES (sid, uid, 'emotional', 7,
E'姐妹们，唠到这阳阳说几句走心的。\n\n评论区有个姐妹打了一句话：「我就想活成自己喜欢的样子。」\n\n你知道吗？看到这句话我眼眶湿了。因为这太难了——我们从小到大活的都是别人喜欢的样子。\n\n小时候活成爸妈喜欢的样子：乖、听话、考高分。\n长大了活成老公喜欢的样子：贤惠、温柔、不乱花钱。\n当了妈活成孩子需要的样子：耐心、无私、永远有精力。\n\n但什么时候——活成自己喜欢的样子？\n\n莫言说：「世界上的事情最忌讳十全十美。你看那天上的月亮，一旦圆满了，马上就要亏厌。」\n\n姐妹们，你不需要完美。你只需要在这个不完美的人生里，偶尔取悦一下自己。\n\n哪怕只是给自己涂一层护肤品的时间——那15分钟，就是属于你自己的。\n\n林清玄先生说：「以清净心看世界，以欢喜心过生活。」\n\n从今天开始，每天给自己留15分钟。不是为了取悦任何人，是为了——你值得。\n\n跟阳阳一起念：「我值得被好好对待。」——评论区打出来——',
300, '治愈向·莫言+林清玄名言', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed', NULL);


-- 8. 互动槽：歇后语PK+福袋（7分钟）
INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status, product_id)
VALUES (sid, uid, 'interaction', 8,
E'好！擦擦眼泪！阳阳不能总催泪弹，来点欢乐的！\n\n歇后语PK时间到！看谁脑子转得快！\n\n第一题：「外甥打灯笼」—— 对！「照旧（舅）」！简单！\n第二题：「猪鼻子插葱」—— 哈哈！「装象（相）」！这个有意思！\n第三题：「孔夫子搬家」—— 「净是书（输）」！可不是嘛阳阳打牌也是净输！\n第四题：「八仙过海」—— 「各显神通」！就像咱们直播间每个人都有自己的故事！\n第五题挑战级：「张飞穿针」—— 「大眼瞪小眼」！哈哈哈哈！\n\n你们太厉害了！全答对了！\n\n来——开福袋！关注+评论「377」三个字参与抽奖！5分钟后开奖，奖品是377面膜体验装！\n\n趁等奖的时候，阳阳继续跟你们唠——',
420, '歇后语PK互动+福袋', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed', NULL);


-- 9. 聊天槽3：育儿+婚姻（8分钟）
INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status, product_id)
VALUES (sid, uid, 'chat', 9,
E'有个宝妈说她儿子上一年级了，辅导作业能把人气出脑溢血——哈哈这个我太懂了！\n\n我跟你们说个事，前两天我侄子来我家写作业，语文题目问「春天来了大地怎么样了」，正确答案是「万物复苏」。你猜他写啥？「春天来了大地——热了。」\n\n我当时差点没从椅子上摔下来！你说他错了吗？好像也没错——春天来了确实热了啊！但老师不这么认为！\n\n「小葱拌豆腐——一清二白」，孩子的世界就是这么简单直接。反倒是我们大人想太多了。\n\n鲁迅先生说过：「教育是植根于爱的。」你吼孩子的时候先想想——他才六七岁，你六七岁的时候比他强吗？\n\n我特别同意一句话：「孩子不是你的作品，你不是他的导演。他是一颗种子，你只需要浇水和给阳光，他自己会长成他该有的样子。」\n\n当然了——浇水的过程中你可能会被气得——「热锅上的蚂蚁，团团转」——但气完了还是你的崽，你还得接着浇！哈哈哈！\n\n宝妈们扣「亲生的」让我看看同道中人——',
480, '育儿趣事+鲁迅名言+歇后语', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed', NULL);


-- 10. 产品槽3：CKEK红石榴套盒（5分钟）
INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status, product_id)
VALUES (sid, uid, 'product', 10,
E'宝妈们辛苦了！辛苦也要爱自己！\n\n来推荐一个好东西——CKEK红石榴玻色因多肽双抗赋活弹嫩淡纹套盒。\n\n这个套盒厉害在哪？三个核心成分：红石榴精粹抗氧化、玻色因紧致抗皱、多肽修护——三管齐下！\n\n大家都知道欧莱雅家的玻色因吧？同样的核心成分，欧莱雅一瓶面霜五六百。这套整套——89块！\n\n「买椟还珠」——咱们不干那事！这个盒子里面的东西比盒子值钱多了！\n\n这个特别适合30+的姐妹：法令纹有了涂、鱼尾纹有了涂、抬头纹有了涂。玻色因就是促进胶原蛋白合成，让你的皮肤自己「撑」起来。\n\n89块一个套盒，跟377搭着用效果翻倍！一个管美白祛斑，一个管紧致抗皱，两套下来不到170块，你去美容院做一次脸也不止这个价。\n\n要的姐妹自己拍4号链接！不急的也行，一会儿还有面膜推荐——',
300, 'CKEK红石榴套盒·玻色因卖点', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed', 100);


-- 11. 聊天槽4：人生感悟+古诗词（8分钟）
INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status, product_id)
VALUES (sid, uid, 'chat', 11,
E'福袋开奖了！恭喜这五位姐妹！截图找客服领面膜！\n\n开完奖说几句走心的。刚才有人问我：「阳阳你做直播这么久了，最大的感悟是什么？」\n\n我想了想，最大的感悟是——人和人之间的缘分真的很奇妙。\n\n你们想啊，茫茫人海，几千万个直播间，你偏偏走进了阳阳这间。可能是抖音给你推的，可能是朋友推荐的，也可能是你失眠了随便划到的。但不管什么原因——你来了，我在，这就是缘分。\n\n苏东坡有句词我特别喜欢：「人生如逆旅，我亦是行人。」人生就是一趟旅程，我们都是路上的行人。能在直播间相遇，就像旅途中碰到的驿站——歇一歇脚，聊两句天，然后各自上路。\n\n但这个驿站是暖的。我希望你们走进阳阳直播间的时候是一个心情，走出去的时候——哪怕好了那么一点点——就值了。\n\n我们东北有句话叫：「多大点事儿！」对吧？天塌不下来，日子总要过的。\n\n季羡林老先生说过：「不完满才是人生。」这句话我读了一百遍——不完满才是人生啊。\n\n你丢了一单生意——不完满。你跟老公吵了一架——不完满。你孩子考试没考好——不完满。但是！这些不完满加在一起，就是你完整的人生。没有谁的人生是一条直线上去的，都是弯弯绕绕的。\n\n所以别太苛求自己了。你已经很好了。',
480, '人生感悟·苏东坡+季羡林+东北哲学', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed', NULL);


-- 12. 产品槽4：CKEK白松露面膜+377最终返场（5分钟）
INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status, product_id)
VALUES (sid, uid, 'product', 12,
E'最后推荐一个面膜——CKEK白松露抗皱莹亮精华面膜！\n\n白松露这个成分有多贵你们知道吗？白松露每公斤几万块，号称「地下黄金」。白松露提取物的抗氧化和焕亮效果，被很多贵妇品牌用在上千块的产品里。\n\n这款面膜——39块9！白松露精华面膜39块9！\n\n贴完第二天早上起来你会发现脸是亮的，不是那种假白，是从里往外透出来的亮。\n\n「麻雀虽小——五脏俱全」，价格虽便宜但成分一点没含糊！\n\n建议搭配377套装一起用：晚上用377套装做基础护理，每周2-3次白松露面膜加强。一个管日常维护，一个管深层滋养，这个组合我自己用了半年了。\n\n然后！377金钻七件套最后一轮来了！最后100套！79块9！\n\n今天买377+白松露面膜的，阳阳额外给你送一个化妆包！\n\n最后的机会——「过了这村没这店」！3号链接377！5号链接白松露！拍！',
300, 'CKEK白松露面膜+377最终返场', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed', 112);


-- 13. 情感收尾（5分钟·高潮）
INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status, product_id)
VALUES (sid, uid, 'emotional', 13,
E'姐妹们，今天最后说的话是阳阳真心想说的。\n\n这两个小时里，我们聊了年龄焦虑、婆媳关系、职场压力、育儿、人生感悟。有笑有泪。\n\n你们知道我为什么喜欢做直播吗？不是为了卖东西——卖东西是生活，但不是全部。\n\n我喜欢的是——你们在评论区跟我说真心话的那些瞬间。\n\n有人说「阳阳我今天被领导骂了」，有人说「阳阳我跟老公冷战第三天了」，有人说「阳阳我考研没考上」。\n\n你们把这些话告诉我，说明你们信任我。这份信任——比卖多少货都珍贵。\n\n我在东北长大，我妈从小教我一句话：「做人要实在。」\n\n所以阳阳卖的每一样东西我都自己先用，好用的才推荐。377金钻七件套是我自己用了才卖的，CKEK的面膜和套盒也是我自己囤了好几盒才敢推荐。\n\n「金杯银杯不如老百姓的口碑。」你们的复购就是对阳阳最大的认可。\n\n最后送给所有今天走进直播间的姐妹——\n\n白居易有句诗：「乱花渐欲迷人眼，浅草才能没马蹄。」人生就是这样，花花草草太多容易眼花，但只要你脚踏实地，一步一步走，终会走到你想去的地方。\n\n阳阳爱你们。不管明天你遇到什么——记住，阳阳的直播间永远是你的「避风港」。',
300, '收尾情感高潮·白居易+真情流露', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed', NULL);


-- 14. 收尾（2分钟）
INSERT INTO live_script (session_id, user_id, script_type, sequence_no, script_content, duration_limit_sec, requirement, deleted, create_time, update_time, ai_generated, generation_status, product_id)
VALUES (sid, uid, 'closing', 14,
E'好啦家人们！两个小时不知不觉就过去了！\n\n总结一下今天的三款好物：\n第一：377金钻美白祛斑七件套——79块9！美白祛斑扛把子！直播间最火爆品！\n第二：CKEK红石榴玻色因多肽套盒——89块！紧致抗皱法宝！\n第三：CKEK白松露面膜——39块9！深层滋养亮白！\n\n三样全拍不到210块！你去美容院洗个脸都不止这价！\n\n还没拍的姐妹抓紧！购物车里链接到今晚12点截止！\n\n记住阳阳说的：\n「心急吃不了热豆腐」——护肤要坚持；\n「是骡子是马拉出来遛遛」——好不好用你试了才知道；\n「过了这村没这店」——清仓价格买到就是赚到！\n\n明天阳阳继续在，后天也在。你随时来，我随时唠。\n\n关注阳阳，明天下午3点护肤答疑专场！有任何皮肤问题都可以来问。\n\n晚安家人们！东北阳阳抱抱你们！\n\n「大姑娘上花轿——头一回」？才不是！阳阳跟你们说晚安说了一百回了！但每一回都是真心的！\n\n好啦——下播！爱你们！拜拜！',
120, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'completed', NULL);

RAISE NOTICE '李阳阳 377专场话术已创建完成！14槽 / 3款真实产品 / 2小时';

END $$;
