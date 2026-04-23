package cn.gaifan.douyinOperations.module.product.service.impl;

import cn.gaifan.douyinOperations.module.product.entity.ScriptAnalysisResult;
import cn.gaifan.douyinOperations.module.product.entity.ScriptOptimizationSuggestion;
import cn.gaifan.douyinOperations.module.product.entity.ScriptRegeneratedVersion;
import cn.gaifan.douyinOperations.module.product.repository.ScriptAnalysisResultRepository;
import cn.gaifan.douyinOperations.module.product.repository.ScriptOptimizationSuggestionRepository;
import cn.gaifan.douyinOperations.module.product.repository.ScriptRegeneratedVersionRepository;
import cn.gaifan.douyinOperations.module.product.service.ScriptOptimizationService;
import cn.gaifan.douyinOperations.module.product.vo.OptimizationSuggestionVO;
import cn.gaifan.douyinOperations.module.product.vo.RegeneratedScriptVO;
import cn.gaifan.douyinOperations.module.product.vo.ScriptAnalysisResultVO;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 话术优化建议服务实现
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Slf4j
@Service
public class ScriptOptimizationServiceImpl implements ScriptOptimizationService {

    private final ScriptAnalysisResultRepository analysisResultRepository;
    private final ScriptOptimizationSuggestionRepository suggestionRepository;
    private final ScriptRegeneratedVersionRepository regeneratedVersionRepository;
    private final ObjectMapper objectMapper;

    public ScriptOptimizationServiceImpl(
            ScriptAnalysisResultRepository analysisResultRepository,
            ScriptOptimizationSuggestionRepository suggestionRepository,
            ScriptRegeneratedVersionRepository regeneratedVersionRepository,
            ObjectMapper objectMapper) {
        this.analysisResultRepository = analysisResultRepository;
        this.suggestionRepository = suggestionRepository;
        this.regeneratedVersionRepository = regeneratedVersionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public ScriptAnalysisResultVO analyzeScript(Long scriptVersionId, String dataSource,
                                                String analysisType, Long userId) {
        log.info("开始分析话术：scriptVersionId={}, userId={}, analysisType={}", scriptVersionId, userId, analysisType);

        // 验证参数
        if (scriptVersionId == null || scriptVersionId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "话术版本 ID 无效");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "用户 ID 无效");
        }

        try {
            // 创建分析结果记录（实际实现中应从监控数据或历史数据读取）
            ScriptAnalysisResult analysisResult = ScriptAnalysisResult.builder()
                    .scriptVersionId(scriptVersionId)
                    .ownerId(userId)
                    .overallScore(new BigDecimal("72.50"))
                    .interactionRate(new BigDecimal("18.50"))
                    .conversionRate(new BigDecimal("12.30"))
                    .fanGrowth(254)
                    .commentSentiment(new BigDecimal("0.68"))
                    .dominantStyle("FRIENDLY")
                    .analysisType(analysisType)
                    .dataSource(dataSource)
                    .weakPoints(generateWeakPointsJson())
                    .deleted(0)
                    .build();

            ScriptAnalysisResult saved = analysisResultRepository.save(analysisResult);
            log.info("话术分析完成：analysisResultId={}", saved.getId());

            return convertToAnalysisVO(saved);
        } catch (Exception e) {
            log.error("话术分析失败：scriptVersionId={}, error={}", scriptVersionId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_BUSY, "话术分析失败");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OptimizationSuggestionVO> getOptimizationSuggestions(Long scriptVersionId, Long analysisResultId,
                                                                     Integer topN, Long userId) {
        log.info("获取优化建议：scriptVersionId={}, analysisResultId={}, userId={}",
                scriptVersionId, analysisResultId, userId);

        if (analysisResultId == null || analysisResultId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "分析结果 ID 无效");
        }
        if (topN == null || topN <= 0) {
            topN = 10;
        }

        // 验证分析结果存在且属于该用户
        Optional<ScriptAnalysisResult> analysisOpt = analysisResultRepository.findById(analysisResultId);
        if (analysisOpt.isEmpty() || !analysisOpt.get().getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "分析结果不存在或无权访问");
        }

        // 获取高优先级建议
        List<ScriptOptimizationSuggestion> suggestions =
                suggestionRepository.findHighPrioritySuggestions(analysisResultId, userId);

        if (suggestions.isEmpty()) {
            // 如果没有建议，生成默认建议（实际实现中应由 AI 生成）
            suggestions = generateDefaultSuggestions(scriptVersionId, analysisResultId, userId);
        }

        return suggestions.stream()
                .limit(topN)
                .map(this::convertToSuggestionVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<RegeneratedScriptVO> regenerateScript(Long scriptVersionId, Long suggestionId,
                                                       List<String> generationStyles, Long userId) {
        log.info("重新生成话术：scriptVersionId={}, suggestionId={}, styles={}, userId={}",
                scriptVersionId, suggestionId, generationStyles, userId);

        if (suggestionId == null || suggestionId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "建议 ID 无效");
        }
        if (generationStyles == null || generationStyles.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "生成风格不能为空");
        }

        // 验证建议存在且属于该用户
        Optional<ScriptOptimizationSuggestion> suggestionOpt = suggestionRepository.findById(suggestionId);
        if (suggestionOpt.isEmpty() || !suggestionOpt.get().getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "建议不存在或无权访问");
        }

        ScriptOptimizationSuggestion suggestion = suggestionOpt.get();
        List<RegeneratedScriptVO> results = new ArrayList<>();

        try {
            for (String style : generationStyles) {
                // 创建重新生成的版本记录（实际实现中应调用 AI 服务）
                ScriptRegeneratedVersion regenerated = ScriptRegeneratedVersion.builder()
                        .scriptVersionId(scriptVersionId)
                        .suggestionId(suggestionId)
                        .ownerId(userId)
                        .generationStyle(style)
                        .regeneratedContent(generateScriptContent(style))
                        .aiQualityScore(new BigDecimal("8.2"))
                        .estimatedMetrics(generateEstimatedMetricsJson())
                        .approvalStatus("PENDING")
                        .isApplied(false)
                        .deleted(0)
                        .build();

                ScriptRegeneratedVersion saved = regeneratedVersionRepository.save(regenerated);
                results.add(convertToRegeneratedVO(saved));
                log.info("生成话术版本成功：style={}, regeneratedVersionId={}", style, saved.getId());
            }
        } catch (Exception e) {
            log.error("话术重新生成失败：suggestionId={}, error={}", suggestionId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_BUSY, "话术重新生成失败");
        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResultVO<ScriptAnalysisResultVO> getOptimizationHistory(Long scriptVersionId, Integer page,
                                                                        Integer rows, Long userId) {
        log.info("获取优化历史：scriptVersionId={}, page={}, rows={}, userId={}",
                scriptVersionId, page, rows, userId);

        if (page == null || page < 0) {
            page = 0;
        }
        if (rows == null || rows < 1 || rows > 1000) {
            rows = 30;
        }

        Pageable pageable = PageRequest.of(page, rows, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ScriptAnalysisResult> pageResult = analysisResultRepository
                .findByScriptVersionIdAndOwnerIdOrderByCreatedAtDesc(scriptVersionId, userId, pageable);

        List<ScriptAnalysisResultVO> voList = pageResult.getContent().stream()
                .map(this::convertToAnalysisVO)
                .collect(Collectors.toList());

        return PageResultVO.of(pageResult.getTotalElements(), voList, page, rows);
    }

    @Override
    @Transactional
    public Boolean acceptSuggestion(Long suggestionId, Long userId) {
        log.info("采纳建议：suggestionId={}, userId={}", suggestionId, userId);

        Optional<ScriptOptimizationSuggestion> suggestionOpt = suggestionRepository.findById(suggestionId);
        if (suggestionOpt.isEmpty() || !suggestionOpt.get().getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "建议不存在或无权访问");
        }

        ScriptOptimizationSuggestion suggestion = suggestionOpt.get();
        suggestion.setAdoptionStatus("ACCEPTED");
        suggestion.setAdoptedAt(LocalDateTime.now());
        suggestionRepository.save(suggestion);

        log.info("建议采纳成功：suggestionId={}", suggestionId);
        return true;
    }

    @Override
    @Transactional
    public Boolean rejectSuggestion(Long suggestionId, String notes, Long userId) {
        log.info("拒绝建议：suggestionId={}, userId={}", suggestionId, userId);

        Optional<ScriptOptimizationSuggestion> suggestionOpt = suggestionRepository.findById(suggestionId);
        if (suggestionOpt.isEmpty() || !suggestionOpt.get().getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "建议不存在或无权访问");
        }

        ScriptOptimizationSuggestion suggestion = suggestionOpt.get();
        suggestion.setAdoptionStatus("REJECTED");
        suggestion.setAdoptedAt(LocalDateTime.now());
        suggestion.setAdoptionNotes(notes);
        suggestionRepository.save(suggestion);

        log.info("建议拒绝成功：suggestionId={}", suggestionId);
        return true;
    }

    @Override
    @Transactional
    public Boolean applyRegeneratedVersion(Long regeneratedVersionId, Long userId) {
        log.info("应用生成版本：regeneratedVersionId={}, userId={}", regeneratedVersionId, userId);

        Optional<ScriptRegeneratedVersion> versionOpt = regeneratedVersionRepository.findById(regeneratedVersionId);
        if (versionOpt.isEmpty() || !versionOpt.get().getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "生成版本不存在或无权访问");
        }

        ScriptRegeneratedVersion version = versionOpt.get();
        if (version.getApprovalStatus().equals("REJECTED")) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "已拒绝的版本无法应用");
        }

        version.setIsApplied(true);
        version.setAppliedAt(LocalDateTime.now());
        regeneratedVersionRepository.save(version);

        log.info("生成版本应用成功：regeneratedVersionId={}", regeneratedVersionId);
        return true;
    }

    @Override
    @Transactional
    public Boolean approveRegeneratedVersion(Long regeneratedVersionId, Long approverUserId, String notes) {
        log.info("审批生成版本：regeneratedVersionId={}, approverUserId={}", regeneratedVersionId, approverUserId);

        Optional<ScriptRegeneratedVersion> versionOpt = regeneratedVersionRepository.findById(regeneratedVersionId);
        if (versionOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "生成版本不存在");
        }

        ScriptRegeneratedVersion version = versionOpt.get();
        version.setApprovalStatus("APPROVED");
        version.setApprovedBy(approverUserId);
        version.setApprovedAt(LocalDateTime.now());
        version.setApprovalNotes(notes);
        regeneratedVersionRepository.save(version);

        log.info("生成版本审批通过：regeneratedVersionId={}", regeneratedVersionId);
        return true;
    }

    @Override
    @Transactional
    public Boolean rejectRegeneratedVersion(Long regeneratedVersionId, Long approverUserId, String notes) {
        log.info("拒绝生成版本：regeneratedVersionId={}, approverUserId={}", regeneratedVersionId, approverUserId);

        Optional<ScriptRegeneratedVersion> versionOpt = regeneratedVersionRepository.findById(regeneratedVersionId);
        if (versionOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "生成版本不存在");
        }

        ScriptRegeneratedVersion version = versionOpt.get();
        version.setApprovalStatus("REJECTED");
        version.setApprovedBy(approverUserId);
        version.setApprovedAt(LocalDateTime.now());
        version.setApprovalNotes(notes);
        regeneratedVersionRepository.save(version);

        log.info("生成版本已拒绝：regeneratedVersionId={}", regeneratedVersionId);
        return true;
    }

    // 辅助方法

    private ScriptAnalysisResultVO convertToAnalysisVO(ScriptAnalysisResult entity) {
        List<ScriptAnalysisResultVO.WeakPoint> weakPoints = parseWeakPoints(entity.getWeakPoints());

        return ScriptAnalysisResultVO.builder()
                .id(entity.getId())
                .scriptVersionId(entity.getScriptVersionId())
                .overallScore(entity.getOverallScore())
                .effectivenessMetrics(ScriptAnalysisResultVO.EffectivenessMetrics.builder()
                        .interactionRate(entity.getInteractionRate())
                        .conversionRate(entity.getConversionRate())
                        .fanGrowth(entity.getFanGrowth())
                        .commentSentiment(entity.getCommentSentiment())
                        .build())
                .weakPoints(weakPoints)
                .styleProfile(ScriptAnalysisResultVO.StyleProfile.builder()
                        .dominantStyle(entity.getDominantStyle())
                        .styleScores(new HashMap<>())
                        .build())
                .analysisType(entity.getAnalysisType())
                .dataSource(entity.getDataSource())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private OptimizationSuggestionVO convertToSuggestionVO(ScriptOptimizationSuggestion entity) {
        OptimizationSuggestionVO.ExpectedImprovement improvement = null;
        if (entity.getExpectedImprovement() != null) {
            try {
                improvement = objectMapper.readValue(entity.getExpectedImprovement(),
                        OptimizationSuggestionVO.ExpectedImprovement.class);
            } catch (Exception e) {
                log.warn("解析期望改进失败", e);
            }
        }

        return OptimizationSuggestionVO.builder()
                .id(entity.getId())
                .scriptVersionId(entity.getScriptVersionId())
                .analysisResultId(entity.getAnalysisResultId())
                .category(entity.getCategory())
                .priority(entity.getPriority())
                .suggestionContent(entity.getSuggestionContent())
                .relatedWeakPoint(entity.getRelatedWeakPoint())
                .expectedImprovement(improvement)
                .adoptionStatus(entity.getAdoptionStatus())
                .adoptedAt(entity.getAdoptedAt())
                .adoptionNotes(entity.getAdoptionNotes())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private RegeneratedScriptVO convertToRegeneratedVO(ScriptRegeneratedVersion entity) {
        RegeneratedScriptVO.EstimatedMetrics metrics = null;
        if (entity.getEstimatedMetrics() != null) {
            try {
                metrics = objectMapper.readValue(entity.getEstimatedMetrics(),
                        RegeneratedScriptVO.EstimatedMetrics.class);
            } catch (Exception e) {
                log.warn("解析估计指标失败", e);
            }
        }

        return RegeneratedScriptVO.builder()
                .id(entity.getId())
                .scriptVersionId(entity.getScriptVersionId())
                .suggestionId(entity.getSuggestionId())
                .generationStyle(entity.getGenerationStyle())
                .regeneratedContent(entity.getRegeneratedContent())
                .aiQualityScore(entity.getAiQualityScore())
                .estimatedMetrics(metrics)
                .isApplied(entity.getIsApplied())
                .appliedAt(entity.getAppliedAt())
                .approvalStatus(entity.getApprovalStatus())
                .approvedBy(entity.getApprovedBy())
                .approvedAt(entity.getApprovedAt())
                .approvalNotes(entity.getApprovalNotes())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private List<ScriptAnalysisResultVO.WeakPoint> parseWeakPoints(String jsonStr) {
        List<ScriptAnalysisResultVO.WeakPoint> result = new ArrayList<>();
        if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
                result = objectMapper.readValue(jsonStr,
                        objectMapper.getTypeFactory().constructCollectionType(List.class,
                                ScriptAnalysisResultVO.WeakPoint.class));
            } catch (Exception e) {
                log.warn("解析弱点 JSON 失败", e);
            }
        }
        return result;
    }

    private String generateWeakPointsJson() {
        List<ScriptAnalysisResultVO.WeakPoint> weakPoints = List.of(
                ScriptAnalysisResultVO.WeakPoint.builder()
                        .type("LOW_INTERACTION")
                        .timeRange("05:30-07:00")
                        .severity("HIGH")
                        .description("互动环节过短，可增加 3-5 分钟提问时间")
                        .build()
        );
        try {
            return objectMapper.writeValueAsString(weakPoints);
        } catch (Exception e) {
            log.warn("生成弱点 JSON 失败", e);
            return "[]";
        }
    }

    private String generateEstimatedMetricsJson() {
        try {
            RegeneratedScriptVO.EstimatedMetrics metrics = RegeneratedScriptVO.EstimatedMetrics.builder()
                    .interactionRate(new BigDecimal("22.7"))
                    .conversionRate(new BigDecimal("15.1"))
                    .estimatedFanGrowth(350)
                    .build();
            return objectMapper.writeValueAsString(metrics);
        } catch (Exception e) {
            log.warn("生成估计指标 JSON 失败", e);
            return "{}";
        }
    }

    private String generateScriptContent(String style) {
        return switch (style.toUpperCase()) {
            case "FRIENDLY" -> "各位亲爱的朋友们，欢迎来到我们的直播间！今天给大家介绍一款非常实用的产品...";
            case "HUMOROUS" -> "各位老铁，你们好呀！今天我给大家分享一件超级好玩的东西...";
            case "PREMIUM" -> "尊敬的各位来宾，感谢大家的关注。我们为您精心准备了一款高端产品...";
            case "INSPIRATIONAL" -> "朋友们，我们一起来改变生活的质量！这款产品将帮助您实现梦想...";
            default -> "产品介绍内容";
        };
    }

    private List<ScriptOptimizationSuggestion> generateDefaultSuggestions(Long scriptVersionId,
                                                                          Long analysisResultId,
                                                                          Long userId) {
        List<ScriptOptimizationSuggestion> suggestions = new ArrayList<>();

        ScriptOptimizationSuggestion s1 = ScriptOptimizationSuggestion.builder()
                .scriptVersionId(scriptVersionId)
                .analysisResultId(analysisResultId)
                .ownerId(userId)
                .category("CONTENT")
                .priority("HIGH")
                .suggestionContent("互动环节可加入'今天直播间有多少新粉丝？'的提问")
                .relatedWeakPoint("LOW_INTERACTION")
                .adoptionStatus("PENDING")
                .deleted(0)
                .build();
        suggestions.add(s1);

        ScriptOptimizationSuggestion s2 = ScriptOptimizationSuggestion.builder()
                .scriptVersionId(scriptVersionId)
                .analysisResultId(analysisResultId)
                .ownerId(userId)
                .category("PACING")
                .priority("MEDIUM")
                .suggestionContent("中间 5 分钟缺乏高能时刻，建议插入爆料或互动")
                .relatedWeakPoint("PACING_ISSUE")
                .adoptionStatus("PENDING")
                .deleted(0)
                .build();
        suggestions.add(s2);

        return suggestionRepository.saveAll(suggestions);
    }
}
