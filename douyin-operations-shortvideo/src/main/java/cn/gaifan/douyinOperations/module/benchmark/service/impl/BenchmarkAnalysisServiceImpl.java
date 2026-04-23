package cn.gaifan.douyinOperations.module.benchmark.service.impl;

import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkAnalysis;
import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkVideo;
import cn.gaifan.douyinOperations.module.benchmark.metrics.BenchmarkMetrics;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkAnalysisRepository;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkVideoRepository;
import cn.gaifan.douyinOperations.module.benchmark.service.*;
import cn.gaifan.douyinOperations.module.benchmark.vo.AnalyzeVideoVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkAnalysisVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.BatchAnalyzeVideosVO;
import cn.gaifan.douyinOperations.module.ai.service.VideoAnalysisService;
import cn.gaifan.douyinOperations.module.ai.service.SceneDetectionService;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.douyinapi.client.DouyinApiClient;
import cn.gaifan.douyinOperations.module.storage.service.BosStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 深度分析服务实现
 * 协调9步分析流程
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BenchmarkAnalysisServiceImpl implements BenchmarkAnalysisService {

    private final BenchmarkVideoRepository videoRepository;
    private final BenchmarkAnalysisRepository analysisRepository;
    private final BenchmarkVideoService videoService;
    private final BenchmarkTaskService taskService;
    private final OcrService ocrService;
    private final ObjectMapper objectMapper;
    private final BenchmarkMetrics metrics;

    @Autowired(required = false)
    private BenchmarkScriptAutoIngestService autoIngestService;

    @Autowired(required = false)
    private VideoAnalysisService videoAnalysisService;

    @Autowired(required = false)
    private BosStorageService bosStorageService;

    @Autowired(required = false)
    private SceneDetectionService sceneDetectionService;

    @Autowired(required = false)
    private LlmClient llmClient;

    @Autowired(required = false)
    private DouyinApiClient douyinApiClient;

    @Autowired(required = false)
    private Retry videoDownloadRetry;

    @Autowired(required = false)
    private Retry bosUploadRetry;

    @Autowired(required = false)
    private Retry aiAnalysisRetry;

    @Override
    @Transactional
    public BenchmarkAnalysisVO analyzeVideo(AnalyzeVideoVO analyzeVO, Long ownerId) {
        // 生成追踪ID用于日志关联
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);
        MDC.put("videoId", String.valueOf(analyzeVO.getBenchmarkVideoId()));
        MDC.put("ownerId", String.valueOf(ownerId));

        try {
            log.info("[{}] 开始分析视频: videoId={}, forceReanalyze={}, enableAsr={}, enableOcr={}, enableApi={}",
                    traceId, analyzeVO.getBenchmarkVideoId(), analyzeVO.getForceReanalyze(),
                    analyzeVO.getEnableAsr(), analyzeVO.getEnableOcr(), analyzeVO.getEnableApi());

            BenchmarkVideo video = videoRepository.findById(analyzeVO.getBenchmarkVideoId())
                    .orElseThrow(() -> new RuntimeException("视频不存在"));

            // 检查是否已分析
            if (!analyzeVO.getForceReanalyze() && "completed".equals(video.getAnalysisStatus())) {
                log.info("[{}] 视频已分析，返回缓存结果", traceId);
                return getByVideoId(video.getId(), ownerId);
            }

            // 更新状态为处理中
            videoService.updateAnalysisStatus(video.getId(), "processing");

            long startTime = System.currentTimeMillis();

            // 执行9步分析流程
            BenchmarkAnalysis analysis = executeAnalysisPipeline(video, analyzeVO, traceId);

            // 计算耗时
            long duration = System.currentTimeMillis() - startTime;
            analysis.setAnalysisDurationMs((int) duration);

            // 保存分析结果
            analysis = analysisRepository.save(analysis);

            // 更新视频状态
            videoService.updateAnalysisStatus(video.getId(), "completed");

            // 记录指标
            metrics.recordVideoAnalyzed("completed");
            metrics.recordAiAnalysisDuration("analysis", duration);

            log.info("[{}] 视频分析完成: videoId={}, duration={}ms, tokens={}",
                    traceId, video.getId(), duration, analysis.getTokensUsed());

            // 自动入库到质量脚本知识库
            if (autoIngestService != null) {
                try {
                    log.info("[{}] 开始自动入库检查", traceId);
                    autoIngestService.autoIngestAfterAnalysis(analysis, ownerId);
                } catch (Exception e) {
                    log.error("[{}] 自动入库失败: error={}", traceId, e.getMessage(), e);
                    // 不影响主流程，继续返回分析结果
                }
            }

            return entityToVO(analysis);

        } catch (Exception e) {
            log.error("[{}] 视频分析失败: videoId={}, error={}",
                    traceId, analyzeVO.getBenchmarkVideoId(), e.getMessage(), e);

            try {
                BenchmarkVideo video = videoRepository.findById(analyzeVO.getBenchmarkVideoId()).orElse(null);
                if (video != null) {
                    videoService.updateAnalysisStatus(video.getId(), "failed");
                }
            } catch (Exception ex) {
                log.error("[{}] 更新失败状态异常", traceId, ex);
            }

            throw new RuntimeException("视频分析失败: " + e.getMessage(), e);
        } finally {
            MDC.clear();
        }
    }

    @Override
    @Transactional
    public Long batchAnalyzeVideos(BatchAnalyzeVideosVO batchVO, Long ownerId) {
        log.info("批量分析视频: count={}", batchVO.getVideoIds().size());

        // 创建任务
        String configJson = toJson(batchVO);
        Long taskId = taskService.createTask("batch_analyze", null, configJson, ownerId).getId();

        // 异步执行批量分析
        executeBatchAnalysisAsync(taskId, batchVO, ownerId);

        return taskId;
    }

    @Override
    public BenchmarkAnalysisVO getByVideoId(Long videoId, Long ownerId) {
        BenchmarkAnalysis analysis = analysisRepository.findByBenchmarkVideoId(videoId)
                .orElseThrow(() -> new RuntimeException("分析结果不存在"));

        return entityToVO(analysis);
    }

    @Override
    public String mergeContent(String transcriptText, String ocrText, String apiDescription) {
        StringBuilder merged = new StringBuilder();

        if (transcriptText != null && !transcriptText.isEmpty()) {
            merged.append("【语音识别】\n").append(transcriptText).append("\n\n");
        }

        if (ocrText != null && !ocrText.isEmpty()) {
            merged.append("【字幕识别】\n").append(ocrText).append("\n\n");
        }

        if (apiDescription != null && !apiDescription.isEmpty()) {
            merged.append("【视频描述】\n").append(apiDescription).append("\n\n");
        }

        return merged.toString().trim();
    }

    /**
     * 执行9步分析流程
     */
    private BenchmarkAnalysis executeAnalysisPipeline(BenchmarkVideo video, AnalyzeVideoVO analyzeVO, String traceId) {
        log.info("[{}] 开始执行分析流程", traceId);

        BenchmarkAnalysis analysis = new BenchmarkAnalysis();
        analysis.setBenchmarkVideoId(video.getId());

        // Step 1: 视频下载（如果还没下载）
        String localVideoPath = downloadVideoIfNeeded(video, traceId);
        video.setLocalVideoPath(localVideoPath);
        videoRepository.save(video);

        // Step 2: 上传到BOS（如果还没上传）
        String bosUrl = uploadToBosIfNeeded(video, localVideoPath, traceId);
        video.setBosVideoUrl(bosUrl);
        videoRepository.save(video);

        // Step 3: 内容提取（并行）
        log.info("[{}] 开始并行内容提取: ASR={}, OCR={}, API={}",
                traceId, analyzeVO.getEnableAsr(), analyzeVO.getEnableOcr(), analyzeVO.getEnableApi());

        CompletableFuture<String> asrFuture = null;
        CompletableFuture<String> ocrFuture = null;
        CompletableFuture<String> apiFuture = null;

        if (analyzeVO.getEnableAsr()) {
            asrFuture = CompletableFuture.supplyAsync(() -> extractTranscript(localVideoPath, traceId));
        }

        if (analyzeVO.getEnableOcr()) {
            ocrFuture = CompletableFuture.supplyAsync(() -> ocrService.recognizeTextFromVideo(localVideoPath));
        }

        if (analyzeVO.getEnableApi()) {
            apiFuture = CompletableFuture.supplyAsync(() -> fetchApiDescription(video.getVideoId(), traceId));
        }

        // 等待所有内容提取完成
        String transcriptText = asrFuture != null ? asrFuture.join() : "";
        String ocrText = ocrFuture != null ? ocrFuture.join() : "";
        String apiDescription = apiFuture != null ? apiFuture.join() : "";

        log.info("[{}] 内容提取完成: ASR={}字, OCR={}字, API={}字",
                traceId, transcriptText.length(), ocrText.length(), apiDescription.length());

        analysis.setTranscriptText(transcriptText);
        analysis.setOcrText(ocrText);
        analysis.setApiDescription(apiDescription);

        // Step 4: 合并内容
        String mergedContent = mergeContent(transcriptText, ocrText, apiDescription);
        analysis.setMergedContent(mergedContent);
        log.info("[{}] 内容合并完成: 总计{}字", traceId, mergedContent.length());

        // Step 5: 场景分析
        Map<String, Object> sceneAnalysis = analyzeScenes(localVideoPath, traceId);
        analysis.setSceneCount((Integer) sceneAnalysis.get("sceneCount"));
        analysis.setKeyFramesJson((String) sceneAnalysis.get("keyFramesJson"));
        analysis.setSceneDescription((String) sceneAnalysis.get("sceneDescription"));

        // Step 6-9: AI深度分析
        Map<String, String> aiAnalysis = performAiAnalysis(mergedContent, sceneAnalysis, analyzeVO.getAiModel(), traceId);
        analysis.setCreativeType(aiAnalysis.get("creativeType"));
        analysis.setHookStrategy(aiAnalysis.get("hookStrategy"));
        analysis.setContentStructure(aiAnalysis.get("contentStructure"));
        analysis.setEmotionalCurve(aiAnalysis.get("emotionalCurve"));
        analysis.setPacingAnalysis(aiAnalysis.get("pacingAnalysis"));
        analysis.setViralFactors(aiAnalysis.get("viralFactors"));
        analysis.setStrengths(aiAnalysis.get("strengths"));
        analysis.setWeaknesses(aiAnalysis.get("weaknesses"));
        analysis.setReplicableElements(aiAnalysis.get("replicableElements"));
        analysis.setAiSummary(aiAnalysis.get("aiSummary"));
        analysis.setScriptBreakdown(aiAnalysis.get("scriptBreakdown"));
        analysis.setImprovementSuggestions(aiAnalysis.get("improvementSuggestions"));
        analysis.setTargetAudience(aiAnalysis.get("targetAudience"));
        analysis.setComparisonReport(aiAnalysis.get("comparisonReport"));
        analysis.setDifferentiationPoints(aiAnalysis.get("differentiationPoints"));
        analysis.setAiModelUsed(aiAnalysis.getOrDefault("aiModel", "gpt-4"));
        analysis.setTokensUsed(Long.parseLong(aiAnalysis.getOrDefault("tokensUsed", "0")));

        log.info("[{}] 分析流程完成", traceId);
        return analysis;
    }

    /**
     * Step 1: 下载视频（带重试）
     */
    private String downloadVideoIfNeeded(BenchmarkVideo video, String traceId) {
        if (video.getLocalVideoPath() != null && !video.getLocalVideoPath().isEmpty()) {
            log.debug("[{}] 视频已下载，跳过: path={}", traceId, video.getLocalVideoPath());
            return video.getLocalVideoPath();
        }

        log.info("[{}] 开始下载视频: videoId={}, url={}", traceId, video.getVideoId(), video.getVideoUrl());

        if (videoAnalysisService == null) {
            log.warn("[{}] VideoAnalysisService 未启用，跳过视频下载", traceId);
            return "temp/videos/" + video.getVideoId() + ".mp4";
        }

        try {
            // 使用重试机制下载视频
            if (videoDownloadRetry != null) {
                String path = Retry.decorateSupplier(videoDownloadRetry, () -> {
                    try {
                        return videoAnalysisService.downloadVideo(video.getVideoUrl());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).get();
                log.info("[{}] 视频下载成功: path={}", traceId, path);
                return path;
            } else {
                String path = videoAnalysisService.downloadVideo(video.getVideoUrl());
                log.info("[{}] 视频下载成功（无重试）: path={}", traceId, path);
                return path;
            }
        } catch (Exception e) {
            log.error("[{}] 下载视频失败: videoId={}, error={}", traceId, video.getVideoId(), e.getMessage(), e);
            throw new RuntimeException("下载视频失败: " + e.getMessage(), e);
        }
    }

    /**
     * Step 2: 上传到BOS（带重试）
     */
    private String uploadToBosIfNeeded(BenchmarkVideo video, String localPath, String traceId) {
        if (video.getBosVideoUrl() != null && !video.getBosVideoUrl().isEmpty()) {
            log.debug("[{}] 视频已上传BOS，跳过: url={}", traceId, video.getBosVideoUrl());
            return video.getBosVideoUrl();
        }

        log.info("[{}] 开始上传视频到BOS: videoId={}, localPath={}", traceId, video.getVideoId(), localPath);

        if (bosStorageService == null || !bosStorageService.isConfigured()) {
            log.warn("[{}] BosStorageService 未配置，跳过上传", traceId);
            return "https://bos.example.com/benchmark/" + video.getVideoId() + ".mp4";
        }

        try {
            File videoFile = new File(localPath);
            if (!videoFile.exists()) {
                log.warn("[{}] 本地视频文件不存在: {}", traceId, localPath);
                return "https://bos.example.com/benchmark/" + video.getVideoId() + ".mp4";
            }

            String bosKey = "benchmark/" + video.getVideoId() + ".mp4";
            byte[] videoData = Files.readAllBytes(videoFile.toPath());

            // 使用重试机制上传
            String bosUrl;
            if (bosUploadRetry != null) {
                bosUrl = Retry.decorateSupplier(bosUploadRetry, () -> {
                    try {
                        return bosStorageService.uploadBytes(bosKey, videoData, "video/mp4");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).get();
            } else {
                bosUrl = bosStorageService.uploadBytes(bosKey, videoData, "video/mp4");
            }

            log.info("[{}] 视频上传成功: videoId={}, bosUrl={}, size={}KB",
                    traceId, video.getVideoId(), bosUrl, videoData.length / 1024);
            return bosUrl;

        } catch (IOException e) {
            log.error("[{}] 读取视频文件失败: path={}, error={}", traceId, localPath, e.getMessage(), e);
            throw new RuntimeException("读取视频文件失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[{}] 上传视频到BOS失败: videoId={}, error={}", traceId, video.getVideoId(), e.getMessage(), e);
            throw new RuntimeException("上传视频到BOS失败: " + e.getMessage(), e);
        }
    }

    /**
     * Step 3a: ASR语音识别
     */
    private String extractTranscript(String videoPath, String traceId) {
        log.info("[{}] 开始ASR语音识别: path={}", traceId, videoPath);

        if (videoAnalysisService == null) {
            log.warn("[{}] VideoAnalysisService 未启用，跳过 ASR", traceId);
            return "";
        }

        try {
            long startTime = System.currentTimeMillis();

            // 先提取音频
            String audioPath = videoAnalysisService.extractAudio(videoPath);
            log.debug("[{}] 音频提取完成: audioPath={}", traceId, audioPath);

            // 再进行 ASR 识别
            String transcript = videoAnalysisService.transcribeAudio(audioPath);

            long duration = System.currentTimeMillis() - startTime;
            log.info("[{}] ASR识别完成: 耗时={}ms, 文本长度={}字", traceId, duration, transcript.length());

            return transcript;
        } catch (Exception e) {
            log.error("[{}] ASR识别失败: path={}, error={}", traceId, videoPath, e.getMessage(), e);
            return "[ASR 识别失败: " + e.getMessage() + "]";
        }
    }

    /**
     * Step 3c: 抖音API获取描述
     */
    private String fetchApiDescription(String videoId, String traceId) {
        log.info("[{}] 获取抖音API描述: videoId={}", traceId, videoId);

        if (douyinApiClient == null) {
            log.warn("[{}] DouyinApiClient 未启用，跳过 API 获取", traceId);
            return "";
        }

        try {
            log.debug("[{}] DouyinApiClient 需要 accessToken，当前实现暂不支持", traceId);
            return "";

        } catch (Exception e) {
            log.error("[{}] 获取抖音API描述失败: videoId={}, error={}", traceId, videoId, e.getMessage(), e);
            return "[API 获取失败: " + e.getMessage() + "]";
        }
    }

    /**
     * Step 4: 场景分析
     */
    private Map<String, Object> analyzeScenes(String videoPath, String traceId) {
        log.info("[{}] 开始场景分析: path={}", traceId, videoPath);

        if (sceneDetectionService == null) {
            log.warn("[{}] SceneDetectionService 未启用，跳过场景分析", traceId);
            Map<String, Object> result = new HashMap<>();
            result.put("sceneCount", 0);
            result.put("keyFramesJson", "[]");
            result.put("sceneDescription", "");
            return result;
        }

        try {
            long startTime = System.currentTimeMillis();

            List<SceneDetectionService.SceneSegment> segments = sceneDetectionService.detectScenes(videoPath);

            // 构建关键帧 JSON
            List<Map<String, Object>> keyFrames = new ArrayList<>();
            for (int i = 0; i < segments.size(); i++) {
                SceneDetectionService.SceneSegment segment = segments.get(i);
                Map<String, Object> frame = new HashMap<>();
                frame.put("index", i);
                frame.put("startTime", segment.startTimeSec());
                frame.put("endTime", segment.endTimeSec());
                frame.put("framePath", segment.keyframePath());
                keyFrames.add(frame);
            }

            String keyFramesJson = objectMapper.writeValueAsString(keyFrames);

            // 构建场景描述
            StringBuilder description = new StringBuilder();
            description.append("视频共检测到 ").append(segments.size()).append(" 个场景：\n");
            for (int i = 0; i < segments.size(); i++) {
                SceneDetectionService.SceneSegment segment = segments.get(i);
                description.append(String.format("场景%d: %.2fs - %.2fs (时长: %.2fs)\n",
                        i + 1,
                        segment.startTimeSec(),
                        segment.endTimeSec(),
                        segment.endTimeSec() - segment.startTimeSec()));
            }

            Map<String, Object> result = new HashMap<>();
            result.put("sceneCount", segments.size());
            result.put("keyFramesJson", keyFramesJson);
            result.put("sceneDescription", description.toString());

            long duration = System.currentTimeMillis() - startTime;
            log.info("[{}] 场景分析完成: 场景数={}, 耗时={}ms", traceId, segments.size(), duration);

            return result;

        } catch (Exception e) {
            log.error("[{}] 场景分析失败: path={}, error={}", traceId, videoPath, e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("sceneCount", 0);
            result.put("keyFramesJson", "[]");
            result.put("sceneDescription", "[场景分析失败: " + e.getMessage() + "]");
            return result;
        }
    }

    /**
     * Step 5-9: AI深度分析（带重试）
     */
    private Map<String, String> performAiAnalysis(String content, Map<String, Object> sceneAnalysis, String aiModel, String traceId) {
        log.info("[{}] 开始AI深度分析: contentLength={}, model={}", traceId, content.length(), aiModel);

        Map<String, String> result = new HashMap<>();
        result.put("creativeType", "");
        result.put("hookStrategy", "");
        result.put("contentStructure", "");
        result.put("emotionalCurve", "");
        result.put("pacingAnalysis", "");
        result.put("viralFactors", "");
        result.put("strengths", "");
        result.put("weaknesses", "");
        result.put("replicableElements", "");
        result.put("aiSummary", "");
        result.put("scriptBreakdown", "");
        result.put("improvementSuggestions", "");
        result.put("targetAudience", "");
        result.put("comparisonReport", "");
        result.put("differentiationPoints", "");
        result.put("aiModel", aiModel != null ? aiModel : "gpt-4");
        result.put("tokensUsed", "0");

        if (llmClient == null) {
            log.warn("[] LlmClient 未启用，跳过 AI 分析", traceId);
            return result;
        }

        if (content == null || content.trim().isEmpty()) {
            log.warn("[{}] 内容为空，跳过 AI 分析", traceId);
            return result;
        }

        try {
            long startTime = System.currentTimeMillis();

            // 构建分析提示词
            String prompt = buildAnalysisPrompt(content, sceneAnalysis);

            // 创建 AI 模型配置
            AiModel model = new AiModel();
            model.setModelName(aiModel != null ? aiModel : "gpt-4");
            model.setModelProvider("openai");
            model.setMaxTokens(4000);
            model.setTemperature(new java.math.BigDecimal("0.7"));

            // 调用 AI 模型（带重试）
            LlmClient.LlmResponse response;
            if (aiAnalysisRetry != null) {
                response = Retry.decorateSupplier(aiAnalysisRetry, () -> {
                    try {
                        return llmClient.chat(model,
                                "你是一位专业的短视频内容分析师，擅长分析抖音爆款视频的创作技巧和传播规律。",
                                prompt);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).get();
            } else {
                response = llmClient.chat(model,
                        "你是一位专业的短视频内容分析师，擅长分析抖音爆款视频的创作技巧和传播规律。",
                        prompt);
            }

            long duration = System.currentTimeMillis() - startTime;

            if (!response.success()) {
                log.error("[{}] AI 分析失败: error={}", traceId, response.errorMsg());
                result.put("aiSummary", "[AI 分析失败: " + response.errorMsg() + "]");
                return result;
            }

            // 解析 AI 返回的 JSON 结果
            String aiContent = response.content();
            long tokensUsed = response.tokensUsed();
            result.put("tokensUsed", String.valueOf(tokensUsed));

            // 记录指标
            metrics.recordAiAnalysis(aiModel != null ? aiModel : "gpt-4", true);
            metrics.recordAiTokens(aiModel != null ? aiModel : "gpt-4", tokensUsed);

            // 尝试解析 JSON 格式的响应
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> aiResult = objectMapper.readValue(aiContent, Map.class);

                // 映射 AI 返回的字段到结果 Map
                result.put("creativeType", aiResult.getOrDefault("creativeType", ""));
                result.put("hookStrategy", aiResult.getOrDefault("hookStrategy", ""));
                result.put("contentStructure", aiResult.getOrDefault("contentStructure", ""));
                result.put("emotionalCurve", aiResult.getOrDefault("emotionalCurve", ""));
                result.put("pacingAnalysis", aiResult.getOrDefault("pacingAnalysis", ""));
                result.put("viralFactors", aiResult.getOrDefault("viralFactors", ""));
                result.put("strengths", aiResult.getOrDefault("strengths", ""));
                result.put("weaknesses", aiResult.getOrDefault("weaknesses", ""));
                result.put("replicableElements", aiResult.getOrDefault("replicableElements", ""));
                result.put("aiSummary", aiResult.getOrDefault("summary", ""));
                result.put("scriptBreakdown", aiResult.getOrDefault("scriptBreakdown", ""));
                result.put("improvementSuggestions", aiResult.getOrDefault("improvementSuggestions", ""));
                result.put("targetAudience", aiResult.getOrDefault("targetAudience", ""));
                result.put("comparisonReport", aiResult.getOrDefault("comparisonReport", ""));
                result.put("differentiationPoints", aiResult.getOrDefault("differentiationPoints", ""));

                log.info("[{}] AI 分析完成: tokens={}, 耗时={}ms", traceId, tokensUsed, duration);

            } catch (Exception e) {
                // 如果不是 JSON 格式，将整个内容作为摘要
                log.debug("[{}] AI 返回非 JSON 格式，使用原始内容: {}", traceId, e.getMessage());
                result.put("aiSummary", aiContent);
                log.info("[{}] AI 分析完成（非JSON）: tokens={}, 耗时={}ms", traceId, tokensUsed, duration);
            }

            return result;

        } catch (Exception e) {
            log.error("[{}] AI 分析异常: error={}", traceId, e.getMessage(), e);
            metrics.recordAiAnalysis(aiModel != null ? aiModel : "gpt-4", false);
            result.put("aiSummary", "[AI 分析异常: " + e.getMessage() + "]");
            return result;
        }
    }

    /**
     * 构建AI分析提示词
     */
    private String buildAnalysisPrompt(String content, Map<String, Object> sceneAnalysis) {
        return String.format("""
                请深度分析以下短视频内容：

                【视频文案】
                %s

                【场景信息】
                场景数量：%d

                请从以下维度进行分析：
                1. 创意类型（情感共鸣/知识科普/娱乐搞笑/产品展示等）
                2. 钩子策略（前3秒如何吸引注意力）
                3. 内容结构（起承转合）
                4. 情绪曲线（情绪变化节奏）
                5. 节奏分析（镜头切换、语速等）
                6. 爆款因素（为什么能火）
                7. 优势分析（做得好的地方）
                8. 弊端分析（可以改进的地方）
                9. 可复制要素（可以借鉴的技巧）
                10. 话术拆解（逐句分析）
                11. 改进建议（具体可行的建议）
                12. 目标受众（适合什么人群）

                请以JSON格式返回分析结果。
                """,
                content,
                sceneAnalysis.get("sceneCount")
        );
    }

    /**
     * 异步执行批量分析
     */
    @Async
    public void executeBatchAnalysisAsync(Long taskId, BatchAnalyzeVideosVO batchVO, Long ownerId) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);
        MDC.put("taskId", String.valueOf(taskId));
        MDC.put("ownerId", String.valueOf(ownerId));

        try {
            log.info("[{}] 开始批量分析任务: taskId={}, videoCount={}", traceId, taskId, batchVO.getVideoIds().size());

            taskService.updateStatus(taskId, "running", null, null);
            metrics.recordTaskCreated("batch_analyze");

            long startTime = System.currentTimeMillis();
            int total = batchVO.getVideoIds().size();
            int processed = 0;
            int failed = 0;

            for (Long videoId : batchVO.getVideoIds()) {
                try {
                    log.debug("[{}] 分析视频 {}/{}: videoId={}", traceId, processed + failed + 1, total, videoId);

                    AnalyzeVideoVO analyzeVO = new AnalyzeVideoVO();
                    analyzeVO.setBenchmarkVideoId(videoId);
                    analyzeVO.setForceReanalyze(batchVO.getForceReanalyze());
                    analyzeVO.setAiModel(batchVO.getAiModel());

                    analyzeVideo(analyzeVO, ownerId);
                    processed++;

                    log.info("[{}] 视频分析成功: videoId={}, progress={}/{}", traceId, videoId, processed, total);

                } catch (Exception e) {
                    log.error("[{}] 分析视频失败: videoId={}, error={}", traceId, videoId, e.getMessage(), e);
                    failed++;
                }

                // 更新进度
                int progress = (processed + failed) * 100 / total;
                taskService.updateProgress(taskId, progress, processed, failed);
            }

            // 完成
            long duration = System.currentTimeMillis() - startTime;
            String summary = String.format("成功: %d, 失败: %d, 耗时: %dms", processed, failed, duration);
            taskService.updateStatus(taskId, "completed", summary, null);

            metrics.recordTaskCompleted("batch_analyze", "completed");
            metrics.recordTaskDuration("batch_analyze", duration);

            log.info("[{}] 批量分析任务完成: taskId={}, processed={}, failed={}, duration={}ms",
                    traceId, taskId, processed, failed, duration);

        } catch (Exception e) {
            log.error("[{}] 批量分析任务失败: taskId={}, error={}", traceId, taskId, e.getMessage(), e);
            taskService.updateStatus(taskId, "failed", null, e.getMessage());
            metrics.recordTaskCompleted("batch_analyze", "failed");
        } finally {
            MDC.clear();
        }
    }

    private BenchmarkAnalysisVO entityToVO(BenchmarkAnalysis entity) {
        BenchmarkAnalysisVO vo = new BenchmarkAnalysisVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
