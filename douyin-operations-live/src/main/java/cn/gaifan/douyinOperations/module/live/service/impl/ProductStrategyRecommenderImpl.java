package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.service.ProductStrategyRecommender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductStrategyRecommenderImpl implements ProductStrategyRecommender {

    private static final Logger log = LoggerFactory.getLogger(ProductStrategyRecommenderImpl.class);

    @Autowired(required = false)
    private LiveProductRepository liveProductRepository;

    @Autowired(required = false)
    private LiveScriptRepository liveScriptRepository;

    @Autowired(required = false)
    private LlmClient llmClient;

    @Autowired(required = false)
    private AiModelRepository aiModelRepository;

    @Override
    public Map<String, Object> recommend(Long userId, Long productId) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "product-strategy");

        if (liveProductRepository == null || productId == null) {
            result.put("status", "unavailable");
            result.put("message", "商品数据不可用");
            return result;
        }

        LiveProduct product = liveProductRepository.findById(productId).orElse(null);
        if (product == null) {
            result.put("status", "not_found");
            result.put("message", "商品不存在");
            return result;
        }

        List<LiveScript> scripts = liveScriptRepository != null
                ? liveScriptRepository.findBySessionIdAndProductId(product.getSessionId(), productId)
                : List.of();

        String scriptSummary = scripts.stream()
                .map(s -> String.format("话术(得分:%s): %s",
                        s.getEffectivenessScore() != null ? s.getEffectivenessScore().toString() : "未评",
                        truncate(s.getScriptContent(), 100)))
                .collect(Collectors.joining("\n"));

        String system = "你是抖音直播选品讲解策略专家。请根据商品信息和历史话术效果，推荐最优讲解风格和时长。" +
                "输出 JSON 格式包含：recommendedStyle(字符串)、recommendedDuration(秒)、keyPoints(数组)、openingHook(字符串)、closingCta(字符串)。";
        String prompt = String.format("商品名称: %s\n商品类型: %s\n销量: %d\n历史话术:\n%s",
                product.getProductName(),
                product.getProductType() != null ? product.getProductType() : "未分类",
                product.getSaleQuantity() != null ? product.getSaleQuantity() : 0,
                scriptSummary.isEmpty() ? "暂无历史话术" : scriptSummary);

        return callLlm(result, system, prompt);
    }

    @Override
    public Map<String, Object> recommendBatchOrder(Long userId, Long sessionId) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "batch-order");

        if (liveProductRepository == null || sessionId == null) {
            result.put("status", "unavailable");
            result.put("message", "场次数据不可用");
            return result;
        }

        List<LiveProduct> products = liveProductRepository.findBySessionIdOrderByPositionAscIdAsc(sessionId);
        if (products.isEmpty()) {
            result.put("status", "empty");
            result.put("message", "场次无商品");
            return result;
        }

        String productList = products.stream()
                .map(p -> String.format("- %s | 类型:%s | 销量:%d | 营收:%s",
                        p.getProductName(),
                        p.getProductType() != null ? p.getProductType() : "未分类",
                        p.getSaleQuantity() != null ? p.getSaleQuantity() : 0,
                        p.getRevenue() != null ? p.getRevenue().toString() : "0"))
                .collect(Collectors.joining("\n"));

        String system = "你是抖音直播排品策略专家。请按照「引流款开场→利润款中场→爆款返场」的规则，" +
                "为以下商品生成推荐排序和每个商品的建议讲解时长。" +
                "输出 JSON 格式包含：recommendedOrder(数组，每项含 productName/position/role/suggestedDurationSec)、" +
                "overallStrategy(字符串)、tips(数组)。";
        String prompt = "场次商品列表:\n" + productList;

        return callLlm(result, system, prompt);
    }

    private Map<String, Object> callLlm(Map<String, Object> result, String system, String prompt) {
        if (llmClient == null || aiModelRepository == null) {
            result.put("status", "unavailable");
            result.put("message", "LLM 服务未配置");
            return result;
        }
        try {
            List<AiModel> models = aiModelRepository.findByStatusAndDeleted(1, 0)
                    .stream().limit(3).toList();
            if (models.isEmpty()) {
                result.put("status", "no_model");
                result.put("message", "无可用 AI 模型");
                return result;
            }
            LlmClient.LlmResponse response = llmClient.chatWithFallback(models, system, prompt);
            if (response.success()) {
                result.put("status", "success");
                result.put("recommendation", response.content());
                result.put("tokensUsed", response.tokensUsed());
            } else {
                result.put("status", "failed");
                result.put("message", response.errorMsg());
            }
        } catch (Exception e) {
            log.warn("ProductStrategyRecommender LLM 调用失败: {}", e.getMessage());
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        return result;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
