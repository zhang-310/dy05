package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.live.service.ContentMaterialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ContentMaterialServiceImpl implements ContentMaterialService {

    private static final Logger log = LoggerFactory.getLogger(ContentMaterialServiceImpl.class);

    private static final Map<String, Map<String, List<String>>> MATERIAL_LIBRARY = new LinkedHashMap<>();

    static {
        // ==================== 段子库 ====================
        Map<String, List<String>> jokes = new LinkedHashMap<>();
        jokes.put("dating", List.of(
            "找对象就像找停车位：远的看不上，近的没位置",
            "恋爱前：我要找个灵魂伴侣；恋爱后：只要不打呼就行",
            "现代恋爱三件套：已读不回、在线对其可见、朋友圈三天可见",
            "男朋友的三大谎言：1.我不看美女 2.游戏不重要 3.你素颜最好看",
            "现代恋爱：微信聊三天，见面吃顿饭，不合适就换",
            "恋爱的时候觉得自己是公主，分手的时候才发现自己是悟空——打不死的",
            "谈恋爱就像洗衣服，泡一泡，搓一搓，拧一拧，晾一晾，完了",
            "有人问我脱单秘诀，我说降低标准就好了。然后我就一直没脱单",
            "爱情来了挡不住，钱包空了留不住",
            "我觉得我不是单身，我只是不想让别人受伤"
        ));
        jokes.put("marriage", List.of(
            "结婚前：宝贝你想吃什么？结婚后：今天没买菜",
            "夫妻相处三阶段：甜蜜期→磨合期→默契期（懒得吵）",
            "婚姻真相：恋爱是诗和远方，婚姻是柴米油盐",
            "老公的三大功能：1.人肉ATM 2.免费司机 3.情绪垃圾桶",
            "结婚五年后最浪漫的话：今晚你不用洗碗",
            "婚前他说我是小公主，婚后我就变成了保洁阿姨",
            "老婆永远是对的，如果老婆错了，请参考第一条",
            "我老公说他什么都给我买，除了单——单他留着自己吃",
            "婚姻就像一道菜，再好吃也有吃腻的时候，但饿了还是想吃",
            "最成功的婚姻是什么？就是两个人都觉得自己吃亏了"
        ));
        jokes.put("mother_in_law", List.of(
            "婆婆的经典台词：我儿子以前可瘦了",
            "好婆婆标准：出钱出力不插手，装聋作哑不挑事",
            "婆媳关系好不好，主要看老公会不会当双面胶",
            "婆婆说话就像天气预报：晴转多云偶有暴风雨",
            "媳妇的反杀话术：妈，您儿子随您，都不会做饭",
            "婆媳和平共处秘诀：不住一起就是最大的和谐",
            "婆婆说我不会做饭，我说您儿子也不会赚钱呀",
            "和婆婆吵完架，我老公说了一句话就平息了——妈，我涨工资了",
            "世界上最远的距离：婆婆在客厅看电视，我在厨房刷碗",
            "有人说婆媳是天敌，我说那是因为中间那个男人不行"
        ));
        jokes.put("workplace", List.of(
            "上班就像西天取经：途中遇到的都是妖精，打完这个又来一个",
            "老板：你把这个当成自己的事来做。我：好的，那我不做了",
            "打工人三连：起不来、吃不饱、睡不够",
            "月初发工资时我以为自己是富婆，月底才知道是负婆",
            "领导说年底双薪，发了才知道是双辛——辛苦的辛",
            "上班是为了更好地享受生活，然而上班也让生活没什么可享受的了",
            "同事问我加班开心吗，我说开什么心？开心的心吗？",
            "老板画的饼那么大，可惜我的胃装不下",
            "面试说钱不重要，入职后才知道——真的不重要，因为真的少",
            "职场最扎心的话不是你被裁了，而是——你的工作AI可以做了"
        ));
        jokes.put("life", List.of(
            "年少不知压力大，错把加班当潇洒",
            "以前觉得谈钱伤感情，现在觉得没钱更伤感情",
            "有人问我有没有理想，我说有，我的理想就是——躺平",
            "小时候觉得快乐很简单，长大了觉得简单很快乐",
            "长大后才知道：不是世界太小，是钱包太小",
            "你以为的生活：诗和远方。真实的生活：挤地铁赶公交",
            "成年人的崩溃是从看银行余额开始的",
            "每次下决心减肥，都被自己的厨艺打败了",
            "人到中年三大爱好：保温杯泡枸杞、收藏不看的文章、准时看养生号",
            "以前的梦想是环游世界，现在的梦想是准时下班"
        ));
        MATERIAL_LIBRARY.put("joke", jokes);

        // ==================== 鸡汤库 ====================
        Map<String, List<String>> chickenSoup = new LinkedHashMap<>();
        chickenSoup.put("love", List.of(
            "最好的爱情不是一见钟情，而是久处不厌",
            "爱不是寻找完美的人，而是学会用完美的眼光看待不完美的人",
            "先爱自己，再爱别人，这样的爱情才健康",
            "遇到对的人不是心动，而是安心",
            "真正的爱，是允许对方做自己",
            "感情不是占有，而是欣赏",
            "好的爱情是你通过一个人看到整个世界",
            "最浪漫的不是花前月下，而是柴米油盐里还有心动",
            "两个人在一起，最重要的不是门当户对，而是三观相合",
            "爱一个人最好的方式，是让TA自由成长"
        ));
        chickenSoup.put("growth", List.of(
            "你今天流的每一滴汗，都是明天闪闪发光的勋章",
            "这世上没有白走的路，每一步都算数",
            "不是看到希望才坚持，而是坚持了才能看到希望",
            "把每一次跌倒当作下一次起跳的蓄力",
            "你不必生来就很强大，但你可以越来越强大",
            "与其等待完美时机，不如现在就出发",
            "成长就是把哭声调成静音的过程",
            "没有人会一直顺利，但总有人一直在坚持",
            "今天你觉得难的事，明年可能只是日常",
            "你所有的努力，终将化为你想要的生活"
        ));
        chickenSoup.put("healing", List.of(
            "慢下来也没关系，重要的是不要停",
            "接纳不完美的自己，才是真正的强大",
            "每一个你讨厌的现在，都有一个不够努力的曾经",
            "允许一切发生，接纳所有结果",
            "你已经很棒了，不需要别人来证明",
            "有些风雨不是来阻挡你的，而是来洗礼你的",
            "生活不会亏待每一个认真生活的人",
            "熬过了最难的日子，你就配得上最好的未来",
            "没关系，天亮了一切都会好的",
            "当你觉得累了，说明你在走上坡路"
        ));
        chickenSoup.put("female_power", List.of(
            "女人最大的底气，是经济独立和精神丰盈",
            "你不需要很厉害才能开始，但你需要开始才能变得很厉害",
            "别让任何人定义你的人生，你就是自己的品牌",
            "真正的女性力量，不是成为超人，而是成为最好的自己",
            "女人这辈子最重要的投资，就是投资自己",
            "不要因为便宜就将就，不管是商品还是人",
            "你的美丽不需要别人的认可，但值得自己的欣赏",
            "30岁不是人生的终点，而是最好的起点",
            "女人最高级的活法：靠自己赚钱，对自己温柔",
            "没有公主命，那就练一身公主病都治不了的本事"
        ));
        MATERIAL_LIBRARY.put("chicken_soup", chickenSoup);

        // ==================== 名言警句库 ====================
        Map<String, List<String>> quotes = new LinkedHashMap<>();
        quotes.put("inspirational", List.of(
            "生活不止眼前的苟且，还有诗和远方——高晓松",
            "世界上只有一种英雄主义，就是看清了生活的真相之后依然热爱生活——罗曼·罗兰",
            "成功不是终点，失败也不是末日，重要的是继续前行的勇气——丘吉尔",
            "天行健，君子以自强不息——《周易》",
            "千里之行，始于足下——老子",
            "不积跬步，无以至千里——荀子",
            "宝剑锋从磨砺出，梅花香自苦寒来——《警世贤文》",
            "人生就像骑自行车，要保持平衡就得不断前行——爱因斯坦",
            "光明给我们经验，读书给我们知识——奥斯特洛夫斯基",
            "只要功夫深，铁杵磨成针——《方舆胜览》"
        ));
        quotes.put("philosophy", List.of(
            "知人者智，自知者明——老子",
            "我思故我在——笛卡尔",
            "未经审视的人生不值得度过——苏格拉底",
            "人不能两次踏进同一条河流——赫拉克利特",
            "存在先于本质——萨特",
            "他人即地狱——萨特",
            "认识你自己——苏格拉底",
            "知之为知之，不知为不知，是知也——孔子",
            "吾生也有涯，而知也无涯——庄子",
            "万物皆流，无物常驻——赫拉克利特"
        ));
        quotes.put("modern", List.of(
            "把时间花在进步上，而不是抱怨上",
            "你的时间有限，不要浪费在重复别人的生活上——乔布斯",
            "真正的自由不是想做什么就做什么，而是不想做什么就可以不做什么",
            "做自己，因为别人都已经有人做了——王尔德",
            "生命中最重要的事，是学会如何付出爱——莫里·施瓦茨",
            "所有的大人都曾经是小孩，虽然只有少数人记得——圣埃克苏佩里",
            "勇气不是不害怕，而是害怕了还能继续往前走——曼德拉",
            "当你真心渴望某样东西时，整个宇宙都会联合起来帮助你——保罗·柯艾略",
            "优秀是一种习惯，而不是一次行为——亚里士多德",
            "世界上最远的距离，不是生与死，而是我站在你面前你不知道我爱你——泰戈尔"
        ));
        MATERIAL_LIBRARY.put("quote", quotes);

        // ==================== 互动游戏库 ====================
        Map<String, List<String>> games = new LinkedHashMap<>();
        games.put("low_threshold", List.of(
            "屏幕扣1：觉得主播说得对的扣1，不对的扣2",
            "点赞到X万：点赞到10万，主播给大家加福利！",
            "666刷起来：觉得这个价格绝了的，评论区疯狂刷666",
            "双击屏幕，看看你的点赞能不能炸出爱心特效",
            "新来的朋友打个'来了'，让主播认识一下你",
            "觉得主播说得在理的，屏幕上打一排波浪号～～～",
            "点关注不迷路，主播带你上高速"
        ));
        games.put("medium_threshold", List.of(
            "猜价格：猜猜这款产品今天的直播价？猜对有奖！最接近的送一份",
            "选A选B：大家觉得A色号好看还是B色号好看？评论区告诉我",
            "今日话题：你们觉得什么才是好的护肤品？评论区说说你的标准",
            "用一个emoji形容你现在的心情，我来猜",
            "来个小调查：你的护肤品超过5样的扣1，5样以下扣2",
            "接龙游戏：说一个你觉得最不值得买的网红产品",
            "评论区说说你是哪个城市的，同城的报个到"
        ));
        games.put("high_threshold", List.of(
            "故事征集：评论区分享你的护肤逆袭故事，最感人的送礼物",
            "使用效果：买了我们产品的姐妹，评论区晒出使用前后对比",
            "创意挑战：用三个字形容你看到这款产品的感受",
            "讲一个你最尴尬的购物经历，最精彩的送小样",
            "挑战：30秒内说出5个你用过的好用产品",
            "开启辩论赛：护肤品贵的一定比便宜的好吗？正方扣1反方扣2",
            "拍一张你的化妆台发评论区，最整洁的送福袋"
        ));
        games.put("emotional_test", List.of(
            "爱情测试：如果你的另一半做了这件事你会怎么办？选A原谅 选B分手 评论区告诉我",
            "性格测试：你更像哪种花？玫瑰扣1 向日葵扣2 薰衣草扣3",
            "缘分测试：你的生日尾数是几？1-3号的跟主播有缘",
            "直觉测试：我左手和右手各有一个福袋，猜猜哪个是大奖？扣左或扣右"
        ));
        games.put("scenario_choice", List.of(
            "情景选择：如果只能留一样化妆品，你留口红还是粉底？评论区选",
            "如果穿越到古代做公主/王后，你选要美貌还是要智慧？",
            "假设你中了100万，你先买房还是先全球旅行？扣1买房 扣2旅行",
            "如果明天世界末日，你今晚会做什么？评论区写出来"
        ));
        games.put("topic_debate", List.of(
            "辩论赛：好看的皮囊vs有趣的灵魂，你选哪个？正方扣1反方扣2",
            "有钱人的快乐你想象不到 vs 普通人也有普通人的幸福，你站哪边？",
            "护肤该从多少岁开始？18岁党扣1 25岁党扣2 30岁党扣3",
            "讨论：你觉得直播间买东西靠谱吗？信任的扣1 观望的扣2"
        ));
        MATERIAL_LIBRARY.put("interactive_game", games);

        // ==================== BGM策略库 ====================
        Map<String, List<String>> bgm = new LinkedHashMap<>();
        bgm.put("luxury", List.of(
            "奢华钢琴曲（缓慢、高级感）- 适合高端产品/品牌故事",
            "古典弦乐（优雅、有格调）- 适合品质展示",
            "轻爵士（慵懒、品位）- 适合生活方式展示",
            "法式手风琴（浪漫精致）- 适合美妆/香水类产品",
            "交响乐片段（大气磅礴）- 适合品牌发布/重大活动",
            "竖琴独奏（空灵清冷）- 适合高端护肤/仪式感场景",
            "Bossa Nova（慵懒时尚）- 适合度假/轻奢生活方式",
            "古典吉他（温暖有质感）- 适合故事型品牌叙事"
        ));
        bgm.put("energetic", List.of(
            "土味嗨曲（突然炸裂）- 适合反差变装/搞笑反转",
            "电音节拍（快节奏）- 适合促销/秒杀倒计时",
            "洗脑神曲（循环上头）- 适合流量型短视频",
            "社会摇BGM（魔性）- 适合搞笑/自嘲类内容",
            "鬼畜混音（节奏感强）- 适合快切/节奏型短视频",
            "运动音乐（激昂向上）- 适合逆袭/挑战类内容",
            "嘻哈说唱风（潮流）- 适合年轻化/潮流类产品",
            "Funk节奏（复古嗨感）- 适合开场暖场/福袋环节"
        ));
        bgm.put("emotional", List.of(
            "治愈钢琴（轻柔温暖）- 适合情感故事/鸡汤内容",
            "民谣吉他（文艺清新）- 适合日常/生活分享",
            "深夜电台风（低沉温柔）- 适合深度分享/价值输出",
            "Lofi Hip-hop（轻松氛围）- 适合陪伴感/长时间直播",
            "新世纪音乐（空灵深远）- 适合冥想/护肤仪式感",
            "民族乐器（古筝/琵琶）- 适合国风/传统文化类",
            "童声合唱（纯净温暖）- 适合亲子/母婴类话题",
            "口哨曲（轻松明快）- 适合日常好物/轻松种草"
        ));
        bgm.put("transition", List.of(
            "鼓点过渡（渐强）- 产品切换/环节过渡",
            "快速琶音上行 - 制造期待感",
            "音效：叮~（清脆）- 要点强调/价格揭晓",
            "音效：啪！（干脆）- 倒计时/上链接",
            "静音1秒 - 悬念制造/反差前奏"
        ));
        MATERIAL_LIBRARY.put("bgm", bgm);

        // ==================== 表演配合库 ====================
        Map<String, List<String>> performance = new LinkedHashMap<>();
        performance.put("expression_love", List.of(
            "甜蜜微笑：嘴角自然上扬，眼睛含笑",
            "委屈撒娇：嘟嘴+微微低头，像在央求",
            "心疼表情：微微皱眉+眼含同情，看向镜头",
            "恍然大悟：眼睛突然睁大，嘴型从O变为笑容",
            "故作生气：叉腰+嘟嘴，但眼睛在笑"
        ));
        performance.put("expression_funny", List.of(
            "夸张惊讶：嘴巴张大到可以塞下鸡蛋",
            "故作无辜：歪头+眨眼，双手摊开",
            "内心崩溃：面无表情配合嘴角微微抽动",
            "假装思考：食指顶下巴，眼睛看天花板",
            "得意忘形：挑眉+邪笑，双手比心"
        ));
        performance.put("gesture_emphasis", List.of(
            "数字手势：一二三四五配合要点讲解",
            "双手对比：左手低右手高展示差异",
            "画圈强调：食指在空中画圈表示重复",
            "拍桌/击掌：配合价格公布的震撼感",
            "交叉否定：双手在胸前交叉表示NO"
        ));
        performance.put("gesture_emotion", List.of(
            "拥抱动作：双手环抱自己表示温暖",
            "心脏位置：手放心口表示真心推荐",
            "指向观众：伸手指向镜头制造亲密感",
            "竖大拇指：配合夸赞和认可",
            "双手合十：表示感谢和诚恳"
        ));
        performance.put("voice_rhythm", List.of(
            "快节奏爆发：倒计时秒杀时语速加快",
            "慢节奏深情：讲故事时语速放慢、声音变低",
            "重音强调：关键价格和卖点加重语气",
            "停顿悬念：关键信息前停顿1-2秒",
            "语调上扬：提问互动时语调上扬制造期待"
        ));
        performance.put("voice_emotion", List.of(
            "惊喜兴奋：声音提高一个八度，语气词密集",
            "温柔治愈：轻声细语，像在耳边说话",
            "坚定有力：铿锵有力，掷地有声",
            "紧迫催促：急促、密集、带喘气感",
            "故作神秘：压低声音，像在说秘密"
        ));
        MATERIAL_LIBRARY.put("performance", performance);

        // ==================== 风险管控话术库 ====================
        Map<String, List<String>> riskControl = new LinkedHashMap<>();
        riskControl.put("sensitive_topic", List.of(
            "这个话题比较敏感哈，我们换个轻松的聊聊",
            "每个人的情况不一样，我分享的只是我的经验",
            "这个要具体问题具体分析，下播后可以私信我",
            "这个话题很深，今天时间有限，改天专门开一场"
        ));
        riskControl.put("controversy", List.of(
            "大家有不同意见很正常，我们求同存异哈",
            "这个观点仅代表我个人，不一定对，欢迎讨论",
            "每个人立场不同看法也不同，尊重每一个观点",
            "有争议说明这个话题值得深入探讨"
        ));
        riskControl.put("cold_scene", List.of(
            "家人们是不是都在认真听？来个互动证明一下",
            "新来的朋友们打个招呼呀，让主播知道你在",
            "接下来要放大招了，千万不要走开哦",
            "觉得主播说得好的扣个1，给主播点信心"
        ));
        riskControl.put("negative_comment", List.of(
            "每个人都有表达的权利，感谢你的反馈",
            "我理解你的感受，但我们保持尊重好吗",
            "有不同意见很正常，我们用事实说话",
            "谢谢提醒，我会改进的"
        ));
        riskControl.put("bad_state", List.of(
            "今天状态不太好，但家人们的支持就是我最大的动力",
            "虽然今天身体不太舒服，但直播间的温暖让我满血复活",
            "人总有状态不好的时候，但承诺了就要做到"
        ));
        riskControl.put("technical_issue", List.of(
            "网络好像卡了一下，大家稍等，马上恢复",
            "设备临时出了点状况，感谢大家的耐心等待",
            "技术问题解决了，我们继续，刚才说到哪里了"
        ));
        riskControl.put("complaint_handling", List.of(
            "姐妹，你的问题我记下来了，下播后第一时间帮你处理",
            "非常理解你的感受，这个情况我们马上核实，绝对给你一个满意的答复",
            "感谢你的反馈，出现这种情况确实不应该，我们会立刻改进",
            "你放心，售后问题找我就对了，咱们一定负责到底"
        ));
        riskControl.put("high_value_urge", List.of(
            "姐妹，这个价格真的是年度最低了，犹豫就没了哦",
            "我知道大额下单需要考虑，但这个组合装省了XX元，错过再等一年",
            "已经有XX位姐妹拍了，你的购物车里还在等你，别让它孤单",
            "大额订单我额外送你XX，私信我备注就行，这是VIP待遇"
        ));
        riskControl.put("product_transition", List.of(
            "好，刚才的XX大家抢得超快，接下来这款更厉害了",
            "从护肤聊到彩妆，下面这款是我今天的压轴好物",
            "前面说的是基础护理，接下来给你们看进阶好物",
            "这款和刚才那个搭配使用效果翻倍，一起拍更划算"
        ));
        MATERIAL_LIBRARY.put("risk_control", riskControl);

        // ==================== 话术结构模板库 ====================
        Map<String, List<String>> scriptStructure = new LinkedHashMap<>();
        scriptStructure.put("opening_emotional", List.of(
            "【情感类开场·黄金3分钟】段子开场(30s)→悬念设置(45s)→福利预告(30s)→互动启动(75s)",
            "段子：今天聊聊恋爱那些事儿，单身的听完想恋爱，恋爱的听完想单身！",
            "悬念：我有个朋友，谈了3年恋爱，昨天分手了，原因你绝对想不到...",
            "福利：今天评论区的金句王，送情感咨询一次！",
            "互动：正在经历情感困惑的打'困惑'，感情甜蜜的打'甜蜜'！"
        ));
        scriptStructure.put("opening_phenomenal", List.of(
            "【现象级IP开场】福袋开场/数据开场/悬念开场，3秒抓注意力",
            "福袋：家人们！先点关注！待会有超级大福袋！不点关注领不了！",
            "数据：今天在线XX人！冲到XX人直接开秒杀！大家帮我冲一冲！",
            "悬念：今天有一个炸裂消息要宣布，但要先做到XXX才揭晓！"
        ));
        scriptStructure.put("opening_top", List.of(
            "【顶级IP开场】价值开场/故事开场/陪伴开场，建立信任",
            "价值：今天我要分享一个99%的人都不知道的护肤真相...",
            "故事：上周一个客户跟我说了一件事，让我决定今天必须聊这个话题...",
            "陪伴：又到了我们的老时间，今天泡杯茶，慢慢聊..."
        ));
        scriptStructure.put("closing_phenomenal", List.of(
            "【现象级IP逼单·5分钟快节奏】",
            "第1分钟：价格锚点+稀缺预告→原价999今天299！仅限今天！",
            "第2分钟：库存施压→已经卖了5000单！只剩2000！",
            "第3分钟：倒计时开始→还有最后3分钟！3分钟后恢复原价！",
            "第4分钟：从众施压→评论区都在晒单！你还在等什么？",
            "第5分钟：最终倒计时→54321！上链接！手慢无！"
        ));
        scriptStructure.put("closing_top", List.of(
            "【顶级IP逼单·8-12分钟慢节奏】",
            "第1-3分钟：价值重申→最后强调三个核心价值...",
            "第4-6分钟：决策支持→根据你的情况，我建议...",
            "第7-9分钟：风险消除→我们的售后保障是...",
            "第10-12分钟：理性呼吁→我相信你能做出明智的选择..."
        ));
        scriptStructure.put("cold_to_hot_sop", List.of(
            "【冷场→热场SOP】检测(冷场>30s)→启动暖场→验证回暖",
            "Step1·检测：在线人数下降/弹幕减少/互动率降低时触发",
            "Step2·选择暖场方式：①抽奖(最快) ②互动游戏(中速) ③话题切换(慢但持久)",
            "Step3·抽奖话术：看来大家需要点刺激！直接抽奖！截屏抽3位送福利！321截！",
            "Step4·游戏话术：来玩个游戏！我说前半句你说后半句！说对的送优惠券！",
            "Step5·话题话术：刚才聊的有点干货了，来说说你们最近遇到的XX故事？",
            "Step6·验证：暖场后30秒检查弹幕/在线人数是否回升，未回升则升级到下一方式"
        ));
        MATERIAL_LIBRARY.put("script_structure", scriptStructure);

        // ==================== 互动话术分类库 ====================
        Map<String, List<String>> interactionScripts = new LinkedHashMap<>();
        interactionScripts.put("like_guide", List.of(
            "觉得主播说得有道理的帮我点个赞！点赞过10万我直接放价！",
            "喜欢这个产品的双击小红心！让我看看有多少人心动了！",
            "用力点赞！赞越多福利越大！冲冲冲！",
            "帮忙点赞转发！让更多姐妹看到今天的好价！"
        ));
        interactionScripts.put("comment_guide", List.of(
            "想要这个颜色的打'想要'！想要另一个色号的打'换色'！",
            "用过的扣2没用过的扣3！让我看看老粉有多少！",
            "你们觉得A好看还是B好看？评论区告诉我！",
            "有什么想问的直接评论区打出来，我一个一个回答！"
        ));
        interactionScripts.put("share_guide", List.of(
            "觉得今天直播有用的分享给闺蜜！好东西要一起看！",
            "转发到3个群的截图发我，额外送你一份小样！",
            "这个价格真的值得安利！帮我转发出去！",
            "分享直播间到朋友圈的截图给我，我私信发你优惠码！"
        ));
        interactionScripts.put("follow_guide", List.of(
            "新来的朋友先点个关注！不迷路！下次开播你就能收到通知！",
            "还没关注的赶紧关注！关注我的专属粉丝价更便宜！",
            "点关注+加粉丝团，享受粉丝专属福袋！不关注领不了！",
            "关注主播，你就是我的家人！家人的价格你懂的！"
        ));
        MATERIAL_LIBRARY.put("interaction_scripts", interactionScripts);

        // ==================== 短视频前3秒黄金开头公式库 ====================
        Map<String, List<String>> goldenOpening = new LinkedHashMap<>();
        goldenOpening.put("suspense", List.of(
            "你绝对想不到，这个XX竟然可以…",
            "我今天要曝光一个行业秘密…",
            "刷到这条视频的都是有福之人…",
            "千万别滑走！接下来你看到的会颠覆认知！"
        ));
        goldenOpening.put("contrast", List.of(
            "室友说我这样找不到男朋友…（转场后惊艳变装）",
            "同事以为我月薪3千…（转场后展示生活）",
            "化妆前 vs 化妆后，你能认出这是同一个人吗？",
            "老公觉得我又在浪费钱…（展示使用效果）"
        ));
        goldenOpening.put("hotspot", List.of(
            "XX婚礼妆容刷屏！但说实话…",
            "全网都在学的XX技巧，我来告诉你真相！",
            "最近大家都在讨论的XX，其实很多人都理解错了…",
            "跟着XX学的方法，我试了7天，结果…"
        ));
        goldenOpening.put("question", List.of(
            "你知道为什么你的妆总是不持久吗？",
            "每天花2小时护肤的你，是不是做错了这3件事？",
            "月薪5千和月薪5万的人，护肤差在哪？",
            "为什么同样的产品，别人用效果好你用没效果？"
        ));
        MATERIAL_LIBRARY.put("golden_opening", goldenOpening);

        // ==================== 搞笑手法库（反差变装/搞笑短视频专用）====================
        Map<String, List<String>> comedyTechniques = new LinkedHashMap<>();
        comedyTechniques.put("facial_comedy", List.of(
            "【夸张瞪眼】看到变装效果后眼睛瞪到最大，嘴巴O型，停顿2秒",
            "【假装无辜】变装前故意做出呆萌表情，配合慢眨眼",
            "【表情渐变】从嫌弃脸→震惊脸→得意脸，3秒内完成过渡",
            "【挑眉杀】变装完成瞬间单挑眉+歪嘴笑，制造反差萌"
        ));
        comedyTechniques.put("action_comedy", List.of(
            "【慢动作甩头】转场时用慢动作甩头发，配合风机效果",
            "【踉跄出场】前段故意走路踉跄→后段优雅猫步，反差出场",
            "【夸张比心】用整个手臂画圈比心，动作幅度越大越搞笑",
            "【定格转身】背对镜头→音乐节拍点突然转身→定格pose"
        ));
        comedyTechniques.put("prop_comedy", List.of(
            "【拖把当话筒】前段拿拖把当话筒唱歌→后段拿真话筒/口红",
            "【大框眼镜】前段戴夸张大框眼镜→后段摘掉变女神",
            "【菜篮出场】前段提菜篮→后段换LV包，物件反差",
            "【头上夹子】前段满头发夹→后段精致发型，道具过渡"
        ));
        comedyTechniques.put("dialogue_comedy", List.of(
            "【自问自答】'这谁啊？' '是我啊！' '不可能！'——三连反差台词",
            "【方言切换】前段说土味方言→后段切标准普通话/英文，语言反差",
            "【倒计时】'3、2、1——变！'经典倒计时台词，制造期待感",
            "【吐槽旁白】前段画外音吐槽'你看看你这造型'→后段'还行嘛！'"
        ));
        comedyTechniques.put("intensity_control", List.of(
            "【轻度幽默】微表情+小动作，适合知性优雅风格，如微笑歪头、轻拍额头",
            "【中度搞笑】夸张表情+肢体语言，适合活泼可爱风格，如瞪眼+比心+小跑",
            "【重度爆笑】全身夸张表演+道具互动+搞笑台词，适合综艺风格，如摔倒+自嘲+神转折"
        ));
        MATERIAL_LIBRARY.put("comedy_techniques", comedyTechniques);

        // ==================== 反差变装视觉设计库 ====================
        Map<String, List<String>> contrastDesign = new LinkedHashMap<>();
        contrastDesign.put("costume_ceo_vs_country", List.of(
            "【前段·霸总】西装三件套+袖扣+胸针+皮鞋 → 发型：油头后梳/高马尾 → 妆容：全妆精致眉+红唇",
            "【后段·农村】碎花棉袄+围裙+布鞋+头巾 → 发型：散乱/双麻花辫 → 妆容：素颜+雀斑贴",
            "【反差指数】★★★★★ 记忆点：同一人的极致身份反转"
        ));
        contrastDesign.put("costume_messy_vs_queen", List.of(
            "【前段·邋遢】睡衣+拖鞋+乱发+黑眼圈贴纸 → 道具：薯片袋+手机",
            "【后段·女王】晚礼服/旗袍+高跟鞋+精致盘发 → 道具：红酒杯/手包",
            "【反差指数】★★★★ 记忆点：生活中的极致变身"
        ));
        contrastDesign.put("scene_contrast", List.of(
            "【空间反差】狭小出租屋(前段) ↔ 豪华酒店/别墅(后段)——空间大小对比",
            "【色彩反差】冷色调灰暗(前段) ↔ 暖色调明亮(后段)——色温对比",
            "【秩序反差】凌乱堆满杂物(前段) ↔ 整洁精致摆设(后段)——整洁度对比",
            "【社会反差】菜市场/工地(前段) ↔ CBD/商场(后段)——阶层场景对比"
        ));
        contrastDesign.put("lighting_ceo_to_country", List.of(
            "【前段灯光】冷色调5600K+硬光侧打+阴影感 → 突出精英气质",
            "【后段灯光】暖色调3200K+自然散射光+柔和 → 突出乡土亲切感",
            "【转场灯光】0.5秒黑屏/闪白过渡 → 增强冲击力"
        ));
        contrastDesign.put("lighting_messy_to_queen", List.of(
            "【前段灯光】顶光+无补光+偏黄 → 突出邋遢憔悴感",
            "【后段灯光】环形补光+柔光箱+偏粉 → 突出精致女神感",
            "【转场灯光】慢速渐变过渡(0.8-1秒) → 制造梦幻变身效果"
        ));
        MATERIAL_LIBRARY.put("contrast_design", contrastDesign);
    }

    @Override
    public List<Map<String, Object>> getRandomMaterials(String materialType, String category, int count) {
        Map<String, List<String>> typeMap = MATERIAL_LIBRARY.get(materialType);
        if (typeMap == null) return List.of();

        List<String> pool = new ArrayList<>();
        if (category != null && !category.isBlank() && typeMap.containsKey(category)) {
            pool.addAll(typeMap.get(category));
        } else {
            typeMap.values().forEach(pool::addAll);
        }

        if (pool.isEmpty()) return List.of();

        Collections.shuffle(pool, ThreadLocalRandom.current());
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < Math.min(count, pool.size()); i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", materialType);
            item.put("category", category);
            item.put("content", pool.get(i));
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getMaterialsByPersona(String materialType, String personaType, String ageRange, int count) {
        String category = null;
        if ("entertainment".equals(personaType)) {
            category = "joke".equals(materialType) ? "life" : null;
        } else if ("knowledge".equals(personaType)) {
            category = "quote".equals(materialType) ? "inspirational" : null;
        } else if ("lifestyle".equals(personaType)) {
            category = "chicken_soup".equals(materialType) ? "healing" : null;
        }
        return getRandomMaterials(materialType, category, count);
    }

    @Override
    public String buildMaterialPrompt(String materialType, String category) {
        List<Map<String, Object>> materials = getRandomMaterials(materialType, category, 3);
        if (materials.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("【参考素材（").append(getMaterialLabel(materialType)).append("）】\n");
        for (int i = 0; i < materials.size(); i++) {
            sb.append(i + 1).append(". ").append(materials.get(i).get("content")).append("\n");
        }
        sb.append("请参考以上素材风格，自然融入话术中，不要原样照搬。\n");
        return sb.toString();
    }

    @Override
    public Map<String, List<String>> getMaterialCategories() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        MATERIAL_LIBRARY.forEach((type, categories) -> {
            result.put(type, new ArrayList<>(categories.keySet()));
        });
        return result;
    }

    @Override
    public String buildPerformancePrompt(String category) {
        List<Map<String, Object>> materials = getRandomMaterials("performance", category, 3);
        if (materials.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("【表演配合指导】\n");
        for (Map<String, Object> m : materials) {
            sb.append("- ").append(m.get("content")).append("\n");
        }
        return sb.toString();
    }

    private String getMaterialLabel(String materialType) {
        return switch (materialType) {
            case "joke" -> "搞笑段子";
            case "chicken_soup" -> "鸡汤金句";
            case "quote" -> "名言警句";
            case "interactive_game" -> "互动游戏";
            case "bgm" -> "BGM策略";
            case "performance" -> "表演配合";
            case "risk_control" -> "风险管控";
            case "script_structure" -> "话术结构模板";
            case "interaction_scripts" -> "互动话术";
            case "golden_opening" -> "短视频黄金开头";
            case "comedy_techniques" -> "搞笑手法";
            case "contrast_design" -> "反差变装视觉设计";
            default -> materialType;
        };
    }

    @Override
    public List<Map<String, Object>> getAllMaterialsForHuashuExport() {
        List<Map<String, Object>> out = new ArrayList<>();
        // joke: marriage(夫妻), mother_in_law(婆媳) → type:家庭爱情 + type:搞笑幽默
        for (String cat : List.of("marriage", "mother_in_law")) {
            Map<String, List<String>> typeMap = MATERIAL_LIBRARY.get("joke");
            if (typeMap != null && typeMap.containsKey(cat)) {
                for (String c : typeMap.get(cat)) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("type", "joke");
                    m.put("category", cat);
                    m.put("content", c);
                    m.put("typeHint", "type:家庭爱情");
                    out.add(m);
                }
            }
        }
        // chicken_soup: love(家庭爱情)/growth/healing(正能量)/female_power(名言)
        for (String cat : List.of("love", "growth", "healing", "female_power")) {
            Map<String, List<String>> typeMap = MATERIAL_LIBRARY.get("chicken_soup");
            if (typeMap != null && typeMap.containsKey(cat)) {
                String hint = switch (cat) {
                    case "love" -> "type:家庭爱情";
                    case "female_power" -> "type:名言";
                    default -> "type:正能量";
                };
                for (String c : typeMap.get(cat)) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("type", "chicken_soup");
                    m.put("category", cat);
                    m.put("content", c);
                    m.put("typeHint", hint);
                    out.add(m);
                }
            }
        }
        // quote: inspirational/philosophy/modern → type:名言
        for (String cat : List.of("inspirational", "philosophy", "modern")) {
            Map<String, List<String>> typeMap = MATERIAL_LIBRARY.get("quote");
            if (typeMap != null && typeMap.containsKey(cat)) {
                for (String c : typeMap.get(cat)) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("type", "quote");
                    m.put("category", cat);
                    m.put("content", c);
                    m.put("typeHint", "type:名言");
                    out.add(m);
                }
            }
        }
        // joke: life, workplace → type:搞笑幽默
        for (String cat : List.of("life", "workplace")) {
            Map<String, List<String>> typeMap = MATERIAL_LIBRARY.get("joke");
            if (typeMap != null && typeMap.containsKey(cat)) {
                for (String c : typeMap.get(cat)) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("type", "joke");
                    m.put("category", cat);
                    m.put("content", c);
                    m.put("typeHint", "type:搞笑幽默");
                    out.add(m);
                }
            }
        }
        return out;
    }

    @Override
    public List<String> matchRiskScripts(String riskType) {
        Map<String, List<String>> riskLib = MATERIAL_LIBRARY.get("risk_control");
        if (riskLib == null) return List.of();
        if (riskType != null && !riskType.isBlank()) {
            List<String> scripts = riskLib.get(riskType);
            if (scripts != null && !scripts.isEmpty()) return scripts;
        }
        return riskLib.values().stream().flatMap(List::stream).limit(5).toList();
    }
}
