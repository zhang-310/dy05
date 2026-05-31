package cn.gaifan.douyinOperations.module.agent.skill.impl;

import cn.gaifan.douyinOperations.module.agent.skill.Skill;
import cn.gaifan.douyinOperations.module.ai.service.RagService;
import cn.gaifan.douyinOperations.module.ai.vo.RagRetrieveItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 话术生成技能
 */
@Component
public class ScriptGenerateSkill implements Skill {

    private static final Logger log = LoggerFactory.getLogger(ScriptGenerateSkill.class);

    @Autowired(required = false)
    private RagService ragService;

    @Override
    public String getName() {
        return "script_generate";
    }

    @Override
    public String getDescription() {
        return "生成直播/短视频话术；生成前强制检索 douyin 与 douyin_weigui，并在末尾输出官方规则引用";
    }

    @Override
    public boolean matches(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase();
        return lower.contains("话术") || lower.contains("脚本") || lower.contains("文案")
                || lower.contains("开场白") || lower.contains("介绍");
    }

    @Override
    public String execute(SkillContext ctx) {
        try {
            // 从参数中获取生成条件
            String scriptType = (String) ctx.params().getOrDefault("type", "product_intro");
            String productName = (String) ctx.params().get("productName");
            String style = (String) ctx.params().getOrDefault("style", "专业");
            String scene = (String) ctx.params().getOrDefault("scene", "直播间");
            String context = (String) ctx.params().getOrDefault("context", "");
            String query = buildReferenceQuery(scriptType, productName, scene, context);

            log.info("[ScriptGenerate] 执行生成: type={}, product={}, style={}, query={}", scriptType, productName, style, query);

            List<RagRetrieveItemVO> officialRefs = retrieveOfficialRefs(ctx.userId(), query);
            if (!hasRequiredOfficialRefs(officialRefs)) {
                return """
                        ⛔ 话术生成已阻断：未检索到 douyin + douyin_weigui 官方规则引用。

                        当前链路不能输出可直接使用的话术，避免生成无官方依据的违规内容。
                        请先修复 kb_rag_search 语义索引、Embedding、ES/Milvus 或知识库范围，再重试。

                        必需引用：
                        - douyin：抖音官方学习中心/运营规则
                        - douyin_weigui：直播、短视频、千川素材违规规则
                        """;
            }

            // 使用模板生成
            return generateFromTemplate(scriptType, productName, style, scene, officialRefs);

        } catch (Exception e) {
            log.error("[ScriptGenerate] 执行失败", e);
            return "话术生成失败: " + e.getMessage();
        }
    }

    /**
     * 从模板生成话术
     */
    private String generateFromTemplate(String type, String productName, String style, String scene, List<RagRetrieveItemVO> officialRefs) {
        StringBuilder sb = new StringBuilder();
        sb.append("✅ 话术生成成功：\n\n");

        String product = productName != null ? productName : "我们的产品";

        switch (type) {
            case "opening" -> {
                sb.append("【开场白】\n\n");
                sb.append("各位宝宝们，大家好！欢迎来到我们的").append(scene).append("！\n");
                sb.append("今天给大家带来的是").append(product).append("，\n");
                sb.append("这款产品真的是我们精心挑选的，品质有保证！\n");
                sb.append("现在下单还有超值优惠，千万不要错过哦！\n");
            }
            case "product_intro" -> {
                sb.append("【产品介绍】\n\n");
                sb.append("来，宝宝们看一下这款").append(product).append("，\n");
                sb.append("它的核心卖点是：\n");
                sb.append("1. 品质优良，原料精选\n");
                sb.append("2. 效果显著，用户好评如潮\n");
                sb.append("3. 性价比超高，物超所值\n\n");
                sb.append("这款产品特别适合").append(scene).append("使用，\n");
                sb.append("现在下单立享优惠，数量有限，先到先得！\n");
            }
            case "promotion" -> {
                sb.append("【促销话术】\n\n");
                sb.append("宝宝们注意了！现在是我们的限时优惠时间！\n");
                sb.append(product).append("原价XXX元，\n");
                sb.append("现在直播间专享价只要XXX元！\n");
                sb.append("而且前100名下单的宝宝还有额外赠品！\n");
                sb.append("机会难得，赶紧点击下方小黄车下单吧！\n");
            }
            case "closing" -> {
                sb.append("【结束语】\n\n");
                sb.append("好的宝宝们，今天的直播就到这里了，\n");
                sb.append("感谢大家的支持和陪伴！\n");
                sb.append("已经下单的宝宝们，我们会尽快安排发货，\n");
                sb.append("还没下单的宝宝也不要着急，关注我们的账号，\n");
                sb.append("下次直播继续给大家带来更多好物！\n");
                sb.append("我们下次见，拜拜！\n");
            }
            default -> {
                sb.append("【通用话术】\n\n");
                sb.append("宝宝们，").append(product).append("真的非常不错，\n");
                sb.append("我们团队亲自试用过，效果确实很好！\n");
                sb.append("现在下单还有优惠，大家可以放心购买！\n");
            }
        }

        sb.append("\n💡 使用建议：\n");
        sb.append("• 根据实际情况调整话术内容\n");
        sb.append("• 注意语气要").append(style).append("、自然\n");
        sb.append("• 避免使用极限词和虚假宣传\n");
        sb.append("• 结合产品特点突出卖点\n");
        sb.append("\n📚 引用来源（官方规则，必须保留）：\n");
        for (int i = 0; i < officialRefs.size(); i++) {
            RagRetrieveItemVO ref = officialRefs.get(i);
            sb.append(i + 1).append(". ");
            sb.append(ref.getKbName()).append(" · docId=").append(ref.getDocId() != null ? ref.getDocId() : "未知");
            if (ref.getTitle() != null && !ref.getTitle().isBlank()) {
                sb.append(" · ").append(ref.getTitle());
            }
            if (ref.getSource() != null && !ref.getSource().isBlank()) {
                sb.append(" · source=").append(ref.getSource());
            }
            sb.append("\n");
        }
        sb.append("\n合规门禁：以上引用缺失时，本话术不得直接发布或进入直播使用。\n");

        return sb.toString();
    }

    private List<RagRetrieveItemVO> retrieveOfficialRefs(Long userId, String query) {
        if (ragService == null) {
            return List.of();
        }
        return ragService.retrieve(userId, query, 8, "douyin,douyin_weigui").stream()
                .filter(ref -> ref.getKbName() != null)
                .filter(ref -> {
                    String kb = ref.getKbName().toLowerCase(Locale.ROOT);
                    return kb.equals("douyin") || kb.equals("douyin_weigui");
                })
                .limit(6)
                .collect(Collectors.toList());
    }

    private boolean hasRequiredOfficialRefs(List<RagRetrieveItemVO> refs) {
        boolean hasDouyin = false;
        boolean hasViolation = false;
        for (RagRetrieveItemVO ref : refs) {
            String kb = ref.getKbName() == null ? "" : ref.getKbName().toLowerCase(Locale.ROOT);
            hasDouyin = hasDouyin || "douyin".equals(kb);
            hasViolation = hasViolation || "douyin_weigui".equals(kb);
        }
        return hasDouyin && hasViolation;
    }

    private String buildReferenceQuery(String type, String productName, String scene, String context) {
        String product = productName != null && !productName.isBlank() ? productName : "商品";
        String typeText = type != null ? type : "product_intro";
        return String.join(" ",
                "抖音 官方规则 违规 风险 直播 短视频 千川 素材",
                scene != null ? scene : "",
                typeText,
                product,
                context != null ? context : "");
    }
}
