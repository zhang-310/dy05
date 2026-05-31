package cn.gaifan.douyinOperations.module.agent.skill.impl;

import cn.gaifan.douyinOperations.common.config.RequestIdentityHolder;
import cn.gaifan.douyinOperations.contract.identity.IdentityContext;
import cn.gaifan.douyinOperations.contract.product.FeatureCode;
import cn.gaifan.douyinOperations.contract.product.ProductCode;
import cn.gaifan.douyinOperations.contract.product.ProductIntegrationInvocationRequest;
import cn.gaifan.douyinOperations.module.agent.skill.Skill;
import cn.gaifan.douyinOperations.module.ai.service.RagService;
import cn.gaifan.douyinOperations.module.platform.product.ProductIntegrationService;
import cn.gaifan.douyinOperations.module.ai.vo.RagRetrieveItemVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库 RAG 检索技能
 * 使用真实的 RagService 进行知识库检索；未配置或失败时明确返回降级状态。
 */
@Component
public class KbRagSearchSkill implements Skill {

    private static final Logger log = LoggerFactory.getLogger(KbRagSearchSkill.class);

    @Autowired(required = false)
    private RagService ragService;

    @Autowired(required = false)
    private ProductIntegrationService productIntegrationService;

    @Override
    public String getName() {
        return "kb_rag_search";
    }

    @Override
    public String getDescription() {
        return "从知识库中检索相关信息，支持限定 douyin、douyin_weigui 等知识库，并返回可引用的 docId/kbName/source";
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
            String scope = normalizeScope((String) ctx.params().get("scope"));
            int topK = parseTopK(ctx.params().get("top_k"));

            if (query == null || query.trim().isEmpty()) {
                return "请提供搜索关键词";
            }

            log.info("[KbRagSearch] 执行检索: query={}, scope={}, topK={}, userId={}", query, scope, topK, ctx.userId());

            if (productIntegrationService != null) {
                IdentityContext identity = RequestIdentityHolder.current();
                String tenantId = identity != null && identity.tenantId() != null ? identity.tenantId() : "demo-tenant";
                String traceId = identity != null && identity.traceId() != null ? identity.traceId() : "kb-rag-" + ctx.userId();
                var gate = productIntegrationService.invoke(new ProductIntegrationInvocationRequest(
                        tenantId,
                        ctx.userId() != null ? String.valueOf(ctx.userId()) : "kb-user",
                        ctx.agentId() != null ? String.valueOf(ctx.agentId()) : null,
                        ProductCode.DOUYIN_OPS,
                        "agent.kb_rag_search",
                        ProductCode.KNOWLEDGE_BASE,
                        FeatureCode.KB_RAG,
                        "AGENT",
                        BigDecimal.ONE,
                        null,
                        traceId,
                        query,
                        false,
                        null,
                        null,
                        null,
                        null,
                        false
                ));
                if (!gate.allowed()) {
                    return "知识库检索被拒绝: " + gate.message();
                }
            }

            // 优先使用真实的 RagService
            if (ragService != null) {
                try {
                    List<RagRetrieveItemVO> results = ragService.retrieve(
                            ctx.userId(), query, topK, scope);

                    if (results != null && !results.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("📚 知识库检索结果（共 ").append(results.size()).append(" 条）\n");
                        sb.append("检索范围：").append(scope == null || scope.isBlank() ? "all" : scope).append("\n");
                        sb.append("引用要求：使用这些结果生成或审核时，必须在输出末尾保留 docId/kbName/source。\n\n");

                        for (int i = 0; i < results.size(); i++) {
                            RagRetrieveItemVO item = results.get(i);
                            sb.append("--- 引用 ").append(i + 1).append(" ---\n");
                            sb.append("docId: ").append(item.getDocId() != null ? item.getDocId() : "未知").append("\n");
                            sb.append("kbName: ").append(textOrDash(item.getKbName())).append("\n");
                            sb.append("source: ").append(textOrDash(item.getSource())).append("\n");
                            if (item.getTitle() != null) {
                                sb.append("title: ").append(item.getTitle()).append("\n");
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
                    return "知识库检索无结果。阻断要求：涉及直播话术、短视频脚本、千川素材审核时，不能继续生成可放行内容；请检查 scope="
                            + (scope == null || scope.isBlank() ? "all" : scope)
                            + " 的知识库文档、索引队列、Embedding 与 ES/Milvus 状态。";
                } catch (Exception e) {
                    log.warn("[KbRagSearch] RagService 调用失败: {}", e.getMessage());
                    return "知识库检索暂不可用。阻断要求：不能输出无官方引用的可放行结论。错误: " + e.getMessage();
                }
            }

            return "知识库检索未配置：RagService 不可用，请启用知识库检索服务后重试。";

        } catch (Exception e) {
            log.error("[KbRagSearch] 执行失败", e);
            return "知识库检索失败: " + e.getMessage();
        }
    }

    private String normalizeScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return "all";
        }
        return Arrays.stream(scope.split("[,，|\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(","));
    }

    private int parseTopK(Object raw) {
        if (raw == null) {
            return 8;
        }
        try {
            int value = raw instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(raw));
            return Math.max(1, Math.min(value, 20));
        } catch (Exception ignored) {
            return 8;
        }
    }

    private String textOrDash(String text) {
        return text == null || text.isBlank() ? "—" : text;
    }
}
