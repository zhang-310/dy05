package cn.gaifan.douyinOperations.module.benchmark.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkAnalysis;
import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkQualityScript;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkAnalysisRepository;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkQualityScriptRepository;
import cn.gaifan.douyinOperations.module.benchmark.service.BenchmarkScriptRecommendationService;
import cn.gaifan.douyinOperations.module.benchmark.service.BenchmarkScriptSimilarityService;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkScriptSimilarityVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 质量脚本推荐引擎服务实现
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class BenchmarkScriptRecommendationServiceImpl implements BenchmarkScriptRecommendationService {

    @Autowired
    private BenchmarkScriptSimilarityService similarityService;

    //@Autowired
    //private BenchmarkQualityScriptService qualityScriptService;

    @Autowired
    private BenchmarkQualityScriptRepository qualityScriptRepository;

    @Autowired
    private BenchmarkAnalysisRepository analysisRepository;

    private static final Double DEFAULT_MIN_SIMILARITY = 0.7;
    private static final Double DEFAULT_MIN_QUALITY = 70.0;

    /**
     * 根据用户需求推荐脚本
     */
    @Override
    public List<BenchmarkScriptSimilarityVO> recommendByRequirement(String requirement, Long ownerId, Integer topK) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.info("[{}] 根据需求推荐脚本: requirement={}, topK={}", traceId, requirement, topK);

        try {
            // 使用语义相似度搜索
            List<BenchmarkScriptSimilarityVO> results = similarityService.findSimilarScriptsByText(
                    requirement, ownerId, topK != null ? topK : 10, DEFAULT_MIN_SIMILARITY);

            // 按质量评分和相似度综合排序
            results.sort((a, b) -> {
                double scoreA = calculateRecommendScore(a);
                double scoreB = calculateRecommendScore(b);
                return Double.compare(scoreB, scoreA);
            });

            log.info("[{}] 推荐完成: count={}", traceId, results.size());
            return results;

        } catch (Exception e) {
            log.error("[{}] 推荐失败: error={}", traceId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "推荐失败: " + e.getMessage());
        }
    }

    /**
     * 根据行业和场景推荐脚本
     */
    @Override
    public List<BenchmarkScriptSimilarityVO> recommendByIndustryAndScene(String industry, String sceneType, Long ownerId, Integer topK) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.info("[{}] 根据行业场景推荐: industry={}, sceneType={}", traceId, industry, sceneType);

        try {
            Specification<BenchmarkQualityScript> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("ownerId"), ownerId));

                if (industry != null && !industry.isEmpty()) {
                    predicates.add(cb.equal(root.get("industry"), industry));
                }
                if (sceneType != null && !sceneType.isEmpty()) {
                    predicates.add(cb.equal(root.get("sceneType"), sceneType));
                }

                // 质量评分过滤
                predicates.add(cb.greaterThanOrEqualTo(root.get("qualityScore"), BigDecimal.valueOf(DEFAULT_MIN_QUALITY)));

                return cb.and(predicates.toArray(new Predicate[0]));
            };

            // 按质量评分降序排序
            PageRequest pageRequest = PageRequest.of(0, topK != null ? topK : 10,
                    Sort.by(Sort.Direction.DESC, "qualityScore"));

            List<BenchmarkQualityScript> scripts = qualityScriptRepository.findAll(spec, pageRequest).getContent();

            List<BenchmarkScriptSimilarityVO> results = scripts.stream()
                    .map(this::convertToSimilarityVO)
                    .collect(Collectors.toList());

            log.info("[{}] 推荐完成: count={}", traceId, results.size());
            return results;

        } catch (Exception e) {
            log.error("[{}] 推荐失败: error={}", traceId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "推荐失败: " + e.getMessage());
        }
    }

    /**
     * 根据脚本类型推荐相似脚本
     */
    @Override
    public List<BenchmarkScriptSimilarityVO> recommendByScriptType(String scriptType, Long referenceScriptId, Long ownerId, Integer topK) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.info("[{}] 根据脚本类型推荐: scriptType={}, referenceScriptId={}", traceId, scriptType, referenceScriptId);

        try {
            List<BenchmarkScriptSimilarityVO> results;

            if (referenceScriptId != null) {
                // 基于参考脚本的相似度推荐
                results = similarityService.findSimilarScripts(referenceScriptId, ownerId, topK != null ? topK : 10, DEFAULT_MIN_SIMILARITY);
            } else {
                // 基于脚本类型的推荐
                Specification<BenchmarkQualityScript> spec = (root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    predicates.add(cb.equal(root.get("ownerId"), ownerId));
                    predicates.add(cb.equal(root.get("scriptType"), scriptType));
                    predicates.add(cb.greaterThanOrEqualTo(root.get("qualityScore"), BigDecimal.valueOf(DEFAULT_MIN_QUALITY)));
                    return cb.and(predicates.toArray(new Predicate[0]));
                };

                PageRequest pageRequest = PageRequest.of(0, topK != null ? topK : 10,
                        Sort.by(Sort.Direction.DESC, "qualityScore"));

                List<BenchmarkQualityScript> scripts = qualityScriptRepository.findAll(spec, pageRequest).getContent();
                results = scripts.stream().map(this::convertToSimilarityVO).collect(Collectors.toList());
            }

            log.info("[{}] 推荐完成: count={}", traceId, results.size());
            return results;

        } catch (Exception e) {
            log.error("[{}] 推荐失败: error={}", traceId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "推荐失败: " + e.getMessage());
        }
    }

    /**
     * 智能推荐：综合多维度推荐
     */
    @Override
    public List<BenchmarkScriptSimilarityVO> smartRecommend(Map<String, Object> filters, String referenceText, Long ownerId, Integer topK) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.info("[{}] 智能推荐: filters={}, hasReferenceText={}", traceId, filters, referenceText != null);

        try {
            List<BenchmarkScriptSimilarityVO> candidates = new ArrayList<>();

            // 1. 如果有参考文本，先进行语义搜索
            if (referenceText != null && !referenceText.isEmpty()) {
                candidates = similarityService.findSimilarScriptsByText(referenceText, ownerId, 50, 0.6);
            } else {
                // 2. 否则根据过滤条件查询
                Specification<BenchmarkQualityScript> spec = buildFilterSpec(filters, ownerId);
                List<BenchmarkQualityScript> scripts = qualityScriptRepository.findAll(spec);
                candidates = scripts.stream().map(this::convertToSimilarityVO).collect(Collectors.toList());
            }

            // 3. 应用过滤条件
            candidates = applyFilters(candidates, filters);

            // 4. 综合评分排序
            candidates.sort((a, b) -> {
                double scoreA = calculateSmartScore(a, filters);
                double scoreB = calculateSmartScore(b, filters);
                return Double.compare(scoreB, scoreA);
            });

            // 5. 限制返回数量
            int limit = topK != null ? topK : 10;
            List<BenchmarkScriptSimilarityVO> results = candidates.stream().limit(limit).collect(Collectors.toList());

            log.info("[{}] 智能推荐完成: count={}", traceId, results.size());
            return results;

        } catch (Exception e) {
            log.error("[{}] 智能推荐失败: error={}", traceId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "智能推荐失败: " + e.getMessage());
        }
    }

    /**
     * 获取热门脚本推荐
     */
    @Override
    public List<BenchmarkScriptSimilarityVO> getPopularScripts(Long ownerId, Integer topK) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.info("[{}] 获取热门脚本: topK={}", traceId, topK);

        try {
            Specification<BenchmarkQualityScript> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("ownerId"), ownerId));
                predicates.add(cb.greaterThanOrEqualTo(root.get("qualityScore"), BigDecimal.valueOf(DEFAULT_MIN_QUALITY)));
                return cb.and(predicates.toArray(new Predicate[0]));
            };

            // 按引用次数和质量评分综合排序
            List<BenchmarkQualityScript> scripts = qualityScriptRepository.findAll(spec);

            List<BenchmarkScriptSimilarityVO> results = scripts.stream()
                    .map(this::convertToSimilarityVO)
                    .sorted((a, b) -> {
                        double scoreA = calculatePopularityScore(a);
                        double scoreB = calculatePopularityScore(b);
                        return Double.compare(scoreB, scoreA);
                    })
                    .limit(topK != null ? topK : 10)
                    .collect(Collectors.toList());

            log.info("[{}] 热门脚本推荐完成: count={}", traceId, results.size());
            return results;

        } catch (Exception e) {
            log.error("[{}] 获取热门脚本失败: error={}", traceId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取热门脚本失败: " + e.getMessage());
        }
    }

    /**
     * 获取最新高质量脚本推荐
     */
    @Override
    public List<BenchmarkScriptSimilarityVO> getLatestQualityScripts(Long ownerId, Integer topK, Double minQualityScore) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.info("[{}] 获取最新高质量脚本: topK={}, minQualityScore={}", traceId, topK, minQualityScore);

        try {
            double minScore = minQualityScore != null ? minQualityScore : DEFAULT_MIN_QUALITY;

            Specification<BenchmarkQualityScript> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("ownerId"), ownerId));
                predicates.add(cb.greaterThanOrEqualTo(root.get("qualityScore"), BigDecimal.valueOf(minScore)));
                return cb.and(predicates.toArray(new Predicate[0]));
            };

            PageRequest pageRequest = PageRequest.of(0, topK != null ? topK : 10,
                    Sort.by(Sort.Direction.DESC, "createTime"));

            List<BenchmarkQualityScript> scripts = qualityScriptRepository.findAll(spec, pageRequest).getContent();

            List<BenchmarkScriptSimilarityVO> results = scripts.stream()
                    .map(this::convertToSimilarityVO)
                    .collect(Collectors.toList());

            log.info("[{}] 最新高质量脚本推荐完成: count={}", traceId, results.size());
            return results;

        } catch (Exception e) {
            log.error("[{}] 获取最新高质量脚本失败: error={}", traceId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取最新高质量脚本失败: " + e.getMessage());
        }
    }

    /**
     * 根据视频分析结果推荐改进脚本
     */
    @Override
    public List<BenchmarkScriptSimilarityVO> recommendImprovementScripts(Long analysisId, Long ownerId, Integer topK) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.info("[{}] 根据分析推荐改进脚本: analysisId={}", traceId, analysisId);

        try {
            // 查询分析结果（BenchmarkAnalysis 没有 ownerId，通过 video 关联验证）
            BenchmarkAnalysis analysis = analysisRepository.findById(analysisId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "分析结果不存在"));

            // 提取分析内容作为参考
            String referenceText = buildReferenceTextFromAnalysis(analysis);

            // 使用语义相似度搜索
            List<BenchmarkScriptSimilarityVO> results = similarityService.findSimilarScriptsByText(
                    referenceText, ownerId, topK != null ? topK : 10, DEFAULT_MIN_SIMILARITY);

            // 过滤掉质量评分低于当前视频的脚本
            results = results.stream()
                    .filter(vo -> vo.getQualityScore() != null && vo.getQualityScore().doubleValue() > 70.0)
                    .collect(Collectors.toList());

            log.info("[{}] 改进脚本推荐完成: count={}", traceId, results.size());
            return results;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[{}] 推荐改进脚本失败: error={}", traceId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "推荐改进脚本失败: " + e.getMessage());
        }
    }

    // ========== 私有方法 ==========

    /**
     * 计算推荐评分（相似度 + 质量评分）
     */
    private double calculateRecommendScore(BenchmarkScriptSimilarityVO vo) {
        double similarityScore = vo.getSimilarityScore() != null ? vo.getSimilarityScore() : 0.0;
        double qualityScore = vo.getQualityScore() != null ? vo.getQualityScore().doubleValue() / 100.0 : 0.0;

        // 相似度权重 60%，质量评分权重 40%
        return similarityScore * 0.6 + qualityScore * 0.4;
    }

    /**
     * 计算热度评分（引用次数 + 质量评分 + 互动数据）
     */
    private double calculatePopularityScore(BenchmarkScriptSimilarityVO vo) {
        double qualityScore = vo.getQualityScore() != null ? vo.getQualityScore().doubleValue() / 100.0 : 0.0;
        double engagementScore = vo.getEngagementRate() != null ? vo.getEngagementRate().doubleValue() / 100.0 : 0.0;
        double viralScore = vo.getViralScore() != null ? vo.getViralScore().doubleValue() / 100.0 : 0.0;

        // 质量 40%，互动 30%，传播 30%
        return qualityScore * 0.4 + engagementScore * 0.3 + viralScore * 0.3;
    }

    /**
     * 计算智能推荐评分
     */
    private double calculateSmartScore(BenchmarkScriptSimilarityVO vo, Map<String, Object> filters) {
        double baseScore = calculateRecommendScore(vo);

        // 根据过滤条件调整权重
        double adjustedScore = baseScore;

        // 如果指定了行业或场景，匹配的脚本加分
        if (filters.containsKey("industry") && vo.getIndustry() != null) {
            if (vo.getIndustry().equals(filters.get("industry"))) {
                adjustedScore += 0.1;
            }
        }

        if (filters.containsKey("sceneType") && vo.getSceneType() != null) {
            if (vo.getSceneType().equals(filters.get("sceneType"))) {
                adjustedScore += 0.1;
            }
        }

        return adjustedScore;
    }

    /**
     * 构建过滤条件 Specification
     */
    private Specification<BenchmarkQualityScript> buildFilterSpec(Map<String, Object> filters, Long ownerId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("ownerId"), ownerId));

            if (filters.containsKey("industry")) {
                predicates.add(cb.equal(root.get("industry"), filters.get("industry")));
            }

            if (filters.containsKey("sceneType")) {
                predicates.add(cb.equal(root.get("sceneType"), filters.get("sceneType")));
            }

            if (filters.containsKey("scriptType")) {
                predicates.add(cb.equal(root.get("scriptType"), filters.get("scriptType")));
            }

            if (filters.containsKey("minQualityScore")) {
                double minScore = Double.parseDouble(filters.get("minQualityScore").toString());
                predicates.add(cb.greaterThanOrEqualTo(root.get("qualityScore"), BigDecimal.valueOf(minScore)));
            } else {
                predicates.add(cb.greaterThanOrEqualTo(root.get("qualityScore"), BigDecimal.valueOf(DEFAULT_MIN_QUALITY)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 应用过滤条件
     */
    private List<BenchmarkScriptSimilarityVO> applyFilters(List<BenchmarkScriptSimilarityVO> candidates, Map<String, Object> filters) {
        return candidates.stream()
                .filter(vo -> {
                    if (filters.containsKey("industry") && vo.getIndustry() != null) {
                        if (!vo.getIndustry().equals(filters.get("industry"))) {
                            return false;
                        }
                    }

                    if (filters.containsKey("sceneType") && vo.getSceneType() != null) {
                        if (!vo.getSceneType().equals(filters.get("sceneType"))) {
                            return false;
                        }
                    }

                    if (filters.containsKey("scriptType") && vo.getScriptType() != null) {
                        if (!vo.getScriptType().equals(filters.get("scriptType"))) {
                            return false;
                        }
                    }

                    if (filters.containsKey("minQualityScore") && vo.getQualityScore() != null) {
                        double minScore = Double.parseDouble(filters.get("minQualityScore").toString());
                        if (vo.getQualityScore().doubleValue() < minScore) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * 从分析结果构建参考文本
     */
    private String buildReferenceTextFromAnalysis(BenchmarkAnalysis analysis) {
        StringBuilder sb = new StringBuilder();

        // 合并后的脚本内容
        if (analysis.getMergedContent() != null && !analysis.getMergedContent().isEmpty()) {
            sb.append(analysis.getMergedContent()).append(" ");
        }

        // 创意类型
        if (analysis.getCreativeType() != null && !analysis.getCreativeType().isEmpty()) {
            sb.append("创意类型: ").append(analysis.getCreativeType()).append(" ");
        }

        // 钩子策略
        if (analysis.getHookStrategy() != null && !analysis.getHookStrategy().isEmpty()) {
            sb.append("钩子策略: ").append(analysis.getHookStrategy()).append(" ");
        }

        // 内容结构
        if (analysis.getContentStructure() != null && !analysis.getContentStructure().isEmpty()) {
            sb.append("内容结构: ").append(analysis.getContentStructure()).append(" ");
        }

        return sb.toString().trim();
    }

    /**
     * 转换为相似度 VO
     */
    private BenchmarkScriptSimilarityVO convertToSimilarityVO(BenchmarkQualityScript script) {
        BenchmarkScriptSimilarityVO vo = new BenchmarkScriptSimilarityVO();
        vo.setScriptId(script.getId());
        vo.setVideoId(script.getVideoId());
        vo.setScriptContent(truncateContent(script.getScriptContent(), 200));
        vo.setScriptType(script.getScriptType());
        vo.setIndustry(script.getIndustry());
        vo.setSceneType(script.getSceneType());
        vo.setQualityScore(script.getQualityScore());
        vo.setSimilarityScore(null); // 非相似度搜索时为 null
        vo.setEngagementRate(script.getEngagementRate());
        vo.setViralScore(script.getViralScore());
        vo.setLikesCount(script.getLikesCount());
        vo.setCommentsCount(script.getCommentsCount());
        vo.setSharesCount(script.getSharesCount());
        vo.setCollectionsCount(script.getCollectionsCount());
        vo.setViewsCount(script.getViewsCount());
        return vo;
    }

    /**
     * 截断内容
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) {
            return null;
        }
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
}

