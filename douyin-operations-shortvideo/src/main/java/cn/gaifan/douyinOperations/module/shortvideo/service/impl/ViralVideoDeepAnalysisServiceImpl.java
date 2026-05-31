package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.service.VideoAnalysisService;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvViralVideoRepository;
import cn.gaifan.douyinOperations.common.id.Ids;
import cn.gaifan.douyinOperations.contract.credit.CreditConsumeRequest;
import cn.gaifan.douyinOperations.contract.product.FeatureCode;
import cn.gaifan.douyinOperations.contract.product.ProductCode;
import cn.gaifan.douyinOperations.module.platform.credit.CommercialProductChargeService;
import cn.gaifan.douyinOperations.module.platform.product.DeliveryProduct;
import cn.gaifan.douyinOperations.module.shortvideo.service.ViralVideoDeepAnalysisService;
import cn.gaifan.douyinOperations.module.shortvideo.service.ViralVideoService;
import cn.gaifan.douyinOperations.common.config.RequestIdentityHolder;
import cn.gaifan.douyinOperations.contract.identity.IdentityContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LF-05 深度分析：异步提交 + 轮询；仅 ASR/抽帧仍为同步接口。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViralVideoDeepAnalysisServiceImpl implements ViralVideoDeepAnalysisService {

    private static final int BATCH_MAX = 30;

    private static final ObjectMapper JSON = new ObjectMapper();

    private final SvViralVideoRepository viralVideoRepository;
    private final ViralVideoService viralVideoService;
    private final ViralDeepAnalyzeAsyncRunner deepAnalyzeAsyncRunner;
    private final ViralDeepAnalyzeExecutor viralDeepAnalyzeExecutor;

    @Autowired(required = false)
    private VideoAnalysisService videoAnalysisService;

    @Autowired(required = false)
    private CommercialProductChargeService commercialProductChargeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> startDeepAnalyze(Long viralVideoId, Long userId, boolean skipCommercialCharge) {
        if (!skipCommercialCharge) {
            chargeVideoInsightIfNeeded(viralVideoId, userId);
        }
        SvViralVideo viral = viralVideoService.getViralVideo(viralVideoId, userId);
        if (StringUtils.hasText(viral.getVideoUrl()) && videoAnalysisService == null) {
            log.info("LF-05 深度分析提交：未启用本机视频解析，将仅基于标题与元数据由 LLM 推演口播/分镜 viralId={}", viralVideoId);
        }
        String st = viral.getDeepAnalyzeStatus();
        if ("processing".equals(st)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("taskId", viralVideoId);
            m.put("status", "processing");
            m.put("message", "深度分析进行中");
            return m;
        }
        viral.setDeepAnalyzeStatus("processing");
        viral.setDeepAnalyzeError(null);
        viral.setDeepAnalyzeStartedAt(new Timestamp(System.currentTimeMillis()));
        viral.setDeepAnalyzeFinishedAt(null);
        viralVideoRepository.save(viral);

        deepAnalyzeAsyncRunner.runDeepAnalyzeAsync(viralVideoId, userId);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("taskId", viralVideoId);
        m.put("status", "processing");
        return m;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startDeepAnalyzeStream(Long viralVideoId, Long userId, SseEmitter emitter) {
        chargeVideoInsightIfNeeded(viralVideoId, userId);
        SvViralVideo viral = viralVideoService.getViralVideo(viralVideoId, userId);
        if (StringUtils.hasText(viral.getVideoUrl()) && videoAnalysisService == null) {
            log.info("LF-05 深度分析 SSE：未启用本机视频解析 viralId={}", viralVideoId);
        }
        String st = viral.getDeepAnalyzeStatus();
        if ("processing".equals(st)) {
            try {
                emitter.send(SseEmitter.event().name("error").data(
                        JSON.valueToTree(Map.of("error", "深度分析进行中，请稍候或刷新状态")),
                        MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                log.debug("SSE error 事件发送失败: {}", e.getMessage());
            }
            try {
                emitter.complete();
            } catch (Exception ignored) {
                // ignore
            }
            return;
        }
        viral.setDeepAnalyzeStatus("processing");
        viral.setDeepAnalyzeError(null);
        viral.setDeepAnalyzeStartedAt(new Timestamp(System.currentTimeMillis()));
        viral.setDeepAnalyzeFinishedAt(null);
        viralVideoRepository.save(viral);

        deepAnalyzeAsyncRunner.runDeepAnalyzeAsync(viralVideoId, userId, emitter);
    }

    @Override
    public Map<String, Object> getDeepAnalyzeStatus(Long viralVideoId, Long userId) {
        SvViralVideo viral = viralVideoService.getViralVideo(viralVideoId, userId);
        ViralEvidenceHelper.EvidenceSnapshot evidence = ViralEvidenceHelper.resolveEvidence(
                viral.getDeepAnalysisResult(), viral.getTranscript(), viral.getSceneDescriptions());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("taskId", viralVideoId);
        m.put("viralVideoId", viralVideoId);
        String st = viral.getDeepAnalyzeStatus();
        m.put("status", st != null ? st : "idle");
        if (viral.getDeepAnalyzeError() != null) {
            m.put("error", viral.getDeepAnalyzeError());
        }
        m.put("deepAnalyzeStartedAt", viral.getDeepAnalyzeStartedAt());
        m.put("deepAnalyzeFinishedAt", viral.getDeepAnalyzeFinishedAt());
        m.put("deepAnalyzedAt", viral.getDeepAnalyzedAt());
        m.put("transcript", viral.getTranscript());
        m.put("sceneDescriptions", viral.getSceneDescriptions());
        m.put("analysisResult", viral.getAnalysisResult());
        m.put("lightAnalysisResult", viral.getLightAnalysisResult());
        m.put("deepAnalysisResult", viral.getDeepAnalysisResult());
        m.put("deepAnalyzeSteps", viral.getDeepAnalyzeSteps());
        m.put("deepAnalyzeProgress", viral.getDeepAnalyzeProgress());
        m.put("remakeVariableTable", viral.getRemakeVariableTable());
        appendEvidenceSummary(m, evidence);
        return m;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> batchStartDeepAnalyze(List<Long> viralVideoIds, Long userId) {
        if (viralVideoIds == null || viralVideoIds.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "ids 不能为空");
        }
        if (viralVideoIds.size() > BATCH_MAX) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "单次最多 " + BATCH_MAX + " 条");
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Long id : viralVideoIds) {
            if (id == null) {
                continue;
            }
            try {
                out.add(startDeepAnalyze(id, userId, false));
            } catch (Exception e) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("taskId", id);
                err.put("status", "error");
                err.put("error", e.getMessage());
                out.add(err);
            }
        }
        return out;
    }

    @Override
    public List<Map<String, Object>> batchGetDeepAnalyzeStatus(List<Long> viralVideoIds, Long userId) {
        if (viralVideoIds == null || viralVideoIds.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "ids 不能为空");
        }
        if (viralVideoIds.size() > BATCH_MAX) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "单次最多 " + BATCH_MAX + " 条");
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Long id : viralVideoIds) {
            if (id == null) {
                continue;
            }
            try {
                out.add(getDeepAnalyzeStatus(id, userId));
            } catch (Exception e) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("viralVideoId", id);
                err.put("taskId", id);
                err.put("status", "error");
                err.put("error", e.getMessage());
                out.add(err);
            }
        }
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String extractTranscript(Long viralVideoId, Long userId) {
        SvViralVideo viral = viralVideoService.getViralVideo(viralVideoId, userId);
        if (viral.getVideoUrl() == null || viral.getVideoUrl().isBlank()) {
            return null;
        }
        if (videoAnalysisService == null) {
            return null;
        }
        String videoPath = null;
        try {
            videoPath = videoAnalysisService.downloadVideo(viral.getVideoUrl());
            String audioPath = videoAnalysisService.extractAudio(videoPath);
            String t = videoAnalysisService.transcribeAudio(audioPath);
            viral.setTranscript(t);
            viralVideoRepository.save(viral);
            return t;
        } catch (Exception e) {
            log.error("[ASR] viralId={} 失败: {}", viralVideoId, e.getMessage());
            return null;
        } finally {
            if (videoPath != null) {
                try {
                    videoAnalysisService.cleanup(videoPath);
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String extractSceneDescriptions(Long viralVideoId, Long userId) {
        SvViralVideo viral = viralVideoService.getViralVideo(viralVideoId, userId);
        if (viral.getVideoUrl() == null || viral.getVideoUrl().isBlank()) {
            return null;
        }
        if (videoAnalysisService == null) {
            return null;
        }
        String videoPath = null;
        try {
            videoPath = videoAnalysisService.downloadVideo(viral.getVideoUrl());
            List<String> framePaths = videoAnalysisService.extractFrames(videoPath, 1);
            if (framePaths == null || framePaths.isEmpty()) {
                return null;
            }
            List<String> sub = framePaths.subList(0, Math.min(10, framePaths.size()));
            List<String> descriptions = videoAnalysisService.analyzeFrames(sub,
                    "描述场景：环境、人物、道具、镜头、情绪。中文一句话。");
            String joined = String.join("\n", descriptions);
            viral.setSceneDescriptions(joined);
            viralVideoRepository.save(viral);
            return joined;
        } catch (Exception e) {
            log.error("[抽帧] viralId={} 失败: {}", viralVideoId, e.getMessage());
            return null;
        } finally {
            if (videoPath != null) {
                try {
                    videoAnalysisService.cleanup(videoPath);
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    public Map<String, Object> retryDeepAnalyzeRound(Long viralVideoId, Long userId, String roundKey) {
        return viralDeepAnalyzeExecutor.retryDeepAnalyzeRound(viralVideoId, userId, roundKey);
    }

    private void appendEvidenceSummary(Map<String, Object> target, ViralEvidenceHelper.EvidenceSnapshot evidence) {
        if (!evidence.hasDisclosure()) {
            return;
        }
        target.put("evidenceLevel", evidence.overallLevel());
        target.put("evidenceDetails", Map.of(
                "overallLevel", evidence.overallLevel(),
                "transcriptLevel", evidence.transcriptLevel(),
                "sceneLevel", evidence.sceneLevel(),
                "commentLevel", evidence.commentLevel(),
                "hasCommentSamples", evidence.hasCommentSamples() != null && evidence.hasCommentSamples()
        ));
        target.put("transcriptEvidenceLevel", evidence.transcriptLevel());
        target.put("sceneEvidenceLevel", evidence.sceneLevel());
        target.put("commentEvidenceLevel", evidence.commentLevel());
        target.put("hasCommentSamples", evidence.hasCommentSamples());
        target.put("transcriptDisplayLabel", transcriptDisplayLabel(evidence.transcriptLevel()));
        target.put("sceneDisplayLabel", sceneDisplayLabel(evidence.sceneLevel()));
        target.put("inferenceRisk", evidence.hasInferenceRisk());
    }

    private String transcriptDisplayLabel(String level) {
        return switch (ViralEvidenceHelper.normalizeEvidenceLevel(level)) {
            case "empirical" -> "实证转写";
            case "inferred" -> "推演口播稿（非 ASR 实录）";
            case "missing" -> "缺失";
            default -> "未标注";
        };
    }

    private String sceneDisplayLabel(String level) {
        return switch (ViralEvidenceHelper.normalizeEvidenceLevel(level)) {
            case "empirical" -> "实证场景拆解";
            case "inferred" -> "推演场景（非真实抽帧）";
            case "missing" -> "缺失";
            default -> "未标注";
        };
    }

    private void chargeVideoInsightIfNeeded(Long viralVideoId, Long userId) {
        if (commercialProductChargeService == null) {
            return;
        }
        commercialProductChargeService.charge(
                CommercialProductChargeService.CommercialProductChargeCommand.of(
                        ProductCode.VIDEO_INSIGHT,
                        FeatureCode.VIDEO_BREAKDOWN,
                        "爆款拆解 viralVideoId=" + viralVideoId,
                        DeliveryProduct.VIDEO_INSIGHT
                ));
    }
}
