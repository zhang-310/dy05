package cn.gaifan.douyinOperations.module.benchmark.service.impl;

import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkAnalysis;
import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkQualityScript;
import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkVideo;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkAnalysisRepository;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkQualityScriptRepository;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkVideoRepository;
import cn.gaifan.douyinOperations.module.benchmark.service.BenchmarkScriptAutoIngestService;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkQualityScriptVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 质量脚本自动入库服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BenchmarkScriptAutoIngestServiceImpl implements BenchmarkScriptAutoIngestService {

    private final BenchmarkAnalysisRepository analysisRepository;
    private final BenchmarkVideoRepository videoRepository;
    private final BenchmarkQualityScriptRepository qualityScriptRepository;
    private final ObjectMapper objectMapper;

    // 默认入库阈值：质量评分 >= 70.0
    private final AtomicReference<BigDecimal> ingestThreshold = new AtomicReference<>(BigDecimal.valueOf(70.0));

    @Override
    @Transactional
    public BenchmarkQualityScriptVO autoIngestAfterAnalysis(BenchmarkAnalysis analysis, Long ownerId) {
        log.info("开始自动入库检查: analysisId={}, videoId={}, ownerId={}",
                analysis.getId(), analysis.getBenchmarkVideoId(), ownerId);

        // 检查是否符合入库条件
        if (!isQualifiedForIngest(analysis)) {
            log.info("不符合入库条件，跳过: analysisId={}, qualityScore={}, threshold={}",
                    analysis.getId(), extractQualityScore(analysis), getIngestThreshold());
            return null;
        }

        // 检查是否已入库
        if (qualityScriptRepository.findByAnalysisIdAndDeleted(analysis.getId(), 0).isPresent()) {
            log.info("该分析结果已入库，跳过: analysisId={}", analysis.getId());
            return null;
        }

        // 检查视频是否已有质量脚本
        if (qualityScriptRepository.findByVideoIdAndDeleted(analysis.getBenchmarkVideoId(), 0).isPresent()) {
            log.info("该视频已有质量脚本，跳过: videoId={}", analysis.getBenchmarkVideoId());
            return null;
        }

        // 获取视频信息
        BenchmarkVideo video = videoRepository.findById(analysis.getBenchmarkVideoId())
                .orElseThrow(() -> new RuntimeException("视频不存在"));

        // 创建质量脚本
        BenchmarkQualityScript script = new BenchmarkQualityScript();
        script.setOwnerId(ownerId);
        script.setVideoId(video.getId());
        script.setAnalysisId(analysis.getId());

        // 提取脚本内容（优先使用合并后的文案）
        String scriptContent = extractScriptContent(analysis);
        script.setScriptContent(scriptContent);

        // 设置分类信息
        script.setScriptType(determineScriptType(analysis));
        script.setSceneType(determineSceneType(analysis));

        // 计算质量评分
        BigDecimal qualityScore = extractQualityScore(analysis);
        script.setQualityScore(qualityScore);

        // 提取互动数据
        script.setLikesCount(video.getLikeCount());
        script.setCommentsCount(video.getCommentCount());
        script.setSharesCount(video.getShareCount());
        script.setCollectionsCount(video.getFavoriteCount());
        script.setViewsCount(video.getViewCount() != null ? video.getViewCount().intValue() : 0);
        script.setVideoDuration(video.getDuration());

        // 计算互动率
        BigDecimal engagementRate = calculateEngagementRate(video);
        script.setEngagementRate(engagementRate);

        // 计算传播力评分
        BigDecimal viralScore = calculateViralScore(video);
        script.setViralScore(viralScore);

        // 计算完播率（如果有数据）
        BigDecimal completionRate = calculateCompletionRate(video);
        script.setCompletionRate(completionRate);

        // 提取 AI 分析结果
        extractAiAnalysisData(analysis, script);

        // 保存
        script = qualityScriptRepository.save(script);

        log.info("自动入库成功: scriptId={}, videoId={}, qualityScore={}",
                script.getId(), script.getVideoId(), script.getQualityScore());

        // 转换为 VO
        BenchmarkQualityScriptVO vo = new BenchmarkQualityScriptVO();
        BeanUtils.copyProperties(script, vo);
        return vo;
    }

    @Override
    @Transactional
    public Integer batchAutoIngest(List<Long> analysisIds, Long ownerId) {
        log.info("开始批量自动入库: count={}, ownerId={}", analysisIds.size(), ownerId);

        int successCount = 0;
        for (Long analysisId : analysisIds) {
            try {
                BenchmarkAnalysis analysis = analysisRepository.findById(analysisId)
                        .orElseThrow(() -> new RuntimeException("分析结果不存在: " + analysisId));

                BenchmarkQualityScriptVO result = autoIngestAfterAnalysis(analysis, ownerId);
                if (result != null) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("自动入库失败: analysisId={}, error={}", analysisId, e.getMessage(), e);
            }
        }

        log.info("批量自动入库完成: total={}, success={}", analysisIds.size(), successCount);
        return successCount;
    }

    @Override
    public boolean isQualifiedForIngest(BenchmarkAnalysis analysis) {
        // 检查是否有脚本内容
        String scriptContent = extractScriptContent(analysis);
        if (!StringUtils.hasText(scriptContent)) {
            return false;
        }

        // 检查质量评分是否达到阈值
        BigDecimal qualityScore = extractQualityScore(analysis);
        return qualityScore.compareTo(getIngestThreshold()) >= 0;
    }

    @Override
    public BigDecimal extractQualityScore(BenchmarkAnalysis analysis) {
        // 从分析结果中提取质量评分
        // 综合考虑：互动率、传播力、AI 评分等

        try {
            // 获取视频信息
            BenchmarkVideo video = videoRepository.findById(analysis.getBenchmarkVideoId())
                    .orElse(null);

            if (video == null) {
                return BigDecimal.valueOf(50.0);
            }

            BigDecimal score = BigDecimal.ZERO;
            int weightCount = 0;

            // 互动率评分（30%）
            BigDecimal engagementRate = calculateEngagementRate(video);
            if (engagementRate != null && engagementRate.compareTo(BigDecimal.ZERO) > 0) {
                score = score.add(engagementRate.multiply(BigDecimal.valueOf(0.3)));
                weightCount++;
            }

            // 传播力评分（25%）
            BigDecimal viralScore = calculateViralScore(video);
            if (viralScore != null && viralScore.compareTo(BigDecimal.ZERO) > 0) {
                score = score.add(viralScore.multiply(BigDecimal.valueOf(0.25)));
                weightCount++;
            }

            // 完播率评分（20%）
            BigDecimal completionRate = calculateCompletionRate(video);
            if (completionRate != null && completionRate.compareTo(BigDecimal.ZERO) > 0) {
                score = score.add(completionRate.multiply(BigDecimal.valueOf(0.2)));
                weightCount++;
            }

            // AI 评分（25%）- 从分析结果中提取
            BigDecimal aiRating = extractAiRating(analysis);
            if (aiRating != null && aiRating.compareTo(BigDecimal.ZERO) > 0) {
                score = score.add(aiRating.multiply(BigDecimal.valueOf(0.25)));
                weightCount++;
            }

            // 如果没有任何评分数据，返回默认值
            if (weightCount == 0) {
                return BigDecimal.valueOf(50.0);
            }

            // 归一化到 0-100 范围
            BigDecimal normalizedScore = score.min(BigDecimal.valueOf(100.0)).max(BigDecimal.ZERO);
            return normalizedScore.setScale(2, RoundingMode.HALF_UP);

        } catch (Exception e) {
            log.error("提取质量评分失败: analysisId={}, error={}", analysis.getId(), e.getMessage());
            return BigDecimal.valueOf(50.0);
        }
    }

    @Override
    public BigDecimal getIngestThreshold() {
        return ingestThreshold.get();
    }

    @Override
    public void setIngestThreshold(BigDecimal threshold) {
        if (threshold.compareTo(BigDecimal.ZERO) < 0 || threshold.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("阈值必须在 0-100 之间");
        }
        ingestThreshold.set(threshold);
        log.info("入库阈值已更新: {}", threshold);
    }

    /**
     * 提取脚本内容
     */
    private String extractScriptContent(BenchmarkAnalysis analysis) {
        // 优先使用合并后的文案
        if (StringUtils.hasText(analysis.getMergedContent())) {
            return analysis.getMergedContent();
        }

        // 其次使用 ASR 文案
        if (StringUtils.hasText(analysis.getTranscriptText())) {
            return analysis.getTranscriptText();
        }

        // 最后使用 API 文案
        if (StringUtils.hasText(analysis.getApiDescription())) {
            return analysis.getApiDescription();
        }

        return "";
    }

    /**
     * 确定脚本类型
     */
    private String determineScriptType(BenchmarkAnalysis analysis) {
        // 从 AI 分析结果中提取脚本类型
        try {
            if (StringUtils.hasText(analysis.getCreativeType())) {
                return analysis.getCreativeType();
            }
        } catch (Exception e) {
            log.debug("提取脚本类型失败: {}", e.getMessage());
        }
        return "general";
    }

    /**
     * 确定场景类型
     */
    private String determineSceneType(BenchmarkAnalysis analysis) {
        // 从 AI 分析结果中提取场景类型
        try {
            if (StringUtils.hasText(analysis.getSceneDescription())) {
                // 简单解析场景描述，提取场景类型
                String desc = analysis.getSceneDescription().toLowerCase();
                if (desc.contains("product")) return "product_showcase";
                if (desc.contains("tutorial")) return "tutorial";
                if (desc.contains("story")) return "storytelling";
                if (desc.contains("review")) return "review";
            }
        } catch (Exception e) {
            log.debug("提取场景类型失败: {}", e.getMessage());
        }
        return "product_showcase";
    }

    /**
     * 计算互动率
     */
    private BigDecimal calculateEngagementRate(BenchmarkVideo video) {
        if (video.getViewCount() == null || video.getViewCount() == 0) {
            return BigDecimal.ZERO;
        }

        int totalEngagement = (video.getLikeCount() != null ? video.getLikeCount() : 0)
                + (video.getCommentCount() != null ? video.getCommentCount() : 0)
                + (video.getShareCount() != null ? video.getShareCount() : 0)
                + (video.getFavoriteCount() != null ? video.getFavoriteCount() : 0);

        BigDecimal rate = BigDecimal.valueOf(totalEngagement)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(video.getViewCount()), 2, RoundingMode.HALF_UP);

        return rate.min(BigDecimal.valueOf(100.0));
    }

    /**
     * 计算传播力评分
     */
    private BigDecimal calculateViralScore(BenchmarkVideo video) {
        if (video.getViewCount() == null || video.getViewCount() == 0) {
            return BigDecimal.ZERO;
        }

        // 传播力 = (分享数 * 3 + 收藏数 * 2) / 播放数 * 100
        int viralActions = (video.getShareCount() != null ? video.getShareCount() * 3 : 0)
                + (video.getFavoriteCount() != null ? video.getFavoriteCount() * 2 : 0);

        BigDecimal score = BigDecimal.valueOf(viralActions)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(video.getViewCount()), 2, RoundingMode.HALF_UP);

        return score.min(BigDecimal.valueOf(100.0));
    }

    /**
     * 计算完播率
     */
    private BigDecimal calculateCompletionRate(BenchmarkVideo video) {
        // 如果有完播率数据，直接返回
        // 否则根据互动数据估算
        if (video.getViewCount() == null || video.getViewCount() == 0) {
            return BigDecimal.ZERO;
        }

        // 简单估算：高互动率通常意味着高完播率
        BigDecimal engagementRate = calculateEngagementRate(video);
        return engagementRate.multiply(BigDecimal.valueOf(0.8)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 提取 AI 评分
     */
    private BigDecimal extractAiRating(BenchmarkAnalysis analysis) {
        try {
            if (StringUtils.hasText(analysis.getViralFactors())) {
                JsonNode node = objectMapper.readTree(analysis.getViralFactors());
                if (node.has("overallRating")) {
                    return BigDecimal.valueOf(node.get("overallRating").asDouble());
                }
            }
        } catch (Exception e) {
            log.debug("提取 AI 评分失败: {}", e.getMessage());
        }
        return BigDecimal.valueOf(75.0); // 默认评分
    }

    /**
     * 提取 AI 分析数据
     */
    private void extractAiAnalysisData(BenchmarkAnalysis analysis, BenchmarkQualityScript script) {
        try {
            // 提取关键特征（从创意类型和钩子策略）
            if (StringUtils.hasText(analysis.getCreativeType())) {
                script.setKeyFeatures("{\"creativeType\": \"" + analysis.getCreativeType() + "\"}");
            }

            // 提取创意元素（从爆款因素）
            if (StringUtils.hasText(analysis.getViralFactors())) {
                script.setCreativeElements(analysis.getViralFactors());
            }

            // 提取钩子策略
            if (StringUtils.hasText(analysis.getHookStrategy())) {
                script.setHookStrategy(analysis.getHookStrategy());
            }

            // 提取内容结构
            if (StringUtils.hasText(analysis.getContentStructure())) {
                script.setContentStructure(analysis.getContentStructure());
            }

            // 提取 AI 评分
            BigDecimal aiRating = extractAiRating(analysis);
            script.setAiRating(aiRating);

        } catch (Exception e) {
            log.error("提取 AI 分析数据失败: analysisId=, error={}", analysis.getId(), e.getMessage());
        }
    }
}
