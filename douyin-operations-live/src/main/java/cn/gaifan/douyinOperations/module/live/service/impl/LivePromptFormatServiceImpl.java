package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.config.LiveBusinessConfig;
import cn.gaifan.douyinOperations.common.config.ShortVideoBusinessConfig;
import cn.gaifan.douyinOperations.module.live.config.LivePromptConfig;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveStylePresetRepository;
import cn.gaifan.douyinOperations.module.live.service.LivePromptFormatService;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiGenerateVO;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 直播话术 Prompt 格式化服务实现：风格展开、模板、产品类型、时段描述。
 */
@Service
public class LivePromptFormatServiceImpl implements LivePromptFormatService {

    private static final Logger log = LoggerFactory.getLogger(LivePromptFormatServiceImpl.class);

    @Resource private DyProductRepository productRepository;
    @Resource private LivePromptConfig promptConfig;
    @Resource(name = "liveStylePresetRepository") private LiveStylePresetRepository liveStylePresetRepository;
    @Resource private LiveBusinessConfig liveBusinessConfig;
    @Resource private ShortVideoBusinessConfig shortVideoBusinessConfig;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private cn.gaifan.douyinOperations.module.ai.service.brain.IpGrowthStageService ipGrowthStageService;

    private final AtomicReference<Map<String, String>> styleCache = new AtomicReference<>();
    private final AtomicLong styleCacheExpiry = new AtomicLong(0);

    @Override
    public String expandStyleForPrompt(String style) {
        if (style == null || style.isBlank()) return "";
        if (style.contains(",")) {
            return Arrays.stream(style.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(this::expandSingleStyle)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining("；"));
        }
        return expandSingleStyle(style);
    }

    private String expandSingleStyle(String style) {
        if (style == null || style.isBlank()) return "";
        if (style.startsWith("custom:")) {
            String customDesc = style.substring("custom:".length()).trim();
            return customDesc.isBlank() ? "" : "话术风格：" + customDesc;
        }
        String dbPrompt = lookupStyleFromDb(style.toLowerCase());
        if (dbPrompt != null) return dbPrompt;
        return expandSingleStyleFallback(style.toLowerCase());
    }

    private String lookupStyleFromDb(String styleKey) {
        long now = System.currentTimeMillis();
        Map<String, String> cached = styleCache.get();
        if (cached == null || now > styleCacheExpiry.get()) {
            try {
                List<cn.gaifan.douyinOperations.module.live.entity.LiveStylePreset> presets =
                        liveStylePresetRepository.findByActiveOrderBySortOrderAsc(1);
                Map<String, String> map = new ConcurrentHashMap<>();
                for (var p : presets) {
                    map.put(p.getStyleKey().toLowerCase(), p.getPromptTemplate());
                }
                styleCache.set(map);
                styleCacheExpiry.set(now + 5 * 60 * 1000L);
                cached = map;
            } catch (Exception e) {
                if (cached != null) return cached.get(styleKey);
                return null;
            }
        }
        return cached.get(styleKey);
    }

    private String expandSingleStyleFallback(String style) {
        return switch (style) {
            case "professional" -> "话术风格：专业、有条理、可信度高";
            case "friendly" -> "话术风格：亲切、像朋友聊天、拉近距离";
            case "passionate" -> "话术风格：热情、有感染力、情绪饱满";
            case "seeding" -> "话术风格：种草向、突出使用场景和体验感";
            case "promotion" -> "话术风格：促销向、限时抢购、库存紧张、制造紧迫";
            case "chicken_soup" -> "话术风格：高鸡汤、正能量、情感饱满。语言特点：多用感叹句和反问句增强感染力，融入人生感悟和励志金句，强调「你值得」「对自己好一点」「投资自己」，产品介绍与情感价值绑定，适当使用排比句和类比";
            case "proverb" -> "话术风格：高歇后语、俗语、接地气、有梗。语言特点：必须自然融入多条歇后语（前半句——后半句形式）或俗语，并与产品名称、卖点、使用场景深度融合；用歇后语引出或总结产品优势，幽默不油腻，有记忆点；禁止只堆歇后语不带产品，也禁止只介绍产品不带歇后语";
            case "emotional" -> "话术风格：高情感、情感饱满、故事感强。语言特点：共情表达、情绪共鸣、像在讲自己的经历，让人产生「这说的就是我」的认同感，拉停留";
            case "lyrical" -> "话术风格：高抒情、诗意优美。语言特点：排比、意境、比喻、金句，有审美感，格调高但不装，自然融入产品";
            case "heart_piercing" -> "话术风格：高扎心。语言特点：人生真相、成长代价、现实共鸣类金句，句式简短有力有冲击感，扎到点子上，可与「对自己好一点」等情感价值自然结合";
            case "humorous" -> "话术风格：搞笑幽默。语言特点：段子、梗、包袱、轻松调侃，让人愉悦，易留存、复播，但不低俗";
            case "positive" -> "话术风格：正能量。语言特点：励志、向上、希望、治愈，让人感觉被鼓励、被温暖，情绪正向";
            case "family" -> "话术风格：家庭类。语言特点：亲子、夫妻、婆媳、家庭责任等场景共鸣，贴近生活，女性受众易共鸣";
            case "love" -> "话术风格：爱情类。语言特点：夫妻关系、恋爱观、婚姻感悟，情感共鸣，高互动";
            case "local_flavor" -> "话术风格：地方特色。语言特点：可融入方言、地域梗、地方习俗，亲切感强，圈层认同（若人设或场次有地域信息请体现）";
            case "persona_flavor" -> "话术风格：人设特色。语言特点：体现人设标签、语气、记忆点，人设鲜明，粉丝粘性强";
            case "creative" -> "话术风格：高创意。语言特点：反套路、金句、类比、反转，新奇感强，传播力高，让人眼前一亮";
            case "gentle" -> "话术风格：温和、柔和、不施压，像闺蜜推荐，自然舒服";
            case "casual" -> "话术风格：轻松随意、聊天式表达，不刻意推销，降低防备";
            case "warm" -> "话术风格：温暖亲切、有关怀感，像家人般体贴，暖心";
            case "storytelling" -> "话术风格：故事型。语言特点：用真实或虚构故事引入，有起承转合，代入感强，拉停留";
            case "suspense" -> "话术风格：悬念型。语言特点：先抛悬念/问题，制造好奇心，再揭晓答案，引导看到最后";
            case "comparison" -> "话术风格：对比型。语言特点：通过前后对比、竞品对比突出优势，数据说话，真实可信";
            case "interactive" -> "话术风格：互动型。语言特点：多用提问、投票、口令、抽奖等互动引导，活跃气氛，提升参与度";
            case "scenario" -> "话术风格：场景代入。语言特点：描绘具体使用场景，让观众想象自己使用的画面，增强购买欲";
            case "fast" -> "话术风格：快节奏。语言特点：信息密度高、语速快、节奏紧凑，适合限时秒杀和促单冲刺";
            case "slow" -> "话术风格：慢节奏。语言特点：语速舒缓、娓娓道来，适合高客单和需要信任感的产品";
            case "caihongpi" -> "话术风格：彩虹屁。语言特点：花式夸赞、甜言蜜语、极致赞美，真诚不做作，让人心花怒放、如沐春风，适合暖场和拉近距离";
            case "poison_soup" -> "话术风格：毒鸡汤。语言特点：反讽、夸张、反转金句，表面正能量实则扎心，幽默中带现实感，如「努力不一定成功，但不努力一定很舒服」，接地气如绝绝子";
            case "pain_resonance" -> "话术风格：痛点共鸣。语言特点：直击生活痛点、制造缺失感，让观众产生「这说的就是我」的代入感，再自然引出解决方案";
            case "healing" -> "话术风格：治愈系。语言特点：轻柔治愈、减压放松，像深夜电台般舒适，适合高压人群，传递「慢下来也没关系」的温暖";
            case "growth_comeback" -> "话术风格：成长逆袭。语言特点：时间跨度对比、强反差、逆袭故事，从低谷到蜕变，激励感强，容易引发评论区分享";
            case "self_mockery" -> "话术风格：自嘲幽默。语言特点：先承认缺点再转折亮点，拉低姿态增强亲和力，让人觉得真实接地气，「我也是普通人」的代入感";
            case "rhyme_jingle" -> "话术风格：顺口溜/押韵。语言特点：节奏感强、朗朗上口、易记易传播，适合口播金句和记忆点打造，高情商回复也适用";
            case "worker_life" -> "话术风格：打工人/职场。语言特点：职场共鸣、摸鱼日常、加班心酸、老板语录，引发打工人强烈认同，评论区炸裂型话题";
            default -> {
                log.warn("未识别的话术风格: {}，使用原始风格描述", style);
                yield "话术风格：" + style;
            }
        };
    }

    @Override
    public String formatProductTypeLabel(String productType) {
        if (productType == null || productType.isBlank()) return null;
        return Arrays.stream(productType.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(t -> switch (t) {
                    case "hot" -> "爆品";
                    case "profit" -> "利润品";
                    case "loss" -> "亏品";
                    case "flat" -> "平价品";
                    case "control" -> "控单产品";
                    case "backup" -> "备用品";
                    case "skincare" -> "护肤";
                    case "护肤" -> "护肤";
                    case "精华" -> "精华";
                    case "面膜" -> "面膜";
                    case "眼霜" -> "眼霜";
                    case "套盒" -> "套盒";
                    default -> t;
                })
                .reduce((a, b) -> a + "、" + b)
                .orElse(null);
    }

    @Override
    public String getProductTypePromptHint(String productType) {
        if (productType == null || productType.isBlank()) return "";
        Set<String> types = new HashSet<>(Arrays.asList(productType.split(",")));
        List<String> hints = new ArrayList<>();
        if (types.contains("hot")) hints.add("爆品：人气高时主推，深度讲解 1-5 分钟");
        if (types.contains("profit")) hints.add("利润品：人气高时主推，高客单价高利润，30-60 秒。话术结构建议：功能（是什么）→ 优势（为什么好）→ 利益（对用户的价值）→ 证据（自用/案例/数据）");
        if (types.contains("loss")) hints.add("亏品：极速过品 3-10 秒");
        if (types.contains("flat")) hints.add("平价品：标准过品约 15 秒");
        if (types.contains("backup")) hints.add("备用品：标准过品约 15 秒，可作补充");
        if (types.contains("control")) hints.add("控单产品：吊胃口、控单规则、分批放单、催促抢购");
        if (types.stream().anyMatch(t -> "护肤".equals(t) || "skincare".equalsIgnoreCase(t) || "精华".equals(t) || "面膜".equals(t) || "眼霜".equals(t) || "套盒".equals(t))) {
            hints.add("护肤品：成分科普、功效合规、专业形象，30-60 秒强调价值感");
        }
        return String.join("；", hints);
    }

    @Override
    public String buildDurationHintForProductType(String productType) {
        if (productType == null || productType.isBlank()) return "";
        Set<String> types = new HashSet<>(Arrays.asList(productType.split(",")));
        String primaryType = types.stream().findFirst().orElse("");
        LivePromptConfig.ProductTypeDuration configDuration = promptConfig != null ? promptConfig.getDuration(primaryType) : null;

        int wordCountMin, wordCountMax;
        String durationHint;
        if (configDuration != null) {
            wordCountMin = configDuration.getWordMin();
            wordCountMax = configDuration.getWordMax();
            durationHint = String.format("%s：%d-%d 秒", formatProductTypeLabel(productType), configDuration.getMin(), configDuration.getMax());
        } else if (types.contains("hot")) {
            wordCountMin = 180;
            wordCountMax = 900;
            durationHint = "爆品：人气高时主推，深度讲解 1-5 分钟，限时抢购、库存紧张";
        } else if (types.contains("control")) {
            wordCountMin = 600;
            wordCountMax = 1200;
            durationHint = "请生成控单话术，包含：吊胃口（30秒）、产品讲解（40秒）、控单规则（20秒）、分批放单引导";
        } else if (types.contains("profit")) {
            wordCountMin = 90;
            wordCountMax = 180;
            durationHint = "利润品：人气高时主推高客单价，30-60 秒，强调品质、成分、价值感";
        } else if (types.contains("loss")) {
            wordCountMin = 10;
            wordCountMax = 30;
            durationHint = "亏品：极速过品 3-10 秒，一句话报价+福利";
        } else if (types.contains("backup")) {
            wordCountMin = 40;
            wordCountMax = 50;
            durationHint = "备用品：标准过品约 15 秒，可作补充";
        } else {
            wordCountMin = 40;
            wordCountMax = 50;
            durationHint = "平价品：标准过品约 15 秒，强调性价比";
        }
        return String.format("产品分类：%s\n话术时长要求：%s\n字数范围：%d-%d 字\n",
                formatProductTypeLabel(productType), durationHint, wordCountMin, wordCountMax);
    }

    @Override
    public String buildIpTypeDesc(String ipType) {
        if (ipType == null || ipType.isBlank()) return "";
        if (liveBusinessConfig == null || shortVideoBusinessConfig == null) return "";
        LiveBusinessConfig.RetentionFrequency rf = liveBusinessConfig.getRetention();
        LiveBusinessConfig.ValueFormula vf = liveBusinessConfig.getValueFormula();
        if (vf == null) return "";
        return switch (ipType) {
            case "phenomenal" -> {
                LiveBusinessConfig.ValueFormula.PhenomenalValue pv = vf.getPhenomenal();
                if (pv == null) yield "";
                yield String.format("""
                    【IP策略：现象级】
                    - 话术特征：高能量、强互动、3秒抓注意力、制造话题
                    - FIRE法则：F(Fun趣味)→I(Interaction互动)→R(Reward奖励)→E(Emotion情绪)
                    - 开场：福袋+悬念+数据开场，制造紧迫感
                    - 产品：稀缺营造+价格锚定+行动号召，快节奏（逼单节奏：5分钟内完成从种草到成交）
                    - 互动：高频互动，每2分钟一次互动指令
                    - 语言：短句为主，语气词丰富，情绪饱满
                    - 留人频率：每%d分钟制造悬念，每%d分钟小高潮，每%d分钟输出实用技巧
                    - 产品价值塑造公式（总%ds）：痛点%ds→方案%ds→效果%ds→背书%ds→价格%ds
                    - 情绪能量曲线：%s（%s）""",
                    rf.getSuspenseMinutes(), rf.getMiniClimaxMinutes(), rf.getPracticalTipMinutes(),
                    pv.getPainPointSec()+pv.getSolutionSec()+pv.getEffectSec()+pv.getEndorsementSec()+pv.getPriceSec(),
                    pv.getPainPointSec(), pv.getSolutionSec(), pv.getEffectSec(), pv.getEndorsementSec(), pv.getPriceSec(),
                    shortVideoBusinessConfig.getEmotionCurve().getPhenomenalCurve(),
                    shortVideoBusinessConfig.getEmotionCurve().getPhenomenalDesc());
            }
            case "top" -> {
                LiveBusinessConfig.ValueFormula.TopValue tv = vf.getTop();
                if (tv == null) yield "";
                yield String.format("""
                    【IP策略：顶级】
                    - 话术特征：深度价值、专业信任、知识输出、长期陪伴
                    - DEPTH法则：D(Depth深度)→E(Education教育)→P(Professional专业)→T(Trust信任)→H(Heart走心)
                    - 开场：价值开场+故事开场+陪伴开场，建立信任
                    - 产品：价值塑造+专业解析+成分讲解，慢节奏深度（逼单节奏：8-12分钟完成价值教育到决策推动）
                    - 互动：深度互动，引导分享经历和感受
                    - 语言：信息密度高，逻辑清晰，专业但不生硬
                    - 留人频率：每%d分钟制造悬念，每%d分钟小高潮，每%d分钟输出实用技巧
                    - 产品价值塑造公式（总%ds）：问题%ds→方案%ds→数据%ds→技术%ds→价值%ds
                    - 情绪能量曲线：%s（%s）""",
                    rf.getSuspenseMinutes(), rf.getMiniClimaxMinutes(), rf.getPracticalTipMinutes(),
                    tv.getProblemSec()+tv.getSolutionSec()+tv.getDataSec()+tv.getTechSec()+tv.getValueSec(),
                    tv.getProblemSec(), tv.getSolutionSec(), tv.getDataSec(), tv.getTechSec(), tv.getValueSec(),
                    shortVideoBusinessConfig.getEmotionCurve().getTopCurve(),
                    shortVideoBusinessConfig.getEmotionCurve().getTopDesc());
            }
            default -> "";
        };
    }

    @Override
    public String buildIpGrowthStageDesc(String ipType, long followerCount, int operatingMonths) {
        if (ipGrowthStageService == null || ipType == null) return "";
        String stage = "phenomenal".equals(ipType)
                ? ipGrowthStageService.getPhenomenalStage(followerCount)
                : ipGrowthStageService.getTopStage(operatingMonths);
        Map<String, Object> strategy = ipGrowthStageService.getStageStrategy(ipType, stage);
        if (strategy.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n【IP增长阶段：%s】\n", strategy.getOrDefault("label", stage)));
        if (strategy.containsKey("contentFocus"))
            sb.append("- 内容策略：").append(strategy.get("contentFocus")).append("\n");
        if (strategy.containsKey("talkingPointPriority"))
            sb.append("- 话术重点：").append(strategy.get("talkingPointPriority")).append("\n");
        if (strategy.containsKey("conversionHint"))
            sb.append("- 变现提示：").append(strategy.get("conversionHint")).append("\n");
        return sb.toString();
    }

    @Override
    public String buildTimeSlotDesc(String timeSlot) {
        if (timeSlot == null || timeSlot.isBlank()) return "";
        String normalized = timeSlot.replace("min", "").trim();
        return switch (normalized) {
            case "0-15" -> "【时段策略：0-15分钟·留人破冰】目标：在线从低拉到高，福袋+预告+互动，制造期待感";
            case "15-45" -> "【时段策略：15-45分钟·价值建立】目标：深度种草，建立信任，输出专业价值，提升停留时长";
            case "45-75" -> "【时段策略：45-75分钟·转化高潮】目标：稀缺营造+价格锚定+限时逼单，推动成交高峰";
            case "75-105" -> "【时段策略：75-105分钟·逼单冲刺】目标：限时限量+倒计时+库存紧张，最后冲刺转化";
            case "105-120" -> "【时段策略：105-120分钟·收尾预告】目标：感谢+复盘+预告下场，引导关注+分享";
            default -> "";
        };
    }

    @Override
    public String buildScriptModuleDesc(String module) {
        if (module == null || module.isBlank()) return "";
        return switch (module) {
            case "emotional_drive" -> """
                【模块：情绪驱动】用情绪拉停留
                话术框架：①痛点共鸣（你是不是也遇到过…）→ ②情绪放大（真的太难了/太心疼了）→ ③身份认同（我们这样的人…）→ ④希望出口（但是今天…）
                技巧：多用感叹号和省略号，语气词丰富（哎/唉/天呐/姐妹），停顿制造情绪空间
                情绪曲线：低开（引共鸣）→ 走低（痛点深挖）→ 反转拉高（方案出现）→ 高收（价值升华）""";
            case "value_creation" -> """
                【模块：价值塑造】用价值建立信任
                话术框架：①问题揭示（很多人不知道…）→ ②专业解析（其实原理是…）→ ③数据佐证（XX%的人…）→ ④方案推荐（所以我建议…）→ ⑤效果展示（用了之后…）
                技巧：信息密度高，用数据和专业术语建立权威感，但要通俗易懂
                情绪曲线：中开（引好奇）→ 稳步上升（知识输出）→ 高收（恍然大悟/受益匪浅）""";
            case "conversion_engine" -> """
                【模块：转化引擎】推动成交
                话术框架：①价值锚定（专柜价XX元）→ ②稀缺制造（只有XX份/最后XX分钟）→ ③价格对比（今天只要XX元）→ ④叠加福利（还送XX/买一送一）→ ⑤行动号召（3、2、1上链接/手慢无）
                技巧：语速加快，倒计时制造紧迫感，重复关键利益点
                情绪曲线：中开→ 逐步拉高（价值堆叠）→ 爆发（价格揭晓）→ 急收（限时逼单）""";
            case "trust_reinforcement" -> """
                【模块：信任加固】消除顾虑
                话术框架：①权威背书（XX品牌/XX明星同款）→ ②风险消除（7天无理由/运费险）→ ③真实案例（上次有个姐妹…）→ ④售后保障（任何问题找我）→ ⑤情感绑定（我自己也在用）
                技巧：真诚走心，个人体验分享，降低防备心理
                情绪曲线：平稳温暖→ 真诚共情→ 信任高峰→ 安心收尾""";
            default -> "";
        };
    }

    @Override
    public String buildMaterialTypeDesc(String materialType) {
        if (materialType == null || materialType.isBlank()) return "";
        return switch (materialType) {
            case "joke" -> "【素材融入：搞笑段子】请在话术中自然穿插幽默段子或梗，让观众轻松愉悦，提升互动";
            case "chicken_soup" -> "【素材融入：鸡汤金句】请在话术中融入正能量金句或人生感悟，引发情感共鸣";
            case "quote" -> "【素材融入：名言警句】请在话术中引用名人名言或经典语录，提升格调和说服力";
            case "interactive_game" -> "【素材融入：互动游戏】请在话术中设计互动环节（猜价格/选A选B/接龙/评论区互动），活跃气氛";
            default -> "";
        };
    }

    @Override
    public String buildRetentionStrategyDesc(String strategy) {
        if (strategy == null || strategy.isBlank()) return "";
        return switch (strategy) {
            case "suspense" -> "【留人策略：悬念式】设置悬念钩子，先预告不揭晓，让观众好奇留下看完";
            case "rhythm" -> "【留人策略：节奏式】通过语速变化、停顿、重复等节奏手法控制观众注意力";
            case "value_output" -> "【留人策略：价值式】持续输出干货价值，让观众觉得「看下去有收获」";
            default -> "";
        };
    }

    @Override
    public String buildInteractionLevelDesc(String level) {
        if (level == null || level.isBlank()) return "";
        return switch (level) {
            case "low" -> "【互动设计：低门槛】设计简单互动：打1/扣666/点赞到X万解锁福利";
            case "medium" -> "【互动设计：中门槛】设计选择式互动：选A还是选B/猜价格/评论区留言分享经历";
            case "high" -> "【互动设计：高门槛】设计深度互动：分享故事/晒使用效果/创意评论/发起挑战";
            default -> "";
        };
    }

    @Override
    public String buildHotKeywordsDesc(List<String> hotKeywords) {
        if (hotKeywords == null || hotKeywords.isEmpty()) return "";
        List<String> valid = hotKeywords.stream().filter(k -> k != null && !k.isBlank()).limit(10).toList();
        if (valid.isEmpty()) return "";
        return "当前热点话题（可适度融入以提升观众共鸣）：" + String.join("、", valid) + "。\n";
    }

    @Override
    public String buildOpeningTemplate(LiveSession session, String personaName, String extra) {
        StringBuilder sb = new StringBuilder();
        sb.append("大家好，欢迎来到").append(session.getLiveTitle()).append("直播间！");
        sb.append("我是").append(personaName).append("，今天给大家带来超多好物和福利。");
        sb.append("新进来的朋友先点个关注，不迷路！");
        sb.append("今天的直播会持续几个小时，全程都有惊喜价，千万不要走开哦~");
        if (extra != null && !extra.isBlank()) sb.append(" ").append(extra);
        return sb.toString();
    }

    @Override
    public String buildProductTemplate(Long productId, String extra) {
        DyProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "商品不存在"));
        StringBuilder sb = new StringBuilder();
        sb.append("接下来给大家介绍的是——").append(product.getProductName()).append("！");
        if (product.getDescription() != null) sb.append(product.getDescription()).append(" ");
        if (product.getPrice() != null) {
            sb.append("市场价").append(product.getPrice()).append("元，");
            sb.append("今天直播间专属价，直接给到最低！");
        }
        sb.append("库存有限，喜欢的宝子赶紧拍，拍完就没了！");
        if (extra != null && !extra.isBlank()) sb.append(" ").append(extra);
        return sb.toString();
    }

    @Override
    public String buildTransitionTemplate(String extra, LiveAiGenerateVO vo) {
        StringBuilder sb = new StringBuilder();
        if (vo.getFromProductName() != null && vo.getToProductName() != null) {
            sb.append("好的，").append(vo.getFromProductName()).append("大家都拍到了吗？");
            sb.append("没拍到的不要着急，我们马上给大家介绍").append(vo.getToProductName()).append("！");
        } else {
            sb.append("好的，刚才那款产品大家都拍到了吗？没拍到的不要着急，后面还有更多好物！");
        }
        sb.append("趁这个间隙，新来的朋友记得点个关注加个粉丝团，");
        sb.append("我们马上进入下一个环节~");
        if (extra != null && !extra.isBlank()) sb.append(" ").append(extra);
        return sb.toString();
    }

    @Override
    public String buildClosingTemplate(LiveSession session, String personaName, String extra) {
        StringBuilder sb = new StringBuilder();
        sb.append("好的家人们，今天的").append(session.getLiveTitle()).append("直播就到这里了！");
        sb.append("感谢每一位在直播间陪伴的朋友，你们的支持是我最大的动力。");
        sb.append("记得关注我们，下次直播不迷路！");
        sb.append("已经下单的宝子们放心，我们会尽快发货。有任何问题随时联系客服。");
        sb.append("我们下次直播再见，爱你们哦~");
        if (extra != null && !extra.isBlank()) sb.append(" ").append(extra);
        return sb.toString();
    }

    @Override
    public String buildChatTemplate(LiveSession session, String personaName, String requirement, String extra) {
        StringBuilder sb = new StringBuilder();
        sb.append("姐妹们，聊点家常~ ");
        if (requirement != null && !requirement.isBlank()) {
            sb.append("（话题：").append(requirement).append("）");
        } else {
            sb.append("（可按夫妻感情/婆媳关系/人生励志/歇后语/名言金句/古诗/幽默等展开）");
        }
        if (extra != null && !extra.isBlank()) sb.append(" ").append(extra);
        return sb.toString();
    }

    @Override
    public String buildChat2hTimeSlotHint(String timeSlot) {
        if (timeSlot == null || timeSlot.isBlank()) return "";
        String normalized = timeSlot.replace("min", "").trim();
        boolean isHighTraffic = "45-75".equals(normalized) || "75-105".equals(normalized);
        return isHighTraffic
                ? String.format("当前时段：第 %s 分钟（高流量转化窗口），人气蓄水后主推，可适当加强转化话术。", normalized)
                : String.format("当前时段：第 %s 分钟。", normalized);
    }
}
