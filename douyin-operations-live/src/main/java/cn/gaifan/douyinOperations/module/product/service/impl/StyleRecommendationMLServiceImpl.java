package cn.gaifan.douyinOperations.module.product.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.product.entity.*;
import cn.gaifan.douyinOperations.module.product.repository.*;
import cn.gaifan.douyinOperations.module.product.service.StylePresetService;
import cn.gaifan.douyinOperations.module.product.service.StyleRecommendationMLService;
import cn.gaifan.douyinOperations.module.product.vo.ModelMetricsVO;
import cn.gaifan.douyinOperations.module.product.vo.StyleRecommendationVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能推荐服务实现 - 协同过滤算法
 *
 * @author Claude Code
 * @since 2026-04-04
 */
@Slf4j
@Service
public class StyleRecommendationMLServiceImpl implements StyleRecommendationMLService {

    @Resource
    private DyProductRepository productRepository;

    @Resource
    private ProductScriptVersionRepository scriptVersionRepository;

    @Resource
    private StyleRecommendationModelRepository modelRepository;

    @Resource
    private StyleRecommendationFeedbackRepository feedbackRepository;

    @Resource
    private ProductFeatureCacheRepository featureCacheRepository;

    @Resource
    private StylePresetService stylePresetService;

    @Resource
    private ObjectMapper objectMapper;

    private static final int MIN_TRAINING_SAMPLES = 10;  // 最少训练样本数
    private static final int SIMILARITY_TOP_K = 5;  // 相似商品数量
    private static final double ML_WEIGHT = 0.7;  // ML模型权重
    private static final double RULE_WEIGHT = 0.3;  // 规则引擎权重

    @Override
    public List<StyleRecommendationVO> recommendWithML(Long productId, Long userId, Integer topK) {
        if (topK == null || topK <= 0) topK = 5;

        // 检查是否有可用的ML模型
        Optional<StyleRecommendationModel> modelOpt = modelRepository
                .findByUserIdAndIsActiveTrueAndDeleted(userId, 0);

        if (modelOpt.isEmpty() || !"collaborative_filtering".equals(modelOpt.get().getModelType())) {
            log.warn("用户 {} 没有可用的ML模型，回退到规则引擎", userId);
            return convertRuleBasedToVO(stylePresetService.recommendStyles(productId, userId), "rule_based");
        }

        // 使用协同过滤推荐
        List<StyleRecommendationVO.SimilarProduct> similarProducts = findSimilarProducts(productId, userId, SIMILARITY_TOP_K);

        if (similarProducts.isEmpty()) {
            log.warn("商品 {} 没有找到相似商品，回退到规则引擎", productId);
            return convertRuleBasedToVO(stylePresetService.recommendStyles(productId, userId), "rule_based");
        }

        // 统计相似商品的最佳风格
        Map<String, List<BigDecimal>> styleScores = new HashMap<>();
        for (StyleRecommendationVO.SimilarProduct similar : similarProducts) {
            String style = similar.getBestStyle();
            if (style != null && similar.getBestStyleScore() != null) {
                styleScores.computeIfAbsent(style, k -> new ArrayList<>()).add(similar.getBestStyleScore());
            }
        }

        // 计算每个风格的加权平均分
        List<StyleRecommendationVO> recommendations = new ArrayList<>();
        for (Map.Entry<String, List<BigDecimal>> entry : styleScores.entrySet()) {
            String style = entry.getKey();
            List<BigDecimal> scores = entry.getValue();

            BigDecimal avgScore = scores.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);

            BigDecimal confidence = BigDecimal.valueOf(scores.size())
                    .divide(BigDecimal.valueOf(similarProducts.size()), 4, RoundingMode.HALF_UP);

            StyleRecommendationVO vo = StyleRecommendationVO.builder()
                    .styleCode(style)
                    .styleName(getStyleName(style))
                    .confidence(confidence)
                    .reason(String.format("基于%d个相似商品的历史效果", scores.size()))
                    .source("ml_model")
                    .expectedScore(avgScore)
                    .similarProducts(similarProducts.stream()
                            .filter(sp -> style.equals(sp.getBestStyle()))
                            .collect(Collectors.toList()))
                    .build();

            recommendations.add(vo);
        }

        // 按置信度和预期评分排序
        recommendations.sort((a, b) -> {
            int confCompare = b.getConfidence().compareTo(a.getConfidence());
            if (confCompare != 0) return confCompare;
            return b.getExpectedScore().compareTo(a.getExpectedScore());
        });

        return recommendations.stream().limit(topK).collect(Collectors.toList());
    }

    @Override
    public List<StyleRecommendationVO> recommendHybrid(Long productId, Long userId, Integer topK) {
        if (topK == null || topK <= 0) topK = 5;

        // 获取ML推荐结果
        List<StyleRecommendationVO> mlResults = recommendWithML(productId, userId, topK);

        // 获取规则引擎推荐结果
        List<String> ruleResults = stylePresetService.recommendStyles(productId, userId);
        List<StyleRecommendationVO> ruleVOs = convertRuleBasedToVO(ruleResults, "rule_based");

        // 合并结果：ML权重70%，规则权重30%
        Map<String, StyleRecommendationVO> mergedMap = new HashMap<>();

        for (StyleRecommendationVO mlVO : mlResults) {
            StyleRecommendationVO merged = StyleRecommendationVO.builder()
                    .styleCode(mlVO.getStyleCode())
                    .styleName(mlVO.getStyleName())
                    .confidence(mlVO.getConfidence().multiply(BigDecimal.valueOf(ML_WEIGHT)))
                    .reason("混合推荐：ML模型 + 规则引擎")
                    .source("hybrid")
                    .expectedScore(mlVO.getExpectedScore())
                    .similarProducts(mlVO.getSimilarProducts())
                    .build();
            mergedMap.put(mlVO.getStyleCode(), merged);
        }

        for (StyleRecommendationVO ruleVO : ruleVOs) {
            if (mergedMap.containsKey(ruleVO.getStyleCode())) {
                // 已存在，增加置信度
                StyleRecommendationVO existing = mergedMap.get(ruleVO.getStyleCode());
                existing.setConfidence(existing.getConfidence()
                        .add(ruleVO.getConfidence().multiply(BigDecimal.valueOf(RULE_WEIGHT))));
            } else {
                // 新增
                StyleRecommendationVO merged = StyleRecommendationVO.builder()
                        .styleCode(ruleVO.getStyleCode())
                        .styleName(ruleVO.getStyleName())
                        .confidence(ruleVO.getConfidence().multiply(BigDecimal.valueOf(RULE_WEIGHT)))
                        .reason("混合推荐：规则引擎")
                        .source("hybrid")
                        .expectedScore(BigDecimal.valueOf(70))  // 默认预期分数
                        .build();
                mergedMap.put(ruleVO.getStyleCode(), merged);
            }
        }

        // 按置信度排序
        List<StyleRecommendationVO> result = new ArrayList<>(mergedMap.values());
        result.sort((a, b) -> b.getConfidence().compareTo(a.getConfidence()));

        return result.stream().limit(topK).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long trainModelAsync(Long userId) {
        // TODO: 实现异步训练（使用Spring @Async或消息队列）
        return trainModel(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long trainModel(Long userId) {
        log.info("开始训练推荐模型: userId={}", userId);

        // 1. 收集训练数据：查询用户的所有商品及其话术效果
        List<DyProduct> products = productRepository.findByUserIdAndDeleted(userId, 0);

        if (products.size() < MIN_TRAINING_SAMPLES) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL,
                    String.format("训练样本不足，至少需要%d个商品，当前只有%d个", MIN_TRAINING_SAMPLES, products.size()));
        }

        // 2. 计算所有商品的特征向量
        int featureCount = 0;
        for (DyProduct product : products) {
            try {
                computeProductFeatures(product.getId(), userId);
                featureCount++;
            } catch (Exception e) {
                log.warn("计算商品特征失败: productId={}", product.getId(), e);
            }
        }

        // 3. 计算商品相似度矩阵（简化版：仅计算特征向量的余弦相似度）
        // 实际生产环境中，这部分应该使用更高效的算法或缓存

        // 4. 创建模型记录
        StyleRecommendationModel model = StyleRecommendationModel.builder()
                .userId(userId)
                .modelType("collaborative_filtering")
                .modelVersion("1.0")
                .modelData("{\"algorithm\":\"cosine_similarity\",\"features\":\"category,price,text\"}")
                .trainingSamples(featureCount)
                .trainedAt(LocalDateTime.now())
                .isActive(true)
                .build();

        // 停用旧模型
        modelRepository.findByUserIdAndIsActiveTrueAndDeleted(userId, 0)
                .ifPresent(oldModel -> {
                    oldModel.setIsActive(false);
                    modelRepository.save(oldModel);
                });

        model = modelRepository.save(model);

        log.info("模型训练完成: modelId={}, samples={}", model.getId(), featureCount);
        return model.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordFeedback(Long productId, List<String> recommendedStyles, List<String> selectedStyles, Long userId) {
        try {
            StyleRecommendationFeedback feedback = StyleRecommendationFeedback.builder()
                    .productId(productId)
                    .recommendedStyles(objectMapper.writeValueAsString(recommendedStyles))
                    .selectedStyles(objectMapper.writeValueAsString(selectedStyles))
                    .recommendationSource("hybrid")  // 默认混合推荐
                    .userId(userId)
                    .feedbackType("implicit")
                    .build();

            feedbackRepository.save(feedback);
            log.info("记录推荐反馈: productId=, recommended={}, selected={}", productId, recommendedStyles, selectedStyles);
        } catch (Exception e) {
            log.error("记录推荐反馈失败", e);
        }
    }

    @Override
    public ModelMetricsVO getModelMetrics(Long userId) {
        Optional<StyleRecommendationModel> modelOpt = modelRepository
                .findByUserIdAndIsActiveTrueAndDeleted(userId, 0);

        if (modelOpt.isEmpty()) {
            return ModelMetricsVO.builder()
                    .status("no_model")
                    .build();
        }

        StyleRecommendationModel model = modelOpt.get();

        // 统计最近30天的反馈
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        Long recentFeedbackCount = feedbackRepository.countByRecommendationSourceAndUserIdAndDeleted("hybrid", userId, 0);

        // 计算Top-3命中率
        Double top3HitRate = feedbackRepository.calculateTop3HitRate(userId, thirtyDaysAgo, LocalDateTime.now());

        return ModelMetricsVO.builder()
                .modelId(model.getId())
                .modelType(model.getModelType())
                .modelVersion(model.getModelVersion())
                .trainingSamples(model.getTrainingSamples())
                .accuracy(model.getAccuracy())
                .precision(model.getPrecisionScore())
                .recall(model.getRecallScore())
                .f1Score(model.getF1Score())
                .top3HitRate(top3HitRate != null ? BigDecimal.valueOf(top3HitRate) : null)
                .avgScoreImprovement(model.getAvgScoreImprovement())
                .trainedAt(model.getTrainedAt())
                .isActive(model.getIsActive())
                .recentFeedbackCount(recentFeedbackCount)
                .status(model.getIsActive() ? "active" : "inactive")
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String computeProductFeatures(Long productId, Long userId) {
        DyProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "商品不存在"));

        // 检查缓存
        Optional<ProductFeatureCache> cacheOpt = featureCacheRepository.findByProductIdAndDeleted(productId, 0);
        if (cacheOpt.isPresent() && cacheOpt.get().getComputedAt().isAfter(LocalDateTime.now().minusHours(24))) {
            return cacheOpt.get().getFeatureVector();
        }

        // 计算特征向量
        Map<String, Object> features = new HashMap<>();

        // 1. 分类特征（One-Hot编码简化版）
        features.put("category", product.getProductCategory() != null ? product.getProductCategory() : "未分类");

        // 2. 价格特征（归一化到0-1）
        double priceNormalized = 0.0;
        if (product.getPrice() != null) {
            priceNormalized = Math.min(1.0, product.getPrice().doubleValue() / 1000.0);
        }
        features.put("price_normalized", priceNormalized);

        // 3. 文本特征（简化版：关键词提取）
        List<String> keywords = extractKeywords(product);
        features.put("keywords", keywords);

        // 4. 历史效果特征
        List<ProductScriptVersion> versions = scriptVersionRepository.findByProductIdAndDeleted(productId, 0);
        double avgScore = versions.stream()
                .filter(v -> v.getEffectivenessScore() != null)
                .mapToDouble(v -> v.getEffectivenessScore().doubleValue())
                .average()
                .orElse(0.0);
        int usageCount = versions.stream()
                .mapToInt(v -> v.getUsageCount() != null ? v.getUsageCount() : 0)
                .sum();

        features.put("historical_avg_score", avgScore);
        features.put("historical_usage_count", usageCount);

        // 保存到缓存
        try {
            String featureJson = objectMapper.writeValueAsString(features);

            ProductFeatureCache cache = cacheOpt.orElse(ProductFeatureCache.builder()
                    .productId(productId)
                    .userId(userId)
                    .build());

            cache.setFeatureVector(featureJson);
            cache.setPriceNormalized(BigDecimal.valueOf(priceNormalized));
            cache.setHistoricalAvgScore(BigDecimal.valueOf(avgScore));
            cache.setHistoricalUsageCount(usageCount);
            cache.setComputedAt(LocalDateTime.now());

            featureCacheRepository.save(cache);

            return featureJson;
        } catch (Exception e) {
            log.error("保存特征缓存失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "计算特征失败");
        }
    }

    @Override
    public List<StyleRecommendationVO.SimilarProduct> findSimilarProducts(Long productId, Long userId, Integer topK) {
        if (topK == null || topK <= 0) topK = SIMILARITY_TOP_K;

        // 获取目标商品的特征
        String targetFeaturesJson = computeProductFeatures(productId, userId);
        Map<String, Object> targetFeatures;
        try {
            targetFeatures = objectMapper.readValue(targetFeaturesJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("解析特征向量失败", e);
            return Collections.emptyList();
        }

        // 获取所有其他商品
        List<DyProduct> allProducts = productRepository.findByUserIdAndDeleted(userId, 0);
        List<StyleRecommendationVO.SimilarProduct> similarProducts = new ArrayList<>();

        for (DyProduct product : allProducts) {
            if (product.getId().equals(productId)) continue;

            try {
                // 计算相似度
                String productFeaturesJson = computeProductFeatures(product.getId(), userId);
                Map<String, Object> productFeatures = objectMapper.readValue(productFeaturesJson,
                        new TypeReference<Map<String, Object>>() {});

                double similarity = calculateCosineSimilarity(targetFeatures, productFeatures);

                if (similarity > 0.3) {  // 相似度阈值
                    // 查找该商品的最佳风格
                    List<ProductScriptVersion> versions = scriptVersionRepository.findByProductIdAndDeleted(product.getId(), 0);
                    Optional<ProductScriptVersion> bestVersion = versions.stream()
                            .filter(v -> v.getEffectivenessScore() != null)
                            .max(Comparator.comparing(ProductScriptVersion::getEffectivenessScore));

                    if (bestVersion.isPresent()) {
                        StyleRecommendationVO.SimilarProduct similar = StyleRecommendationVO.SimilarProduct.builder()
                                .productId(product.getId())
                                .productName(product.getProductName())
                                .similarity(BigDecimal.valueOf(similarity).setScale(4, RoundingMode.HALF_UP))
                                .bestStyle(bestVersion.get().getStyle())
                                .bestStyleScore(bestVersion.get().getEffectivenessScore())
                                .build();
                        similarProducts.add(similar);
                    }
                }
            } catch (Exception e) {
                log.warn("计算商品相似度失败: productId={}", product.getId(), e);
            }
        }

        // 按相似度排序
        similarProducts.sort((a, b) -> b.getSimilarity().compareTo(a.getSimilarity()));

        return similarProducts.stream().limit(topK).collect(Collectors.toList());
    }

    // ================ 私有辅助方法 ================

    /**
     * 计算余弦相似度
     */
    private double calculateCosineSimilarity(Map<String, Object> features1, Map<String, Object> features2) {
        // 简化版：仅计算数值特征的余弦相似度
        double priceNorm1 = getDoubleValue(features1, "price_normalized");
        double priceNorm2 = getDoubleValue(features2, "price_normalized");
        double avgScore1 = getDoubleValue(features1, "historical_avg_score");
        double avgScore2 = getDoubleValue(features2, "historical_avg_score");

        // 分类相同加分
        String category1 = (String) features1.get("category");
        String category2 = (String) features2.get("category");
        double categorySimilarity = (category1 != null && category1.equals(category2)) ? 1.0 : 0.0;

        // 加权计算
        double priceSim = 1.0 - Math.abs(priceNorm1 - priceNorm2);
        double scoreSim = 1.0 - Math.abs(avgScore1 - avgScore2) / 100.0;

        return (priceSim * 0.3 + scoreSim * 0.3 + categorySimilarity * 0.4);
    }

    private double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    /**
     * 提取关键词（简化版）
     */
    private List<String> extractKeywords(DyProduct product) {
        List<String> keywords = new ArrayList<>();
        if (product.getAiSellingPoints() != null) {
            // 简单分词（实际应使用分词器）
            String[] words = product.getAiSellingPoints().split("[，。、\\s]+");
            keywords.addAll(Arrays.asList(words));
        }
        return keywords.stream().limit(10).collect(Collectors.toList());
    }

    /**
     * 转换规则引擎结果为VO
     */
    private List<StyleRecommendationVO> convertRuleBasedToVO(List<String> styleCodes, String source) {
        List<StyleRecommendationVO> result = new ArrayList<>();
        for (int i = 0; i < styleCodes.size(); i++) {
            String code = styleCodes.get(i);
            StyleRecommendationVO vo = StyleRecommendationVO.builder()
                    .styleCode(code)
                    .styleName(getStyleName(code))
                    .confidence(BigDecimal.valueOf(1.0 - i * 0.1))  // 递减置信度
                    .reason("基于规则引擎推荐")
                    .source(source)
                    .expectedScore(BigDecimal.valueOf(70))
                    .build();
            result.add(vo);
        }
        return result;
    }

    /**
     * 获取风格名称
     */
    private String getStyleName(String styleCode) {
        Map<String, String> styleNames = Map.of(
                "professional", "专业",
                "warm", "温暖",
                "enthusiastic", "热情",
                "casual", "随意",
                "friendly", "亲和",
                "passionate", "激情",
                "elegant", "优雅",
                "trendy", "时尚",
                "energetic", "活力"
        );
        return styleNames.getOrDefault(styleCode, styleCode);
    }
}
