package cn.gaifan.douyinOperations.module.product.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.product.entity.ProductScriptComparisonCache;
import cn.gaifan.douyinOperations.module.product.entity.ProductScriptEffectivenessRecord;
import cn.gaifan.douyinOperations.module.product.entity.ProductScriptVersion;
import cn.gaifan.douyinOperations.module.product.repository.ProductScriptComparisonCacheRepository;
import cn.gaifan.douyinOperations.module.product.repository.ProductScriptEffectivenessRecordRepository;
import cn.gaifan.douyinOperations.module.product.repository.ProductScriptVersionRepository;
import cn.gaifan.douyinOperations.module.product.service.EffectivenessScoreService;
import cn.gaifan.douyinOperations.module.product.vo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品话术效果评分业务逻辑实现
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Slf4j
@Service("productEffectivenessScoreServiceImpl")
public class EffectivenessScoreServiceImpl implements EffectivenessScoreService {

    @Resource
    private ProductScriptVersionRepository versionRepository;

    @Resource
    private ProductScriptEffectivenessRecordRepository recordRepository;

    @Resource
    private ProductScriptComparisonCacheRepository cacheRepository;

    @Resource
    private ObjectMapper objectMapper;

    // ================ 评分算法常量 ================
    private static final double BASE_SCORE = 50.0;
    private static final double USE_SCORE_MAX = 25.0;
    private static final double CONVERSION_SCORE_MAX = 15.0;
    private static final double INTERACTION_SCORE_MAX = 10.0;

    private static final int USE_WEIGHT_THRESHOLD = 100; // 使用次数权重阈值
    private static final double CONVERSION_BENCHMARK = 15.0; // 转化率基准
    private static final int INTERACTION_WEIGHT_THRESHOLD = 200; // 互动权重阈值

    @Override
    public Double calculateScore(Long versionId, Long userId) {
        ProductScriptVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术版本不存在"));

        if (!version.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "无权限访问此版本");
        }

        // 计算各部分得分
        double baseScore = BASE_SCORE;

        // 使用次数得分（0-25）
        Integer usageCount = version.getUsageCount() != null ? version.getUsageCount() : 0;
        double useScore = Math.min(USE_SCORE_MAX, (usageCount / (double) USE_WEIGHT_THRESHOLD) * USE_SCORE_MAX);

        // 转化率得分（0-15）
        BigDecimal conversionRate = version.getConversionRate() != null ? version.getConversionRate() : BigDecimal.ZERO;
        double conversionScore = Math.min(CONVERSION_SCORE_MAX,
                (conversionRate.doubleValue() / CONVERSION_BENCHMARK) * CONVERSION_SCORE_MAX);

        // 互动得分（点赞+评论）（0-10）
        Integer likesCount = version.getLikesCount() != null ? version.getLikesCount() : 0;
        Integer commentsCount = version.getCommentsCount() != null ? version.getCommentsCount() : 0;
        int totalInteraction = likesCount + commentsCount;
        double interactionScore = Math.min(INTERACTION_SCORE_MAX,
                (totalInteraction / (double) INTERACTION_WEIGHT_THRESHOLD) * INTERACTION_SCORE_MAX);

        // 总分 = 50 + 25 + 15 + 10 = 100
        double totalScore = baseScore + useScore + conversionScore + interactionScore;
        totalScore = Math.min(100.0, totalScore); // 最高 100 分

        log.info("计算话术版本效果评分: versionId={}, score={}, use={}, conversion={}, interaction={}",
                versionId, totalScore, useScore, conversionScore, interactionScore);

        return totalScore;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer recalculateAllScores(Long productId, Long userId) {
        // 查询产品的所有活跃版本
        Specification<ProductScriptVersion> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("productId"), productId),
                cb.equal(root.get("ownerId"), userId),
                cb.equal(root.get("deleted"), 0)
        );

        List<ProductScriptVersion> versions = versionRepository.findAll(spec);
        if (versions.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (ProductScriptVersion version : versions) {
            try {
                Double newScore = calculateScore(version.getId(), userId);
                String scoreLevel = determineScoreLevel(newScore);

                // 更新版本的评分
                version.setEffectivenessScore(BigDecimal.valueOf(newScore).setScale(2, RoundingMode.HALF_UP));
                versionRepository.save(version);

                // 记录评分历史
                ProductScriptEffectivenessRecord record = ProductScriptEffectivenessRecord.builder()
                        .productId(productId)
                        .scriptVersionId(version.getId())
                        .calculatedAt(LocalDateTime.now())
                        .scoreValue(BigDecimal.valueOf(newScore).setScale(2, RoundingMode.HALF_UP))
                        .scoreLevel(scoreLevel)
                        .usageCountSnapshot(version.getUsageCount() != null ? version.getUsageCount() : 0)
                        .conversionRateSnapshot(version.getConversionRate() != null ? version.getConversionRate() : BigDecimal.ZERO)
                        .likesSnapshot(version.getLikesCount() != null ? version.getLikesCount() : 0)
                        .commentsSnapshot(version.getCommentsCount() != null ? version.getCommentsCount() : 0)
                        .ownerId(userId)
                        .deleted(0)
                        .build();
                recordRepository.save(record);
                count++;
            } catch (Exception e) {
                log.error("重新计算话术版本效果评分失败: versionId={}", version.getId(), e);
            }
        }

        // 清除该产品的对比缓存
        cacheRepository.deleteByProductId(productId);

        log.info("重新计算产品效果评分完成: productId={}, count={}", productId, count);
        return count;
    }

    @Override
    public PageResultVO<ScriptRankingVO> getRanking(Long productId, Integer topN, String sortBy,
                                                     Integer page, Integer rows, Long userId) {
        // 构建查询条件
        Specification<ProductScriptVersion> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("productId"), productId),
                cb.equal(root.get("ownerId"), userId),
                cb.equal(root.get("deleted"), 0)
        );

        // 构建排序
        Sort.Order sortOrder;
        switch (sortBy != null ? sortBy : "score") {
            case "usage":
                sortOrder = new Sort.Order(Sort.Direction.DESC, "usageCount");
                break;
            case "conversion":
                sortOrder = new Sort.Order(Sort.Direction.DESC, "conversionRate");
                break;
            case "interaction":
                sortOrder = new Sort.Order(Sort.Direction.DESC, "likesCount");
                break;
            default:
                sortOrder = new Sort.Order(Sort.Direction.DESC, "effectivenessScore");
        }

        Pageable pageable = PageRequest.of(page, Math.min(rows, 1000), Sort.by(sortOrder));
        Page<ProductScriptVersion> versions = versionRepository.findAll(spec, pageable);

        // 构建排行榜
        List<ScriptRankingVO> rankingList = new ArrayList<>();
        int rank = page * rows + 1;
        for (ProductScriptVersion version : versions) {
            ScriptRankingVO rankingVO = ScriptRankingVO.builder()
                    .rank(rank++)
                    .versionId(version.getId())
                    .versionNumber(version.getVersionNumber())
                    .style(version.getStyle())
                    .score(version.getEffectivenessScore() != null ? version.getEffectivenessScore() : BigDecimal.ZERO)
                    .scoreLevel(determineScoreLevel(version.getEffectivenessScore() != null ?
                            version.getEffectivenessScore().doubleValue() : 0))
                    .usageCount(version.getUsageCount() != null ? version.getUsageCount() : 0)
                    .conversionRate(version.getConversionRate() != null ? version.getConversionRate() : BigDecimal.ZERO)
                    .likesCount(version.getLikesCount() != null ? version.getLikesCount() : 0)
                    .commentsCount(version.getCommentsCount() != null ? version.getCommentsCount() : 0)
                    .isRecommended(version.getIsRecommended() != null ? version.getIsRecommended() : false)
                    .lastUpdated(version.getUpdatedAt())
                    .rankingType("overall")
                    .build();
            rankingList.add(rankingVO);

            // 如果指定了 topN 限制，只取前 N 条
            if (topN != null && topN > 0 && rankingList.size() >= topN) {
                break;
            }
        }

        PageResultVO<ScriptRankingVO> result = PageResultVO.of(
                versions.getTotalElements(),
                rankingList,
                page,
                rows
        );

        return result;
    }

    @Override
    public ScriptComparisonVO compareVersions(List<Long> versionIds, Long userId) {
        if (versionIds == null || versionIds.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "版本 ID 列表不能为空");
        }
        if (versionIds.size() > 5) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "最多只能对比 5 个版本");
        }

        // 查询所有版本
        List<ProductScriptVersion> versions = versionRepository.findAllById(versionIds);
        if (versions.isEmpty()) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "版本不存在");
        }

        // 验证权限（所有版本都属于当前用户）
        for (ProductScriptVersion version : versions) {
            if (!version.getOwnerId().equals(userId)) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "无权限对比此版本");
            }
        }

        // 构建对比项目
        List<ScriptComparisonVO.ComparisonItem> items = new ArrayList<>();
        for (ProductScriptVersion version : versions) {
            ScriptComparisonVO.ComparisonItem item = ScriptComparisonVO.ComparisonItem.builder()
                    .versionId(version.getId())
                    .versionNumber(version.getVersionNumber())
                    .style(version.getStyle())
                    .score(version.getEffectivenessScore() != null ? version.getEffectivenessScore().doubleValue() : 0.0)
                    .scoreLevel(determineScoreLevel(version.getEffectivenessScore() != null ?
                            version.getEffectivenessScore().doubleValue() : 0))
                    .usageCount(version.getUsageCount() != null ? version.getUsageCount() : 0)
                    .conversionRate(version.getConversionRate() != null ? version.getConversionRate().doubleValue() : 0.0)
                    .likesCount(version.getLikesCount() != null ? version.getLikesCount() : 0)
                    .commentsCount(version.getCommentsCount() != null ? version.getCommentsCount() : 0)
                    .build();
            items.add(item);
        }

        // 找出最好的版本
        ScriptComparisonVO.ComparisonItem bestVersion = items.stream()
                .max(Comparator.comparingDouble(item -> item.getScore() != null ? item.getScore() : 0))
                .orElse(items.get(0));

        String recommendation = String.format("版本 %d 表现最好，评分 %.2f 分",
                bestVersion.getVersionNumber(), bestVersion.getScore());

        ScriptComparisonVO result = ScriptComparisonVO.builder()
                .versions(items)
                .comparisons(buildComparisonMetrics(items))
                .comparisonType("version_compare")
                .productId(versions.get(0).getProductId())
                .recommendation(recommendation)
                .timestamp(System.currentTimeMillis())
                .build();

        return result;
    }

    @Override
    public ScriptTrendVO getTrend(Long versionId, Integer days, Long userId) {
        ProductScriptVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术版本不存在"));

        if (!version.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "无权限访问此版本");
        }

        // 查询评分历史
        LocalDateTime startTime = LocalDateTime.now().minusDays(days != null ? days : 30);
        LocalDateTime endTime = LocalDateTime.now();

        List<ProductScriptEffectivenessRecord> records = recordRepository
                .findByScriptVersionIdOrderByCalculatedAtDesc(versionId).stream()
                .filter(r -> r.getCalculatedAt().isAfter(startTime) && r.getCalculatedAt().isBefore(endTime))
                .sorted(Comparator.comparing(ProductScriptEffectivenessRecord::getCalculatedAt))
                .collect(Collectors.toList());

        // 构建趋势点
        List<ScriptTrendVO.TrendPoint> trendPoints = new ArrayList<>();
        for (ProductScriptEffectivenessRecord record : records) {
            ScriptTrendVO.TrendPoint point = ScriptTrendVO.TrendPoint.builder()
                    .date(record.getCalculatedAt())
                    .score(record.getScoreValue())
                    .scoreLevel(record.getScoreLevel())
                    .usageCount(record.getUsageCountSnapshot())
                    .conversionRate(record.getConversionRateSnapshot())
                    .likesCount(record.getLikesSnapshot())
                    .build();
            trendPoints.add(point);
        }

        // 计算统计数据
        BigDecimal avgScore = BigDecimal.ZERO;
        BigDecimal maxScore = BigDecimal.ZERO;
        BigDecimal minScore = BigDecimal.valueOf(100);

        if (!records.isEmpty()) {
            avgScore = records.stream()
                    .map(ProductScriptEffectivenessRecord::getScoreValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(records.size()), 2, RoundingMode.HALF_UP);

            maxScore = records.stream()
                    .map(ProductScriptEffectivenessRecord::getScoreValue)
                    .max(Comparator.naturalOrder())
                    .orElse(BigDecimal.ZERO);

            minScore = records.stream()
                    .map(ProductScriptEffectivenessRecord::getScoreValue)
                    .min(Comparator.naturalOrder())
                    .orElse(BigDecimal.ZERO);
        }

        // 判断趋势
        String trendDirection = "稳定";
        if (records.size() > 1) {
            BigDecimal firstScore = records.get(0).getScoreValue();
            BigDecimal lastScore = records.get(records.size() - 1).getScoreValue();
            if (lastScore.compareTo(firstScore) > 0) {
                trendDirection = "上升";
            } else if (lastScore.compareTo(firstScore) < 0) {
                trendDirection = "下降";
            }
        }

        ScriptTrendVO result = ScriptTrendVO.builder()
                .trendPoints(trendPoints)
                .averageScore(avgScore)
                .maxScore(maxScore)
                .minScore(minScore)
                .trendDirection(trendDirection)
                .versionId(versionId)
                .days(days != null ? days : 30)
                .build();

        return result;
    }

    @Override
    public ScriptComparisonVO getStyleComparison(Long productId, Long userId) {
        // 检查缓存
        Optional<ProductScriptComparisonCache> cachedResult = cacheRepository
                .findByProductIdAndComparisonTypeAndOwnerIdAndDeleted(productId, "style_compare", userId, 0);

        if (cachedResult.isPresent() && !cachedResult.get().isExpired()) {
            try {
                return objectMapper.readValue(cachedResult.get().getComparisonData(), ScriptComparisonVO.class);
            } catch (Exception e) {
                log.warn("缓存反序列化失败，重新计算", e);
            }
        }

        // 查询产品的所有版本，按风格分组
        Specification<ProductScriptVersion> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("productId"), productId),
                cb.equal(root.get("ownerId"), userId),
                cb.equal(root.get("deleted"), 0)
        );

        List<ProductScriptVersion> allVersions = versionRepository.findAll(spec);
        Map<String, List<ProductScriptVersion>> versionsByStyle = allVersions.stream()
                .collect(Collectors.groupingBy(v -> v.getStyle() != null ? v.getStyle() : "未分类"));

        // 计算每个风格的平均分
        List<ScriptComparisonVO.ComparisonItem> styleItems = new ArrayList<>();
        for (Map.Entry<String, List<ProductScriptVersion>> entry : versionsByStyle.entrySet()) {
            List<ProductScriptVersion> styleVersions = entry.getValue();
            double avgScore = styleVersions.stream()
                    .mapToDouble(v -> v.getEffectivenessScore() != null ? v.getEffectivenessScore().doubleValue() : 0)
                    .average()
                    .orElse(0);

            ScriptComparisonVO.ComparisonItem item = ScriptComparisonVO.ComparisonItem.builder()
                    .style(entry.getKey())
                    .score(avgScore)
                    .scoreLevel(determineScoreLevel(avgScore))
                    .usageCount(styleVersions.stream()
                            .mapToInt(v -> v.getUsageCount() != null ? v.getUsageCount() : 0)
                            .sum())
                    .conversionRate(styleVersions.stream()
                            .mapToDouble(v -> v.getConversionRate() != null ? v.getConversionRate().doubleValue() : 0)
                            .average()
                            .orElse(0))
                    .likesCount(styleVersions.stream()
                            .mapToInt(v -> v.getLikesCount() != null ? v.getLikesCount() : 0)
                            .sum())
                    .commentsCount(styleVersions.stream()
                            .mapToInt(v -> v.getCommentsCount() != null ? v.getCommentsCount() : 0)
                            .sum())
                    .build();
            styleItems.add(item);
        }

        // 找出表现最好的风格
        ScriptComparisonVO.ComparisonItem bestStyle = styleItems.stream()
                .max(Comparator.comparingDouble(item -> item.getScore() != null ? item.getScore() : 0))
                .orElse(styleItems.get(0));

        String recommendation = String.format("风格 %s 表现最好，平均评分 %.2f 分",
                bestStyle.getStyle(), bestStyle.getScore());

        ScriptComparisonVO result = ScriptComparisonVO.builder()
                .versions(styleItems)
                .comparisons(buildComparisonMetrics(styleItems))
                .comparisonType("style_compare")
                .productId(productId)
                .recommendation(recommendation)
                .timestamp(System.currentTimeMillis())
                .build();

        // 存储到缓存
        try {
            String cacheData = objectMapper.writeValueAsString(result);
            ProductScriptComparisonCache cache = ProductScriptComparisonCache.builder()
                    .productId(productId)
                    .comparisonType("style_compare")
                    .comparisonData(cacheData)
                    .cachedAt(LocalDateTime.now())
                    .ttlMinutes(60)
                    .ownerId(userId)
                    .deleted(0)
                    .build();

            // 如果缓存已存在，更新；否则创建新的
            Optional<ProductScriptComparisonCache> existing = cacheRepository
                    .findByProductIdAndComparisonTypeAndOwnerIdAndDeleted(productId, "style_compare", userId, 0);
            if (existing.isPresent()) {
                cache.setId(existing.get().getId());
            }
            cacheRepository.save(cache);
        } catch (Exception e) {
            log.warn("缓存风格对比结果失败", e);
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean recordSnapshot(Long versionId, Long userId) {
        ProductScriptVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术版本不存在"));

        if (!version.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "无权限访问此版本");
        }

        // 计算最新的评分
        Double score = calculateScore(versionId, userId);
        String scoreLevel = determineScoreLevel(score);

        // 创建评分历史记录
        ProductScriptEffectivenessRecord record = ProductScriptEffectivenessRecord.builder()
                .productId(version.getProductId())
                .scriptVersionId(versionId)
                .calculatedAt(LocalDateTime.now())
                .scoreValue(BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP))
                .scoreLevel(scoreLevel)
                .usageCountSnapshot(version.getUsageCount() != null ? version.getUsageCount() : 0)
                .conversionRateSnapshot(version.getConversionRate() != null ? version.getConversionRate() : BigDecimal.ZERO)
                .likesSnapshot(version.getLikesCount() != null ? version.getLikesCount() : 0)
                .commentsSnapshot(version.getCommentsCount() != null ? version.getCommentsCount() : 0)
                .ownerId(userId)
                .deleted(0)
                .build();

        recordRepository.save(record);
        log.info("记录快照成功: versionId={}, score={}", versionId, score);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer clearComparisonCache(Long productId, Long userId) {
        // 验证产品所有权
        Specification<ProductScriptVersion> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("productId"), productId),
                cb.equal(root.get("ownerId"), userId),
                cb.equal(root.get("deleted"), 0)
        );

        long count = versionRepository.count(spec);
        if (count == 0) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "无权限操作此产品");
        }

        int deletedCount = cacheRepository.deleteByProductId(productId);
        log.info("清除对比缓存: productId={}, count={}", productId, deletedCount);
        return deletedCount;
    }

    @Override
    public ScriptEffectivenessAnalysisVO getAnalysis(Long versionId, Long userId) {
        ProductScriptVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术版本不存在"));

        if (!version.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "无权限访问此版本");
        }

        // 获取最近30天的趋势
        ScriptTrendVO trend = getTrend(versionId, 30, userId);

        LocalDateTime trendStartDate = trend.getTrendPoints().isEmpty() ?
                LocalDateTime.now().minusDays(30) : trend.getTrendPoints().get(0).getDate();
        LocalDateTime trendEndDate = trend.getTrendPoints().isEmpty() ?
                LocalDateTime.now() : trend.getTrendPoints().get(trend.getTrendPoints().size() - 1).getDate();

        // 计算评分变化
        BigDecimal scoreChange = BigDecimal.ZERO;
        BigDecimal scoreChangePercent = BigDecimal.ZERO;

        if (trend.getTrendPoints().size() >= 2) {
            BigDecimal firstScore = trend.getTrendPoints().get(0).getScore();
            BigDecimal lastScore = trend.getTrendPoints().get(trend.getTrendPoints().size() - 1).getScore();
            scoreChange = lastScore.subtract(firstScore);
            if (firstScore.compareTo(BigDecimal.ZERO) > 0) {
                scoreChangePercent = scoreChange.divide(firstScore, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
        }

        String recommendation = getRecommendation(trend.getTrendPoints().isEmpty() ?
                "D" : trend.getTrendPoints().get(trend.getTrendPoints().size() - 1).getScoreLevel());

        ScriptEffectivenessAnalysisVO result = ScriptEffectivenessAnalysisVO.builder()
                .overallTrend(trend.getTrendDirection())
                .scoreChange(scoreChange)
                .scoreChangePercent(scoreChangePercent)
                .trendStartDate(trendStartDate)
                .trendEndDate(trendEndDate)
                .currentScore(version.getEffectivenessScore() != null ? version.getEffectivenessScore() : BigDecimal.ZERO)
                .currentScoreLevel(determineScoreLevel(version.getEffectivenessScore() != null ?
                        version.getEffectivenessScore().doubleValue() : 0))
                .recommendation(recommendation)
                .analysisTime(LocalDateTime.now())
                .build();

        return result;
    }

    @Override
    public String determineScoreLevel(Double score) {
        if (score == null || score < 0) {
            return "F";
        }
        if (score >= 90) {
            return "A";
        } else if (score >= 80) {
            return "B";
        } else if (score >= 70) {
            return "C";
        } else if (score >= 60) {
            return "D";
        } else {
            return "F";
        }
    }

    @Override
    public String getRecommendation(String scoreLevel) {
        return switch (scoreLevel) {
            case "A" -> "表现优秀，保持现状或适度优化";
            case "B" -> "表现良好，可尝试微调优化";
            case "C" -> "表现一般，建议进行优化改进";
            case "D" -> "表现较差，需要重点优化";
            default -> "表现严重不足，建议重新创建版本";
        };
    }

    // ================ 私有辅助方法 ================

    /**
     * 构建对比维度的指标数据
     */
    private Map<String, Object> buildComparisonMetrics(List<ScriptComparisonVO.ComparisonItem> items) {
        Map<String, Object> metrics = new HashMap<>();

        if (items.isEmpty()) {
            return metrics;
        }

        // 计算各维度的对比
        double avgScore = items.stream()
                .mapToDouble(item -> item.getScore() != null ? item.getScore() : 0)
                .average()
                .orElse(0);

        int totalUsage = items.stream()
                .mapToInt(item -> item.getUsageCount() != null ? item.getUsageCount() : 0)
                .sum();

        double avgConversion = items.stream()
                .mapToDouble(item -> item.getConversionRate() != null ? item.getConversionRate() : 0)
                .average()
                .orElse(0);

        int totalInteraction = items.stream()
                .mapToInt(item -> (item.getLikesCount() != null ? item.getLikesCount() : 0) +
                        (item.getCommentsCount() != null ? item.getCommentsCount() : 0))
                .sum();

        metrics.put("averageScore", avgScore);
        metrics.put("totalUsage", totalUsage);
        metrics.put("averageConversion", avgConversion);
        metrics.put("totalInteraction", totalInteraction);
        metrics.put("itemCount", items.size());

        return metrics;
    }
}
