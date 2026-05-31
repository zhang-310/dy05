package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the structured creative brief that keeps short-video planning, copy,
 * shots, materials, subtitles and final composition aligned.
 */
@Component
public class ShortVideoCreativePlanner {

    private static final ObjectMapper JSON = new ObjectMapper();

    public Map<String, Object> buildCreativeBrief(String theme, String keywords, String style, Integer durationSeconds) {
        String normalizedTheme = hasText(theme) ? theme.trim() : "短视频选题";
        String normalizedStyle = hasText(style) ? style.trim() : "专业干货";
        int duration = durationSeconds != null && durationSeconds > 0 ? Math.min(Math.max(durationSeconds, 15), 180) : 45;
        List<String> keywordList = splitKeywords(keywords);
        String primaryKeyword = keywordList.isEmpty() ? normalizedTheme : keywordList.get(0);
        String audience = inferAudience(normalizedTheme, keywordList);
        String promise = inferPromise(normalizedTheme, primaryKeyword);
        boolean commerceDigitalHuman = isCommerceDigitalHuman(normalizedTheme, keywordList, normalizedStyle);

        Map<String, Object> brief = new LinkedHashMap<>();
        brief.put("theme", normalizedTheme);
        brief.put("keywords", keywordList);
        brief.put("style", normalizedStyle);
        brief.put("durationSeconds", duration);
        brief.put("aspectRatio", "9:16");
        brief.put("targetAudience", audience);
        brief.put("corePromise", promise);
        brief.put("hookOptions", hookOptions(normalizedTheme, primaryKeyword, normalizedStyle));
        brief.put("commerceFormat", commerceDigitalHuman ? "digital_human_product_detail" : "standard_short_video");
        brief.put("storyBeats", commerceDigitalHuman
                ? commerceStoryBeats(duration, normalizedTheme, promise)
                : storyBeats(duration, normalizedTheme, promise));
        brief.put("shotStrategy", shotStrategy(normalizedStyle, duration, commerceDigitalHuman));
        brief.put("materialPlan", materialPlan(normalizedTheme, keywordList, commerceDigitalHuman));
        if (commerceDigitalHuman) {
            brief.put("digitalHumanPlan", digitalHumanPlan(duration));
            brief.put("productDetailPlan", productDetailPlan(normalizedTheme, keywordList));
            brief.put("bRollSequence", bRollSequence(duration));
            brief.put("conversionPlan", conversionPlan(normalizedTheme));
        }
        brief.put("audioPlan", audioPlan(normalizedStyle));
        brief.put("subtitlePlan", subtitlePlan());
        brief.put("publishPlan", publishPlan(normalizedTheme, keywordList));
        brief.put("riskChecklist", riskChecklist(normalizedTheme, keywordList));
        brief.put("acceptanceCriteria", acceptanceCriteria());
        return brief;
    }

    public String toPromptBlock(Map<String, Object> brief) {
        if (brief == null || brief.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("【短视频创作简报】\n");
        appendLine(sb, "主题", brief.get("theme"));
        appendLine(sb, "关键词", joinList(brief.get("keywords")));
        appendLine(sb, "风格", brief.get("style"));
        appendLine(sb, "目标时长", brief.get("durationSeconds") + "秒");
        appendLine(sb, "画幅", brief.get("aspectRatio"));
        appendLine(sb, "目标人群", brief.get("targetAudience"));
        appendLine(sb, "核心承诺", brief.get("corePromise"));
        appendLine(sb, "商业内容形态", brief.get("commerceFormat"));
        appendLine(sb, "开头钩子候选", joinList(brief.get("hookOptions")));
        appendLine(sb, "叙事节拍", joinMapList(brief.get("storyBeats"), "time", "goal"));
        appendLine(sb, "数字人口播计划", joinMapList(brief.get("digitalHumanPlan"), "type", "requirement"));
        appendLine(sb, "产品细节展示计划", joinMapList(brief.get("productDetailPlan"), "type", "requirement"));
        appendLine(sb, "B-roll 镜头序列", joinMapList(brief.get("bRollSequence"), "time", "goal"));
        appendLine(sb, "转化计划", joinMapList(brief.get("conversionPlan"), "type", "requirement"));
        appendLine(sb, "素材计划", joinMapList(brief.get("materialPlan"), "type", "requirement"));
        appendLine(sb, "风险清单", joinList(brief.get("riskChecklist")));
        return sb.toString();
    }

    public String toJson(Map<String, Object> value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private static List<String> splitKeywords(String keywords) {
        List<String> result = new ArrayList<>();
        if (!hasText(keywords)) {
            return result;
        }
        for (String raw : keywords.split("[,，、\\n]")) {
            String item = raw.trim();
            if (!item.isEmpty() && result.stream().noneMatch(it -> it.equalsIgnoreCase(item))) {
                result.add(item);
            }
            if (result.size() >= 8) {
                break;
            }
        }
        return result;
    }

    private static String inferAudience(String theme, List<String> keywords) {
        String text = (theme + " " + String.join(" ", keywords)).toLowerCase(Locale.ROOT);
        if (containsAny(text, "护肤", "美妆", "粉底", "面霜", "精华")) {
            return "对成分、安全性、效果证据和性价比敏感的美妆护肤用户";
        }
        if (containsAny(text, "直播", "带货", "运营", "投流")) {
            return "需要提升内容效率和转化率的运营/主播/商家";
        }
        if (containsAny(text, "职场", "效率", "工具", "ai")) {
            return "关注效率提升、可执行方法和工具清单的职场用户";
        }
        return "对该主题已有兴趣、但需要快速判断价值和行动路径的垂直用户";
    }

    private static String inferPromise(String theme, String primaryKeyword) {
        if (containsAny(theme, "测评", "开箱", "对比")) {
            return "用清晰证据帮助观众判断「值不值得买/做/学」";
        }
        if (containsAny(theme, "教程", "方法", "入门")) {
            return "让观众在一分钟内拿到可照做的步骤";
        }
        return "把" + primaryKeyword + "拆成观众能马上理解和行动的结论";
    }

    private static List<String> hookOptions(String theme, String keyword, String style) {
        List<String> hooks = new ArrayList<>();
        hooks.add("如果你正在纠结" + keyword + "，先别急着下单，先看这 3 个判断点。");
        hooks.add("我用一个普通用户能听懂的方式，把" + theme + "讲清楚。");
        hooks.add("很多人做错不是因为不努力，而是第一步就选错了。");
        if (containsAny(style, "搞笑", "幽默")) {
            hooks.add("这东西看起来平平无奇，但坑点比评论区还热闹。");
        }
        return hooks;
    }

    private static List<Map<String, Object>> storyBeats(int duration, String theme, String promise) {
        int hookEnd = Math.min(3, Math.max(2, duration / 12));
        int setupEnd = Math.min(duration - 12, Math.max(hookEnd + 6, duration / 4));
        int proofEnd = Math.min(duration - 6, Math.max(setupEnd + 10, duration * 3 / 4));
        return List.of(
                beat("0-" + hookEnd + "s", "钩子", "用反差/痛点留住人，明确这条视频解决什么问题"),
                beat(hookEnd + "-" + setupEnd + "s", "问题定义", "说明观众常见误区，并承接到主题：" + theme),
                beat(setupEnd + "-" + proofEnd + "s", "证据/步骤", promise + "，给出 2-3 个证据或操作步骤"),
                beat(proofEnd + "-" + duration + "s", "结论/行动", "总结判断标准，引导收藏、评论或进入下一步")
        );
    }

    private static List<Map<String, Object>> commerceStoryBeats(int duration, String theme, String promise) {
        int hookEnd = Math.min(3, Math.max(2, duration / 10));
        int productIntroEnd = Math.min(duration - 9, Math.max(hookEnd + 2, duration * 2 / 10));
        int detailEnd = Math.min(duration - 6, Math.max(productIntroEnd + 3, duration * 4 / 10));
        int demoEnd = Math.min(duration - 3, Math.max(detailEnd + 3, duration * 6 / 10));
        int decisionEnd = Math.min(duration - 1, Math.max(demoEnd + 2, duration * 8 / 10));
        return List.of(
                beat("0-" + hookEnd + "s", "数字人钩子", "正脸口播点出痛点/反差，第一句话必须让目标人群自我识别"),
                beat(hookEnd + "-" + productIntroEnd + "s", "产品入镜", "产品和使用场景同时出现，说明它解决的具体问题：" + theme),
                beat(productIntroEnd + "-" + detailEnd + "s", "细节证明", "用材质、成分、结构、做工、参数或包装信息支撑卖点，避免空口夸"),
                beat(detailEnd + "-" + demoEnd + "s", "使用演示/对比", promise + "，必须让观众看到使用动作或前后差异"),
                beat(demoEnd + "-" + decisionEnd + "s", "数字人总结", "回到数字人正脸，总结适合人群、不适合人群和选择理由"),
                beat(decisionEnd + "-" + duration + "s", "行动引导", "给出轻量 CTA，并提醒按自身需求判断，不做绝对效果承诺")
        );
    }

    private static Map<String, Object> beat(String time, String label, String goal) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("time", time);
        item.put("label", label);
        item.put("goal", goal);
        return item;
    }

    private static List<Map<String, Object>> shotStrategy(String style, int duration, boolean commerceDigitalHuman) {
        String rhythm = duration <= 45 ? "2-4 秒一切，保留强字幕和近景表情" : "4-6 秒一切，增加步骤图和对比镜头";
        List<Map<String, Object>> result = new ArrayList<>();
        result.addAll(List.of(
                planItem("camera", "前三秒近景/手持推进，中段用俯拍或屏幕录制解释，结尾回到人物正面"),
                planItem("rhythm", rhythm),
                planItem("visual", containsAny(style, "高端", "专业") ? "干净背景、少色块、重视证据截图和参数卡" : "更强表情、节奏点和转场")
        ));
        if (commerceDigitalHuman) {
            result.add(planItem("role_mix", "数字人正脸口播约 35%-45%，产品细节/使用演示/证据 B-roll 约 55%-65%，避免整条视频只有头像念稿"));
            result.add(planItem("shot_roles", "每条分镜标注 avatar_talking_head、product_closeup、usage_demo、proof_overlay、cta 中的一类"));
        }
        return result;
    }

    private static List<Map<String, Object>> materialPlan(String theme, List<String> keywords, boolean commerceDigitalHuman) {
        String keywordText = keywords.isEmpty() ? theme : String.join("、", keywords);
        List<Map<String, Object>> result = new ArrayList<>();
        result.addAll(List.of(
                planItem("reference", "收集 3-5 条同主题爆款，记录钩子、节奏、评论区争议点"),
                planItem("b-roll", "准备与 " + keywordText + " 相关的产品/场景/操作过程素材"),
                planItem("proof", "准备截图、数据、报告、前后对比或用户反馈，但避免夸大承诺"),
                planItem("cover", "封面保留主题关键词 + 一个明确结果词")
        ));
        if (commerceDigitalHuman) {
            result.add(planItem("avatar", "准备数字人形象、口播音色、竖屏干净背景和 2-3 个表情/手势版本"));
            result.add(planItem("product_closeup", "补齐包装正反面、质地/材质、关键结构、使用前后、手持比例尺、场景摆拍"));
            result.add(planItem("edit_assets", "准备卖点字幕条、参数卡、禁用词替代表述和 CTA 角标，便于批量复用"));
        }
        return result;
    }

    private static List<Map<String, Object>> digitalHumanPlan(int duration) {
        return List.of(
                planItem("opening_avatar", "0-3 秒数字人正脸近景，直接说痛点或反差，不铺垫"),
                planItem("bridge_avatar", "每 8-12 秒回到数字人 1 次，用一句话承接产品细节和演示镜头"),
                planItem("closing_avatar", "最后 4-6 秒数字人总结适合人群、购买理由和轻量行动引导"),
                planItem("script_length", "按 " + duration + " 秒控制口播总字数，中文约每秒 3.5-4.5 字，保留 B-roll 空拍时间")
        );
    }

    private static List<Map<String, Object>> productDetailPlan(String theme, List<String> keywords) {
        String keywordText = keywords.isEmpty() ? theme : String.join("、", keywords);
        return List.of(
                planItem("hero_product", "产品首屏必须清楚露出名称/外观/核心使用场景"),
                planItem("detail_closeup", "至少 3 个产品细节特写，围绕 " + keywordText + " 展示材质、成分、结构、参数、质地或做工"),
                planItem("usage_demo", "至少 1 段真实使用动作，交代手法、用量、步骤或搭配场景"),
                planItem("proof_overlay", "证据用参数卡/报告截图/用户反馈摘要呈现，不把体验描述成绝对结果")
        );
    }

    private static List<Map<String, Object>> bRollSequence(int duration) {
        int hookEnd = Math.min(3, Math.max(2, duration / 10));
        int midA = Math.min(duration - 9, Math.max(hookEnd + 2, duration / 4));
        int midB = Math.min(duration - 5, Math.max(midA + 3, duration / 2));
        int end = Math.min(duration - 1, Math.max(midB + 3, duration - 6));
        return List.of(
                beat("0-" + hookEnd + "s", "avatar_talking_head", "数字人正脸强钩子，字幕同步强调痛点"),
                beat(hookEnd + "-" + midA + "s", "product_closeup", "产品入镜 + 包装/外观/核心细节特写"),
                beat(midA + "-" + midB + "s", "usage_demo", "手部演示、上身/上脸/开箱/对比动作，画面必须证明口播"),
                beat(midB + "-" + end + "s", "proof_overlay", "参数卡、报告、评论区问题或对比图覆盖在产品 B-roll 上"),
                beat(end + "-" + duration + "s", "cta", "数字人回到正脸，总结适合人群并做合规 CTA")
        );
    }

    private static List<Map<String, Object>> conversionPlan(String theme) {
        return List.of(
                planItem("pain_point", "围绕「" + theme + "」只打一个主痛点，避免一条视频塞太多卖点"),
                planItem("decision_filter", "明确适合谁、不适合谁，降低硬广感并提升信任"),
                planItem("cta", "行动引导使用「想看清单/对比/避坑点可以评论」等轻量表达，不诱导夸大承诺"),
                planItem("compliance", "价格、功效、数据和限时利益点必须能被商品信息或官方规则支撑")
        );
    }

    private static List<Map<String, Object>> audioPlan(String style) {
        return List.of(
                planItem("voice", containsAny(style, "专业", "高端") ? "中速清晰口播，减少夸张语气" : "口语化、短句、重读关键词"),
                planItem("bgm", containsAny(style, "温馨") ? "轻快低音量，不压过口播" : "节奏感 BGM，音量控制在口播下方"),
                planItem("sfx", "只在转场、对比揭示、结论处使用短音效")
        );
    }

    private static List<Map<String, Object>> subtitlePlan() {
        return List.of(
                planItem("style", "每屏 8-14 字，关键词加粗/高亮，避免遮挡主体"),
                planItem("timing", "按口播语义切分，不逐字刷屏"),
                planItem("review", "发布前检查错别字、敏感承诺、禁用词")
        );
    }

    private static List<Map<String, Object>> publishPlan(String theme, List<String> keywords) {
        List<String> tags = new ArrayList<>();
        tags.add(theme);
        tags.addAll(keywords);
        return List.of(
                planItem("title", "标题包含主题词 + 明确收益/避坑点"),
                planItem("tags", String.join("、", tags.stream().filter(StringUtils::hasText).limit(6).toList())),
                planItem("interaction", "结尾用一个二选一问题引导评论，而不是泛泛求关注")
        );
    }

    private static List<String> riskChecklist(String theme, List<String> keywords) {
        List<String> risks = new ArrayList<>();
        String text = theme + " " + String.join(" ", keywords);
        risks.add("不要承诺绝对效果，如「立刻见效」「百分百」「永久」");
        risks.add("引用数据/报告时保留来源或用「样本/体验」表述");
        if (containsAny(text, "护肤", "美妆", "祛斑", "美白")) {
            risks.add("美妆护肤内容避免医疗化表达，功效词需要与产品资质一致");
        }
        if (containsAny(text, "赚钱", "暴富", "副业")) {
            risks.add("收益类内容避免保底收益和诱导性承诺");
        }
        return risks;
    }

    private static List<String> acceptanceCriteria() {
        return List.of(
                "前三秒能明确痛点或反差",
                "每个分镜都有画面、动作、口播或字幕任务",
                "素材清单覆盖主体、证据、过渡和封面",
                "带货视频必须包含数字人口播、产品细节特写、使用演示、证据覆盖和合规 CTA",
                "发布标题、标签和评论引导已准备",
                "敏感词和夸大承诺已检查"
        );
    }

    private static Map<String, Object> planItem(String type, String requirement) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", type);
        item.put("requirement", requirement);
        return item;
    }

    private static void appendLine(StringBuilder sb, String label, Object value) {
        String text = value == null ? "" : value.toString();
        if (StringUtils.hasText(text)) {
            sb.append("- ").append(label).append("：").append(text.trim()).append("\n");
        }
    }

    private static String joinList(Object raw) {
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).filter(StringUtils::hasText).reduce((a, b) -> a + "；" + b).orElse("");
        }
        return raw == null ? "" : raw.toString();
    }

    private static String joinMapList(Object raw, String keyA, String keyB) {
        if (!(raw instanceof List<?> list)) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Object a = map.get(keyA);
                Object b = map.get(keyB);
                if (a != null || b != null) {
                    parts.add(String.valueOf(a == null ? "" : a) + "：" + String.valueOf(b == null ? "" : b));
                }
            }
        }
        return String.join("；", parts);
    }

    private static boolean hasText(String text) {
        return StringUtils.hasText(text);
    }

    private static boolean isCommerceDigitalHuman(String theme, List<String> keywords, String style) {
        String text = (String.valueOf(theme) + " " + String.join(" ", keywords) + " " + String.valueOf(style))
                .toLowerCase(Locale.ROOT);
        return containsAny(text,
                "数字人", "avatar", "digital", "talking head", "口播带货", "带货口播",
                "数字人口播", "产品细节展示", "digital_human_product_detail")
                || (containsAny(text, "带货", "软广", "commerce", "ecommerce")
                && containsAny(text, "口播", "商品", "产品", "product"));
    }

    private static boolean containsAny(String text, String... words) {
        if (text == null) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        for (String word : words) {
            if (word != null && normalized.contains(word.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
