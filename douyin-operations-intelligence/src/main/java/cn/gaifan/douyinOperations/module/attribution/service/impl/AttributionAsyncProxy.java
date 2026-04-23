package cn.gaifan.douyinOperations.module.attribution.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.attribution.entity.Attribution;
import cn.gaifan.douyinOperations.module.attribution.repository.AttributionRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Async proxy bean for AttributionService.
 * Solves the @Async self-invocation problem by being a separate Spring-managed bean.
 */
@Service
public class AttributionAsyncProxy {

    private static final Logger log = LoggerFactory.getLogger(AttributionAsyncProxy.class);

    @Resource private AttributionRepository attributionRepository;
    @Resource private LiveProductRepository productRepository;
    @Resource private LiveScriptRepository scriptRepository;
    @Resource private AiModelRepository aiModelRepository;
    @Resource private LlmClient llmClient;

    @Async
    public void asyncAttribution(Long sessionId, Long ownerId) {
        try {
            List<LiveProduct> products = productRepository.findBySessionId(sessionId);
            List<LiveScript> scripts = scriptRepository.findBySessionIdAndDeleted(sessionId, 0);

            BigDecimal totalGmv = products.stream()
                    .map(p -> p.getRevenue() != null ? p.getRevenue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int totalSales = products.stream()
                    .mapToInt(p -> p.getSaleQuantity() != null ? p.getSaleQuantity() : 0)
                    .sum();

            for (LiveProduct product : products) {
                Attribution attr = new Attribution();
                attr.setSessionId(sessionId);
                attr.setOwnerId(ownerId);
                attr.setAttributionType("product_gmv");
                attr.setProductId(product.getProductId());
                attr.setProductName(product.getProductName());
                attr.setContributedGmv(product.getRevenue() != null ? product.getRevenue() : BigDecimal.ZERO);
                attr.setContributedSales(product.getSaleQuantity() != null ? product.getSaleQuantity() : 0);
                if (totalGmv.compareTo(BigDecimal.ZERO) > 0) {
                    attr.setContributionRatio(attr.getContributedGmv()
                            .divide(totalGmv, 4, RoundingMode.HALF_UP));
                }
                attr.setEffectScore(calculateProductScore(product, totalGmv));
                attr.setStatus(1);
                attributionRepository.save(attr);
            }

            List<LiveScript> executedScripts = scripts.stream()
                    .filter(s -> s.getExecuted() != null && s.getExecuted() == 1)
                    .toList();

            for (LiveScript script : executedScripts) {
                Attribution attr = new Attribution();
                attr.setSessionId(sessionId);
                attr.setOwnerId(ownerId);
                attr.setAttributionType("script_sales");
                attr.setScriptId(script.getId());
                attr.setScriptContent(script.getScriptContent());

                if (!executedScripts.isEmpty() && totalGmv.compareTo(BigDecimal.ZERO) > 0) {
                    attr.setContributedGmv(totalGmv.divide(
                            BigDecimal.valueOf(executedScripts.size()), 2, RoundingMode.HALF_UP));
                    attr.setContributionRatio(BigDecimal.ONE.divide(
                            BigDecimal.valueOf(executedScripts.size()), 4, RoundingMode.HALF_UP));
                }
                attr.setEffectScore(calculateScriptScore(script));
                attr.setStatus(1);
                attributionRepository.save(attr);
            }

            generateAiAttribution(sessionId, ownerId, products, scripts, totalGmv, totalSales);

            log.info("归因分析完成: sessionId={}, products={}, scripts={}", sessionId, products.size(), executedScripts.size());
        } catch (Exception e) {
            log.error("归因分析异常: sessionId={}", sessionId, e);
        }
    }

    private void generateAiAttribution(Long sessionId, Long ownerId,
                                        List<LiveProduct> products, List<LiveScript> scripts,
                                        BigDecimal totalGmv, int totalSales) {
        AiModel model = findAvailableModel();
        if (model == null) {
            log.warn("无可用 AI 模型，跳过 AI 归因分析");
            return;
        }

        String productSummary = products.stream()
                .map(p -> String.format("- %s: 销量%d, 销售额%s元",
                        p.getProductName(), p.getSaleQuantity(), p.getRevenue()))
                .collect(Collectors.joining("\n"));

        String scriptSummary = scripts.stream()
                .map(s -> String.format("- [%s][%s] %s",
                        s.getScriptType(),
                        s.getExecuted() == 1 ? "已执行" : "未执行",
                        s.getScriptContent() != null && s.getScriptContent().length() > 60
                                ? s.getScriptContent().substring(0, 60) + "..." : s.getScriptContent()))
                .collect(Collectors.joining("\n"));

        String system = "你是一位电商直播数据分析专家，擅长效果归因分析。请用中文回答，简洁专业。";
        String prompt = String.format("""
                请对以下直播场次进行效果归因分析。

                ## 销售数据
                - 总GMV: %s元
                - 总销量: %d件
                - 商品明细:
                %s

                ## 话术数据
                - 总话术: %d条
                - 已执行: %d条
                - 话术明细:
                %s

                请分析：
                1. 哪些商品贡献最大？为什么？
                2. 哪些话术对转化最有效？
                3. 话术与商品销售之间的关联性
                4. 给出综合效果评分（0-100）和改进建议
                """,
                totalGmv, totalSales,
                productSummary.isEmpty() ? "无商品数据" : productSummary,
                scripts.size(),
                scripts.stream().filter(s -> s.getExecuted() != null && s.getExecuted() == 1).count(),
                scriptSummary.isEmpty() ? "无话术数据" : scriptSummary);

        try {
            LlmClient.LlmResponse response = llmClient.chat(model, system, prompt);
            if (response.success() && response.content() != null) {
                List<Attribution> overalls = attributionRepository
                        .findBySessionIdAndAttributionTypeAndDeleted(sessionId, "overall", 0);
                if (!overalls.isEmpty()) {
                    Attribution overall = overalls.get(0);
                    overall.setAnalysis(response.content());
                    overall.setContributedGmv(totalGmv);
                    overall.setContributedSales(totalSales);
                    overall.setModelUsed(model.getModelVersion());
                    overall.setTokensUsed(response.tokensUsed());
                    overall.setEffectScore(extractScore(response.content()));
                    overall.setStatus(1);
                    attributionRepository.save(overall);
                }
                aiModelRepository.incrementQuotaUsed(model.getId(), response.tokensUsed());
            }
        } catch (Exception e) {
            log.error("AI 归因分析失败: sessionId={}", sessionId, e);
        }
    }

    private int calculateProductScore(LiveProduct product, BigDecimal totalGmv) {
        if (totalGmv.compareTo(BigDecimal.ZERO) == 0) return 0;
        BigDecimal revenue = product.getRevenue() != null ? product.getRevenue() : BigDecimal.ZERO;
        double ratio = revenue.divide(totalGmv, 4, RoundingMode.HALF_UP).doubleValue();
        return Math.min((int) (ratio * 100 + 20), 100);
    }

    private int calculateScriptScore(LiveScript script) {
        int score = 40;
        if (script.getExecuted() != null && script.getExecuted() == 1) score += 30;
        if ("product".equals(script.getScriptType())) score += 15;
        if ("opening".equals(script.getScriptType())) score += 10;
        if (script.getAiGenerated() != null && script.getAiGenerated() == 1) score += 5;
        return Math.min(score, 100);
    }

    private int extractScore(String content) {
        try {
            var matcher = java.util.regex.Pattern.compile("(\\d{1,3})\\s*[/\uff0f\u5206]").matcher(content);
            if (matcher.find()) return Math.min(Integer.parseInt(matcher.group(1)), 100);
            matcher = java.util.regex.Pattern.compile("\u8bc4\u5206[\uff1a:]?\\s*(\\d{1,3})").matcher(content);
            if (matcher.find()) return Math.min(Integer.parseInt(matcher.group(1)), 100);
        } catch (Exception ignored) {}
        return 50;
    }

    private AiModel findAvailableModel() {
        List<AiModel> models = aiModelRepository.findByStatusAndDeleted(1, 0);
        if (models.isEmpty()) return null;
        return models.stream()
                .filter(m -> m.getQuotaLimit() == null || m.getQuotaLimit() == 0 || m.getQuotaUsed() < m.getQuotaLimit())
                .findFirst().orElse(models.get(0));
    }
}
