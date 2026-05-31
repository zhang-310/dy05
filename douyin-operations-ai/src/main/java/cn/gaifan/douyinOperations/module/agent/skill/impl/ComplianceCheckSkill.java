package cn.gaifan.douyinOperations.module.agent.skill.impl;

import cn.gaifan.douyinOperations.module.agent.skill.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 违规检测技能
 */
@Component
public class ComplianceCheckSkill implements Skill {

    private static final Logger log = LoggerFactory.getLogger(ComplianceCheckSkill.class);

    @Override
    public String getName() {
        return "compliance_check";
    }

    @Override
    public String getDescription() {
        return "检测文本中的违规内容，包括敏感词、虚假宣传、极限词等";
    }

    @Override
    public boolean matches(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase();
        return lower.contains("检测") || lower.contains("违规") || lower.contains("合规")
                || lower.contains("敏感词") || lower.contains("审核");
    }

    @Override
    public String execute(SkillContext ctx) {
        try {
            // 从参数中获取待检测文本
            String text = (String) ctx.params().getOrDefault("text", ctx.rawInput());

            if (text == null || text.trim().isEmpty()) {
                return "请提供需要检测的文本内容";
            }

            log.info("[ComplianceCheck] 执行检测: textLength={}", text.length());

            // 使用简单的规则检测
            return performSimpleCheck(text);

        } catch (Exception e) {
            log.error("[ComplianceCheck] 执行失败", e);
            return "违规检测失败: " + e.getMessage();
        }
    }

    /**
     * 简单的规则检测
     */
    private String performSimpleCheck(String text) {
        StringBuilder sb = new StringBuilder();
        boolean hasViolation = false;

        // 敏感词列表（示例）
        List<String> sensitiveWords = List.of(
                "最好", "第一", "顶级", "极品", "国家级", "世界级",
                "治疗", "疗效", "药用", "医用", "处方",
                "绝对", "100%", "完全", "彻底"
        );

        List<String> foundWords = sensitiveWords.stream()
                .filter(text::contains)
                .toList();

        if (!foundWords.isEmpty()) {
            hasViolation = true;
            sb.append("⚠️ 检测到以下违规词汇：\n\n");
            int index = 1;
            for (String word : foundWords) {
                sb.append(index++).append(". ");
                sb.append("【极限词/虚假宣传】").append(word).append("\n");
                sb.append("   建议: 删除或替换为更客观的表述\n\n");
            }
        }

        if (!hasViolation) {
            sb.append("✅ 检测通过，未发现明显违规内容\n");
        }

        sb.append("\n💡 提示：这是基础检测，建议结合人工审核确保合规。\n");

        return sb.toString();
    }
}
