package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.douyin.entity.DyPersona;
import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.vo.EmotionalScriptGenerateVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiGenerateVO;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 直播话术 Prompt 构建器，统一人设/产品/风格/时长注入。
 * 与 #6 Prompt 构建统一化 合并实现。
 */
@Component
public class LivePromptBuilder {

    @Resource
    private DyProductRepository productRepository;
    @Resource
    private LiveProductRepository liveProductRepository;
    @Resource
    private LiveSessionRepository sessionRepository;

    /** 全局话术生成系统提示 */
    public static final String SYSTEM_PROMPT = """
            你是资深直播话术专家，输出「即用型」话术供主播微调。
            核心原则：
            1. 口语化：像真人说话，有停顿感、语气词，避免书面腔
            2. 有感染力：情绪饱满、节奏感强，能拉停留、促互动
            3. 引导动作：明确引导关注/点赞/下单，每段至少 1 个动作指令
            4. 合规：严禁违禁词、绝对化用语、虚假宣传
            5. 输出格式：只输出话术正文，无标题、无解释、无序号，可直接粘贴使用
            """;

    /**
     * 获取系统提示词（支持按 Prompt 模板 ID 查找）
     * @param promptTemplateId 可为 null，null 时返回默认系统提示
     * @param scriptType 话术类型
     */
    public String getSystemPrompt(Long promptTemplateId, String scriptType) {
        return SYSTEM_PROMPT;
    }

    public String buildPrompt(String scriptType, LiveSession session, DyPersona persona, LiveAiGenerateVO vo) {
        String personaDesc = persona != null
                ? String.format("主播人设：%s，语气：%s", persona.getPersonaName(), persona.getTone() != null ? persona.getTone() : "亲切")
                : "主播风格：专业亲切";
        String styleDesc = expandStyleForPrompt(resolveStyle(vo));
        String extraDesc = vo.getExtraPrompt() != null ? "额外要求：" + vo.getExtraPrompt() : "";
        String reqDesc = vo.getRequirement() != null && !vo.getRequirement().isBlank()
                ? "本段需求/意图：" + vo.getRequirement() : "";
        String durationDesc = vo.getDurationLimitSec() != null && vo.getDurationLimitSec() > 0
                ? String.format("时长限制：%d秒以内（约%d字）", vo.getDurationLimitSec(), vo.getDurationLimitSec() * 3)
                : "";

        return switch (scriptType) {
            case "opening" -> String.format("""
                    生成开场话术，直播主题：%s
                    %s
                    %s
                    %s
                    %s
                    %s
                    要点：① 热情欢迎+点明主题 ② 引导关注/加粉丝团（拉停留） ③ 预告福利/惊喜价 ④ 口语化，有感染力
                    """, session.getLiveTitle() != null ? session.getLiveTitle() : "直播", personaDesc, styleDesc, reqDesc, durationDesc, extraDesc)
                    .replaceAll("\\n{3,}", "\n\n").trim();

            case "product" -> {
                DyProduct product = productRepository.findById(vo.getProductId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "商品不存在"));
                String productTypeSection = (vo.getProductType() != null && !vo.getProductType().isBlank())
                        ? buildDurationHintForProductType(vo.getProductType())
                        : (vo.getProductId() != null ? getProductTypeHintForPrompt(vo.getProductId(), session.getId()) : "");
                String durationForProduct = (vo.getProductType() != null && !vo.getProductType().isBlank())
                        ? "" : durationDesc;
                yield String.format("""
                        生成产品话术，商品：%s
                        描述：%s | 价格：%s元
                        %s
                        %s
                        %s
                        %s
                        %s
                        %s
                        要点：① 突出核心卖点 ② 制造稀缺/紧迫感 ③ 明确引导下单 ④ 口语化，避免与前后产品话术雷同
                        """, product.getProductName(),
                        product.getDescription() != null ? product.getDescription() : "暂无",
                        product.getPrice() != null ? product.getPrice().toString() : "待定",
                        productTypeSection, personaDesc, styleDesc, reqDesc, durationForProduct, extraDesc)
                        .replaceAll("\\n{3,}", "\n\n").trim();
            }

            case "transition" -> {
                String fromTo = "";
                if (vo.getFromProductName() != null && vo.getToProductName() != null) {
                    fromTo = String.format("从「%s」过渡到「%s」。", vo.getFromProductName(), vo.getToProductName());
                }
                yield String.format("""
                    生成转场话术。%s
                    %s
                    %s
                    %s
                    %s
                    %s
                    要点：① 承上启下，自然衔接 ② 保持观众注意力 ③ 可引导互动（点赞/评论） ④ 简短有力，不拖沓
                    """, fromTo, personaDesc, styleDesc, reqDesc, durationDesc, extraDesc)
                        .replaceAll("\\n{3,}", "\n\n").trim();
            }

            case "closing" -> String.format("""
                    生成收尾话术，直播主题：%s
                    %s
                    %s
                    %s
                    %s
                    %s
                    要点：① 真诚感谢 ② 引导关注/分享 ③ 预告下次直播 ④ 已下单用户安抚（发货/售后）
                    """, session.getLiveTitle() != null ? session.getLiveTitle() : "直播", personaDesc, styleDesc, reqDesc, durationDesc, extraDesc)
                    .replaceAll("\\n{3,}", "\n\n").trim();

            default -> throw new BusinessException(ErrorCode.VALIDATION_FAIL, "不支持的话术类型: " + scriptType);
        };
    }

    public String buildScriptTemplate(String scriptType, LiveSession session, DyPersona persona, LiveAiGenerateVO vo) {
        String personaName = persona != null ? persona.getPersonaName() : "主播";
        String extra = vo.getExtraPrompt();
        return switch (scriptType) {
            case "opening" -> buildOpeningTemplate(session, personaName, extra);
            case "product" -> buildProductTemplate(vo.getProductId(), extra);
            case "transition" -> buildTransitionTemplate(extra, vo);
            case "closing" -> buildClosingTemplate(session, personaName, extra);
            default -> throw new BusinessException(ErrorCode.VALIDATION_FAIL, "不支持的话术类型: " + scriptType);
        };
    }

    public String expandStyleForPrompt(String style) {
        if (style == null || style.isBlank()) return "";
        return switch (style.toLowerCase()) {
            case "professional" -> "话术风格：专业、有条理、可信度高";
            case "friendly" -> "话术风格：亲切、像朋友聊天、拉近距离";
            case "passionate" -> "话术风格：热情、有感染力、情绪饱满";
            case "seeding" -> "话术风格：种草向、突出使用场景和体验感";
            case "promotion" -> "话术风格：促销向、限时抢购、库存紧张、制造紧迫";
            default -> "话术风格：" + style;
        };
    }

    public String buildProductScriptPrompt(DyProduct product, DyPersona persona,
            cn.gaifan.douyinOperations.module.live.vo.ProductScriptGenerateVO vo) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("产品信息：\n");
        prompt.append("- 名称：").append(product.getProductName()).append("\n");
        prompt.append("- 价格：").append(product.getPrice()).append(" 元\n");
        if (product.getDescription() != null && !product.getDescription().isBlank()) {
            prompt.append("- 描述：").append(product.getDescription()).append("\n");
        }
        if (product.getTags() != null && !product.getTags().isBlank()) {
            prompt.append("- 标签：").append(product.getTags()).append("\n");
        }
        if (product.getInventory() != null && product.getInventory() > 0) {
            prompt.append("- 库存：").append(product.getInventory()).append(" 件\n");
        }
        prompt.append("\n");
        if (persona != null) {
            prompt.append("主播人设：\n");
            prompt.append("- 名称：").append(persona.getPersonaName()).append("\n");
            prompt.append("- 类型：").append(persona.getPersonaType()).append("\n");
            prompt.append("- 语气：").append(persona.getTone()).append("\n");
            if (persona.getDescription() != null && !persona.getDescription().isBlank()) {
                prompt.append("- 描述：").append(persona.getDescription()).append("\n");
            }
            prompt.append("\n");
        }
        String typeDesc = switch (vo.getScriptType()) {
            case "seed" -> "种草话术（强调体验和感受）";
            case "promotion" -> "促销话术（强调优惠和限时）";
            case "formal" -> "正式话术（专业介绍）";
            default -> "产品介绍话术";
        };
        prompt.append("话术类型：").append(typeDesc).append("\n");
        if (vo.getStyle() != null && !vo.getStyle().isBlank()) {
            String styleDesc = switch (vo.getStyle()) {
                case "enthusiastic" -> "热情洋溢";
                case "professional" -> "专业严谨";
                case "casual" -> "轻松随意";
                case "warm" -> "温暖亲切";
                case "friendly" -> "亲切友好";
                case "passionate" -> "热情有感染力";
                case "seeding" -> "种草推荐";
                case "promotion" -> "促销紧迫";
                default -> vo.getStyle();
            };
            prompt.append("话术风格：").append(styleDesc).append("\n");
        }
        int duration = vo.getDuration() != null ? vo.getDuration() : 60;
        int wordCount = duration * 3;
        prompt.append("话术长度：约 ").append(wordCount).append(" 字（").append(duration).append(" 秒）\n\n");

        // 场景说明
        if (vo.getScene() != null && !vo.getScene().isBlank()) {
            String sceneDesc = switch (vo.getScene()) {
                case "short_video" -> "短视频带货场景：15-60秒快节奏，强调视觉冲击和核心卖点，开头3秒抓眼球";
                case "guopin" -> "过品带货场景：快速介绍多个产品，突出核心亮点，适合多品轮播";
                case "cangbo" -> "仓播带货场景：强调库存充足、优惠力度大、限时抢购，营造紧迫感";
                case "danpin" -> "单品直播间场景：深度讲解单一产品，反复强调卖点，适合高客单价商品";
                case "yubo" -> "娱播穿插场景：轻松自然融入娱乐内容，不生硬推销，像朋友推荐";
                default -> "应用场景：" + vo.getScene();
            };
            prompt.append("应用场景：").append(sceneDesc).append("\n\n");
        }

        if (vo.getExtraPrompt() != null && !vo.getExtraPrompt().isBlank()) {
            prompt.append("额外要求：").append(vo.getExtraPrompt()).append("\n\n");
        }
        prompt.append("请生成符合以上要求的产品话术。");
        return prompt.toString();
    }

    public String buildProductScriptSystemPrompt(String scriptType) {
        return switch (scriptType) {
            case "seed" -> """
                    你是一位专业的种草话术撰写专家，擅长通过真实体验和感受打动用户。
                    要求：1. 语言自然真诚 2. 强调使用体验 3. 用具体场景增强说服力 4. 避免过度夸张 5. 直接输出话术，无标题无解释
                    """;
            case "promotion" -> """
                    你是一位专业的促销话术撰写专家，擅长营造紧迫感和优惠吸引力。
                    要求：1. 语言有感染力 2. 突出优惠和限时 3. 强调性价比 4. 引导立即下单 5. 直接输出话术，无标题无解释
                    """;
            case "formal" -> """
                    你是一位专业的产品介绍专家，擅长用专业语言介绍产品。
                    要求：1. 语言专业严谨 2. 详细介绍功能和特点 3. 提供客观信息 4. 建立专业形象 5. 直接输出话术，无标题无解释
                    """;
            default -> SYSTEM_PROMPT;
        };
    }

    public String buildEmotionalSystemPrompt() {
        return """
            你是一位女性主播，在护肤品直播间。输出「即用型」情绪价值话术。
            核心原则：1. 口语化 2. 共鸣 3. 正向引导 4. 合规 5. 只输出话术正文
            """;
    }

    public String buildEmotionalUserPrompt(EmotionalScriptGenerateVO vo, LiveSession session) {
        String category = vo.getCategory();
        String subCategory = vo.getSubCategory();
        String theme = session.getLiveTitle() != null ? session.getLiveTitle() : "直播";
        int wordCountMin, wordCountMax;
        String categoryDesc, subDesc;
        switch (category) {
            case "quote" -> {
                wordCountMin = 50;
                wordCountMax = 100;
                categoryDesc = "名言金句（励志、人生哲理、女性力量）";
                subDesc = subCategory != null ? "子类别：" + subCategory : "可选：励志奋斗、人生哲理、女性力量";
            }
            case "proverb" -> {
                wordCountMin = 30;
                wordCountMax = 80;
                categoryDesc = "歇后语/俗语（接地气、幽默）";
                subDesc = subCategory != null ? "子类别：" + subCategory : "可选：生活智慧、幽默调侃";
            }
            case "emotional_healing" -> {
                wordCountMin = 80;
                wordCountMax = 150;
                categoryDesc = "心理鸡汤（疗愈、共鸣）";
                subDesc = subCategory != null ? "子类别：" + subCategory : "可选：自我疗愈、成长蜕变、价值认同";
            }
            case "female_perspective" -> {
                wordCountMin = 100;
                wordCountMax = 200;
                categoryDesc = "女性视角话题";
                subDesc = subCategory != null ? "子类别：" + subCategory : "可选：婆媳关系、育儿压力、家务分配等";
            }
            default -> {
                wordCountMin = 80;
                wordCountMax = 150;
                categoryDesc = category;
                subDesc = subCategory != null ? subCategory : "";
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("【话术类型】").append(categoryDesc).append("\n");
        if (!subDesc.isBlank()) sb.append("【子类别】").append(subDesc).append("\n");
        sb.append("【直播主题】").append(theme).append("\n\n");
        sb.append("【话术目的】引发共鸣、建立人设、自然过渡到产品\n");
        sb.append("【目标受众】25-45岁女性\n");
        sb.append("【字数】").append(wordCountMin).append("-").append(wordCountMax).append("字\n");
        sb.append("请生成话术：");
        return sb.toString();
    }

    public String resolveStyle(LiveAiGenerateVO vo) {
        if (vo.getStyle() != null && !vo.getStyle().isBlank()) return vo.getStyle();
        LiveSession session = sessionRepository.findById(vo.getSessionId()).orElse(null);
        return session != null && session.getScriptStyle() != null && !session.getScriptStyle().isBlank()
                ? session.getScriptStyle() : null;
    }

    private String getProductTypeHintForPrompt(Long productId, Long sessionId) {
        if (productId == null || sessionId == null) return "";
        return liveProductRepository.findBySessionIdAndProductId(sessionId, productId)
                .map(LiveProduct::getProductType)
                .filter(t -> t != null && !t.isBlank())
                .map(this::getProductTypePromptHint)
                .filter(h -> !h.isBlank())
                .map(h -> "产品类型及话术侧重：" + h)
                .orElse("");
    }

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
                    default -> t;
                })
                .reduce((a, b) -> a + "、" + b)
                .orElse(null);
    }

    public String getProductTypePromptHint(String productType) {
        if (productType == null || productType.isBlank()) return "";
        Set<String> types = new HashSet<>(Arrays.asList(productType.split(",")));
        List<String> hints = new java.util.ArrayList<>();
        if (types.contains("hot")) hints.add("爆品强调限时抢购、库存紧张、深度讲解2-5分钟");
        if (types.contains("profit")) hints.add("利润品强调品质、价值感");
        if (types.contains("loss")) hints.add("亏品强调引流、福利回馈");
        if (types.contains("flat")) hints.add("平价品强调性价比");
        if (types.contains("control")) hints.add("控单产品：吊胃口、控单规则、分批放单、催促抢购");
        return String.join("；", hints);
    }

    public String buildDurationHintForProductType(String productType) {
        if (productType == null || productType.isBlank()) return "";
        Set<String> types = new HashSet<>(Arrays.asList(productType.split(",")));
        int wordCountMin, wordCountMax;
        String durationHint;
        if (types.contains("hot")) {
            wordCountMin = 900;
            wordCountMax = 1500;
            durationHint = "请生成深度讲解话术，分为 3-5 段：第1段产品介绍(60秒) 第2段成分功效(90秒) 第3段使用体验(60秒) 第4段对比优势(60秒) 第5段限时促销(30秒)";
        } else if (types.contains("control")) {
            wordCountMin = 600;
            wordCountMax = 1200;
            durationHint = "请生成控单话术，包含：吊胃口（30秒）、产品讲解（40秒）、控单规则（20秒）、分批放单引导";
        } else if (types.contains("profit")) {
            wordCountMin = 450;
            wordCountMax = 600;
            durationHint = "请生成重点推介话术，强调品质、成分、效果，60-90秒";
        } else if (types.contains("loss")) {
            wordCountMin = 200;
            wordCountMax = 300;
            durationHint = "请生成快速引流话术，直接报价、算账、强调福利，30-60秒";
        } else {
            wordCountMin = 300;
            wordCountMax = 450;
            durationHint = "请生成性价比导向话术，朴实真诚，45-75秒";
        }
        return String.format("产品分类：%s\n话术时长要求：%s\n字数范围：%d-%d 字\n",
                formatProductTypeLabel(productType), durationHint, wordCountMin, wordCountMax);
    }

    private String buildOpeningTemplate(LiveSession session, String personaName, String extra) {
        StringBuilder sb = new StringBuilder();
        sb.append("大家好，欢迎来到").append(session.getLiveTitle()).append("直播间！");
        sb.append("我是").append(personaName).append("，今天给大家带来超多好物和福利。");
        sb.append("新进来的朋友先点个关注，不迷路！");
        sb.append("今天的直播会持续几个小时，全程都有惊喜价，千万不要走开哦~");
        if (extra != null && !extra.isBlank()) sb.append(" ").append(extra);
        return sb.toString();
    }

    private String buildProductTemplate(Long productId, String extra) {
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

    private String buildTransitionTemplate(String extra, LiveAiGenerateVO vo) {
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

    private String buildClosingTemplate(LiveSession session, String personaName, String extra) {
        StringBuilder sb = new StringBuilder();
        sb.append("好的家人们，今天的").append(session.getLiveTitle()).append("直播就到这里了！");
        sb.append("感谢每一位在直播间陪伴的朋友，你们的支持是我最大的动力。");
        sb.append("记得关注我们，下次直播不迷路！");
        sb.append("已经下单的宝子们放心，我们会尽快发货。有任何问题随时联系客服。");
        sb.append("我们下次直播再见，爱你们哦~");
        if (extra != null && !extra.isBlank()) sb.append(" ").append(extra);
        return sb.toString();
    }
}
