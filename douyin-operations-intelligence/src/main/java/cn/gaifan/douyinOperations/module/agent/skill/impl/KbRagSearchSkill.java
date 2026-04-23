package cn.gaifan.douyinOperations.module.agent.skill.impl;

import cn.gaifan.douyinOperations.module.agent.skill.Skill;
import cn.gaifan.douyinOperations.module.ai.service.RagService;
import cn.gaifan.douyinOperations.module.ai.vo.RagRetrieveItemVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识库 RAG 检索技能
 * 使用真实的 RagService 进行知识库检索，失败时降级为模拟数据
 */
@Component
public class KbRagSearchSkill implements Skill {

    private static final Logger log = LoggerFactory.getLogger(KbRagSearchSkill.class);

    @Autowired(required = false)
    private RagService ragService;

    @Override
    public String getName() {
        return "kb_rag_search";
    }

    @Override
    public String getDescription() {
        return "从知识库中检索相关信息，支持语义搜索和关键词搜索";
    }

    @Override
    public boolean matches(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase();
        return lower.contains("搜索") || lower.contains("查找") || lower.contains("检索")
                || lower.contains("知识库") || lower.contains("资料") || lower.contains("查询");
    }

    @Override
    public String execute(SkillContext ctx) {
        try {
            String query = (String) ctx.params().getOrDefault("query", ctx.rawInput());

            if (query == null || query.trim().isEmpty()) {
                return "请提供搜索关键词";
            }

            log.info("[KbRagSearch] 执行检索: query={}, userId={}", query, ctx.userId());

            // 优先使用真实的 RagService
            if (ragService != null) {
                try {
                    List<RagRetrieveItemVO> results = ragService.retrieve(
                            ctx.userId(), query, 5, "all");

                    if (results != null && !results.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("📚 知识库检索结果（共 ").append(results.size()).append(" 条）：\n\n");

                        for (int i = 0; i < results.size(); i++) {
                            RagRetrieveItemVO item = results.get(i);
                            sb.append("--- 素材 ").append(i + 1).append(" ---\n");
                            if (item.getTitle() != null) {
                                sb.append("【").append(item.getTitle()).append("】\n");
                            }
                            if (item.getContent() != null) {
                                String content = item.getContent();
                                // 截取前500字符
                                if (content.length() > 500) {
                                    content = content.substring(0, 500) + "...(省略)";
                                }
                                sb.append(content).append("\n");
                            }
                            if (item.getScore() > 0) {
                                sb.append("相关度: ").append(String.format("%.2f", item.getScore())).append("\n");
                            }
                            sb.append("\n");
                        }

                        return sb.toString();
                    }
                } catch (Exception e) {
                    log.warn("[KbRagSearch] RagService 调用失败: {}", e.getMessage());
                    // 降级到模拟数据
                }
            }

            // 降级方案：返回模拟数据
            return buildMockResponse(query);

        } catch (Exception e) {
            log.error("[KbRagSearch] 执行失败", e);
            return "知识库检索失败: " + e.getMessage();
        }
    }

    private String buildMockResponse(String query) {
        StringBuilder sb = new StringBuilder();
        sb.append("📚 知识库检索结果（演示数据）：\n\n");

        sb.append("--- 素材 1 ---\n");
        sb.append("【护肤品成分知识】\n");
        sb.append("玻尿酸（透明质酸）是一种天然保湿因子，能够吸收自身重量1000倍的水分，");
        sb.append("为肌肤提供深层补水。适合所有肤质使用，特别是干性和缺水肌肤。\n");
        sb.append("相关度: 0.92\n\n");

        sb.append("--- 素材 2 ---\n");
        sb.append("【直播话术技巧】\n");
        sb.append("介绍护肤品时，要突出产品的核心功效和使用感受，避免使用极限词。");
        sb.append("可以结合用户评价和使用案例，增强说服力。建议使用\"改善\"、\"帮助\"等温和表述。\n");
        sb.append("相关度: 0.88\n\n");

        sb.append("--- 素材 3 ---\n");
        sb.append("【产品卖点提炼】\n");
        sb.append("护肤品的核心卖点包括：成分安全性、功效明确性、使用便捷性、性价比优势。");
        sb.append("在直播中要根据目标用户群体，有针对性地强调不同卖点。\n");
        sb.append("相关度: 0.85\n\n");

        sb.append("💡 提示：当前使用演示数据，如需真实数据请确保知识库已正确配置。\n");

        return sb.toString();
    }
}
