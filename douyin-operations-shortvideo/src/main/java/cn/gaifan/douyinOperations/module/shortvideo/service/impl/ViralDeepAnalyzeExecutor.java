package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiKnowledgeBaseRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.ai.service.OperationalStrategyKnowledgeService;
import cn.gaifan.douyinOperations.module.ai.service.SceneDetectionService;
import cn.gaifan.douyinOperations.module.ai.service.VideoAnalysisService;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvComment;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvCommentRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvViralVideoRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.ViralVideoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;

/**
 * LF-05 深度分析实际执行（异步线程中调用，含并发限制）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViralDeepAnalyzeExecutor {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final int MAX_CONCURRENT_DEEP_ANALYZE = 3;
    private static final Semaphore DEEP_ANALYZE_PERMITS = new Semaphore(MAX_CONCURRENT_DEEP_ANALYZE);
    private static final int ACQUIRE_TIMEOUT_SECONDS = 30;

    /** 有分享链接但未注册 {@link VideoAnalysisService}（未开启 app.video-analysis 等）时注入 LLM 的边界说明 */
    private static final String TRANSCRIPT_PIPELINE_UNAVAILABLE =
            "（未启用本机视频下载/ASR，无真实口播转写。请仅依据下方「视频元数据」与标题，在 JSON 的 transcript 中输出完整推演稿与分句时间轴；体现为基于标题与互动数据的合理推断，勿伪称原片逐字稿。）";

    private static final String SCENE_PIPELINE_UNAVAILABLE =
            "（未启用本机抽帧/视觉分析，无真实画面。请仅依据元数据与标题，在 JSON 的 scenes 中输出分镜推演。）";

    private static final String DEEP_ANALYSIS_PROMPT = """
            你是短视频爆款分析专家。请基于以下信息，按固定结构输出分析报告 JSON。

            【口播文案（ASR 转写）】
            %s

            【画面场景描述（逐帧）】
            %s

            【视频元数据】
            标题: %s
            点赞: %s | 分享: %s | 评论: %s | 播放: %s

            【评论抽样】（用于判断观众共鸣；若无则写「无」）
            %s

            若上文「口播」「画面」段落标明无真实音视频或仅为基于元数据的推演，仍须输出完整 JSON；口播稿与分镜应为合理推断，勿伪称来自 ASR 或逐帧实证。

            请输出以下固定结构 JSON：
            {
              "transcript": {
                "fullText": "完整口播稿（清洗后）",
                "sentences": [
                  {"time": "0:00-0:03", "text": "台词内容", "role": "hook/content/cta"}
                ],
                "goldenSentences": ["金句1", "金句2"]
              },
              "structure": {
                "hook": {"time": "0-3s", "type": "悬念/冲突/提问/反转/共情", "text": "钩子内容", "score": 0},
                "painPoint": {"time": "3-8s", "description": "痛点/好奇引发"},
                "proof": {"time": "8-20s", "description": "证明/演示/对比"},
                "trust": {"time": "20-25s", "description": "信任/背书/用户证言"},
                "cta": {"time": "最后3s", "text": "互动/转化句", "type": "点赞/关注/购买/评论"}
              },
              "scenes": [
                {"time": "0-3s", "environment": "场景", "person": "人物", "props": "道具", "camera": "镜头类型", "mood": "情绪氛围"}
              ],
              "viralHypotheses": {
                "emotionTrigger": "踩中什么情绪/身份认同",
                "infoDensity": "high/medium/low",
                "rhythmPattern": "节奏特征",
                "visualContrast": "视觉对比/反差",
                "controversyFactor": "争议点（如有）",
                "trendAlignment": "是否与当前热点/音乐/模板同构",
                "replicability": "easy/medium/hard"
              },
              "remakeVariableTable": {
                "mustKeep": ["必须保留的结构元素1", "元素2"],
                "replaceable": [
                  {"variable": "人设", "original": "原视频人设", "suggestion": "替换为你的人设风格"},
                  {"variable": "产品", "original": "原视频产品", "suggestion": "替换为你的产品"},
                  {"variable": "场景", "original": "原场景", "suggestion": "可替换场景"},
                  {"variable": "口头禅", "original": "原口头禅", "suggestion": "替换为主播口癖"},
                  {"variable": "BGM", "original": "原BGM风格", "suggestion": "同风格替换"}
                ]
              },
              "viralScore": 0,
              "bestRemakeType": "form_imitation/content_flip/element_recombination/dimensional_upgrade",
              "complianceNotes": ["合规注意事项"],
              "evidenceLevel": "empirical 或 inferred（empirical=本机 ASR/抽帧等实证；inferred=元数据或封面推演）"
            }
            只输出 JSON，不要其他文字。
            """;

    private final SvViralVideoRepository viralVideoRepository;
    private final ViralVideoService viralVideoService;
    private final AiModelRepository aiModelRepository;

    @Autowired(required = false)
    private VideoAnalysisService videoAnalysisService;

    @Autowired(required = false)
    private LlmClient llmClient;

    @Autowired(required = false)
    private SceneDetectionService sceneDetectionService;

    @Autowired(required = false)
    private ViralMetadataExtractor viralMetadataExtractor;

    @Autowired(required = false)
    private ViralBosUploader viralBosUploader;

    @Autowired(required = false)
    private ViralCommentExtractor viralCommentExtractor;

    @Autowired(required = false)
    private SvCommentRepository svCommentRepository;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Autowired(required = false)
    private cn.gaifan.douyinOperations.module.ai.service.EvolutionService evolutionService;

    /** 用于深度拆解完成后解析系统侧知识库并入队 {@link cn.gaifan.douyinOperations.module.ai.entity.AiIndexQueue} */
    @Autowired(required = false)
    private AiKnowledgeBaseRepository knowledgeBaseRepository;

    @Autowired(required = false)
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired(required = false)
    private OperationalStrategyKnowledgeService operationalStrategyKnowledgeService;

    @Autowired(required = false)
    private ViralPatternKnowledgeFormatter viralPatternKnowledgeFormatter;

    /** 与 app.ai.kb.shared-owner-id 一致：系统/共享知识库在 ai_knowledge_base.user_id 上的归属（非视频 owner） */
    @Value("${app.ai.kb.shared-owner-id:0}")
    private long viralDeepIndexKbOwnerId;

    /** 非空时优先解析为该名称下的系统库（user_id=viralDeepIndexKbOwnerId） */
    @Value("${app.ai.kb.viral-deep-index-kb-name:}")
    private String viralDeepIndexKbName;

    @jakarta.annotation.PostConstruct
    void registerMetrics() {
        if (meterRegistry != null) {
            meterRegistry.gauge("viral.deep_analyze.permits_available", DEEP_ANALYZE_PERMITS, Semaphore::availablePermits);
        }
    }

    /**
     * 抖音 Web 详情接口常返回空 JSON（签名校验 / 风控），yt-dlp 长期报 Fresh cookies；为 true 时跳过本机下载，深度分析仍走元数据+封面+LLM。
     */
    @Value("${app.video-analysis.douyin-skip-yt-dlp-download:false}")
    private boolean douyinSkipYtDlpDownload;

    /** 多轮结构化拆解开关 */
    @Value("${app.video-analysis.multi-round-analysis:true}")
    private boolean multiRoundAnalysis;

    /** 优先使用的多模态模型 ID（ai_model.id），未配置则按 model_version 启发式 */
    @Value("${app.video-analysis.vision-model-id:0}")
    private long visionModelId;

    /** 多轮 Round1 最多调用视觉模型的场景数，≤0 表示不限制 */
    @Value("${app.video-analysis.multi-round-max-vision-scenes:0}")
    private int multiRoundMaxVisionScenes;

    /** 注入 Round3 评论抽样的最大条数 */
    @Value("${app.shortvideo.viral-analysis.deep-round3-comment-samples:12}")
    private int deepRound3CommentSamples;

    @Value("${app.viral-analysis.llm-continue-on-failure:true}")
    private boolean llmContinueOnFailure;

    private static final Pattern ROUND1_SCENE_LINE = Pattern.compile(
            "^\\[场景(\\d+)\\s+([\\d.]+)s-([\\d.]+)s\\]\\s*(.*)$");

    /**
     * 执行完整深度分析并更新 {@link SvViralVideo} 状态为 completed / failed。
     */
    public void executeDeepAnalyze(Long viralVideoId, Long userId) {
        executeDeepAnalyze(viralVideoId, userId, null);
    }

    /**
     * @param sseEmitter 非空时每步推送 {@code progress} 事件，结束时推送 {@code done} / {@code error}
     */
    public void executeDeepAnalyze(Long viralVideoId, Long userId, SseEmitter sseEmitter) {
        boolean acquired = false;
        try {
            acquired = DEEP_ANALYZE_PERMITS.tryAcquire(ACQUIRE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                String msg = "深度分析并发已满，请稍后重试（最大并发 " + MAX_CONCURRENT_DEEP_ANALYZE + "）";
                markFailed(viralVideoId, userId, msg);
                if (sseEmitter != null) {
                    pushDeepAnalyzeErrorSse(sseEmitter, msg);
                }
                throw new BusinessException(ErrorCode.SYSTEM_BUSY, msg);
            }
            runPipeline(viralVideoId, userId, sseEmitter);
            if (sseEmitter != null) {
                pushDeepAnalyzeDoneSse(sseEmitter, viralVideoId, userId);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            markFailed(viralVideoId, userId, "分析被中断");
            if (sseEmitter != null) {
                pushDeepAnalyzeErrorSse(sseEmitter, "分析被中断");
            }
        } catch (Exception e) {
            log.error("[深度分析] viralId={} 失败: {}", viralVideoId, e.getMessage(), e);
            String msg = truncateErr(e.getMessage());
            markFailed(viralVideoId, userId, msg);
            if (sseEmitter != null) {
                pushDeepAnalyzeErrorSse(sseEmitter, msg);
            }
        } finally {
            if (acquired) {
                DEEP_ANALYZE_PERMITS.release();
            }
            if (sseEmitter != null) {
                try {
                    sseEmitter.complete();
                } catch (Exception ignored) {
                    // ignore
                }
            }
        }
    }

    private void markFailed(Long viralVideoId, Long userId, String err) {
        try {
            SvViralVideo v = viralVideoService.getViralVideo(viralVideoId, userId);
            v.setDeepAnalyzeStatus("failed");
            v.setDeepAnalyzeError(err);
            v.setDeepAnalyzeFinishedAt(new Timestamp(System.currentTimeMillis()));
            progressSetError(v, err);
        } catch (Exception ex) {
            log.warn("[深度分析] 写入失败状态跳过: {}", ex.getMessage());
        }
    }

    private static String truncateErr(String m) {
        if (m == null) {
            return "unknown";
        }
        return m.length() > 2000 ? m.substring(0, 2000) : m;
    }

    private static final int PROGRESS_TOTAL_STEPS = 11;

    /** 深度分析进度落库节流（毫秒）；可选 SSE 实时推送 */
    private static final class ProgressFlushState {
        long lastFlushMs;
        SseEmitter sseEmitter;
    }

    /**
     * 更新 {@link SvViralVideo#deepAnalyzeProgress} JSON；force=true 或距上次落库 ≥2s 时写库。
     */
    private void progressTouch(SvViralVideo viral, ProgressFlushState flush, String currentStepLabel,
                               String stepKey, String status, String detail, Integer progressPct, boolean force) {
        if (viral == null || viral.getId() == null) {
            return;
        }
        try {
            String now = Instant.now().toString();
            ObjectNode root;
            if (StringUtils.hasText(viral.getDeepAnalyzeProgress())) {
                JsonNode parsed = JSON.readTree(viral.getDeepAnalyzeProgress());
                root = parsed.isObject() ? (ObjectNode) parsed : JSON.getNodeFactory().objectNode();
            } else {
                root = JSON.getNodeFactory().objectNode();
                root.put("startedAt", now);
            }
            ObjectNode steps = root.has("steps") && root.get("steps").isObject()
                    ? (ObjectNode) root.get("steps")
                    : JSON.getNodeFactory().objectNode();
            ObjectNode stepNode = JSON.getNodeFactory().objectNode();
            stepNode.put("status", status);
            stepNode.put("at", now);
            if (detail != null) {
                stepNode.put("detail", detail);
            }
            if (progressPct != null) {
                stepNode.put("progress", progressPct);
            }
            steps.set(stepKey, stepNode);
            root.set("steps", steps);
            root.put("currentStep", currentStepLabel != null ? currentStepLabel : stepKey);
            int completed = 0;
            for (Iterator<JsonNode> it = steps.elements(); it.hasNext(); ) {
                JsonNode n = it.next();
                if (n != null && n.has("status")) {
                    String st = n.get("status").asText("");
                    if ("done".equals(st) || "skipped".equals(st) || "failed".equals(st)) {
                        completed++;
                    }
                }
            }
            root.put("completedSteps", completed);
            root.put("totalSteps", PROGRESS_TOTAL_STEPS);
            if (!root.has("startedAt")) {
                root.put("startedAt", now);
            }
            root.putNull("error");
            int pct = Math.min(100, (int) Math.round(completed * 100.0 / PROGRESS_TOTAL_STEPS));
            root.put("progress", pct);
            String progressJson = JSON.writeValueAsString(root);
            viral.setDeepAnalyzeProgress(progressJson);
            if (flush != null && flush.sseEmitter != null) {
                pushDeepAnalyzeSse(flush.sseEmitter, progressJson);
            }
            long t = System.currentTimeMillis();
            if (flush != null && !force && t - flush.lastFlushMs < 2000L) {
                return;
            }
            if (flush != null) {
                flush.lastFlushMs = t;
            }
            viralVideoRepository.save(viral);
        } catch (Exception e) {
            log.debug("deep_analyze_progress 更新跳过: {}", e.getMessage());
        }
    }

    private void pushDeepAnalyzeSse(SseEmitter emitter, String progressJson) {
        if (emitter == null || progressJson == null) {
            return;
        }
        try {
            JsonNode n = JSON.readTree(progressJson);
            emitter.send(SseEmitter.event().name("progress").data(n, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            log.debug("SSE progress 推送跳过: {}", e.getMessage());
        }
    }

    private void pushDeepAnalyzeDoneSse(SseEmitter emitter, Long viralVideoId, Long userId) {
        if (emitter == null) {
            return;
        }
        try {
            SvViralVideo v = viralVideoService.getViralVideo(viralVideoId, userId);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("taskId", viralVideoId);
            payload.put("viralVideoId", viralVideoId);
            payload.put("status", v.getDeepAnalyzeStatus() != null ? v.getDeepAnalyzeStatus() : "");
            payload.put("deepAnalyzeProgress", v.getDeepAnalyzeProgress());
            if (v.getDeepAnalyzeError() != null && !v.getDeepAnalyzeError().isBlank()) {
                payload.put("error", v.getDeepAnalyzeError());
            }
            emitter.send(SseEmitter.event().name("done").data(JSON.valueToTree(payload), MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            log.debug("SSE done 推送跳过: {}", e.getMessage());
        }
    }

    private void pushDeepAnalyzeErrorSse(SseEmitter emitter, String message) {
        if (emitter == null) {
            return;
        }
        try {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", message != null ? message : "unknown");
            emitter.send(SseEmitter.event().name("error").data(JSON.valueToTree(err), MediaType.APPLICATION_JSON));
        } catch (Exception ex) {
            log.debug("SSE error 推送跳过: {}", ex.getMessage());
        }
    }

    private void progressSetError(SvViralVideo viral, String err) {
        if (viral == null) {
            return;
        }
        try {
            ObjectNode root;
            if (StringUtils.hasText(viral.getDeepAnalyzeProgress())) {
                JsonNode parsed = JSON.readTree(viral.getDeepAnalyzeProgress());
                root = parsed.isObject() ? (ObjectNode) parsed : JSON.getNodeFactory().objectNode();
            } else {
                root = JSON.getNodeFactory().objectNode();
            }
            root.put("error", err != null ? err : "unknown");
            viral.setDeepAnalyzeProgress(JSON.writeValueAsString(root));
            viralVideoRepository.save(viral);
        } catch (Exception e) {
            log.debug("deep_analyze_progress error 写入跳过: {}", e.getMessage());
        }
    }

    private void progressFinalizeSuccess(SvViralVideo viral) {
        if (viral == null) {
            return;
        }
        try {
            ObjectNode root;
            if (StringUtils.hasText(viral.getDeepAnalyzeProgress())) {
                JsonNode parsed = JSON.readTree(viral.getDeepAnalyzeProgress());
                root = parsed.isObject() ? (ObjectNode) parsed : JSON.getNodeFactory().objectNode();
            } else {
                root = JSON.getNodeFactory().objectNode();
            }
            root.put("currentStep", "completed");
            root.put("progress", 100);
            root.putNull("error");
            viral.setDeepAnalyzeProgress(JSON.writeValueAsString(root));
        } catch (Exception e) {
            log.debug("deep_analyze_progress finalize 跳过: {}", e.getMessage());
        }
    }

    private void runPipeline(Long viralVideoId, Long userId, SseEmitter sseEmitter) {
        SvViralVideo viral = viralVideoService.getViralVideo(viralVideoId, userId);
        ProgressFlushState flush = new ProgressFlushState();
        flush.sseEmitter = sseEmitter;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("viralVideoId", viralVideoId);

        // ─── Step 0: 元数据提取（yt-dlp --dump-json，独立 try-catch 不阻塞管线）
        progressTouch(viral, flush, "metadata", "metadata", "running", "正在提取元数据…", null, true);
        extractAndApplyMetadata(viral);
        progressTouch(viral, flush, "metadata", "metadata", "done", "元数据已提取", null, true);

        String transcript;
        String sceneDescriptions;
        String videoPath = null;
        List<String> keyframePaths = null;
        List<SceneDetectionService.SceneSegment> sceneSegments = null;

        boolean hasVideoUrl = viral.getVideoUrl() != null && !viral.getVideoUrl().isBlank();

        if (hasVideoUrl && videoAnalysisService != null) {
            if (douyinSkipYtDlpDownload && isDouyinShareUrl(viral.getVideoUrl())) {
                log.info("[深度分析] douyin-skip-yt-dlp-download=true，跳过本机下载 viralId={}", viralVideoId);
                progressTouch(viral, flush, "download", "download", "skipped", "已配置跳过本机下载（抖音链接）", null, true);
                progressTouch(viral, flush, "asr", "asr", "skipped", "跳过本机 ASR", null, true);
                progressTouch(viral, flush, "scene", "scene", "skipped", "跳过本机抽帧", null, true);
                transcript = "（抖音链接：已配置跳过本机 yt-dlp 下载。抖音 Web 详情接口常需签名校验，yt-dlp 可能长期报 Fresh cookies；口播以下为基于标题与互动数据的推演，勿伪称原片转写。）";
                sceneDescriptions = "（无本机抽帧；若条目有封面图，将尽量用封面做视觉补充。）";
            } else {
                String t = "（无视频文件，仅有标题和元数据）";
                String s = "（无视频文件）";
                boolean downloadSucceeded = false;
                try {
                    progressTouch(viral, flush, "download", "download", "running", "正在下载视频…", null, true);
                    log.info("[深度分析] Step1 下载视频 viralId={}", viralVideoId);
                    videoPath = videoAnalysisService.downloadVideo(viral.getVideoUrl());
                    progressTouch(viral, flush, "download", "download", "done", "视频已下载", null, true);
                    downloadSucceeded = true;

                    log.info("[深度分析] Step2 ASR viralId={}", viralVideoId);
                    progressTouch(viral, flush, "asr", "asr", "running", "口播转写（ASR）…", null, true);
                    try {
                        String audioPath = videoAnalysisService.extractAudio(videoPath);
                        t = videoAnalysisService.transcribeAudio(audioPath);
                        // ASR 未启用时，给 LLM 提供推演指引而非原始技术标记
                        if ("[ASR 未启用]".equals(t)) {
                            t = "（ASR 未启用，无真实口播转写。请基于标题与互动数据推演完整口播稿，勿伪称来自 ASR。）";
                        }
                        progressTouch(viral, flush, "asr", "asr", "done", "ASR 完成", null, true);
                    } catch (Exception asrEx) {
                        log.warn("[深度分析] ASR 失败 viralId={}: {}，继续视觉分析", viralVideoId, asrEx.getMessage());
                        t = "（ASR 识别失败，无真实口播转写。请基于标题与互动数据推演完整口播稿，勿伪称来自 ASR。）";
                        progressTouch(viral, flush, "asr", "asr", "done", "ASR 失败，已用推演文案继续", null, true);
                    }

                    log.info("[深度分析] Step3 抽帧+视觉 viralId={}", viralVideoId);
                    progressTouch(viral, flush, "scene", "scene", "running", "场景检测与抽帧…", null, true);
                    // 优先用 scenecut 智能切场景
                    if (multiRoundAnalysis && sceneDetectionService != null) {
                        try {
                            sceneSegments = sceneDetectionService.detectScenes(videoPath);
                            log.info("[深度分析] SceneDetection 检测到 {} 个场景", sceneSegments.size());
                            // 收集关键帧路径用于 BOS 上传
                            keyframePaths = new ArrayList<>();
                            List<String> descs = new ArrayList<>();
                            for (int i = 0; i < sceneSegments.size(); i++) {
                                var seg = sceneSegments.get(i);
                                if (seg.keyframePath() != null) {
                                    keyframePaths.add(seg.keyframePath());
                                }
                                descs.add(String.format("场景%d [%.1fs-%.1fs]: 关键帧已提取",
                                        i + 1, seg.startTimeSec(), seg.endTimeSec()));
                            }
                            s = String.join("\n", descs);
                        } catch (Exception sce) {
                            log.warn("[深度分析] 场景检测失败，回退固定 FPS: {}", sce.getMessage());
                            sceneSegments = null;
                        }
                    }
                    // 回退到固定 FPS 抽帧
                    if (sceneSegments == null || sceneSegments.isEmpty()) {
                        List<String> framePaths = videoAnalysisService.extractFrames(videoPath, 1);
                        if (framePaths != null && !framePaths.isEmpty()) {
                            keyframePaths = new ArrayList<>(framePaths);
                            List<String> sub = framePaths.subList(0, Math.min(10, framePaths.size()));
                            List<String> descriptions = videoAnalysisService.analyzeFrames(sub,
                                    "描述这一帧的场景：环境、人物表情动作、道具产品、镜头构图、文字覆盖、情绪氛围。用中文，一句话概括。");
                            s = String.join("\n", descriptions);
                        }
                    }
                    progressTouch(viral, flush, "scene", "scene", "done", "场景/抽帧处理完成", null, true);
                } catch (Exception e) {
                    String full = e.getMessage();
                    String oneLine = full != null && !full.isEmpty()
                            ? full.split("\n", 2)[0]
                            : String.valueOf(e);
                    log.warn("[深度分析] 视频管线失败 viralId={}: {}", viralVideoId, oneLine);
                    if (full != null && full.length() > oneLine.length()) {
                        log.debug("[深度分析] 视频管线失败 详情 viralId={}: {}", viralVideoId, full);
                    }
                    // 下载若未成功收口，progress JSON 会长期为 download=running，与后续步骤矛盾
                    if (!downloadSucceeded) {
                        String dtl = "下载失败：" + truncateErr(oneLine);
                        progressTouch(viral, flush, "download", "download", "failed", dtl, null, true);
                        progressTouch(viral, flush, "asr", "asr", "skipped", "下载未成功，跳过 ASR", null, true);
                        t = "（视频下载失败，无真实口播转写。请基于标题与互动数据推演口播稿，勿伪称来自 ASR。）";
                        s = "（视频下载失败，无真实画面。请基于标题与互动数据推演分镜。）";
                    } else {
                        t = "（下载已成功，但抽帧或画面分析失败，无可靠分镜描述。请结合标题与互动数据推演，勿伪称来自实拍抽帧。）";
                        s = "（下载已成功，但抽帧或画面分析失败，无真实关键帧。请基于标题与互动数据推演分镜。）";
                    }
                    progressTouch(viral, flush, "scene", "scene", "done", "视频管线异常，已降级为推演模式", null, true);
                }
                // 注意：视频文件清理延迟到 BOS 上传之后
                transcript = t;
                sceneDescriptions = s;
            }
        } else if (hasVideoUrl) {
            log.info("[深度分析] 无 VideoAnalysisService，元数据推演模式 viralId={}", viralVideoId);
            progressTouch(viral, flush, "download", "download", "skipped", "未启用本机视频解析", null, true);
            progressTouch(viral, flush, "asr", "asr", "skipped", "未启用 ASR", null, true);
            progressTouch(viral, flush, "scene", "scene", "skipped", "未启用抽帧", null, true);
            transcript = TRANSCRIPT_PIPELINE_UNAVAILABLE;
            sceneDescriptions = SCENE_PIPELINE_UNAVAILABLE;
        } else {
            progressTouch(viral, flush, "download", "download", "skipped", "无视频链接", null, true);
            progressTouch(viral, flush, "asr", "asr", "skipped", "无视频链接", null, true);
            progressTouch(viral, flush, "scene", "scene", "skipped", "无视频链接", null, true);
            transcript = "（无视频文件，仅有标题和元数据）";
            sceneDescriptions = "（无视频文件）";
        }

        boolean sceneNeedsCoverFallback = "（无视频文件）".equals(sceneDescriptions)
                || sceneDescriptions.startsWith("（未启用本机抽帧")
                || sceneDescriptions.startsWith("（抽帧分析失败")
                || sceneDescriptions.startsWith("（无本机抽帧；若条目有封面图");
        if (sceneNeedsCoverFallback && viral.getCoverUrl() != null && !viral.getCoverUrl().isBlank()
                && llmClient != null) {
            try {
                AiModel vision = pickVisionModel();
                if (vision != null) {
                    var resp = llmClient.chatWithImage(vision, "你是视觉分析专家。",
                            "描述这张短视频封面：场景、人物、产品、文字、构图、色调。用中文。",
                            List.of(viral.getCoverUrl()));
                    if (resp != null && resp.success()) {
                        sceneDescriptions = "封面分析: " + resp.content();
                    }
                }
            } catch (Exception e) {
                log.warn("[深度分析] 封面分析失败: {}", e.getMessage());
            }
        }

        // ─── Step 3.5: BOS 上传（视频+关键帧+封面） ───
        progressTouch(viral, flush, "bos", "bos", "running", "上传 BOS（视频/关键帧/封面）…", null, true);
        uploadToBos(viral, videoPath, keyframePaths);
        progressTouch(viral, flush, "bos", "bos", "done", "BOS 上传完成", null, true);

        // 清理本地临时文件（BOS 上传完成后）
        progressTouch(viral, flush, "cleanup", "cleanup", "running", "清理本地临时文件…", null, true);
        cleanupLocalFiles(videoPath, sceneSegments);
        progressTouch(viral, flush, "cleanup", "cleanup", "done", "临时文件已清理", null, true);

        viral.setTranscript(transcript);
        viral.setSceneDescriptions(sceneDescriptions);
        viralVideoRepository.save(viral);

        result.put("transcript", transcript);
        result.put("sceneDescriptions", sceneDescriptions);

        // ─── Step 4: 评论抓取（需在 Round3 多轮 LLM 之前，以便注入评论抽样） ───
        progressTouch(viral, flush, "comments", "comments", "running", "抓取评论…", null, true);
        extractAndSaveComments(viral, result);
        progressTouch(viral, flush, "comments", "comments",
                result.containsKey("commentsSaved") ? "done" : "skipped",
                result.containsKey("commentsSaved") ? "已保存评论" : "无评论或未抓取", null, true);
        String commentSnippet = buildCommentSnippetForRound3(viral.getId());
        String evidenceLevel = resolveEvidenceLevel(transcript, sceneDescriptions, videoPath != null);

        // 选择单次大 prompt 或多轮结构化拆解
        if (multiRoundAnalysis && sceneSegments != null && !sceneSegments.isEmpty()
                && llmClient != null && pickVisionModel() != null) {
            runMultiRoundAnalysis(viral, transcript, sceneSegments, result, commentSnippet, evidenceLevel, flush);
        } else {
            runSinglePromptAnalysis(viral, transcript, sceneDescriptions, result, commentSnippet, evidenceLevel, flush);
        }

        // 延迟清理场景关键帧（多轮分析 Round 1 需要读取这些文件）
        if (videoPath != null && sceneDetectionService != null) {
            try {
                sceneDetectionService.cleanup(videoPath);
            } catch (Exception ex) {
                log.debug("scene cleanup: {}", ex.getMessage());
            }
        }

        result.put("status", result.containsKey("error") ? "partial" : "completed");
        log.info("[深度分析] 结束 viralId={} status={}", viralVideoId, result.get("status"));
    }

    // ─── 元数据提取 ──────────────────────────────────────

    private void extractAndApplyMetadata(SvViralVideo viral) {
        if (viralMetadataExtractor == null) return;
        if (viral.getVideoUrl() == null || viral.getVideoUrl().isBlank()) return;

        try {
            log.info("[深度分析] Step0 元数据提取 viralId={}", viral.getId());
            var meta = viralMetadataExtractor.extract(viral.getVideoUrl());
            if (meta == null) {
                log.info("[深度分析] 元数据提取无结果 viralId={}", viral.getId());
                return;
            }

            // 只覆盖原来为空或为 0 的字段
            if (meta.viewCount() != null && meta.viewCount() > 0) viral.setViewCount(meta.viewCount());
            if (meta.likeCount() != null && meta.likeCount() > 0) viral.setLikeCount(meta.likeCount());
            if (meta.favoriteCount() != null) viral.setFavoriteCount(meta.favoriteCount());
            if (meta.commentCount() != null) viral.setCommentCount(meta.commentCount());
            if (meta.shareCount() != null && meta.shareCount() > 0) viral.setShareCount(meta.shareCount());
            if (meta.videoDuration() != null && meta.videoDuration() > 0) viral.setVideoDuration(meta.videoDuration());
            if (meta.description() != null) viral.setDescription(meta.description());
            if (meta.authorId() != null) viral.setAuthorId(meta.authorId());
            if (meta.authorName() != null) viral.setAuthorName(meta.authorName());
            if (meta.authorFollowers() != null) viral.setAuthorFollowers(meta.authorFollowers());
            if (meta.musicName() != null) viral.setMusicName(meta.musicName());
            if (meta.hashtags() != null && !meta.hashtags().isEmpty()) {
                try {
                    viral.setHashtags(JSON.writeValueAsString(meta.hashtags()));
                } catch (Exception ignored) {
                    // JSON序列化失败，跳过hashtags
                }
            }
            if (meta.coverUrl() != null && (viral.getCoverUrl() == null || viral.getCoverUrl().isBlank())) {
                viral.setCoverUrl(meta.coverUrl());
            }
            if (meta.rawJson() != null) viral.setMetadataJson(meta.rawJson());

            viralVideoRepository.save(viral);
            log.info("[深度分析] 元数据已更新 viralId={}", viral.getId());
        } catch (Exception e) {
            log.warn("[深度分析] 元数据提取异常（不阻塞管线）viralId={}: {}", viral.getId(), e.getMessage());
        }
    }

    // ─── BOS 上传 ──────────────────────────────────────

    private void uploadToBos(SvViralVideo viral, String videoPath, List<String> keyframePaths) {
        if (viralBosUploader == null) return;

        try {
            log.info("[深度分析] Step3.5 BOS 上传 viralId={}", viral.getId());

            // 上传视频
            if (videoPath != null) {
                var videoResult = viralBosUploader.uploadVideo(viral.getOwnerId(), viral.getId(), videoPath);
                if (videoResult != null) {
                    viral.setVideoBosKey(videoResult.bosKey());
                    viral.setVideoBosUrl(videoResult.cdnUrl());
                }
            }

            // 上传关键帧
            if (keyframePaths != null && !keyframePaths.isEmpty()) {
                var kfResults = viralBosUploader.uploadKeyframes(viral.getOwnerId(), viral.getId(), keyframePaths);
                if (!kfResults.isEmpty()) {
                    List<String> keys = new ArrayList<>();
                    List<String> urls = new ArrayList<>();
                    for (var r : kfResults) {
                        keys.add(r.bosKey());
                        urls.add(r.cdnUrl());
                    }
                    viral.setKeyframeBosKeys(JSON.writeValueAsString(keys));
                    viral.setKeyframeBosUrls(JSON.writeValueAsString(urls));
                }
            }

            // 上传封面
            if (viral.getCoverUrl() != null && !viral.getCoverUrl().isBlank()) {
                var coverResult = viralBosUploader.uploadCover(viral.getOwnerId(), viral.getId(), viral.getCoverUrl());
                if (coverResult != null) {
                    viral.setCoverBosKey(coverResult.bosKey());
                    viral.setCoverBosUrl(coverResult.cdnUrl());
                }
            }

            viralVideoRepository.save(viral);
        } catch (Exception e) {
            log.warn("[深度分析] BOS 上传异常（不阻塞管线）viralId={}: {}", viral.getId(), e.getMessage());
        }
    }

    // ─── 评论抓取 ──────────────────────────────────────

    private void extractAndSaveComments(SvViralVideo viral, Map<String, Object> result) {
        if (viralCommentExtractor == null || svCommentRepository == null) {
            return;
        }
        if (viral.getVideoUrl() == null || viral.getVideoUrl().isBlank()) {
            return;
        }

        try {
            log.info("[深度分析] Step4 评论抓取 viralId={}", viral.getId());
            List<SvComment> comments = viralCommentExtractor.extractComments(viral.getVideoUrl(), viral.getId());
            if (!comments.isEmpty()) {
                svCommentRepository.saveAll(comments);
                log.info("[深度分析] 已保存 {} 条评论 viralId={}", comments.size(), viral.getId());
                if (result != null) {
                    result.put("commentsSaved", true);
                }
            }
        } catch (Exception e) {
            log.warn("[深度分析] 评论抓取异常（不影响分析结果）viralId={}: {}", viral.getId(), e.getMessage());
        }
    }

    // ─── 本地文件清理 ──────────────────────────────────────

    private void cleanupLocalFiles(String videoPath, List<SceneDetectionService.SceneSegment> _sceneSegments) {
        if (videoPath != null && videoAnalysisService != null) {
            try {
                videoAnalysisService.cleanup(videoPath);
            } catch (Exception ex) {
                log.debug("cleanup video: {}", ex.getMessage());
            }
        }
    }

    // ─── 多轮结构化拆解 ──────────────────────────────────────

    private static final String ROUND1_SCENE_VISION_PROMPT = """
            你是短视频画面分析专家。请仔细观察这张关键帧，描述：
            1. 环境/场景（室内/室外、地点特征）
            2. 人物（外貌、表情、动作、服装）
            3. 道具/产品（品牌、展示方式）
            4. 镜头构图（特写/中景/远景、角度、运镜）
            5. 画面文字/字幕
            6. 色调/滤镜/情绪氛围
            用中文，3-5 句话。这是第 %d 个场景（时间 %.1fs-%.1fs）。
            """;

    private static final String ROUND2_NARRATIVE_PROMPT = """
            你是短视频叙事结构分析专家。基于以下口播全文和场景描述，识别五段式结构。

            【口播全文（ASR）】
            %s

            【场景描述（含时间戳）】
            %s

            严格要求：只输出一个合法 JSON 对象，不要输出任何其他文字、解释或注释。所有字符串值中不要使用未转义的双引号。
            JSON 结构：
            {
              "transcript": {
                "fullText": "清洗后完整口播稿",
                "sentences": [{"time": "0:00-0:03", "text": "台词", "role": "hook/content/cta"}],
                "goldenSentences": ["金句1", "金句2"]
              },
              "structure": {
                "hook": {"time": "0-3s", "type": "悬念/冲突/提问/反转/共情", "text": "钩子内容", "score": 85},
                "painPoint": {"time": "3-8s", "description": "痛点/好奇引发"},
                "proof": {"time": "8-20s", "description": "证明/演示/对比"},
                "trust": {"time": "20-25s", "description": "信任/背书/用户证言"},
                "cta": {"time": "最后3s", "text": "互动/转化句", "type": "点赞/关注/购买/评论"}
              }
            }
            """;

    private static final String ROUND3_VIRAL_HYPOTHESES_PROMPT = """
            你是短视频爆款分析专家。基于以下场景视觉分析、叙事结构和互动数据，从 6 个维度分析爆款因子。

            【场景视觉分析】
            %s

            【叙事结构】
            %s

            【互动数据】
            标题: %s
            点赞: %s | 分享: %s | 评论数: %s | 播放: %s

            【评论抽样】（用于判断观众共鸣；若无抓取则写「无」）
            %s

            严格要求：只输出一个合法 JSON 对象，不要输出任何其他文字、解释或注释。所有字符串值中不要使用未转义的双引号。
            JSON 结构：
            {
              "viralHypotheses": {
                "emotionTrigger": "踩中什么情绪/身份认同（详细说明）",
                "infoDensity": "high/medium/low + 理由",
                "rhythmPattern": "节奏特征描述（快节奏/慢起快落/等分等）",
                "visualContrast": "视觉对比/反差描述",
                "controversyFactor": "争议点（如有）",
                "trendAlignment": "与当前热点/音乐/模板的关系"
              }
            }
            """;

    private static final String ROUND4_REMAKE_PROMPT = """
            你是短视频二创策略专家。基于以下所有分析结果，输出可复刻变量表和评分。

            【场景视觉分析】
            %s

            【叙事结构】
            %s

            【爆款因子假设】
            %s

            严格要求：只输出一个合法 JSON 对象，不要输出任何其他文字、解释或注释。所有字符串值中不要使用未转义的双引号。
            JSON 结构：
            {
              "remakeVariableTable": {
                "mustKeep": ["必须保留的结构元素1", "元素2"],
                "replaceable": [
                  {"variable": "人设", "original": "原视频人设", "suggestion": "替换建议"},
                  {"variable": "产品", "original": "原视频产品", "suggestion": "替换建议"},
                  {"variable": "场景", "original": "原场景", "suggestion": "替换建议"},
                  {"variable": "口头禅", "original": "原口头禅", "suggestion": "替换建议"},
                  {"variable": "BGM", "original": "原BGM风格", "suggestion": "同风格替换建议"}
                ]
              },
              "viralScore": 0,
              "bestRemakeType": "form_imitation/content_flip/element_recombination/dimensional_upgrade",
              "complianceNotes": ["合规注意事项"]
            }
            """;

    /**
     * 多轮结构化拆解：4 轮 LLM 调用，场景级视觉→叙事结构→爆款因子→二创变量表
     */
    private void runMultiRoundAnalysis(SvViralVideo viral, String transcript,
                                       List<SceneDetectionService.SceneSegment> scenes,
                                       Map<String, Object> result,
                                       String commentSnippet,
                                       String evidenceLevel,
                                       ProgressFlushState flush) {
        AiModel visionModel = pickVisionModel();
        AiModel textModel = pickDefaultTextModel();

        try {
            List<Integer> visionIndices = indicesForVisionCalls(scenes);
            // Round 1: 场景级视觉分析
            progressTouch(viral, flush, "llm_round1", "llm_round1", "running", "多轮分析 Round1：场景视觉…", null, true);
            log.info("[多轮拆解] Round 1: 场景级视觉分析 ({} 个场景, 识图 {} 个)", scenes.size(), visionIndices.size());
            List<String> sceneVisualDescs = new ArrayList<>();
            for (int i = 0; i < scenes.size(); i++) {
                var seg = scenes.get(i);
                if (!visionIndices.contains(i)) {
                    sceneVisualDescs.add(String.format("[场景%d %.1fs-%.1fs] （按 multi-round-max-vision-scenes 策略跳过单独识图，请结合口播与相邻场景理解）",
                            i + 1, seg.startTimeSec(), seg.endTimeSec()));
                    continue;
                }
                try {
                    byte[] imgBytes = Files.readAllBytes(Paths.get(seg.keyframePath()));
                    String base64 = Base64.getEncoder().encodeToString(imgBytes);
                    String dataUrl = "data:image/jpeg;base64," + base64;
                    String prompt = String.format(ROUND1_SCENE_VISION_PROMPT, i + 1, seg.startTimeSec(), seg.endTimeSec());
                    var resp = llmClient.chatWithImage(visionModel,
                            "你是短视频画面分析专家，用中文回答。", prompt, List.of(dataUrl));
                    if (meterRegistry != null) {
                        meterRegistry.counter("viral.deep_analyze.vision_round1_call").increment();
                    }
                    if (resp != null && resp.success() && resp.content() != null) {
                        sceneVisualDescs.add(String.format("[场景%d %.1fs-%.1fs] %s",
                                i + 1, seg.startTimeSec(), seg.endTimeSec(), resp.content()));
                    } else {
                        sceneVisualDescs.add(String.format("[场景%d %.1fs-%.1fs] （视觉分析无响应）",
                                i + 1, seg.startTimeSec(), seg.endTimeSec()));
                    }
                } catch (Exception e) {
                    log.warn("[多轮拆解] 场景 {} 视觉分析失败: {}", i + 1, e.getMessage());
                    sceneVisualDescs.add(String.format("[场景%d %.1fs-%.1fs] （分析失败）",
                            i + 1, seg.startTimeSec(), seg.endTimeSec()));
                }
            }
            String round1Result = String.join("\n", sceneVisualDescs);
            result.put("round1_sceneVision", round1Result);
            // 更新 sceneDescriptions
            viral.setSceneDescriptions(round1Result);
            viralVideoRepository.save(viral);
            progressTouch(viral, flush, "llm_round1", "llm_round1", "done", "Round1 场景视觉完成", null, true);

            // Round 2: 叙事结构拆解
            progressTouch(viral, flush, "llm_round2", "llm_round2", "running", "Round2：叙事结构…", null, true);
            log.info("[多轮拆解] Round 2: 叙事结构拆解");
            String round2Result = "";
            try {
                String round2Prompt = String.format(ROUND2_NARRATIVE_PROMPT, transcript, round1Result);
                var round2Resp = llmClient.chat(textModel,
                        "你是短视频叙事结构分析专家，输出精确的五段式结构。", round2Prompt);
                if (round2Resp != null && round2Resp.success() && round2Resp.content() != null) {
                    round2Result = stripJsonFence(round2Resp.content());
                    result.put("round2_narrative", round2Result);
                }
            } catch (Exception e) {
                log.warn("[多轮拆解] Round2 失败: {}", e.getMessage());
                result.put("round2_error", e.getMessage());
                if (!llmContinueOnFailure) {
                    throw e;
                }
            }
            progressTouch(viral, flush, "llm_round2", "llm_round2", "done",
                    round2Result.isBlank() ? "Round2 无有效输出" : "Round2 叙事结构完成",
                    null, true);

            // Round 3: 爆款因子假设
            progressTouch(viral, flush, "llm_round3", "llm_round3", "running", "Round3：爆款因子…", null, true);
            log.info("[多轮拆解] Round 3: 爆款因子假设");
            String round3Result = "";
            try {
                String round3Prompt = String.format(ROUND3_VIRAL_HYPOTHESES_PROMPT,
                        round1Result, round2Result,
                        viral.getTitle() != null ? viral.getTitle() : "",
                        viral.getLikeCount() != null ? String.valueOf(viral.getLikeCount()) : "N/A",
                        viral.getShareCount() != null ? String.valueOf(viral.getShareCount()) : "N/A",
                        viral.getCommentCount() != null ? String.valueOf(viral.getCommentCount()) : "N/A",
                        viral.getViewCount() != null ? String.valueOf(viral.getViewCount()) : "N/A",
                        commentSnippet != null ? commentSnippet : "无");
                var round3Resp = llmClient.chat(textModel,
                        "你是短视频爆款分析专家。", round3Prompt);
                if (round3Resp != null && round3Resp.success() && round3Resp.content() != null) {
                    round3Result = stripJsonFence(round3Resp.content());
                    result.put("round3_viralHypotheses", round3Result);
                }
            } catch (Exception e) {
                log.warn("[多轮拆解] Round3 失败: {}", e.getMessage());
                result.put("round3_error", e.getMessage());
                if (!llmContinueOnFailure) {
                    throw e;
                }
            }
            progressTouch(viral, flush, "llm_round3", "llm_round3", "done",
                    round3Result.isBlank() ? "Round3 无有效输出" : "Round3 爆款因子完成",
                    null, true);

            // Round 4: 可复刻变量表
            progressTouch(viral, flush, "llm_round4", "llm_round4", "running", "Round4：二创变量表…", null, true);
            log.info("[多轮拆解] Round 4: 可复刻变量表");
            String round4Result = "";
            try {
                String round4Prompt = String.format(ROUND4_REMAKE_PROMPT,
                        round1Result, round2Result, round3Result);
                var round4Resp = llmClient.chat(textModel,
                        "你是短视频二创策略专家。", round4Prompt);
                if (round4Resp != null && round4Resp.success() && round4Resp.content() != null) {
                    round4Result = stripJsonFence(round4Resp.content());
                    result.put("round4_remake", round4Result);
                }
            } catch (Exception e) {
                log.warn("[多轮拆解] Round4 失败: {}", e.getMessage());
                result.put("round4_error", e.getMessage());
                if (!llmContinueOnFailure) {
                    throw e;
                }
            }
            progressTouch(viral, flush, "llm_round4", "llm_round4", "done",
                    round4Result.isBlank() ? "Round4 无有效输出" : "Round4 二创变量完成",
                    null, true);

            // 合并 4 轮结果为完整 JSON
            String mergedJson = mergeMultiRoundResults(round1Result, round2Result, round3Result, round4Result,
                    scenes, transcript, commentSnippet, evidenceLevel);
            mergedJson = attachRoundsRaw(mergedJson, round1Result, round2Result, round3Result, round4Result, scenes.size());

            // 检查合并结果是否完整（至少需要有 structure 或 viralHypotheses）
            boolean mergeIncomplete = false;
            try {
                JsonNode merged = JSON.readTree(mergedJson);
                mergeIncomplete = !merged.has("structure") || !merged.has("viralHypotheses");
            } catch (Exception e) {
                mergeIncomplete = true;
            }

            if (mergeIncomplete) {
                if (llmContinueOnFailure && anyRoundTextNonBlank(round2Result, round3Result, round4Result)) {
                    mergedJson = mergeMultiRoundResults(round1Result, round2Result, round3Result, round4Result, scenes,
                            transcript, commentSnippet, partialEvidenceLevel(evidenceLevel, round2Result, round3Result, round4Result));
                    mergedJson = attachRoundsRaw(mergedJson, round1Result, round2Result, round3Result, round4Result, scenes.size());
                    persistDeepAnalysisSuccess(viral, mergedJson, buildStepSummaryMap(result, true, true));
                    log.info("[多轮拆解] 部分合并成功（structure/viralHypotheses 不全，已保留可用字段）");
                    recordDeepAnalyzeMetric(true);
                    return;
                }
                log.warn("[多轮拆解] 合并结果不完整（缺少 structure 或 viralHypotheses），回退单次大 prompt 补充");
                String sceneDesc = round1Result.isBlank() ?
                        (viral.getSceneDescriptions() != null ? viral.getSceneDescriptions() : "") : round1Result;
                runSinglePromptAnalysis(viral, transcript, sceneDesc, result, commentSnippet, evidenceLevel, flush);
                return;
            }

            persistDeepAnalysisSuccess(viral, mergedJson, buildStepSummaryMap(result, true, true));
            log.info("[多轮拆解] 4 轮分析全部完成");
            recordDeepAnalyzeMetric(true);

        } catch (Exception e) {
            log.error("[多轮拆解] 失败，回退单次大 prompt: {}", e.getMessage());
            String sceneDesc = viral.getSceneDescriptions() != null ? viral.getSceneDescriptions() : "";
            runSinglePromptAnalysis(viral, transcript, sceneDesc, result, commentSnippet, evidenceLevel, flush);
        }
    }

    private static boolean anyRoundTextNonBlank(String r2, String r3, String r4) {
        return StringUtils.hasText(r2) || StringUtils.hasText(r3) || StringUtils.hasText(r4);
    }

    private static String partialEvidenceLevel(String base, String r2, String r3, String r4) {
        if (!StringUtils.hasText(r2) || !StringUtils.hasText(r3) || !StringUtils.hasText(r4)) {
            return "inferred";
        }
        return base != null ? base : "inferred";
    }

    private String attachRoundsRaw(String mergedJson, String r1, String r2, String r3, String r4, int sceneCount) {
        try {
            ObjectNode root = (ObjectNode) JSON.readTree(mergedJson);
            ObjectNode raw = JSON.createObjectNode();
            raw.put("round1_sceneVision", r1 != null ? r1 : "");
            raw.put("round2_narrative", r2 != null ? r2 : "");
            raw.put("round3_viralHypotheses", r3 != null ? r3 : "");
            raw.put("round4_remake", r4 != null ? r4 : "");
            raw.put("sceneCount", sceneCount);
            root.set("_roundsRaw", raw);
            return JSON.writeValueAsString(root);
        } catch (Exception e) {
            log.debug("attachRoundsRaw 跳过: {}", e.getMessage());
            return mergedJson;
        }
    }

    private static String normalizeRoundKey(String raw) {
        if (raw == null) {
            return "";
        }
        String r = raw.trim().toLowerCase();
        if (r.startsWith("round2") || "round2_narrative".equals(r)) {
            return "round2_narrative";
        }
        if (r.startsWith("round3") || r.contains("hypotheses")) {
            return "round3_viralHypotheses";
        }
        if (r.startsWith("round4") || r.contains("remake") || r.contains("variable")) {
            return "round4_remake";
        }
        return r;
    }

    private List<SceneDetectionService.SceneSegment> buildFakeScenes(int sceneCount) {
        List<SceneDetectionService.SceneSegment> list = new ArrayList<>();
        int n = Math.max(1, sceneCount);
        for (int i = 0; i < n; i++) {
            list.add(new SceneDetectionService.SceneSegment(i * 1.0, (i + 1) * 1.0, null));
        }
        return list;
    }

    private String executeRound2Llm(String transcript, String round1, AiModel textModel) {
        String round2Prompt = String.format(ROUND2_NARRATIVE_PROMPT, transcript, round1);
        var round2Resp = llmClient.chat(textModel,
                "你是短视频叙事结构分析专家，输出精确的五段式结构。", round2Prompt);
        if (round2Resp == null || !round2Resp.success() || round2Resp.content() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Round2 LLM 无有效响应");
        }
        return stripJsonFence(round2Resp.content());
    }

    private String executeRound3Llm(SvViralVideo viral, String round1, String round2, String commentSnippet, AiModel textModel) {
        String round3Prompt = String.format(ROUND3_VIRAL_HYPOTHESES_PROMPT,
                round1, round2,
                viral.getTitle() != null ? viral.getTitle() : "",
                viral.getLikeCount() != null ? String.valueOf(viral.getLikeCount()) : "N/A",
                viral.getShareCount() != null ? String.valueOf(viral.getShareCount()) : "N/A",
                viral.getCommentCount() != null ? String.valueOf(viral.getCommentCount()) : "N/A",
                viral.getViewCount() != null ? String.valueOf(viral.getViewCount()) : "N/A",
                commentSnippet != null ? commentSnippet : "无");
        var round3Resp = llmClient.chat(textModel,
                "你是短视频爆款分析专家。", round3Prompt);
        if (round3Resp == null || !round3Resp.success() || round3Resp.content() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Round3 LLM 无有效响应");
        }
        return stripJsonFence(round3Resp.content());
    }

    private String executeRound4Llm(String round1, String round2, String round3, AiModel textModel) {
        String round4Prompt = String.format(ROUND4_REMAKE_PROMPT, round1, round2, round3);
        var round4Resp = llmClient.chat(textModel,
                "你是短视频二创策略专家。", round4Prompt);
        if (round4Resp == null || !round4Resp.success() || round4Resp.content() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Round4 LLM 无有效响应");
        }
        return stripJsonFence(round4Resp.content());
    }

    /**
     * 重跑多轮分析中的某一文本轮次（需 deep_analysis_result 中含 {@code _roundsRaw}）。
     */
    public Map<String, Object> retryDeepAnalyzeRound(Long viralId, Long userId, String roundKey) {
        if (roundKey == null || roundKey.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "round 不能为空");
        }
        SvViralVideo viral = viralVideoService.getViralVideo(viralId, userId);
        if (llmClient == null || pickDefaultTextModel() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM 未配置");
        }
        String merged = viral.getDeepAnalysisResult();
        if (merged == null || merged.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "无深度分析结果");
        }
        JsonNode root;
        try {
            root = JSON.readTree(merged);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "深度分析结果 JSON 无效");
        }
        JsonNode raw = root.get("_roundsRaw");
        if (raw == null || raw.isNull()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "无多轮原始数据（_roundsRaw），请重新完整深度分析");
        }
        String r1 = raw.path("round1_sceneVision").asText("");
        String r2 = raw.path("round2_narrative").asText("");
        String r3 = raw.path("round3_viralHypotheses").asText("");
        String r4 = raw.path("round4_remake").asText("");
        int sceneCount = raw.path("sceneCount").asInt(1);
        String transcript = viral.getTranscript();
        if (transcript == null || transcript.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "无口播稿，无法重试");
        }
        String norm = normalizeRoundKey(roundKey);
        AiModel textModel = pickDefaultTextModel();
        String commentSnippet = buildCommentSnippetForRound3(viral.getId());
        String evidenceLevel = resolveEvidenceLevel(transcript, r1, true);

        switch (norm) {
            case "round2_narrative":
                r2 = executeRound2Llm(transcript, r1, textModel);
                break;
            case "round3_viralHypotheses":
                r3 = executeRound3Llm(viral, r1, r2, commentSnippet, textModel);
                break;
            case "round4_remake":
                r4 = executeRound4Llm(r1, r2, r3, textModel);
                break;
            default:
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "不支持的 round：" + roundKey);
        }

        List<SceneDetectionService.SceneSegment> fakeScenes = buildFakeScenes(sceneCount);
        String mergedJson = mergeMultiRoundResults(r1, r2, r3, r4, fakeScenes, transcript, commentSnippet, evidenceLevel);
        mergedJson = attachRoundsRaw(mergedJson, r1, r2, r3, r4, sceneCount);
        persistDeepAnalysisSuccess(viral, mergedJson, buildStepSummaryMap(new LinkedHashMap<>(), true, true));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("viralVideoId", viralId);
        return out;
    }

    /**
     * 合并 4 轮分析结果为一个完整 JSON（与现有 DEEP_ANALYSIS_PROMPT 格式兼容）
     */
    private String mergeMultiRoundResults(String round1, String round2, String round3, String round4,
                                           List<SceneDetectionService.SceneSegment> scenes,
                                           String transcript,
                                           String commentSnippet,
                                           String evidenceLevel) {
        try {
            Map<String, Object> merged = new LinkedHashMap<>();

            // Round 2: transcript + structure（独立 try-catch，LLM 输出格式有时不规范）
            if (!round2.isBlank()) {
                try {
                    JsonNode r2 = JSON.readTree(round2);
                    if (r2.has("transcript")) merged.put("transcript", JSON.readValue(r2.get("transcript").toString(), Object.class));
                    if (r2.has("structure")) merged.put("structure", JSON.readValue(r2.get("structure").toString(), Object.class));
                } catch (Exception e) {
                    log.debug("[多轮拆解] Round2 JSON 解析跳过: {}", e.getMessage());
                }
            }

            List<String> visionPerScene = parseRound1VisionBySceneIndex(round1, scenes.size());

            // Round 1: scenes（时间轴 + Round1 视觉摘要写入 environment）
            List<Map<String, String>> scenesList = new ArrayList<>();
            for (int i = 0; i < scenes.size(); i++) {
                var seg = scenes.get(i);
                Map<String, String> sceneMap = new LinkedHashMap<>();
                sceneMap.put("time", String.format("%.1fs-%.1fs", seg.startTimeSec(), seg.endTimeSec()));
                String env = i < visionPerScene.size() ? visionPerScene.get(i) : "";
                sceneMap.put("environment", env != null ? env : "");
                sceneMap.put("person", "");
                sceneMap.put("props", "");
                sceneMap.put("camera", "");
                sceneMap.put("mood", "");
                scenesList.add(sceneMap);
            }
            merged.put("scenes", scenesList);

            // Round 3: viralHypotheses
            if (!round3.isBlank()) {
                try {
                    JsonNode r3 = JSON.readTree(round3);
                    if (r3.has("viralHypotheses")) merged.put("viralHypotheses", JSON.readValue(r3.get("viralHypotheses").toString(), Object.class));
                } catch (Exception e) {
                    log.debug("[多轮拆解] Round3 JSON 解析跳过: {}", e.getMessage());
                }
            }

            // Round 4: remakeVariableTable, viralScore, bestRemakeType, complianceNotes
            if (!round4.isBlank()) {
                try {
                    JsonNode r4 = JSON.readTree(round4);
                    if (r4.has("remakeVariableTable")) merged.put("remakeVariableTable", JSON.readValue(r4.get("remakeVariableTable").toString(), Object.class));
                    if (r4.has("viralScore")) merged.put("viralScore", r4.get("viralScore").asInt(0));
                    if (r4.has("bestRemakeType")) merged.put("bestRemakeType", r4.get("bestRemakeType").asText(""));
                    if (r4.has("complianceNotes")) merged.put("complianceNotes", JSON.readValue(r4.get("complianceNotes").toString(), Object.class));
                } catch (Exception e) {
                    log.debug("[多轮拆解] Round4 JSON 解析跳过: {}", e.getMessage());
                }
            }

            merged.put("_multiRound", true);
            merged.put("_sceneVisionDetails", round1);
            merged.put("evidenceLevel", evidenceLevel != null ? evidenceLevel : "inferred");
            merged.put("evidenceDetails", buildEvidenceDetails(transcript, round1, commentSnippet, evidenceLevel));

            return JSON.writeValueAsString(merged);
        } catch (Exception e) {
            log.warn("[多轮拆解] 合并 JSON 失败: {}", e.getMessage());
            return round4.isBlank() ? round2 : round4;
        }
    }

    /**
     * 原有单次大 prompt 分析（降级路径）
     */
    private void runSinglePromptAnalysis(SvViralVideo viral, String transcript,
                                          String sceneDescriptions, Map<String, Object> result,
                                          String commentSnippet,
                                          String evidenceLevel,
                                          ProgressFlushState flush) {
        AiModel textModel = pickDefaultTextModel();
        if (llmClient != null && textModel != null) {
            progressTouch(viral, flush, "llm_single", "llm_single", "running", "单次大模型拆解…", null, true);
            try {
                String cc = viral.getCommentCount() != null ? String.valueOf(viral.getCommentCount()) : "N/A";
                String snippet = commentSnippet != null ? commentSnippet : "无";
                String prompt = String.format(DEEP_ANALYSIS_PROMPT,
                        transcript,
                        sceneDescriptions,
                        viral.getTitle() != null ? viral.getTitle() : "",
                        viral.getLikeCount() != null ? String.valueOf(viral.getLikeCount()) : "N/A",
                        viral.getShareCount() != null ? String.valueOf(viral.getShareCount()) : "N/A",
                        cc,
                        viral.getViewCount() != null ? String.valueOf(viral.getViewCount()) : "N/A",
                        snippet);

                var resp = llmClient.chat(textModel,
                        "你是短视频爆款分析专家，擅长拆解爆款结构并输出可执行的二创变量表。",
                        prompt);

                if (resp != null && resp.success() && resp.content() != null) {
                    String raw = stripJsonFence(resp.content());
                    raw = ensureEvidenceLevelOnJson(raw, transcript, sceneDescriptions, commentSnippet, evidenceLevel);
                    result.put("analysis", raw);
                    result.put("tokensUsed", resp.tokensUsed());
                    progressTouch(viral, flush, "llm_single", "llm_single", "done", "单次 LLM 拆解完成", null, true);
                    persistDeepAnalysisSuccess(viral, raw, buildStepSummaryMap(result, false, true));
                    recordDeepAnalyzeMetric(true);
                } else {
                    result.put("error", resp != null ? resp.errorMsg() : "LLM 无响应");
                    progressTouch(viral, flush, "llm_single", "llm_single", "failed", String.valueOf(result.get("error")), null, true);
                    markDeepAnalyzeLlmFailed(viral, String.valueOf(result.get("error")), result);
                    recordDeepAnalyzeMetric(false);
                }
            } catch (Exception e) {
                log.error("[深度分析] LLM 拆解失败: {}", e.getMessage());
                result.put("error", "LLM 分析失败: " + e.getMessage());
                progressTouch(viral, flush, "llm_single", "llm_single", "failed", e.getMessage(), null, true);
                markDeepAnalyzeLlmFailed(viral, String.valueOf(result.get("error")), result);
                recordDeepAnalyzeMetric(false);
            }
        } else {
            result.put("error", "LLM 或文本模型未配置");
            progressTouch(viral, flush, "llm_single", "llm_single", "failed", "LLM 或文本模型未配置", null, true);
            markDeepAnalyzeLlmFailed(viral, "LLM 或文本模型未配置", result);
            recordDeepAnalyzeMetric(false);
        }
    }

    private void persistDeepAnalysisSuccess(SvViralVideo viral, String mergedJson, Map<String, Object> stepSummary) {
        viral.setDeepAnalysisResult(mergedJson);
        viral.setAnalysisResult(mergedJson);
        applyParsedFields(viral, mergedJson);
        viral.setDeepAnalyzedAt(new Timestamp(System.currentTimeMillis()));
        viral.setDeepAnalyzeStatus("completed");
        viral.setDeepAnalyzeError(null);
        viral.setDeepAnalyzeFinishedAt(new Timestamp(System.currentTimeMillis()));
        progressFinalizeSuccess(viral);
        if (stepSummary != null && !stepSummary.isEmpty()) {
            try {
                viral.setDeepAnalyzeSteps(JSON.writeValueAsString(stepSummary));
            } catch (Exception e) {
                log.debug("deepAnalyzeSteps 序列化跳过: {}", e.getMessage());
            }
        }
        viralVideoRepository.save(viral);

        // 入队知识库：使深度拆解结果可被 RAG 检索
        enqueueToKnowledgeBase(viral, mergedJson);
    }

    private void markDeepAnalyzeLlmFailed(SvViralVideo viral, String err, Map<String, Object> result) {
        viral.setDeepAnalyzeStatus("failed");
        viral.setDeepAnalyzeError(err);
        viral.setDeepAnalyzeFinishedAt(new Timestamp(System.currentTimeMillis()));
        progressSetError(viral, err);
        try {
            viral.setDeepAnalyzeSteps(JSON.writeValueAsString(buildStepSummaryMap(result, false, false)));
        } catch (Exception e) {
            log.debug("deepAnalyzeSteps 序列化跳过: {}", e.getMessage());
        }
        viralVideoRepository.save(viral);
    }

    private Map<String, Object> buildStepSummaryMap(Map<String, Object> result, boolean multiRound, boolean llmOk) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("multiRound", multiRound);
        m.put("llmOk", llmOk);
        m.put("metadataOk", Boolean.TRUE);
        m.put("commentsOk", result.containsKey("commentsSaved"));
        return m;
    }

    private void recordDeepAnalyzeMetric(boolean success) {
        if (meterRegistry == null) {
            return;
        }
        if (success) {
            meterRegistry.counter("viral.deep_analyze.completed").increment();
        } else {
            meterRegistry.counter("viral.deep_analyze.failed").increment();
        }
    }

    private List<Integer> indicesForVisionCalls(List<SceneDetectionService.SceneSegment> scenes) {
        int n = scenes.size();
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            all.add(i);
        }
        if (multiRoundMaxVisionScenes <= 0 || n <= multiRoundMaxVisionScenes) {
            return all;
        }
        all.sort(Comparator.comparingDouble((Integer i) -> {
            var s = scenes.get(i);
            return s.endTimeSec() - s.startTimeSec();
        }).reversed());
        List<Integer> top = new ArrayList<>(all.subList(0, multiRoundMaxVisionScenes));
        top.sort(Integer::compareTo);
        return top;
    }

    /** 包可见：单测解析 Round1 行 */
    static List<String> parseRound1VisionBySceneIndex(String round1, int sceneCount) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < sceneCount; i++) {
            out.add("");
        }
        if (round1 == null || round1.isBlank() || sceneCount <= 0) {
            return out;
        }
        for (String line : round1.split("\n")) {
            Matcher m = ROUND1_SCENE_LINE.matcher(line.trim());
            if (m.matches()) {
                int idx = Integer.parseInt(m.group(1)) - 1;
                if (idx >= 0 && idx < sceneCount) {
                    out.set(idx, m.group(4).trim());
                }
            }
        }
        return out;
    }

    private String buildCommentSnippetForRound3(Long viralVideoId) {
        if (svCommentRepository == null || viralVideoId == null) {
            return "无";
        }
        try {
            var page = svCommentRepository.findByVideoIdAndVideoSourceAndDeleted(
                    viralVideoId, "viral", 0,
                    PageRequest.of(0, Math.max(1, deepRound3CommentSamples), Sort.by(Sort.Direction.DESC, "commentTime")));
            if (page.isEmpty()) {
                return "无";
            }
            StringBuilder sb = new StringBuilder();
            for (SvComment c : page) {
                if (c.getContent() == null) {
                    continue;
                }
                String t = c.getContent().trim().replace("\n", " ");
                if (t.length() > 120) {
                    t = t.substring(0, 120) + "…";
                }
                if (!t.isEmpty()) {
                    sb.append("- ").append(t).append("\n");
                }
            }
            return sb.isEmpty() ? "无" : sb.toString().trim();
        } catch (Exception e) {
            log.debug("[深度分析] 评论抽样跳过: {}", e.getMessage());
            return "无";
        }
    }

    private static String resolveEvidenceLevel(String transcript, String sceneDescriptions, boolean hadLocalVideoFile) {
        if (!hadLocalVideoFile) {
            return "inferred";
        }
        if (transcript != null && (transcript.contains("推演") || transcript.contains("未启用")
                || transcript.contains("识别失败") || transcript.contains("下载失败") || transcript.contains("跳过本机"))) {
            return "inferred";
        }
        if (sceneDescriptions != null && (sceneDescriptions.contains("无真实画面")
                || sceneDescriptions.contains("无本机抽帧")
                || sceneDescriptions.contains("无视频文件")
                || sceneDescriptions.startsWith("封面分析:")
                || sceneDescriptions.contains("封面推演"))) {
            return "inferred";
        }
        return "empirical";
    }

    private static String ensureEvidenceLevelOnJson(String rawJson, String transcript, String sceneDescriptions,
                                                    String commentSnippet, String evidenceLevel) {
        try {
            JsonNode root = JSON.readTree(rawJson);
            if (!(root instanceof ObjectNode obj)) {
                return rawJson;
            }
            obj.put("evidenceLevel", evidenceLevel != null ? evidenceLevel : "inferred");
            obj.set("evidenceDetails", JSON.valueToTree(buildEvidenceDetails(transcript, sceneDescriptions, commentSnippet, evidenceLevel)));
            return JSON.writeValueAsString(obj);
        } catch (Exception e) {
            return rawJson;
        }
    }

    private static Map<String, Object> buildEvidenceDetails(String transcript, String sceneDescriptions,
                                                            String commentSnippet, String evidenceLevel) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("overallLevel", evidenceLevel != null ? evidenceLevel : "inferred");
        details.put("transcriptLevel", inferTranscriptEvidence(transcript));
        details.put("sceneLevel", inferSceneEvidence(sceneDescriptions));
        details.put("commentLevel", inferCommentEvidence(commentSnippet));
        details.put("hasCommentSamples", commentSnippet != null && !"无".equals(commentSnippet.trim()));
        return details;
    }

    private static String inferTranscriptEvidence(String transcript) {
        if (transcript == null || transcript.isBlank()) {
            return "missing";
        }
        if (transcript.contains("推演") || transcript.contains("未启用")
                || transcript.contains("识别失败") || transcript.contains("下载失败")
                || transcript.contains("无真实口播")
                || transcript.contains("无视频文件")
                || transcript.contains("仅有标题和元数据")) {
            return "inferred";
        }
        return "empirical";
    }

    private static String inferSceneEvidence(String sceneDescriptions) {
        if (sceneDescriptions == null || sceneDescriptions.isBlank()) {
            return "missing";
        }
        if (sceneDescriptions.contains("无真实画面")
                || sceneDescriptions.contains("无本机抽帧")
                || sceneDescriptions.contains("无视频文件")
                || sceneDescriptions.startsWith("封面分析:")
                || sceneDescriptions.contains("封面推演")
                || sceneDescriptions.contains("推演分镜")
                || sceneDescriptions.contains("视觉分析无响应")
                || sceneDescriptions.contains("分析失败")
                || sceneDescriptions.contains("跳过单独识图")
                || sceneDescriptions.contains("multi-round-max-vision-scenes")) {
            return "inferred";
        }
        return "empirical";
    }

    private static String inferCommentEvidence(String commentSnippet) {
        if (commentSnippet == null || commentSnippet.isBlank() || "无".equals(commentSnippet.trim())) {
            return "missing";
        }
        return "empirical";
    }

    private static boolean isDouyinShareUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String u = url.toLowerCase();
        return u.contains("douyin.com") || u.contains("douyin.cn");
    }

    void applyParsedFields(SvViralVideo viral, String json) {
        try {
            JsonNode root = JSON.readTree(json);
            JsonNode evidenceDetails = root.path("evidenceDetails");
            String transcriptLevel = evidenceDetails.path("transcriptLevel").asText("");
            String sceneLevel = evidenceDetails.path("sceneLevel").asText("");
            JsonNode rvt = root.get("remakeVariableTable");
            if (rvt != null && !rvt.isNull()) {
                viral.setRemakeVariableTable(rvt.toString());
            }
            JsonNode tr = root.get("transcript");
            if (tr != null && !tr.isNull()) {
                String fullText = tr.path("fullText").asText(null);
                if (fullText != null && !fullText.isBlank()) {
                    viral.setTranscript(normalizePersistedTranscript(fullText, transcriptLevel));
                }
            }
            JsonNode scenes = root.get("scenes");
            if (scenes != null && scenes.isArray() && !scenes.isEmpty()) {
                boolean multiRound = root.path("_multiRound").asBoolean(false);
                if (multiRound && scenesArrayHasNoVisualText(scenes)) {
                    return;
                }
                List<String> lines = new ArrayList<>();
                for (JsonNode s : scenes) {
                    String time = s.path("time").asText("");
                    String env = s.path("environment").asText("");
                    String person = s.path("person").asText("");
                    String props = s.path("props").asText("");
                    String camera = s.path("camera").asText("");
                    String mood = s.path("mood").asText("");
                    lines.add(String.format("[%s] %s | 人物:%s 道具:%s 镜头:%s 情绪:%s",
                            time, env, person, props, camera, mood));
                }
                viral.setSceneDescriptions(normalizePersistedScenes(String.join("\n", lines), sceneLevel));
            }
        } catch (Exception e) {
            log.debug("解析深度分析 JSON 字段跳过: {}", e.getMessage());
        }
    }

    private static String normalizePersistedTranscript(String fullText, String transcriptLevel) {
        if (!StringUtils.hasText(fullText)) {
            return fullText;
        }
        if ("inferred".equalsIgnoreCase(transcriptLevel)) {
            return "【推演口播】" + fullText;
        }
        return fullText;
    }

    private static String normalizePersistedScenes(String sceneText, String sceneLevel) {
        if (!StringUtils.hasText(sceneText)) {
            return sceneText;
        }
        if ("inferred".equalsIgnoreCase(sceneLevel)) {
            return "【推演场景】\n" + sceneText;
        }
        return sceneText;
    }

    private static boolean scenesArrayHasNoVisualText(JsonNode scenes) {
        for (JsonNode s : scenes) {
            if (StringUtils.hasText(s.path("environment").asText("").trim())) {
                return false;
            }
            if (StringUtils.hasText(s.path("person").asText("").trim())) {
                return false;
            }
            if (StringUtils.hasText(s.path("props").asText("").trim())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 从 LLM 输出中提取 JSON：去掉 markdown 代码块、前后多余文字
     */
    private static String stripJsonFence(String content) {
        if (content == null) {
            return "";
        }
        String t = content.trim();
        // 去掉 markdown 代码块
        if (t.startsWith("```")) {
            int nl = t.indexOf('\n');
            if (nl > 0) {
                t = t.substring(nl + 1);
            }
            int end = t.lastIndexOf("```");
            if (end > 0) {
                t = t.substring(0, end);
            }
            t = t.trim();
        }
        // 如果不是以 { 开头，尝试找到第一个 { 和最后一个 }
        if (!t.isEmpty() && t.charAt(0) != '{') {
            int start = t.indexOf('{');
            int end = t.lastIndexOf('}');
            if (start >= 0 && end > start) {
                t = t.substring(start, end + 1);
            }
        }
        return t.trim();
    }

    private AiModel pickDefaultTextModel() {
        List<AiModel> models = aiModelRepository.findByStatusAndDeleted(1, 0);
        return models.isEmpty() ? null : models.get(0);
    }

    private AiModel pickVisionModel() {
        if (visionModelId > 0) {
            var opt = aiModelRepository.findById(visionModelId);
            if (opt.isPresent()) {
                AiModel m = opt.get();
                if (m.getDeleted() != null && m.getDeleted() == 0
                        && m.getStatus() != null && m.getStatus() == 1) {
                    if (!supportsOpenAiStyleVisionMultimodal(m)) {
                        log.warn("[深度分析] vision-model-id={} 指向的模型不支持 OpenAI 式多模态(image_url)，Round1 将不可用该模型: {}",
                                visionModelId, m.getModelVersion());
                    } else {
                        return m;
                    }
                }
            }
            log.warn("[深度分析] vision-model-id={} 未找到或未启用，回退启发式选择", visionModelId);
        }
        List<AiModel> enabled = aiModelRepository.findByStatusAndDeleted(1, 0);
        // 优先：名称像视觉模型且确支持多模态
        return enabled.stream()
                .filter(ViralDeepAnalyzeExecutor::supportsOpenAiStyleVisionMultimodal)
                .filter(m -> {
                    String v = m.getModelVersion() != null ? m.getModelVersion().toLowerCase() : "";
                    return v.contains("gpt-4") || v.contains("gpt-5") || v.contains("o4-mini")
                            || v.contains("claude") || v.contains("vision") || v.contains("qwen-vl")
                            || v.contains("glm-4v") || v.contains("gemini");
                })
                .findFirst()
                .orElseGet(() -> enabled.stream()
                        .filter(ViralDeepAnalyzeExecutor::supportsOpenAiStyleVisionMultimodal)
                        .findFirst()
                        .orElse(null));
    }

    /**
     * 是否可用 OpenAI 兼容「content 数组含 image_url」的多模态调用。
     * DeepSeek 纯文本 / Reasoner 等会 400：unknown variant {@code image_url}, expected {@code text}。
     */
    private static boolean supportsOpenAiStyleVisionMultimodal(AiModel m) {
        if (m == null) {
            return false;
        }
        String v = m.getModelVersion() != null ? m.getModelVersion().toLowerCase() : "";
        String p = m.getModelProvider() != null ? m.getModelProvider().toLowerCase() : "";
        if (p.contains("deepseek") && !v.contains("vl")) {
            return false;
        }
        if (v.contains("deepseek-reasoner") || "deepseek-chat".equals(v)) {
            return false;
        }
        return v.contains("gpt-") || v.contains("claude") || v.contains("gemini")
                || v.contains("vision") || v.contains("qwen-vl") || v.contains("glm-4v")
                || v.contains("llava") || v.contains("4o");
    }

    // ─── 知识库入队 ──────────────────────────────────────

    /**
     * 将深度拆解结果入队到知识库，使其可被 RAG 检索。
     * <p>
     * 入队内容包括：标题、口播文案、爆款假设、二创变量表等。
     */
    private void enqueueToKnowledgeBase(SvViralVideo viral, String deepAnalysisJson) {
        if (evolutionService == null) {
            log.debug("[深度分析] EvolutionService 未注入，跳过知识库入队 viralId={}", viral.getId());
            return;
        }

        try {
            String content = buildKnowledgeContent(viral, deepAnalysisJson);
            if (content == null || content.isBlank()) {
                log.debug("[深度分析] 知识库内容为空，跳过入队 viralId={}", viral.getId());
                return;
            }

            Long targetKbId = resolveSystemIndexTargetKbId();
            if (targetKbId == null) {
                log.warn("[深度分析] 系统知识库归属 user_id={} 下无可用知识库，跳过入队（IndexQueueConsumer 要求 targetKbId） viralId={}, videoOwnerId={}",
                        viralDeepIndexKbOwnerId, viral.getId(), viral.getOwnerId());
                return;
            }

            // 入队到知识库（优先级 5）；必须与 ai_index_queue 消费端一致传入 targetKbId
            long queueId = evolutionService.enqueueIndex(
                    "viral_video",
                    viral.getId(),
                    content,
                    5,
                    targetKbId);
            writeViralPatternKnowledge(viral, content);

            log.info("[深度分析] 已入队知识库: viralId={}, queueId={}, targetKbId={}, contentLength={}",
                    viral.getId(), queueId, targetKbId, content.length());
        } catch (Exception e) {
            log.warn("[深度分析] 知识库入队失败 viralId={}: {}", viral.getId(), e.getMessage());
        }
    }

    private void writeViralPatternKnowledge(SvViralVideo viral, String content) {
        if (operationalStrategyKnowledgeService == null || viralPatternKnowledgeFormatter == null
                || viral == null || viral.getOwnerId() == null
                || viral.getOwnerId() <= 0 || !StringUtils.hasText(content)) {
            return;
        }
        try {
            ViralPatternKnowledgeFormatter.PatternDocument doc = viralPatternKnowledgeFormatter.build(
                    viral, "viral_deep_analyze", content);
            operationalStrategyKnowledgeService.writeViralPatternKnowledge(
                    viral.getOwnerId(),
                    doc.title(),
                    doc.content(),
                    doc.metadata()
            );
        } catch (Exception e) {
            log.debug("[深度分析] 爆款模式知识库沉淀跳过 viralId={}, err={}", viral.getId(), e.getMessage());
        }
    }

    /**
     * 系统级索引入库目标：使用 {@link #viralDeepIndexKbOwnerId}（默认与 app.ai.kb.shared-owner-id 相同），
     * 不按爆款视频的 ownerId 绑定个人知识库。
     */
    private Long resolveSystemIndexTargetKbId() {
        if (knowledgeBaseRepository == null) {
            return null;
        }
        long kbUserId = viralDeepIndexKbOwnerId;
        if (StringUtils.hasText(viralDeepIndexKbName) && knowledgeBaseService != null) {
            Long byName = knowledgeBaseService.resolveKbIdByName(kbUserId, viralDeepIndexKbName.trim());
            if (byName != null) {
                return byName;
            }
        }
        List<AiKnowledgeBase> kbs = knowledgeBaseRepository.findByUserIdAndDeletedOrderByCreateTimeDesc(kbUserId, 0);
        for (AiKnowledgeBase kb : kbs) {
            if ("huashu".equals(kb.getKbName()) || "huashu".equals(kb.getKbType())) {
                return kb.getId();
            }
        }
        for (AiKnowledgeBase kb : kbs) {
            if ("zhishi".equals(kb.getKbName()) || "zhishi".equals(kb.getKbType())) {
                return kb.getId();
            }
        }
        return kbs.isEmpty() ? null : kbs.get(0).getId();
    }

    /**
     * 构建知识库内容：标题 + 口播 + 拆解结论。
     */
    private String buildKnowledgeContent(SvViralVideo viral, String deepAnalysisJson) {
        StringBuilder sb = new StringBuilder();

        // 标题
        if (viral.getTitle() != null && !viral.getTitle().isBlank()) {
            sb.append("【爆款视频】").append(viral.getTitle()).append("\n\n");
        }

        // 作者信息
        if (viral.getAuthorName() != null && !viral.getAuthorName().isBlank()) {
            sb.append("作者：").append(viral.getAuthorName()).append("\n");
        }

        // 互动数据
        if (viral.getViewCount() != null || viral.getLikeCount() != null || viral.getShareCount() != null) {
            sb.append("数据：");
            if (viral.getViewCount() != null) {
                sb.append("播放 ").append(viral.getViewCount()).append(" ");
            }
            if (viral.getLikeCount() != null) {
                sb.append("点赞 ").append(viral.getLikeCount()).append(" ");
            }
            if (viral.getShareCount() != null) {
                sb.append("分享 ").append(viral.getShareCount());
            }
            sb.append("\n\n");
        }

        // 口播文案（ASR 转写或推演）
        if (viral.getTranscript() != null && !viral.getTranscript().isBlank()) {
            String transcript = viral.getTranscript();
            // 过滤掉技术标记
            if (!transcript.startsWith("（") && !transcript.contains("无真实口播")) {
                sb.append("【口播文案】\n").append(transcript).append("\n\n");
            }
        }

        // 深度拆解结论
        if (deepAnalysisJson != null && !deepAnalysisJson.isBlank()) {
            try {
                JsonNode json = JSON.readTree(deepAnalysisJson);

                // 爆款假设
                if (json.has("viralHypotheses")) {
                    JsonNode hypotheses = json.get("viralHypotheses");
                    sb.append("【爆款假设】\n");
                    if (hypotheses.has("emotionTrigger")) {
                        sb.append("情绪触发：").append(hypotheses.get("emotionTrigger").asText()).append("\n");
                    }
                    if (hypotheses.has("infoDensity")) {
                        sb.append("信息密度：").append(hypotheses.get("infoDensity").asText()).append("\n");
                    }
                    if (hypotheses.has("rhythmPattern")) {
                        sb.append("节奏特征：").append(hypotheses.get("rhythmPattern").asText()).append("\n");
                    }
                    if (hypotheses.has("visualContrast")) {
                        sb.append("视觉对比：").append(hypotheses.get("visualContrast").asText()).append("\n");
                    }
                    if (hypotheses.has("replicability")) {
                        sb.append("可复制性：").append(hypotheses.get("replicability").asText()).append("\n");
                    }
                    sb.append("\n");
                }

                // 二创变量表
                if (json.has("remakeVariableTable")) {
                    JsonNode remakeTable = json.get("remakeVariableTable");
                    sb.append("【二创变量表】\n");

                    if (remakeTable.has("mustKeep")) {
                        JsonNode mustKeep = remakeTable.get("mustKeep");
                        if (mustKeep.isArray() && mustKeep.size() > 0) {
                            sb.append("必须保留：");
                            for (JsonNode item : mustKeep) {
                                sb.append(item.asText()).append("、");
                            }
                            sb.setLength(sb.length() - 1); // 删除最后的顿号
                            sb.append("\n");
                        }
                    }

                    if (remakeTable.has("replaceable")) {
                        JsonNode replaceable = remakeTable.get("replaceable");
                        if (replaceable.isArray() && replaceable.size() > 0) {
                            sb.append("可替换元素：\n");
                            for (JsonNode item : replaceable) {
                                if (item.has("variable") && item.has("suggestion")) {
                                    sb.append("  - ").append(item.get("variable").asText())
                                            .append("：").append(item.get("suggestion").asText()).append("\n");
                                }
                            }
                        }
                    }
                    sb.append("\n");
                }

                // 结构分析
                if (json.has("structure")) {
                    JsonNode structure = json.get("structure");
                    sb.append("【结构分析】\n");
                    if (structure.has("hook") && structure.get("hook").has("type")) {
                        sb.append("钩子类型：").append(structure.get("hook").get("type").asText()).append("\n");
                    }
                    if (structure.has("cta") && structure.get("cta").has("type")) {
                        sb.append("转化类型：").append(structure.get("cta").get("type").asText()).append("\n");
                    }
                }

            } catch (Exception e) {
                log.debug("[深度分析] 解析 deepAnalysisJson 失败 viralId={}: {}", viral.getId(), e.getMessage());
            }
        }

        return sb.toString().trim();
    }
}
