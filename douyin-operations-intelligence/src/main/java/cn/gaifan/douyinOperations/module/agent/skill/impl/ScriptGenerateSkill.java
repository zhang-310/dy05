package cn.gaifan.douyinOperations.module.agent.skill.impl;

import cn.gaifan.douyinOperations.module.agent.skill.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 话术生成技能
 */
@Component
public class ScriptGenerateSkill implements Skill {

    private static final Logger log = LoggerFactory.getLogger(ScriptGenerateSkill.class);

    @Override
    public String getName() {
        return "script_generate";
    }

    @Override
    public String getDescription() {
        return "生成直播话术，包括开场白、产品介绍、促销话术等";
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

            log.info("[ScriptGenerate] 执行生成: type={}, product={}, style={}", scriptType, productName, style);

            // 使用模板生成
            return generateFromTemplate(scriptType, productName, style, scene);

        } catch (Exception e) {
            log.error("[ScriptGenerate] 执行失败", e);
            return "话术生成失败: " + e.getMessage();
        }
    }

    /**
     * 从模板生成话术
     */
    private String generateFromTemplate(String type, String productName, String style, String scene) {
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

        return sb.toString();
    }
}
